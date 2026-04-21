# Configuration Guide

This repository is set up so that API keys and local machine paths are **not committed**. Before building the Android app or running optional backend enrichment scripts, create the following local-only files.

In practice, you mainly need:

- one TMDB credential group for movie metadata
- one Google Maps / LBS credential group for mapping and routing
- one Firebase local config file plus callback URL for auth

## 1. Android Local Properties

Create `HKFilmMap/local.properties` with:

```properties
sdk.dir=C:\\Path\\To\\Android\\Sdk
GOOGLE_MAPS_API_KEY=your_google_maps_sdk_key
DIRECTIONS_API_KEY=your_google_directions_key
FIREBASE_AUTH_CONTINUE_URL=https://your-project.firebaseapp.com/__/auth/links
```

What each key is used for:

- `sdk.dir`: local Android SDK path used by Gradle
- `GOOGLE_MAPS_API_KEY`: map rendering in the Android app
- `DIRECTIONS_API_KEY`: in-app walking route requests
- `FIREBASE_AUTH_CONTINUE_URL`: Firebase email-link sign-in URL; Gradle also derives the app intent-filter host/path from this value

Reference template: [HKFilmMap/local.properties.example](../HKFilmMap/local.properties.example)

## 2. Firebase Android Config

Place your downloaded Firebase Android config file at:

```text
HKFilmMap/app/google-services.json
```

This file is required for the Firebase Auth integration used by `LoginActivity` and `RegisterActivity`.

## 3. Backend TMDB Credentials

For TMDB enrichment, use either environment variables or a local JSON file:

```text
backend/config/tmdb_config.local.json
```

Example:

```json
{
  "api_key": "your_tmdb_api_key",
  "bearer_token": "your_tmdb_bearer_token"
}
```

Supported environment variables:

- `TMDB_API_KEY`
- `TMDB_BEARER_TOKEN`
- `TMDB_READ_ACCESS_TOKEN`

Reference template: [backend/config/tmdb_config.local.example.json](../backend/config/tmdb_config.local.example.json)

## 4. Backend Google Maps / Geocoding Key

For coordinate suggestion scripts, use either environment variables or:

```text
backend/config/google_maps_config.local.json
```

Example:

```json
{
  "api_key": "your_google_maps_geocoding_key"
}
```

Supported environment variables:

- `GOOGLE_MAPS_API_KEY`
- `GOOGLE_GEOCODING_API_KEY`
- `MAPS_API_KEY`

Reference template: [backend/config/google_maps_config.local.example.json](../backend/config/google_maps_config.local.example.json)

## 5. Spreadsheet Input

The backend expects the source spreadsheet at the repository root:

```text
hk_movie_locations.xlsx
```

For compatibility, the backend also accepts the older legacy filename `hk_movie_locations_bilingual_expanded_2000.xlsx` if present.

## 6. Common Commands

Android app:

```powershell
Open `HKFilmMap/` in Android Studio, sync Gradle, then build from the IDE.
```

Backend environment:

```powershell
conda env create -f backend/environment.yml
conda activate backend
pip install -r backend/requirements.txt
```

Backend refresh:

```powershell
python backend/scripts/refresh_database.py
```

Backend validation:

```powershell
python backend/scripts/validate_seed_database.py
```

## 7. Git Ignore Rules

The repository ignores:

- `HKFilmMap/local.properties`
- `HKFilmMap/app/google-services.json`
- `backend/config/*.local.json`
- backend-generated output files
- Android build caches and Python cache files

If you create the files above and run `git status`, they should remain untracked.

## 8. Android Build Note

This project snapshot is intended to be opened in Android Studio. The repository currently does not include `gradlew` / `gradlew.bat` wrapper scripts, so IDE-based sync/build is the documented path for this release.
