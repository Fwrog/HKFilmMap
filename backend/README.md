# HKFilmMap Backend Pipeline

This folder contains the offline data workflow for HKFilmMap.

For the GitHub-ready setup flow, start with:

- [../README.md](../README.md)
- [../docs/CONFIGURATION.md](../docs/CONFIGURATION.md)

## What The Backend Does

- reads the root spreadsheet `hk_movie_locations.xlsx`
- deduplicates movies from the spreadsheet
- optionally enriches metadata and posters from TMDB
- merges spreadsheet rows with curated verified coordinates from the app seed
- rebuilds the packaged SQLite seed database
- copies the final database into `HKFilmMap/app/src/main/assets/`

Generated backend outputs are local artifacts and are gitignored.

## Environment Setup

Create the conda environment:

```powershell
conda env create -f backend/environment.yml
conda activate backend
pip install -r backend/requirements.txt
```

If the environment already exists:

```powershell
conda activate backend
pip install -r backend/requirements.txt
```

## Credentials

TMDB metadata enrichment supports:

- `TMDB_API_KEY`
- `TMDB_BEARER_TOKEN`
- `TMDB_READ_ACCESS_TOKEN`
- local-only `backend/config/tmdb_config.local.json`

Google Maps geocoding for coordinate suggestion supports:

- `GOOGLE_MAPS_API_KEY`
- `GOOGLE_GEOCODING_API_KEY`
- `MAPS_API_KEY`
- local-only `backend/config/google_maps_config.local.json`

## Main Commands

Full refresh from the repository root:

```powershell
python backend/scripts/refresh_database.py
```

Refresh TMDB metadata only:

```powershell
python backend/scripts/fetch_tmdb_metadata.py --featured-only --refresh-config
```

Rebuild and validate without refetching metadata:

```powershell
python backend/scripts/build_seed_database.py
python backend/scripts/validate_seed_database.py
```

## Expected Inputs

- spreadsheet: `hk_movie_locations.xlsx`
- legacy spreadsheet fallback: `hk_movie_locations_bilingual_expanded_2000.xlsx`
- place overrides: `backend/config/place_overrides.json`
- movie overrides: `backend/config/movie_overrides.json`

## Packaged Output

- local backend outputs are written to `backend/output/`
- the app-ready database is copied to `HKFilmMap/app/src/main/assets/hkfilmmap_seed.db`
