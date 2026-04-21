import argparse
import io
import json
import os
import shutil
import time
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Dict, List, Optional, Tuple

try:
    from PIL import Image  # type: ignore
except Exception:
    Image = None

from catalog_common import (
    ANDROID_DRAWABLE_DIR,
    DEFAULT_POSTER_ASSET,
    LEGACY_POSTER_MAP,
    MISSING_POSTERS_LOG,
    OUTPUT_DIR,
    POSTER_MANIFEST_PATH,
    POSTERS_DIR,
    TMDB_CACHE_PATH,
    TMDB_CONFIG_LOCAL_PATH,
    ensure_directories,
    extract_unique_movies,
    load_legacy_rows,
    load_movie_overrides,
    normalize_key,
    normalize_punctuation,
    slugify,
    write_json,
)

TMDB_API_BASE = "https://api.themoviedb.org/3"
TMDB_CONFIGURATION_CACHE_PATH = OUTPUT_DIR / "tmdb_configuration.json"
PREFERRED_POSTER_SIZES = ["w500", "w780", "w342", "original"]
MAX_SEARCH_RESULTS = 10
TRADITIONAL_CHINESE_LANGUAGES = [
    ("zh-HK", "tmdb_zh_hk"),
    ("zh-TW", "tmdb_zh_tw"),
]


def clean_text(value: object) -> Optional[str]:
    text = normalize_punctuation(str(value or "")).strip()
    return text or None


def contains_cjk(text: Optional[str]) -> bool:
    if not text:
        return False
    for ch in text:
        if "\u3400" <= ch <= "\u9fff" or "\uf900" <= ch <= "\ufaff":
            return True
    return False


def is_traditional_title_candidate(title: Optional[str], title_en: Optional[str]) -> bool:
    title = clean_text(title)
    if not title or not contains_cjk(title):
        return False
    if title_en and normalize_key(title) == normalize_key(title_en):
        return False
    return True


def select_localized_text(candidates: List[Tuple[Optional[str], str]], require_cjk: bool = False) -> Tuple[Optional[str], Optional[str]]:
    for value, source in candidates:
        text = clean_text(value)
        if not text:
            continue
        if require_cjk and not contains_cjk(text):
            continue
        return text, source
    return None, None


def get_translation_data(translations_payload: Dict[str, object], region: str) -> Dict[str, object]:
    translations = translations_payload.get("translations", [])
    if not isinstance(translations, list):
        return {}
    for item in translations:
        if not isinstance(item, dict):
            continue
        if item.get("iso_639_1") == "zh" and item.get("iso_3166_1") == region:
            data = item.get("data", {})
            return data if isinstance(data, dict) else {}
    return {}


def read_json(path: Path, default):
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8-sig"))


def load_tmdb_credentials() -> Dict[str, Optional[str]]:
    config = read_json(TMDB_CONFIG_LOCAL_PATH, {})
    bearer_token = (
        os.environ.get("TMDB_BEARER_TOKEN")
        or os.environ.get("TMDB_READ_ACCESS_TOKEN")
        or config.get("bearer_token")
        or config.get("read_access_token")
    )
    api_key = os.environ.get("TMDB_API_KEY") or config.get("api_key")
    return {"bearer_token": bearer_token, "api_key": api_key}


def build_tmdb_request(path: str, params: Dict[str, object]) -> urllib.request.Request:
    credentials = load_tmdb_credentials()
    query = {key: value for key, value in params.items() if value not in (None, "")}
    if credentials["bearer_token"]:
        query_string = urllib.parse.urlencode(query)
        url = f"{TMDB_API_BASE}{path}"
        if query_string:
            url += f"?{query_string}"
        request = urllib.request.Request(url)
        request.add_header("Authorization", f"Bearer {credentials['bearer_token']}")
        request.add_header("accept", "application/json")
        return request
    if credentials["api_key"]:
        query["api_key"] = credentials["api_key"]
        request = urllib.request.Request(f"{TMDB_API_BASE}{path}?{urllib.parse.urlencode(query)}")
        request.add_header("accept", "application/json")
        return request
    raise SystemExit("TMDB credentials are required. Set TMDB_API_KEY / TMDB_BEARER_TOKEN or add backend/config/tmdb_config.local.json.")


def api_get_json(path: str, **params):
    request = build_tmdb_request(path, params)
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def api_get_json_optional(path: str, **params):
    try:
        return api_get_json(path, **params)
    except Exception:
        return {}


def load_tmdb_configuration(force_refresh: bool = False) -> Dict[str, object]:
    if not force_refresh and TMDB_CONFIGURATION_CACHE_PATH.exists():
        cached = read_json(TMDB_CONFIGURATION_CACHE_PATH, {})
        if cached:
            return cached
    configuration = api_get_json("/configuration")
    write_json(TMDB_CONFIGURATION_CACHE_PATH, configuration)
    return configuration


def select_poster_size(configuration: Dict[str, object]) -> str:
    images = configuration.get("images", {}) if isinstance(configuration, dict) else {}
    poster_sizes = images.get("poster_sizes", []) if isinstance(images, dict) else []
    for size in PREFERRED_POSTER_SIZES:
        if size in poster_sizes:
            return size
    if poster_sizes:
        non_original = [size for size in poster_sizes if size != "original"]
        return non_original[-1] if non_original else poster_sizes[0]
    return "w500"


def build_poster_url(configuration: Dict[str, object], poster_path: str) -> str:
    images = configuration.get("images", {}) if isinstance(configuration, dict) else {}
    secure_base_url = images.get("secure_base_url") or images.get("base_url") or "https://image.tmdb.org/t/p/"
    return f"{secure_base_url}{select_poster_size(configuration)}{poster_path}"


def score_result(item: Dict[str, object], title_en: str, title_zh: Optional[str], year_hint: Optional[int]) -> int:
    score = 0
    title_targets = [
        (normalize_key(title_en), 120, 45),
        (normalize_key(title_zh), 150, 60) if title_zh else ("", 0, 0),
    ]
    candidate_values = [normalize_key(str(item.get("title") or "")), normalize_key(str(item.get("original_title") or ""))]
    for target, exact_score, partial_score in title_targets:
        if not target:
            continue
        for candidate in candidate_values:
            if not candidate:
                continue
            if candidate == target:
                score += exact_score
            elif candidate in target or target in candidate:
                score += partial_score
    release_date = str(item.get("release_date") or "")
    if year_hint and release_date[:4].isdigit():
        diff = abs(int(release_date[:4]) - int(year_hint))
        score += max(0, 25 - diff * 5)
    if item.get("poster_path"):
        score += 8
    if item.get("vote_count"):
        score += min(int(item.get("vote_count") or 0), 20)
    return score


def pick_best_result(results: List[Dict[str, object]], title_en: str, title_zh: Optional[str], year_hint: Optional[int]) -> Optional[Dict[str, object]]:
    best = None
    best_score = -1
    for item in results:
        score = score_result(item, title_en, title_zh, year_hint)
        if score > best_score:
            best = item
            best_score = score
    return best if best_score >= 40 else None


def resolve_override_tmdb_id(entry: Dict[str, object], overrides: Dict[str, Dict[str, object]]) -> Optional[int]:
    tmdb_ids = overrides.get("tmdb_ids", {})
    title_en = str(entry.get("titleEn") or "")
    title_zh = str(entry.get("titleZh") or "")
    for candidate in [title_en, normalize_punctuation(title_en), title_zh]:
        if candidate and candidate in tmdb_ids:
            return int(tmdb_ids[candidate])
    return None


def search_movie(entry: Dict[str, object], overrides: Dict[str, Dict[str, object]]) -> Optional[int]:
    override_id = resolve_override_tmdb_id(entry, overrides)
    if override_id:
        return override_id

    title_en = str(entry["titleEn"])
    title_zh = normalize_punctuation(str(entry.get("titleZh") or "")).strip() or None
    year_hint = entry.get("yearHint")

    candidates: List[Dict[str, object]] = []
    seen_ids = set()
    search_queries: List[Tuple[str, str]] = []
    if title_zh:
        search_queries.extend((title_zh, language) for language, _ in TRADITIONAL_CHINESE_LANGUAGES)
    search_queries.append((title_en, "en-US"))

    for query, language in search_queries:
        payload = api_get_json("/search/movie", query=query, language=language, include_adult="false", year=year_hint or "")
        for result in payload.get("results", [])[:MAX_SEARCH_RESULTS]:
            movie_id = result.get("id")
            if not movie_id or movie_id in seen_ids:
                continue
            seen_ids.add(movie_id)
            candidates.append(result)

    best = pick_best_result(candidates, title_en, title_zh, year_hint)
    return int(best["id"]) if best and best.get("id") else None


def fetch_director(movie_id: int) -> Optional[str]:
    credits = api_get_json(f"/movie/{movie_id}/credits", language="en-US")
    for crew in credits.get("crew", []):
        if crew.get("job") == "Director":
            return crew.get("name")
    return None


def fetch_movie_payload(movie_id: int, fallback_title_zh: Optional[str] = None, fallback_title_zh_source: str = "source_title_zh") -> Dict[str, object]:
    details_en = api_get_json(f"/movie/{movie_id}", language="en-US")
    details_zh_hk = api_get_json_optional(f"/movie/{movie_id}", language="zh-HK")
    details_zh_tw = api_get_json_optional(f"/movie/{movie_id}", language="zh-TW")
    translations_payload = api_get_json_optional(f"/movie/{movie_id}/translations")
    translation_hk = get_translation_data(translations_payload, "HK")
    translation_tw = get_translation_data(translations_payload, "TW")
    director = fetch_director(movie_id)
    release_date = str(details_en.get("release_date") or details_zh_hk.get("release_date") or details_zh_tw.get("release_date") or "")
    year = int(release_date[:4]) if release_date[:4].isdigit() else None
    genres = [genre.get("name") for genre in details_en.get("genres", []) if genre.get("name")]
    title_en = clean_text(details_en.get("title")) or clean_text(details_en.get("original_title")) or ""
    title_zh_candidates = [
        (details_zh_hk.get("title"), "tmdb_zh_hk_details"),
        (translation_hk.get("title"), "tmdb_zh_hk_translations"),
        (details_zh_tw.get("title"), "tmdb_zh_tw_details"),
        (translation_tw.get("title"), "tmdb_zh_tw_translations"),
    ]
    if is_traditional_title_candidate(fallback_title_zh, title_en):
        title_zh_candidates.append((fallback_title_zh, fallback_title_zh_source))
    title_zh, title_zh_source = select_localized_text(
        [(value, source) for value, source in title_zh_candidates if is_traditional_title_candidate(value, title_en)]
    )
    overview_zh, overview_zh_source = select_localized_text([
        (details_zh_hk.get("overview"), "tmdb_zh_hk_details"),
        (translation_hk.get("overview"), "tmdb_zh_hk_translations"),
        (details_zh_tw.get("overview"), "tmdb_zh_tw_details"),
        (translation_tw.get("overview"), "tmdb_zh_tw_translations"),
    ], require_cjk=True)
    poster_slug = f"poster_{slugify(title_en or details_en.get('original_title') or title_zh)}"
    return {
        "tmdbId": movie_id,
        "titleEn": title_en,
        "titleZh": title_zh,
        "titleZhSource": title_zh_source,
        "year": year,
        "director": director,
        "genreRaw": ", ".join(genres) if genres else None,
        "overviewEn": clean_text(details_en.get("overview")),
        "overviewZh": overview_zh,
        "overviewZhSource": overview_zh_source,
        "posterPath": details_en.get("poster_path") or details_zh_hk.get("poster_path") or details_zh_tw.get("poster_path"),
        "posterAsset": poster_slug,
        "posterFileName": None,
    }


def infer_binary_suffix(poster_path: str, content_type: str) -> str:
    suffix = Path(urllib.parse.urlparse(poster_path).path).suffix.lower()
    if suffix:
        return suffix
    if "png" in content_type:
        return ".png"
    if "webp" in content_type:
        return ".webp"
    return ".jpg"


def remove_existing_poster_variants(poster_asset: str) -> None:
    for directory in [POSTERS_DIR, ANDROID_DRAWABLE_DIR]:
        for candidate in directory.glob(f"{poster_asset}.*"):
            if candidate.is_file():
                candidate.unlink()


def download_poster(poster_path: str, poster_asset: str, configuration: Dict[str, object]) -> str:
    url = build_poster_url(configuration, poster_path)
    request = urllib.request.Request(url)
    request.add_header("accept", "image/*")
    with urllib.request.urlopen(request, timeout=30) as response:
        raw = response.read()
        content_type = response.headers.get("Content-Type", "")

    remove_existing_poster_variants(poster_asset)

    if Image is not None:
        destination = POSTERS_DIR / f"{poster_asset}.webp"
        image = Image.open(io.BytesIO(raw)).convert("RGB")
        image.save(destination, format="WEBP", quality=88, method=6)
        return destination.name

    suffix = infer_binary_suffix(poster_path, content_type)
    destination = POSTERS_DIR / f"{poster_asset}{suffix}"
    destination.write_bytes(raw)
    return destination.name


def try_reuse_existing_poster(existing: Optional[Dict[str, object]], poster_asset: str) -> Optional[str]:
    file_name = ""
    if existing and existing.get("posterAsset") == poster_asset:
        file_name = str(existing.get("posterFileName") or "")
    if file_name and (POSTERS_DIR / file_name).exists():
        source = POSTERS_DIR / file_name
    else:
        matches = sorted(candidate for candidate in POSTERS_DIR.glob(f"{poster_asset}.*") if candidate.is_file())
        if not matches:
            return None
        source = matches[0]
        file_name = source.name
    destination = ANDROID_DRAWABLE_DIR / file_name
    if not destination.exists():
        shutil.copy2(source, destination)
    return file_name


def needs_localized_refresh(existing: Optional[Dict[str, object]]) -> bool:
    if not existing or existing.get("matchStatus") != "matched":
        return False
    if "titleZhSource" not in existing:
        return True
    title_zh = clean_text(existing.get("titleZh"))
    if title_zh and not is_traditional_title_candidate(title_zh, clean_text(existing.get("titleEn"))):
        return True
    return False


def save_state(metadata_cache, poster_manifest, missing_lines, configuration: Dict[str, object]) -> None:
    for legacy_title, legacy_asset in LEGACY_POSTER_MAP.items():
        poster_manifest.setdefault(legacy_title, {"posterAsset": legacy_asset, "fileName": None})
    write_json(TMDB_CACHE_PATH, metadata_cache)
    write_json(POSTER_MANIFEST_PATH, poster_manifest)
    write_json(TMDB_CONFIGURATION_CACHE_PATH, configuration)
    unique_missing = list(dict.fromkeys(line for line in missing_lines if line))
    MISSING_POSTERS_LOG.write_text("\n".join(unique_missing) + ("\n" if unique_missing else ""), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--no-resume", action="store_true")
    parser.add_argument("--featured-only", action="store_true")
    parser.add_argument("--refresh-config", action="store_true")
    args = parser.parse_args()

    ensure_directories()
    configuration = load_tmdb_configuration(force_refresh=args.refresh_config)
    overrides = load_movie_overrides()
    unique_movies = extract_unique_movies()
    if args.featured_only:
        title_aliases = overrides.get("title_aliases", {})
        featured_keys = {
            normalize_key(title_aliases.get(normalize_punctuation(str(row.get("movieTitle", "")).strip()), normalize_punctuation(str(row.get("movieTitle", "")).strip())))
            for row in load_legacy_rows()
        }
        unique_movies = [entry for entry in unique_movies if normalize_key(str(entry["titleEn"])) in featured_keys]

    metadata_cache = {} if args.no_resume else read_json(TMDB_CACHE_PATH, {})
    poster_manifest = {} if args.no_resume else read_json(POSTER_MANIFEST_PATH, {})
    missing_lines = []

    processed = 0
    for entry in unique_movies:
        title = str(entry["titleEn"])
        existing = metadata_cache.get(title)
        override_id = resolve_override_tmdb_id(entry, overrides)
        if existing and existing.get("matchStatus") == "matched" and not needs_localized_refresh(existing) and not (override_id and existing.get("tmdbId") != override_id):
            continue
        if args.limit and processed >= args.limit:
            break

        try:
            movie_id = search_movie(entry, overrides)
            fallback_title_zh = clean_text(entry.get("titleZh"))
            fallback_title_zh_source = f"{entry.get('source') or 'source'}_title_zh"
            if not movie_id:
                metadata_cache[title] = {
                    "matchStatus": "missing",
                    "titleEn": title,
                    "titleZh": fallback_title_zh if is_traditional_title_candidate(fallback_title_zh, title) else None,
                    "titleZhSource": fallback_title_zh_source if is_traditional_title_candidate(fallback_title_zh, title) else None,
                    "year": entry.get("yearHint"),
                    "director": None,
                    "genreRaw": None,
                    "posterAsset": DEFAULT_POSTER_ASSET,
                    "posterFileName": None,
                }
                missing_lines.append(f"NO_MATCH\t{title}")
            else:
                payload = fetch_movie_payload(movie_id, fallback_title_zh, fallback_title_zh_source)
                payload["matchStatus"] = "matched"
                if payload.get("posterPath"):
                    file_name = try_reuse_existing_poster(existing, str(payload["posterAsset"]))
                    if not file_name:
                        file_name = download_poster(str(payload["posterPath"]), str(payload["posterAsset"]), configuration)
                        shutil.copy2(POSTERS_DIR / file_name, ANDROID_DRAWABLE_DIR / file_name)
                    payload["posterFileName"] = file_name
                    poster_manifest[payload["titleEn"]] = {
                        "posterAsset": payload["posterAsset"],
                        "fileName": file_name,
                    }
                else:
                    payload["posterAsset"] = DEFAULT_POSTER_ASSET
                    payload["posterFileName"] = None
                    missing_lines.append(f"NO_POSTER\t{title}")
                metadata_cache[title] = payload
            processed += 1
            if processed % 5 == 0:
                save_state(metadata_cache, poster_manifest, missing_lines, configuration)
                print(f"Processed {processed} new movies...")
            time.sleep(0.15)
        except Exception as exc:
            fallback_title_zh = clean_text(entry.get("titleZh"))
            fallback_title_zh_source = f"{entry.get('source') or 'source'}_title_zh"
            if existing:
                payload = dict(existing)
                payload["localizationError"] = str(exc)
                if not is_traditional_title_candidate(clean_text(payload.get("titleZh")), clean_text(payload.get("titleEn")) or title):
                    payload["titleZh"] = fallback_title_zh if is_traditional_title_candidate(fallback_title_zh, title) else None
                    payload["titleZhSource"] = fallback_title_zh_source if payload["titleZh"] else None
                metadata_cache[title] = payload
            else:
                metadata_cache[title] = {
                    "matchStatus": "error",
                    "titleEn": title,
                    "titleZh": fallback_title_zh if is_traditional_title_candidate(fallback_title_zh, title) else None,
                    "titleZhSource": fallback_title_zh_source if is_traditional_title_candidate(fallback_title_zh, title) else None,
                    "year": entry.get("yearHint"),
                    "director": None,
                    "genreRaw": None,
                    "posterAsset": DEFAULT_POSTER_ASSET,
                    "posterFileName": None,
                    "error": str(exc),
                }
            missing_lines.append(f"ERROR\t{title}\t{exc}")
            processed += 1
            time.sleep(0.15)

    save_state(metadata_cache, poster_manifest, missing_lines, configuration)
    print(f"TMDB image base: {configuration.get('images', {}).get('secure_base_url', 'https://image.tmdb.org/t/p/')}")
    print(f"TMDB poster size: {select_poster_size(configuration)}")
    print(f"TMDB cache written: {TMDB_CACHE_PATH}")
    print(f"Poster manifest written: {POSTER_MANIFEST_PATH}")
    print(f"Missing poster report: {MISSING_POSTERS_LOG}")
    print(f"Poster files: {len(list(POSTERS_DIR.glob('poster_*.*')))}")
    print(f"New movies processed in this run: {processed}")


if __name__ == "__main__":
    main()
