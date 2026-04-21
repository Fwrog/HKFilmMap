import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PYTHON = sys.executable
TMDB_CONFIG = ROOT / "backend" / "config" / "tmdb_config.local.json"


def run(step: str, args):
    print(f"\n== {step} ==")
    result = subprocess.run([PYTHON] + args, cwd=str(ROOT), check=False)
    if result.returncode != 0:
        raise SystemExit(result.returncode)


def has_tmdb_credentials() -> bool:
    return bool(os.environ.get("TMDB_API_KEY") or os.environ.get("TMDB_BEARER_TOKEN") or TMDB_CONFIG.exists())


def main() -> None:
    if has_tmdb_credentials():
        run("Fetch TMDB metadata and posters", ["backend/scripts/fetch_tmdb_metadata.py"])
    else:
        print("\n== Fetch TMDB metadata and posters ==")
        print("TMDB credentials not found. Skipping API enrichment and using cached/local fallback metadata.")
    run("Build seed database", ["backend/scripts/build_seed_database.py"])
    run("Validate seed database", ["backend/scripts/validate_seed_database.py"])
    print("\nRefresh complete.")


if __name__ == "__main__":
    main()
