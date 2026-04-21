# HKFilmMap

An Android coursework project for exploring Hong Kong film locations, browsing movie scenes, and planning movie-themed half-day routes with an offline seed database.

**The Hong Kong Polytechnic University (PolyU)**  
**LSGI541 | Semester 2, 2025/26**

## Demo

[![HKFilmMap demo preview](docs/media/demo-preview.gif)](docs/media/demo.mp4)

- Full demo video: [docs/media/demo.mp4](docs/media/demo.mp4)

## Overview

HKFilmMap combines:

- an Android app for map-based film location exploration
- a movie and scene browsing experience
- route planning and check-in features
- a Python backend pipeline that turns spreadsheet data into the packaged SQLite seed database used by the app

## Highlights

- Explore mapped Hong Kong film locations with Google Maps
- Browse movie entries and scene-linked places
- Build half-day movie routes
- Track check-ins and achievements
- Use a backend workflow to regenerate the local seed database from spreadsheet input plus optional TMDB enrichment

## Repository Layout

```text
.
|-- HKFilmMap/                 Android Studio project
|-- backend/                   Offline data pipeline and override configs
|-- docs/
|   `-- media/                 Demo assets used in this README
`-- hk_movie_locations.xlsx    Root spreadsheet input for backend refresh
```

## Quick Start

1. Read [docs/CONFIGURATION.md](docs/CONFIGURATION.md).
2. Create your local-only config files such as `HKFilmMap/local.properties` and `HKFilmMap/app/google-services.json`.
3. Open `HKFilmMap/` in Android Studio.
4. Sync Gradle from Android Studio and build the app.
5. If you want to regenerate the packaged dataset, set up the backend environment and run the backend scripts from the repository root.

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

The generated `hkfilmmap_seed.db` is copied into `HKFilmMap/app/src/main/assets/` for packaging.

## Documentation

- Local configuration: [docs/CONFIGURATION.md](docs/CONFIGURATION.md)
- Backend workflow: [backend/README.md](backend/README.md)
- Third-party and usage notice: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)

## License

Unless otherwise noted, the original source code and original documentation in this repository are provided under the [MIT License](LICENSE).

Third-party services, trademarks, media-related materials, and derived metadata are not automatically relicensed by that file. Please read [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before reuse or redistribution.

## Coursework Statement

This repository is published as a **PolyU LSGI541 Semester 2, 2025/26 coursework project** and portfolio-style code sample. It is shared to document the implementation, data pipeline, and demo outcome of the assignment.

## Important Notice

- This repository includes original coursework code and documentation together with references to third-party platforms, datasets, and media-related content.
- Third-party materials, service marks, APIs, and derived data remain the property of their respective owners.
- API credentials are intentionally excluded from version control and must be supplied locally by anyone running the project.
- Please review [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before redistributing, reusing, or relicensing repository contents.

## Current Project Notes

- The Android project is currently documented for Android Studio sync/build.
- Gradle wrapper scripts are not included in this repository snapshot.
- The app currently keeps some Room access on the main thread and does not yet include automated tests.
