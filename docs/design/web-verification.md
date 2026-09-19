# Web update verification — 2026-09-19

The course deliverable is Android. These checks concern the subsequent Web update.

## Automated checks

`node --test web/tests/model.test.mjs` — 7 passing tests:

- Catalogue relationships, bundled poster files and 85 map-ready scenes.
- English/Chinese search and genre/district intersection.
- Route deduplication and eight-stop limit.
- Haversine reference distance and ordered route length.
- Exact route optimiser agrees with exhaustive permutations, with the first stop fixed.
- Google Maps directions preserve every stop and its order.
- Imported notes validate as a whole batch; malformed, duplicate and unknown-place records are rejected.

## Browser checks

- English catalogue loads; Chungking Express search returns one film and five mapped places.
- Film selection changes markers and opens its location list; selecting the Central–Mid-Levels Escalator opens scene details.
- Add a place, append the Central collection without duplicates, reload and retain four stops.
- Move a stop, optimise the order, remove a stop and clear the itinerary.
- Create a note, edit its title, reload, search the updated title, delete and undo.
- Import a valid JSON file without replacing existing notes. Exported JSON was read back and contained both records.
- Switch language with the notebook open; controls and place names change to Chinese.
- Desktop 1440 × 900 and narrow widths 320, 375, 414 and 768 checked for viewport overflow; screenshots reviewed at 320, 375, 768 and 1440.
- Final desktop screenshot captured with all visible map tiles loaded and no horizontal overflow.
- No application console errors observed during these flows. Test notes and route stops were removed after testing.

## Limits

The Web catalogue has not undergone record-by-record independent film-location verification. Basemap availability depends on the network. The route model uses straight-line distance, not street routing. These checks do not establish Android build/device acceptance or shared cloud storage.
