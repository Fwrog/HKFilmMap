# <img src="web/assets/icon.svg" width="36" height="36" alt=""> HKFilmMap

### Hong Kong, through cinema.

An **Android app developed for PolyU LSGI541** (Semester 2, 2025/26), now extended with an interactive, desktop-first **Web atlas**. Browse films, discover their locations and put together a city itinerary.

**[Open the Web atlas ↗](https://fwrog.github.io/HKFilmMap/)** · [中文说明](README.zh-CN.md) · [Project story](https://fwrog.github.io/HKFilmMap/project.html) · [Course report · PDF](web/report/HKFilmMap_Project_Report_Public.pdf)

[![HKFilmMap Web atlas — linked film catalogue, location map and details](web/assets/web-desktop.png)](https://fwrog.github.io/HKFilmMap/)

> The coursework deliverable is the **Android app**. The Web atlas is a subsequent update built from its catalogue, with a separate interface and feature scope. The course report documents the Android project.

## Explore in your browser

1. **Find a film** — search in English or Chinese, or filter by genre and district.
2. **Follow its locations** — select a film, then a poster marker or location to read the scene record.
3. **Make an itinerary** — add or remove stops, reorder them, optimise the straight-line order, and open walking directions in Google Maps.
4. **Keep your notes** — create, edit, search and delete personal place notes. Undo a deletion, or export/import a JSON backup.

No account or API key is needed for the Web atlas. Itineraries and notes stay in the current browser. Basemap tiles need an internet connection; clearing browser storage removes personal content.

| Catalogue | Records |
| :--- | ---: |
| Films | 68 |
| Places | 97 |
| Scenes | 142 |
| Map-ready scenes / distinct places | 85 / 57 |

Counts come from the packaged SQLite database. Unmapped scenes remain accessible in film details. Film-location associations are course dataset records, not a claim that every record has been independently verified.

## Android original · Web update

| | Android coursework | Web update |
| :--- | :--- | :--- |
| Exploration | Google Maps, film catalogue, scene details | Leaflet map, poster markers, search, bilingual interface |
| Itineraries | Editing, automatic half-day generation, optimisation | Up to 8 stops, manual editing, two starter collections, exact order optimisation |
| Personal content | Accounts, scene check-ins, achievements | Browser-local place notes with import/export |
| Nearby food | 34 recommendation records | Not included |
| Navigation | Google Directions API | Opens Google Maps walking directions |
| Storage | Room / SQLite, Firebase Authentication | Public JSON catalogue + local browser storage |

The Web route optimiser fixes the first stop and minimises Haversine distance. Dotted lines show the stop sequence, **not walkable streets or travel times**. Check current access and closures before visiting.

<details>
<summary><strong>Watch the original Android demo</strong></summary>

[![Android app demo](docs/media/demo-preview.gif)](docs/media/demo.mp4)

[Full video](docs/media/demo.mp4) · [Android project description](docs/PROJECT_DESCRIPTION.md)

</details>

## Run locally

**Web — no build step**

```sh
python3 -m http.server 4021 --directory web
# Open http://localhost:4021
```

After changing the Android seed catalogue:

```sh
python3 web/export_data.py
node --test web/tests/model.test.mjs
```

**Android** — follow the [configuration guide](docs/CONFIGURATION.md), provide local Google Maps/Firebase configuration, then open `HKFilmMap/` in Android Studio. The repository snapshot does not include Gradle wrapper scripts.

**Data pipeline** — see [backend/README.md](backend/README.md) for spreadsheet cleaning, manual overrides, TMDB enrichment and SQLite generation.

## Inside the repository

| Path | Purpose |
| :--- | :--- |
| [`HKFilmMap/`](HKFilmMap/) | Original Android Studio project |
| [`web/`](web/) | Static Web atlas, catalogue, posters and public report |
| [`backend/`](backend/) | Offline Python data pipeline |
| [`docs/PROJECT_DESCRIPTION.md`](docs/PROJECT_DESCRIPTION.md) | Android architecture and algorithm notes |
| [`docs/design/`](docs/design/) | Web design direction and original visual study |
| [`.github/workflows/pages.yml`](.github/workflows/pages.yml) | Validate and deploy the Web atlas to GitHub Pages |

The Web exporter copies only movies, places and scenes. It does not publish user check-ins or saved Android routes. Deployment packages the public `web/` assets only; no server or database service is required.

## Report & contributors

**[HKFilmMap: Exploring Hong Kong film locations through an interactive map](web/report/HKFilmMap_Project_Report_Public.pdf)** — 21 pages, English. Public copy: student identifiers and the account screenshot have been removed; authorship and report content are retained.

| Contributor | Coursework contribution |
| :--- | :--- |
| Yikai Wu | Coding, algorithms, database and report lead |
| Yu Cai | Map exploration, catalogue/detail interactions and presentation materials |
| Anran Chen | Presentation design and report materials |
| Junkai Meng | Demo video and presentation |

Roles are summarised from the report's individual contribution statements. The Web checks cover the new browser interface and data model; they do not replace an Android build or device test.

## Credits & reuse

Original code and documentation: [MIT](LICENSE). Posters, film metadata and third-party materials retain their respective rights; see [third-party notices](THIRD_PARTY_NOTICES.md). Map: © OpenStreetMap contributors. Interface: Leaflet. The Web interaction design takes inspiration from [Anitabi](https://www.anitabi.cn/map), adapted to the project's own film catalogue.
