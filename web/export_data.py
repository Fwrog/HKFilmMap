"""Export the public Android catalogue; never export user or route tables."""
import json
import shutil
import sqlite3
from pathlib import Path

web = Path(__file__).resolve().parent
app = web.parent / "HKFilmMap/app/src/main"
connection = sqlite3.connect(f"file:{app / 'assets/hkfilmmap_seed.db'}?mode=ro", uri=True)
connection.row_factory = sqlite3.Row
catalogue = {
    table: [dict(row) for row in connection.execute(f"SELECT * FROM {table}")]
    for table in ("movies", "places", "scenes")
}
posters = {p.stem: p for p in (app / "res/drawable").glob("poster_*.*")}
posters.update({p.stem: p for p in (app / "res/drawable-nodpi").glob("poster_*.*")})
(web / "assets/posters").mkdir(parents=True, exist_ok=True)
for movie in catalogue["movies"]:
    poster = posters[movie.pop("posterAsset")]
    assert poster.suffix in (".jpg", ".webp", ".png"), poster
    shutil.copyfile(poster, web / "assets/posters" / poster.name)
    movie["poster"] = "assets/posters/" + poster.name
(web / "catalogue.json").write_text(json.dumps(catalogue, ensure_ascii=False), encoding="utf-8")
print(f"Exported {len(catalogue['movies'])} films, {len(catalogue['places'])} places, {len(catalogue['scenes'])} scenes.")
