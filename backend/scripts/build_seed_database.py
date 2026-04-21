import sqlite3
from collections import Counter
from typing import Dict, List, Optional, Tuple

from catalog_common import (
    CLEANING_REPORT_PATH,
    DEFAULT_POSTER_ASSET,
    LEGACY_POSTER_MAP,
    MISSING_POSTERS_LOG,
    OUTPUT_DIR,
    PLACE_OVERRIDES_PATH,
    POSTER_MANIFEST_PATH,
    SEED_DB_ASSET,
    SEED_DB_OUTPUT,
    TMDB_CACHE_PATH,
    ensure_directories,
    iter_clean_spreadsheet_rows,
    load_legacy_rows,
    load_movie_overrides,
    normalize_genre,
    normalize_key,
    normalize_punctuation,
    read_json,
    write_json,
)


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


def choose_title_zh(metadata: Dict[str, object], featured_meta: Dict[str, object], source_title_zh: Optional[str], title_en: str, override_title_zh: Optional[str] = None) -> Tuple[Optional[str], Optional[str]]:
    candidates = [
        (override_title_zh, "manual_override"),
        (metadata.get("titleZh"), metadata.get("titleZhSource") or "tmdb_cache"),
        (featured_meta.get("titleZh"), featured_meta.get("titleZhSource") or "featured_seed"),
        (source_title_zh, "spreadsheet"),
    ]
    for value, source in candidates:
        text = clean_text(value)
        if is_traditional_title_candidate(text, title_en):
            return text, str(source)
    return None, None


def split_place_meta(place_name: str) -> Tuple[Optional[str], Optional[str]]:
    if not place_name:
        return None, None
    parts = [part.strip() for part in place_name.split(",") if part.strip()]
    if len(parts) >= 2:
        return place_name, parts[-1]
    return place_name, None


def load_place_overrides() -> Dict[str, object]:
    return read_json(PLACE_OVERRIDES_PATH, {"title_aliases": {}, "place_aliases": {}, "coordinate_overrides": {}})


def load_featured_seed() -> Dict[str, Dict[str, object]]:
    legacy_rows = load_legacy_rows()
    featured = {}
    movie_overrides = load_movie_overrides()
    title_aliases = movie_overrides.get("title_aliases", {})
    poster_manifest = read_json(POSTER_MANIFEST_PATH, {})
    poster_manifest_by_key = {normalize_key(title): payload for title, payload in poster_manifest.items()}
    metadata_cache = read_json(TMDB_CACHE_PATH, {})
    metadata_by_key = {normalize_key(title): payload for title, payload in metadata_cache.items()}
    for row in legacy_rows:
        title = normalize_punctuation(str(row.get("movieTitle", "")).strip())
        title = title_aliases.get(title, title)
        if not title:
            continue
        title_key = normalize_key(title)
        metadata = metadata_cache.get(title, {}) or metadata_by_key.get(title_key, {})
        poster_asset = metadata.get("posterAsset") or poster_manifest.get(title, {}).get("posterAsset") or poster_manifest_by_key.get(title_key, {}).get("posterAsset") or LEGACY_POSTER_MAP.get(title, DEFAULT_POSTER_ASSET)
        featured.setdefault(normalize_key(title), {
            "titleEn": metadata.get("titleEn") or title,
            "titleZh": metadata.get("titleZh") or row.get("movieTitleZh"),
            "year": metadata.get("year") or row.get("year"),
            "director": metadata.get("director") or row.get("director"),
            "genreRaw": metadata.get("genreRaw"),
            "genreGroup": row.get("genre") or normalize_genre(metadata.get("genreRaw")),
            "posterAsset": poster_asset,
            "isFeatured": 1,
            "places": [],
        })
        featured[normalize_key(title)]["places"].append({
            "locationName": row.get("locationName"),
            "placeKey": normalize_key(str(row.get("locationName") or "")),
            "address": row.get("address"),
            "district": row.get("district"),
            "latitude": row.get("latitude"),
            "longitude": row.get("longitude"),
        })
    return featured


def choose_verified_place(place_name: str, movie_title: str, title_aliases: Dict[str, str], place_aliases: Dict[str, str], featured_seed: Dict[str, Dict[str, object]]) -> Optional[Dict[str, object]]:
    movie_key = normalize_key(title_aliases.get(movie_title, movie_title))
    movie_seed = featured_seed.get(movie_key)
    if not movie_seed:
        return None

    expected_name = place_aliases.get(place_name, place_name)
    place_key = normalize_key(place_name)
    expected_key = normalize_key(expected_name)

    for candidate in movie_seed["places"]:
        candidate_key = candidate["placeKey"]
        if candidate_key == place_key or candidate_key == expected_key:
            return candidate

    for candidate in movie_seed["places"]:
        candidate_key = candidate["placeKey"]
        if candidate_key and (candidate_key in place_key or place_key in candidate_key):
            return candidate
        if expected_key and (candidate_key in expected_key or expected_key in candidate_key):
            return candidate
    return None


def get_coordinate_override(place_name: str, place_aliases: Dict[str, str], overrides: Dict[str, object]) -> Optional[Dict[str, object]]:
    coordinate_overrides = overrides.get("coordinate_overrides", {})
    if not isinstance(coordinate_overrides, dict):
        return None

    expected_name = place_aliases.get(place_name, place_name)
    override_by_key = {
        normalize_key(str(name)): payload
        for name, payload in coordinate_overrides.items()
        if isinstance(payload, dict)
    }
    for candidate in [place_name, expected_name]:
        payload = coordinate_overrides.get(candidate)
        if isinstance(payload, dict):
            return payload
    return override_by_key.get(normalize_key(expected_name))


def build_catalog() -> Tuple[List[Dict[str, object]], List[Dict[str, object]], List[Dict[str, object]]]:
    overrides = load_place_overrides()
    movie_overrides = load_movie_overrides()
    place_aliases = overrides.get("place_aliases", {})
    title_aliases = dict(overrides.get("title_aliases", {}))
    title_aliases.update(movie_overrides.get("title_aliases", {}))
    title_zh_overrides = movie_overrides.get("title_zh_overrides", {})
    if not isinstance(title_zh_overrides, dict):
        title_zh_overrides = {}
    title_zh_overrides_by_key = {
        normalize_key(str(title)): str(value)
        for title, value in title_zh_overrides.items()
        if value
    }
    featured_seed = load_featured_seed()
    metadata_cache = read_json(TMDB_CACHE_PATH, {})
    metadata_by_key = {normalize_key(title): payload for title, payload in metadata_cache.items()}
    poster_manifest = read_json(POSTER_MANIFEST_PATH, {})
    poster_manifest_by_key = {normalize_key(title): payload for title, payload in poster_manifest.items()}
    rows = iter_clean_spreadsheet_rows()

    movies: Dict[str, Dict[str, object]] = {}
    places: Dict[str, Dict[str, object]] = {}
    scenes: List[Dict[str, object]] = []
    missing_poster_titles: List[str] = []

    for row in rows:
        title_en = normalize_punctuation(str(row.get("movieTitleEn", "")).strip())
        title_zh = normalize_punctuation(str(row.get("movieTitleZh") or "").strip()) or None
        title_en = title_aliases.get(title_en, title_en)
        movie_key = normalize_key(title_en)
        if not title_en or not movie_key:
            continue

        metadata = metadata_cache.get(title_en, {}) or metadata_by_key.get(movie_key, {})
        featured_meta = featured_seed.get(movie_key, {})
        movie_record = movies.get(movie_key)
        if movie_record is None:
            genre_raw = metadata.get("genreRaw") or featured_meta.get("genreRaw") or row.get("movieGenreEn") or None
            poster_asset = metadata.get("posterAsset") or poster_manifest.get(title_en, {}).get("posterAsset") or poster_manifest_by_key.get(movie_key, {}).get("posterAsset") or featured_meta.get("posterAsset") or DEFAULT_POSTER_ASSET
            if poster_asset == DEFAULT_POSTER_ASSET:
                missing_poster_titles.append(title_en)
            override_title_zh = title_zh_overrides.get(title_en) or title_zh_overrides_by_key.get(movie_key)
            selected_title_zh, selected_title_zh_source = choose_title_zh(metadata, featured_meta, title_zh, title_en, override_title_zh)
            movie_record = {
                "movieId": len(movies) + 1,
                "titleEn": metadata.get("titleEn") or featured_meta.get("titleEn") or title_en,
                "titleZh": selected_title_zh,
                "titleZhSource": selected_title_zh_source,
                "year": metadata.get("year") or featured_meta.get("year"),
                "director": metadata.get("director") or featured_meta.get("director"),
                "genreRaw": genre_raw,
                "genreGroup": featured_meta.get("genreGroup") or normalize_genre(genre_raw),
                "posterAsset": poster_asset,
                "isFeatured": int(bool(featured_meta)),
            }
            movies[movie_key] = movie_record

        place_name_en = normalize_punctuation(str(row.get("placeNameEn", "")).strip())
        place_name_zh = normalize_punctuation(str(row.get("placeNameZh") or "").strip()) or None
        matched_place = choose_verified_place(place_name_en, title_en, title_aliases, place_aliases, featured_seed)
        coordinate_override = get_coordinate_override(place_name_en, place_aliases, overrides)
        place_key = normalize_key(place_aliases.get(place_name_en, place_name_en))
        place_record = places.get(place_key)
        if place_record is None:
            address_en, district_en = split_place_meta(place_name_en)
            address_zh, district_zh = split_place_meta(place_name_zh or "")
            latitude = matched_place.get("latitude") if matched_place else (coordinate_override or {}).get("latitude")
            longitude = matched_place.get("longitude") if matched_place else (coordinate_override or {}).get("longitude")
            if matched_place:
                coord_status = "verified"
            elif latitude is not None and longitude is not None:
                coord_status = "geocoded"
            else:
                coord_status = "missing"
            place_record = {
                "placeId": len(places) + 1,
                "nameEn": place_name_en,
                "nameZh": place_name_zh,
                "districtEn": matched_place.get("district") if matched_place else (coordinate_override or {}).get("districtEn") or district_en,
                "districtZh": district_zh,
                "addressEn": matched_place.get("address") if matched_place else (coordinate_override or {}).get("addressEn") or address_en,
                "addressZh": address_zh,
                "latitude": latitude,
                "longitude": longitude,
                "coordStatus": coord_status,
            }
            places[place_key] = place_record
        elif matched_place and place_record["coordStatus"] != "verified":
            place_record["latitude"] = matched_place.get("latitude")
            place_record["longitude"] = matched_place.get("longitude")
            place_record["addressEn"] = matched_place.get("address") or place_record["addressEn"]
            place_record["districtEn"] = matched_place.get("district") or place_record["districtEn"]
            place_record["coordStatus"] = "verified"
        elif coordinate_override and place_record["coordStatus"] == "missing":
            place_record["latitude"] = coordinate_override.get("latitude")
            place_record["longitude"] = coordinate_override.get("longitude")
            place_record["addressEn"] = coordinate_override.get("addressEn") or place_record["addressEn"]
            place_record["districtEn"] = coordinate_override.get("districtEn") or place_record["districtEn"]
            if place_record["latitude"] is not None and place_record["longitude"] is not None:
                place_record["coordStatus"] = "geocoded"

        description_en = normalize_punctuation(str(row.get("sceneDescriptionEn") or "").strip()) or None
        description_zh = normalize_punctuation(str(row.get("sceneDescriptionZh") or "").strip()) or None
        scenes.append({
            "sceneId": len(scenes) + 1,
            "movieId": movie_record["movieId"],
            "placeId": place_record["placeId"],
            "sceneTitleEn": description_en or ("Scene at " + place_name_en),
            "sceneTitleZh": description_zh,
            "descriptionEn": description_en or metadata.get("overviewEn") or None,
            "descriptionZh": description_zh or metadata.get("overviewZh") or None,
            "sourceRow": int(row.get("sourceRow") or len(scenes) + 1),
            "isMapVisible": 1 if place_record["coordStatus"] in {"verified", "geocoded"} else 0,
        })

    MISSING_POSTERS_LOG.write_text("\n".join(sorted(set(missing_poster_titles))) + ("\n" if missing_poster_titles else ""), encoding="utf-8")
    title_source_counts = Counter(str(movie.get("titleZhSource") or "missing") for movie in movies.values())
    movies_missing_title_zh = [
        str(movie.get("titleEn"))
        for movie in movies.values()
        if not movie.get("titleZh")
    ]
    title_sources = {
        str(movie.get("titleEn")): {
            "titleZh": movie.get("titleZh"),
            "source": movie.get("titleZhSource"),
        }
        for movie in movies.values()
    }
    report = read_json(CLEANING_REPORT_PATH, {})
    report.update({
        "movieCount": len(movies),
        "placeCount": len(places),
        "sceneCount": len(scenes),
        "verifiedPlaceCount": sum(1 for place in places.values() if place["coordStatus"] == "verified"),
        "geocodedPlaceCount": sum(1 for place in places.values() if place["coordStatus"] == "geocoded"),
        "missingPosterMovieCount": len(sorted(set(missing_poster_titles))),
        "traditionalChineseTitleCoverage": {
            "totalMovies": len(movies),
            "moviesWithTraditionalChineseTitle": len(movies) - len(movies_missing_title_zh),
            "moviesMissingTraditionalChineseTitle": len(movies_missing_title_zh),
            "sourceCounts": dict(sorted(title_source_counts.items())),
            "missingTitles": sorted(movies_missing_title_zh),
            "titles": title_sources,
        },
    })
    write_json(CLEANING_REPORT_PATH, report)
    return list(movies.values()), list(places.values()), scenes


def create_seed_database(movies: List[Dict[str, object]], places: List[Dict[str, object]], scenes: List[Dict[str, object]]) -> None:
    if SEED_DB_OUTPUT.exists():
        SEED_DB_OUTPUT.unlink()

    connection = sqlite3.connect(str(SEED_DB_OUTPUT))
    cursor = connection.cursor()
    cursor.executescript(
        """
        PRAGMA journal_mode = DELETE;
        PRAGMA foreign_keys = ON;

        CREATE TABLE movies (
            movieId INTEGER NOT NULL PRIMARY KEY,
            titleEn TEXT NOT NULL,
            titleZh TEXT,
            year INTEGER,
            director TEXT,
            genreRaw TEXT,
            genreGroup TEXT NOT NULL,
            posterAsset TEXT NOT NULL,
            isFeatured INTEGER NOT NULL
        );

        CREATE TABLE places (
            placeId INTEGER NOT NULL PRIMARY KEY,
            nameEn TEXT,
            nameZh TEXT,
            districtEn TEXT,
            districtZh TEXT,
            addressEn TEXT,
            addressZh TEXT,
            latitude REAL,
            longitude REAL,
            coordStatus TEXT
        );

        CREATE TABLE scenes (
            sceneId INTEGER NOT NULL PRIMARY KEY,
            movieId INTEGER NOT NULL,
            placeId INTEGER NOT NULL,
            sceneTitleEn TEXT,
            sceneTitleZh TEXT,
            descriptionEn TEXT,
            descriptionZh TEXT,
            sourceRow INTEGER NOT NULL,
            isMapVisible INTEGER NOT NULL
        );

        CREATE TABLE user_check_ins (
            sceneId INTEGER NOT NULL PRIMARY KEY,
            checkedInAt INTEGER NOT NULL
        );

        CREATE TABLE route_plans (
            planId INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            name TEXT,
            createdAt INTEGER NOT NULL,
            originLat REAL,
            originLng REAL
        );

        CREATE TABLE route_plan_stops (
            planId INTEGER NOT NULL,
            sceneId INTEGER NOT NULL,
            visitOrder INTEGER NOT NULL,
            PRIMARY KEY (planId, sceneId)
        );
        """
    )
    cursor.executemany(
        "INSERT INTO movies(movieId, titleEn, titleZh, year, director, genreRaw, genreGroup, posterAsset, isFeatured) VALUES(:movieId, :titleEn, :titleZh, :year, :director, :genreRaw, :genreGroup, :posterAsset, :isFeatured)",
        movies,
    )
    cursor.executemany(
        "INSERT INTO places(placeId, nameEn, nameZh, districtEn, districtZh, addressEn, addressZh, latitude, longitude, coordStatus) VALUES(:placeId, :nameEn, :nameZh, :districtEn, :districtZh, :addressEn, :addressZh, :latitude, :longitude, :coordStatus)",
        places,
    )
    cursor.executemany(
        "INSERT INTO scenes(sceneId, movieId, placeId, sceneTitleEn, sceneTitleZh, descriptionEn, descriptionZh, sourceRow, isMapVisible) VALUES(:sceneId, :movieId, :placeId, :sceneTitleEn, :sceneTitleZh, :descriptionEn, :descriptionZh, :sourceRow, :isMapVisible)",
        scenes,
    )
    cursor.execute("INSERT INTO route_plans(name, createdAt, originLat, originLng) VALUES(?, strftime('%s', 'now'), NULL, NULL)", ("My Route",))
    connection.commit()
    connection.close()
    SEED_DB_ASSET.write_bytes(SEED_DB_OUTPUT.read_bytes())


def main() -> None:
    ensure_directories()
    movies, places, scenes = build_catalog()
    write_json(OUTPUT_DIR / "movies.json", movies)
    write_json(OUTPUT_DIR / "places.json", places)
    write_json(OUTPUT_DIR / "scenes.json", scenes)
    create_seed_database(movies, places, scenes)

    verified_places = sum(1 for place in places if place["coordStatus"] == "verified")
    geocoded_places = sum(1 for place in places if place["coordStatus"] == "geocoded")
    print(f"Movies: {len(movies)}")
    print(f"Places: {len(places)}")
    print(f"Scenes: {len(scenes)}")
    print(f"Verified map places: {verified_places}")
    print(f"Geocoded map places: {geocoded_places}")
    print(f"Seed database: {SEED_DB_OUTPUT}")
    print(f"Copied asset: {SEED_DB_ASSET}")
    print(f"Missing poster log: {MISSING_POSTERS_LOG}")
    print(f"Cleaning report: {CLEANING_REPORT_PATH}")


if __name__ == "__main__":
    main()
