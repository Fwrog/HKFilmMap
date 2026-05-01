# HKFilmMap

**The Hong Kong Polytechnic University (PolyU)**  
**LSGI541 | Semester 2, 2025/26**

HKFilmMap is an Android location-based services project for exploring Hong Kong film locations, browsing movie-scene connections, and planning movie-themed half-day routes. It combines a mobile map interface with an offline Python data pipeline that packages film, place, scene, route, and recommendation data into a local SQLite seed database.

![HKFilmMap concept banner](docs/media/readme-hero.png)

## Demo

[![HKFilmMap demo preview](docs/media/demo-preview.gif)](docs/media/demo.mp4)

- Full demo video: [docs/media/demo.mp4](docs/media/demo.mp4)
- Project description: [docs/PROJECT_DESCRIPTION.md](docs/PROJECT_DESCRIPTION.md)
- Configuration guide: [docs/CONFIGURATION.md](docs/CONFIGURATION.md)

## Project At A Glance

| Item | Current project value |
| --- | --- |
| Platform | Android app opened and synced with Android Studio |
| Core data | 68 movies, 97 places, 142 scene records |
| Map-ready scenes | 85 scenes with usable coordinates |
| Recommendations | 34 nearby food, coffee, and dessert records |
| Main use case | Turn static film-location data into a city route users can actually follow |
| Coursework context | PolyU LSGI541, Semester 2, 2025/26 |

## Main User Flow

```text
Register or log in
  -> Browse Hong Kong film places on the map
  -> Open movie or place details
  -> Add favorite places to a route
  -> Generate or edit a half-day route
  -> Preview navigation, check in, and view nearby recommendations
```

## Core Features

- Map-based exploration of Hong Kong film locations
- Movie catalog with poster, title, year, director, genre, and scene count
- Place and movie detail pages that connect films, scenes, and locations
- Route Planner with search, browse, manual ordering, route generation, and route optimization
- Scene-level check-ins, achievement tracking, and nearby recommendation ranking
- Offline backend workflow for cleaning spreadsheet data and rebuilding the app seed database

## Technical Overview

| Layer | Implementation summary |
| --- | --- |
| Android app | Java, Android SDK, Material Components, Google Maps SDK, Google Directions API, Firebase Authentication |
| Local storage | Room / SQLite with tables for movies, places, scenes, check-ins, route plans, and route stops |
| Data pipeline | Python scripts read `hk_movie_locations.xlsx`, merge manual overrides, optionally enrich TMDB metadata, validate links, and rebuild `hkfilmmap_seed.db` |
| Map display | Zoom-responsive markers: clustered markers at low zoom, poster-stack markers at medium zoom, and fan-out poster markers at high zoom |
| Route logic | Half-day route scoring balances distance, movie richness, coordinate confidence, genre preference, check-in history, and controlled randomness |
| Optimization | Exact dynamic programming for up to 10 stops; nearest-neighbor plus 2-opt heuristic for larger route lists |
| Recommendations | Nearby food and coffee ranking uses distance, district fit, time of day, price, film mood, and category diversity |

## Repository Layout

```text
.
|-- HKFilmMap/                 Android Studio project
|-- backend/                   Offline data pipeline and override configs
|-- docs/
|   |-- media/                 README images, GIF preview, and demo video
|   |-- CONFIGURATION.md       Local key and setup guide
|   `-- PROJECT_DESCRIPTION.md Project report summary and algorithms
`-- hk_movie_locations.xlsx    Root spreadsheet input for backend refresh
```

## Quick Start

1. Read [docs/CONFIGURATION.md](docs/CONFIGURATION.md).
2. Create local-only config files, including `HKFilmMap/local.properties` and `HKFilmMap/app/google-services.json`.
3. Open `HKFilmMap/` in Android Studio.
4. Sync Gradle from Android Studio and build the app.
5. If you want to regenerate data, set up the backend environment and run the backend scripts from the repository root.

## Backend Environment

```powershell
conda env create -f backend/environment.yml
conda activate backend
pip install -r backend/requirements.txt
```

Main refresh command:

```powershell
python backend/scripts/refresh_database.py
```

The generated database is copied into:

```text
HKFilmMap/app/src/main/assets/hkfilmmap_seed.db
```

## Local Configuration Summary

This repository intentionally excludes API keys and local machine paths. To run the full project, provide:

- TMDB credentials for movie metadata enrichment
- Google Maps / LBS credentials for map, routing, and optional geocoding features
- Firebase local config for authentication flows

Exact paths and example files are documented in [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## Documentation

- Local configuration: [docs/CONFIGURATION.md](docs/CONFIGURATION.md)
- Backend workflow: [backend/README.md](backend/README.md)
- Full project description: [docs/PROJECT_DESCRIPTION.md](docs/PROJECT_DESCRIPTION.md)
- Third-party and usage notice: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)

## License

Unless otherwise noted, the original source code and original documentation in this repository are provided under the [MIT License](LICENSE).

Third-party services, trademarks, media-related materials, and derived metadata are not automatically relicensed by that file. Please read [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before reuse or redistribution.

## Coursework And Scope Notes

- This repository is published as a PolyU LSGI541 Semester 2, 2025/26 coursework project and portfolio-style code sample.
- Sensitive credentials are intentionally excluded from version control and must be supplied locally by anyone running the project.
- The Android project is currently documented for Android Studio sync/build; Gradle wrapper scripts are not included in this repository snapshot.
- The app currently keeps some Room access on the main thread and does not yet include automated tests.
