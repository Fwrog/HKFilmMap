import argparse
import json
import os
import time
import urllib.parse
import urllib.request
from typing import Dict, Iterable, List, Optional, Tuple

from catalog_common import (
    GOOGLE_MAPS_CONFIG_LOCAL_PATH,
    OUTPUT_DIR,
    PLACE_OVERRIDES_PATH,
    ensure_directories,
    normalize_key,
    normalize_punctuation,
    read_json,
    write_json,
)


GOOGLE_GEOCODING_API = "https://maps.googleapis.com/maps/api/geocode/json"
PLACES_JSON_PATH = OUTPUT_DIR / "places.json"
SCENES_JSON_PATH = OUTPUT_DIR / "scenes.json"
MOVIES_JSON_PATH = OUTPUT_DIR / "movies.json"
GEOCODING_REPORT_PATH = OUTPUT_DIR / "place_geocoding_candidates.json"
DEFAULT_PLACE_OVERRIDES = {"title_aliases": {}, "place_aliases": {}, "coordinate_overrides": {}}

HIGH_CONFIDENCE_SCORE = 72
MAX_RESULTS = 4


def load_google_maps_key() -> str:
    env_key = (
        os.environ.get("GOOGLE_MAPS_API_KEY")
        or os.environ.get("GOOGLE_GEOCODING_API_KEY")
        or os.environ.get("MAPS_API_KEY")
    )
    if env_key:
        return env_key

    config = read_json(GOOGLE_MAPS_CONFIG_LOCAL_PATH, {})
    for key_name in ["api_key", "google_maps_api_key", "google_geocoding_api_key"]:
        value = config.get(key_name)
        if value:
            return str(value)

    raise SystemExit(
        "Google Maps API key not found. Set GOOGLE_MAPS_API_KEY or add backend/config/google_maps_config.local.json."
    )


def api_get_json(params: Dict[str, str]) -> Dict[str, object]:
    request = urllib.request.Request(
        f"{GOOGLE_GEOCODING_API}?{urllib.parse.urlencode(params)}",
        headers={"accept": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def build_query_variants(place: Dict[str, object]) -> List[Tuple[str, str]]:
    name_en = normalize_punctuation(str(place.get("nameEn") or "").strip())
    name_zh = normalize_punctuation(str(place.get("nameZh") or "").strip())
    address_en = normalize_punctuation(str(place.get("addressEn") or "").strip())
    address_zh = normalize_punctuation(str(place.get("addressZh") or "").strip())
    district_en = normalize_punctuation(str(place.get("districtEn") or "").strip())
    district_zh = normalize_punctuation(str(place.get("districtZh") or "").strip())

    variants: List[Tuple[str, str]] = []
    seen = set()

    def add(query: str, label: str) -> None:
        normalized = normalize_punctuation(query).strip(" ,")
        if not normalized or normalized in seen:
            return
        seen.add(normalized)
        variants.append((normalized, label))

    if name_en:
        add(f"{name_en}, Hong Kong", "name_en")
        if district_en and district_en.lower() not in name_en.lower():
            add(f"{name_en}, {district_en}, Hong Kong", "name_en_district")
    if address_en and address_en != name_en:
        add(f"{address_en}, Hong Kong", "address_en")
        if district_en and district_en.lower() not in address_en.lower():
            add(f"{address_en}, {district_en}, Hong Kong", "address_en_district")
    if name_zh:
        add(f"{name_zh} 香港", "name_zh")
        if district_zh and district_zh not in name_zh:
            add(f"{name_zh} {district_zh} 香港", "name_zh_district")
    if address_zh and address_zh != name_zh:
        add(f"{address_zh} 香港", "address_zh")
        if district_zh and district_zh not in address_zh:
            add(f"{address_zh} {district_zh} 香港", "address_zh_district")
    return variants


def extract_component(result: Dict[str, object], wanted_types: Iterable[str]) -> Optional[str]:
    wanted = set(wanted_types)
    for component in result.get("address_components", []):
        types = set(component.get("types", []))
        if wanted & types:
            return component.get("long_name") or component.get("short_name")
    return None


def reverse_geocode(lat: float, lng: float, api_key: str) -> Dict[str, object]:
    payload = api_get_json(
        {
            "latlng": f"{lat},{lng}",
            "language": "en",
            "key": api_key,
        }
    )
    results = payload.get("results", [])
    return results[0] if results else {}


def token_overlap_score(query: str, formatted_address: str) -> int:
    query_tokens = set(normalize_key(query).split())
    address_tokens = set(normalize_key(formatted_address).split())
    if not query_tokens or not address_tokens:
        return 0
    overlap = len(query_tokens & address_tokens)
    return min(20, overlap * 4)


def score_candidate(
    place: Dict[str, object],
    query: str,
    result: Dict[str, object],
    reverse_result: Dict[str, object],
) -> int:
    score = 0
    formatted_address = normalize_punctuation(str(result.get("formatted_address") or ""))
    reverse_address = normalize_punctuation(str(reverse_result.get("formatted_address") or ""))
    location_type = str(result.get("geometry", {}).get("location_type") or "")
    result_types = set(result.get("types", []))
    district_en = normalize_punctuation(str(place.get("districtEn") or "")).lower()
    district_zh = normalize_punctuation(str(place.get("districtZh") or "")).lower()

    score += token_overlap_score(query, formatted_address)
    if not result.get("partial_match"):
        score += 18
    if "Hong Kong" in formatted_address or "Hong Kong" in reverse_address:
        score += 14
    if location_type == "ROOFTOP":
        score += 18
    elif location_type == "GEOMETRIC_CENTER":
        score += 12
    elif location_type:
        score += 6

    preferred_types = {"premise", "street_address", "intersection", "route", "establishment", "point_of_interest", "tourist_attraction"}
    if result_types & preferred_types:
        score += 12
    if "plus_code" in result_types:
        score -= 8

    district_candidates = {
        district_en,
        normalize_punctuation(extract_component(result, ["sublocality_level_1", "sublocality", "administrative_area_level_2"]) or "").lower(),
        normalize_punctuation(extract_component(reverse_result, ["sublocality_level_1", "sublocality", "administrative_area_level_2"]) or "").lower(),
    }
    district_candidates.discard("")
    if district_en and district_en in district_candidates:
        score += 12
    if district_zh and district_zh in formatted_address.lower():
        score += 8

    return score


def build_place_movie_lookup() -> Dict[int, List[str]]:
    scenes = read_json(SCENES_JSON_PATH, [])
    movies = read_json(MOVIES_JSON_PATH, [])
    movie_titles = {int(movie["movieId"]): str(movie.get("titleEn") or "") for movie in movies}
    lookup: Dict[int, List[str]] = {}
    for scene in scenes:
        place_id = int(scene["placeId"])
        movie_title = movie_titles.get(int(scene["movieId"]), "")
        lookup.setdefault(place_id, [])
        if movie_title and movie_title not in lookup[place_id]:
            lookup[place_id].append(movie_title)
    return lookup


def find_missing_places(limit: int = 0) -> List[Dict[str, object]]:
    places = read_json(PLACES_JSON_PATH, [])
    missing = [place for place in places if place.get("coordStatus") != "verified"]
    return missing[:limit] if limit > 0 else missing


def load_place_overrides() -> Dict[str, object]:
    return read_json(PLACE_OVERRIDES_PATH, DEFAULT_PLACE_OVERRIDES)


def merge_coordinate_overrides(suggestions: List[Dict[str, object]], min_score: int) -> Tuple[int, Dict[str, object]]:
    overrides = load_place_overrides()
    coordinate_overrides = dict(overrides.get("coordinate_overrides", {}))
    applied = 0
    for suggestion in suggestions:
        best = suggestion.get("bestCandidate") or {}
        if int(best.get("score") or 0) < min_score:
            continue
        coordinate_overrides[suggestion["placeNameEn"]] = {
            "latitude": best["latitude"],
            "longitude": best["longitude"],
            "addressEn": best.get("formattedAddress"),
            "districtEn": best.get("districtEn"),
            "reverseAddressEn": best.get("reverseAddress"),
            "source": "google_geocoding",
            "confidence": best.get("score"),
            "query": best.get("query"),
        }
        applied += 1
    overrides["coordinate_overrides"] = coordinate_overrides
    write_json(PLACE_OVERRIDES_PATH, overrides)
    return applied, overrides


def fetch_candidates_for_place(place: Dict[str, object], api_key: str) -> Dict[str, object]:
    candidates = []
    queries_tried = []

    for query, label in build_query_variants(place):
        queries_tried.append({"query": query, "label": label})
        payload = api_get_json(
            {
                "address": query,
                "components": "country:HK",
                "language": "en",
                "key": api_key,
            }
        )
        for result in payload.get("results", [])[:MAX_RESULTS]:
            geometry = result.get("geometry", {})
            location = geometry.get("location", {})
            if "lat" not in location or "lng" not in location:
                continue
            reverse_result = reverse_geocode(float(location["lat"]), float(location["lng"]), api_key)
            candidate = {
                "query": query,
                "queryLabel": label,
                "formattedAddress": normalize_punctuation(str(result.get("formatted_address") or "")),
                "reverseAddress": normalize_punctuation(str(reverse_result.get("formatted_address") or "")),
                "latitude": float(location["lat"]),
                "longitude": float(location["lng"]),
                "locationType": geometry.get("location_type"),
                "resultTypes": result.get("types", []),
                "districtEn": extract_component(
                    reverse_result or result,
                    ["sublocality_level_1", "sublocality", "administrative_area_level_2"],
                ),
            }
            candidate["score"] = score_candidate(place, query, result, reverse_result)
            candidates.append(candidate)
        time.sleep(0.12)

    candidates.sort(key=lambda item: (-int(item["score"]), item["formattedAddress"]))
    deduped = []
    seen = set()
    for candidate in candidates:
        key = (round(candidate["latitude"], 6), round(candidate["longitude"], 6))
        if key in seen:
            continue
        seen.add(key)
        deduped.append(candidate)

    return {
        "queriesTried": queries_tried,
        "topCandidates": deduped[:5],
        "bestCandidate": deduped[0] if deduped else None,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--min-score", type=int, default=HIGH_CONFIDENCE_SCORE)
    parser.add_argument("--apply-high-confidence", action="store_true")
    args = parser.parse_args()

    ensure_directories()
    api_key = load_google_maps_key()
    movie_lookup = build_place_movie_lookup()
    missing_places = find_missing_places(limit=args.limit)

    suggestions = []
    for index, place in enumerate(missing_places, start=1):
        suggestion = {
            "placeId": int(place["placeId"]),
            "placeNameEn": place.get("nameEn"),
            "placeNameZh": place.get("nameZh"),
            "addressEn": place.get("addressEn"),
            "districtEn": place.get("districtEn"),
            "relatedMovies": movie_lookup.get(int(place["placeId"]), []),
        }
        suggestion.update(fetch_candidates_for_place(place, api_key))
        suggestions.append(suggestion)
        if index % 5 == 0:
            print(f"Processed {index}/{len(missing_places)} places...")

    report = {
        "generatedAt": int(time.time()),
        "inputPlaces": len(missing_places),
        "highConfidenceScore": args.min_score,
        "suggestions": suggestions,
    }
    write_json(GEOCODING_REPORT_PATH, report)

    applied = 0
    if args.apply_high_confidence:
        applied, _ = merge_coordinate_overrides(suggestions, args.min_score)

    high_confidence = sum(
        1 for suggestion in suggestions if (suggestion.get("bestCandidate") or {}).get("score", 0) >= args.min_score
    )
    print(f"Geocoding suggestions: {len(suggestions)}")
    print(f"High-confidence candidates: {high_confidence}")
    print(f"Applied overrides: {applied}")
    print(f"Report: {GEOCODING_REPORT_PATH}")
    if args.apply_high_confidence:
        print(f"Updated overrides: {PLACE_OVERRIDES_PATH}")


if __name__ == "__main__":
    main()
