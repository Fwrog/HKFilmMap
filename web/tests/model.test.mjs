import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
import {distanceKm,routeDistance,addStops,directionsUrl,matchingScenes,optimiseStops} from '../model.js';
const data=JSON.parse(readFileSync(new URL('../catalogue.json',import.meta.url)));
const movies=new Map(data.movies.map(m=>[m.movieId,m]));
const places=new Map(data.places.map(p=>[p.placeId,p]));
test('catalogue preserves relations, assets and the 85 map-ready scene records',()=>{
  assert.deepEqual(Object.keys(data),['movies','places','scenes']);
  assert.equal(data.scenes.filter(s=>s.isMapVisible===1).length,85);
  for(const scene of data.scenes){assert.ok(movies.has(scene.movieId));assert.ok(places.has(scene.placeId));}
  for(const movie of data.movies)assert.ok(existsSync(new URL('../'+movie.poster,import.meta.url)));
  for(const id of [3,18,64,67,16,62,9])assert.ok(data.scenes.some(s=>s.placeId===id && s.isMapVisible===1));
});
test('English/Chinese movie search, scene search and genre/district intersection',()=>{
  const search=query=>matchingScenes(data.scenes,movies,places,{query,genre:'',district:''});
  assert.deepEqual(search('Chungking Express').map(s=>s.sceneId),search('重慶森林').map(s=>s.sceneId));
  const found=matchingScenes(data.scenes,movies,places,{query:'',genre:'Romance',district:'Central'});
  assert.ok(found.length>0);assert.ok(found.every(s=>movies.get(s.movieId).genreGroup==='Romance' && places.get(s.placeId).districtEn==='Central'));
  assert.equal(search('no-such-film-000000').length,0);
  assert.ok(search('flyover').length>0);
});
test('route adds places once and limits directions to eight stops',()=>{
  assert.deepEqual(addStops([3,18],[18,64,3,67]),[3,18,64,67]);
  assert.equal(addStops([1,2,3,4,5,6],[7,8,9]).length,8);
});
test('Haversine distances have a known reference and routes retain order',()=>{
  const a={latitude:0,longitude:0},b={latitude:0,longitude:1};
  assert.ok(Math.abs(distanceKm(a,b)-111.195)<.01);
  assert.equal(distanceKm(a,a),0);assert.equal(routeDistance([a,b]),distanceKm(a,b));
});
test('exact optimisation agrees with exhaustive permutations and fixes first stop',()=>{
  const stops=[3,67,64,18,2].map(id=>places.get(id));
  function permutations(list){return list.length ? list.flatMap((x,i)=>permutations(list.filter((_,j)=>i!==j)).map(rest=>[x,...rest])):[[]];}
  const optimum=Math.min(...permutations(stops.slice(1)).map(rest=>routeDistance([stops[0],...rest])));
  const result=optimiseStops(stops);
  assert.equal(result[0],stops[0]);assert.ok(Math.abs(routeDistance(result)-optimum)<1e-8);
  assert.deepEqual(new Set(result),new Set(stops));
});
test('directions transfer every stop in order',()=>{
  const stops=[3,18,64,67].map(id=>places.get(id));const url=new URL(directionsUrl(stops));
  assert.equal(url.searchParams.get('origin'),`${stops[0].latitude},${stops[0].longitude}`);
  assert.equal(url.searchParams.get('waypoints'),stops.slice(1,-1).map(p=>`${p.latitude},${p.longitude}`).join('|'));
  assert.equal(url.searchParams.get('travelmode'),'walking');
});


test('notes imports validate a whole batch and retain only the public note fields', async () => {
  const { validateNotes } = await import('../notes-model.js');
  const ids = new Set([3,18]);
  const note = {id:'note-1',placeId:3,title:'A scene',body:'Remember this place.'};
  assert.deepEqual(validateNotes([{...note,unexpected:'ignore'}],ids),[note]);
  assert.throws(()=>validateNotes([note,{...note,id:'note-2',placeId:999}],ids));
  assert.throws(()=>validateNotes([note,note],ids));
  assert.throws(()=>validateNotes([{...note,title:' '}],ids));
  assert.throws(()=>validateNotes({notes:[note]},ids));
});
