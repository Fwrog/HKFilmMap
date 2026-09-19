export function distanceKm(a, b) {
  const rad = Math.PI / 180;
  const latitude = (b.latitude - a.latitude) * rad;
  const longitude = (b.longitude - a.longitude) * rad;
  const h = Math.sin(latitude / 2) ** 2 + Math.cos(a.latitude * rad) * Math.cos(b.latitude * rad) * Math.sin(longitude / 2) ** 2;
  return 12742 * Math.asin(Math.sqrt(Math.min(1, h)));
}

export function routeDistance(stops) {
  return stops.slice(1).reduce((km, stop, i) => km + distanceKm(stops[i], stop), 0);
}

// Exact open path with a fixed first stop; the Web itinerary is capped at eight.
export function optimiseStops(stops) {
  if (stops.length < 3) return stops;
  const count = stops.length - 1, full = (1 << count) - 1;
  const cost = Array.from({length:full + 1}, () => Array(count).fill(Infinity));
  const previous = Array.from({length:full + 1}, () => Array(count).fill(-1));
  for (let end = 0; end < count; end++) cost[1 << end][end] = distanceKm(stops[0], stops[end + 1]);
  for (let mask = 1; mask <= full; mask++) {
    for (let end = 0; end < count; end++) {
      if (!(mask & (1 << end))) continue;
      const rest = mask ^ (1 << end);
      for (let prior = 0; prior < count; prior++) {
        if (!(rest & (1 << prior))) continue;
        const candidate = cost[rest][prior] + distanceKm(stops[prior + 1], stops[end + 1]);
        if (candidate < cost[mask][end]) {cost[mask][end] = candidate; previous[mask][end] = prior;}
      }
    }
  }
  let end = cost[full].indexOf(Math.min(...cost[full])), mask = full;
  const ordered = [];
  while (end !== -1) {ordered.push(stops[end + 1]); const prior = previous[mask][end]; mask ^= 1 << end; end = prior;}
  return [stops[0], ...ordered.reverse()];
}

export function addStops(route, ids, limit = 8) {
  return [...new Set([...route, ...ids])].slice(0, limit);
}

export function directionsUrl(stops) {
  const coordinate = p => `${p.latitude},${p.longitude}`;
  const query = new URLSearchParams({api: '1', origin: coordinate(stops[0]), destination: coordinate(stops.at(-1)), travelmode: 'walking'});
  if (stops.length > 2) query.set('waypoints', stops.slice(1, -1).map(coordinate).join('|'));
  return `https://www.google.com/maps/dir/?${query}`;
}

export function matchingScenes(scenes, movies, places, {query, genre, district}) {
  const needle = query.trim().toLocaleLowerCase();
  return scenes.filter(scene => {
    const movie = movies.get(scene.movieId), place = places.get(scene.placeId);
    return (!genre || movie.genreGroup === genre) && (!district || place.districtEn === district) &&
      [movie.titleEn, movie.titleZh, movie.director, place.nameEn, place.nameZh, place.districtEn, place.addressEn, place.addressZh, scene.sceneTitleEn, scene.sceneTitleZh, scene.descriptionEn, scene.descriptionZh].join(' ').toLocaleLowerCase().includes(needle);
  });
}
