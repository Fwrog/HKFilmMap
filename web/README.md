# HKFilmMap Web update

The course deliverable is the Android app. This directory contains a subsequent static Web extension using its public film catalogue.

Run from the repository root:

```sh
python3 -m http.server 4021 --directory web
```

Open `http://localhost:4021/`. Use `?lang=zh` for Chinese. `project.html` explains the project and links to the public course-report PDF.

- `export_data.py` exports only movies, places and scenes from the Android seed database and copies its poster assets. Run after updating the seed.
- `app.js` links search, catalogue selection, map markers, place details and the itinerary.
- `model.js` handles catalogue matching, stop deduplication, Haversine distances and exact fixed-first-stop route ordering.
- `notebook.js` implements local place-note CRUD, search, deletion undo and JSON import/export. `notes-model.js` validates imported records before saving.
- `tokens.css` / `style.css` define the shared typography and map-first interface. See `docs/design/city-frames.md` for the visual direction.
- `localStorage` holds the itinerary (`hkfilmmap.route.v1`) and notes (`hkfilmmap.notes.v1`) for this browser. There is no account, shared write API or cloud sync. Catalogue records are read-only.
- GitHub Pages builds through `.github/workflows/pages.yml`; all internal URLs are relative so the project subpath works.

Run `node --test web/tests/model.test.mjs` to check data relations, poster assets, bilingual search, route behaviour and note imports. Browser interaction checks are documented in `docs/design/web-verification.md`.

The map uses OpenStreetMap's standard raster tiles on demand. It requires network access; a tile failure appears on the page. Posters identify films associated with a place and are not scene photographs. Route lines show order, not walkable roads.
