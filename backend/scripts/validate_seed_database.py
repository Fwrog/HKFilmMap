import sqlite3
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
DB_PATH = REPO_ROOT / "backend" / "output" / "hkfilmmap_seed.db"


def fetch_value(cursor: sqlite3.Cursor, sql: str) -> int:
    cursor.execute(sql)
    row = cursor.fetchone()
    return int(row[0] or 0)


def main() -> None:
    if not DB_PATH.exists():
        raise SystemExit(f"Seed database not found: {DB_PATH}")

    connection = sqlite3.connect(str(DB_PATH))
    cursor = connection.cursor()

    counts = {
        "movies": fetch_value(cursor, "SELECT COUNT(*) FROM movies"),
        "places": fetch_value(cursor, "SELECT COUNT(*) FROM places"),
        "scenes": fetch_value(cursor, "SELECT COUNT(*) FROM scenes"),
        "verified_places": fetch_value(cursor, "SELECT COUNT(*) FROM places WHERE coordStatus = 'verified'"),
        "broken_movie_links": fetch_value(
            cursor,
            "SELECT COUNT(*) FROM scenes s LEFT JOIN movies m ON s.movieId = m.movieId WHERE m.movieId IS NULL"
        ),
        "broken_place_links": fetch_value(
            cursor,
            "SELECT COUNT(*) FROM scenes s LEFT JOIN places p ON s.placeId = p.placeId WHERE p.placeId IS NULL"
        ),
        "invalid_map_rows": fetch_value(
            cursor,
            "SELECT COUNT(*) FROM scenes s JOIN places p ON s.placeId = p.placeId WHERE s.isMapVisible = 1 AND (p.latitude IS NULL OR p.longitude IS NULL)"
        )
    }

    connection.close()

    print("Validation summary")
    for key, value in counts.items():
        print(f"- {key}: {value}")

    if counts["broken_movie_links"] != 0 or counts["broken_place_links"] != 0 or counts["invalid_map_rows"] != 0:
        raise SystemExit("Seed database validation failed.")


if __name__ == "__main__":
    main()
