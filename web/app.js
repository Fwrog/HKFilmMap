import { initNotebook } from './notebook.js';
import { addStops, routeDistance, directionsUrl, matchingScenes, optimiseStops } from './model.js';

const $ = id => document.getElementById(id);
const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
let lang = new URLSearchParams(location.search).get('lang') === 'zh' ? 'zh' : 'en';
const words = {
  en: {
    notes:'My notes',newNote:'＋ New note',exportNotes:'Export',importNotes:'Import',noteTitle:'Title',notePlace:'Place',noteText:'Your note',notePrivacy:'Personal notes stay in this browser. Export a copy to keep them.',saveNote:'Save note',
    skip:'Skip to film library', brandSub:'ANDROID PROJECT · WEB UPDATE', explore:'Explore the city', about:'About the project', eyebrow:'A FIELD GUIDE TO HONG KONG CINEMA', heading:'Hong Kong, on film.', intro:'From a familiar frame to a street you can explore.', genre:'Genre', district:'District', films:'Films', places:'Places', loading:'Loading the film atlas…', showAll:'Show all', harbour:'Victoria Harbour', locations:'Posters mark film locations', yourRoute:'Your itinerary', makeItADay:'MAKE IT A DAY OUT', itinerary:'Your itinerary', routeIntro:'A few scenes. A different way to see Hong Kong.', clear:'Clear', emptyRoute:'Your next scene starts here.', emptyHelp:'Choose a place on the map and add it to your itinerary, or start with a collection below.', collections:'A place to start', centralTitle:'Central, on screen', centralSub:'Markets, escalators & side streets · 4 stops', kowloonTitle:'Across the harbour', kowloonSub:'Chungking Mansions to the waterfront · 3 stops', collectionHint:'Collections add to your current itinerary.', credits:'Data & credits', search:'Film, place or director', allGenres:'All genres', allDistricts:'All districts', results:'results', onMap:'on the map', mapped:'mapped places', notMapped:'No mapped places yet', noResults:'No scenes match your search.', resetFilters:'Reset filters', add:'＋ Add to itinerary', added:'✓ In your itinerary', viewMap:'Open in Google Maps ↗', stops:'stops', maxStops:'8 stops maximum. Remove a stop to add another.', saved:'Saved in this browser.', saveFailed:'Your itinerary is available for this visit, but this browser could not save it.', noStorage:'Browser storage is unavailable. Your itinerary will last for this visit.', km:'km', distanceNote:'Straight-line total, not walking distance. Dotted lines show stop order; use Google Maps to check access and walking directions.', directions:'Walking directions ↗', remove:'Remove', up:'Move up', down:'Move down', close:'Close', historical:'A film record, not an access guarantee. Some places have changed or are private.', verified:'Coordinates reviewed in the project', geocoded:'Approximate geocoded location', unavailable:'Location not yet mapped', noFilmMap:'This film has no map-ready locations in the project dataset yet.', filmPlaces:'Locations in this film', loadError:'The film catalogue could not load. Check your connection and reload.', reload:'Reload', mapWarning:'The basemap could not load. Film locations and your itinerary remain available. Check your connection and reload.'
  },
  zh: {
    notes:'我的笔记',newNote:'＋ 新建笔记',exportNotes:'导出',importNotes:'导入',noteTitle:'标题',notePlace:'地点',noteText:'笔记内容',notePrivacy:'个人笔记保存在当前浏览器，可导出备份。',saveNote:'保存笔记',
    skip:'跳到电影列表', brandSub:'ANDROID 课程项目 · WEB 更新', explore:'探索城市', about:'关于项目', eyebrow:'一份香港电影取景地指南', heading:'银幕里的香港。', intro:'把熟悉的银幕画面，变成可以探索的街道。', genre:'类型', district:'地区', films:'电影', places:'地点', loading:'正在载入电影地图…', showAll:'查看全部', harbour:'维多利亚港', locations:'海报标记取景地', yourRoute:'我的行程', makeItADay:'给自己安排一场城市漫游', itinerary:'我的行程', routeIntro:'几段电影场景，换个角度看香港。', clear:'清空', emptyRoute:'下一段故事，从这里开始。', emptyHelp:'在地图上选择地点，加入行程；也可以从下面的主题路线开始。', collections:'从这里出发', centralTitle:'银幕里的中环', centralSub:'街市、扶梯与小巷 · 4 站', kowloonTitle:'海港的另一边', kowloonSub:'从重庆大厦走向海滨 · 3 站', collectionHint:'主题路线会追加到当前行程。', credits:'数据与鸣谢', search:'搜索电影、地点或导演', allGenres:'全部类型', allDistricts:'全部地区', results:'项结果', onMap:'个地图地点', mapped:'个地图地点', notMapped:'暂无可定位地点', noResults:'没有找到匹配的场景。', resetFilters:'重置筛选', add:'＋ 加入行程', added:'✓ 已加入行程', viewMap:'在 Google Maps 查看 ↗', stops:'站', maxStops:'最多添加 8 站，请移除一站后再添加。', saved:'已保存在当前浏览器。', saveFailed:'当前行程可继续使用，但浏览器未能保存。', noStorage:'浏览器存储不可用，行程仅在本次访问中保留。', km:'公里', distanceNote:'此为直线总距离，并非步行距离。虚线仅连接行程顺序；通行条件与步行路线请在 Google Maps 中确认。', directions:'查看步行路线 ↗', remove:'移除', up:'向前移', down:'向后移', close:'关闭', historical:'电影记录不代表当前可进入，部分地点已变更或属于私人场所。', verified:'项目已核对坐标', geocoded:'地理编码估计位置', unavailable:'地点尚未定位', noFilmMap:'项目数据暂未提供这部电影可用于地图展示的地点。', filmPlaces:'影片取景地', loadError:'电影数据未能加载，请检查网络并重新加载。', reload:'重新加载', mapWarning:'底图未能加载，仍可浏览电影地点和编辑行程。请检查网络后刷新。'
  }
};
const t = key => words[lang][key];
const local = (object, key) => object[key + (lang === 'zh' ? 'Zh' : 'En')] || object[key + 'En'] || object[key + 'Zh'] || '';
const genresZh = {Romance:'爱情', Crime:'犯罪', Action:'动作', Drama:'剧情', Comedy:'喜剧', Various:'综合', 'Sci-Fi':'科幻'};
const districtsZh = {Central:'中环', 'Sheung Wan':'上环', 'Tsim Sha Tsui':'尖沙咀', 'Yau Ma Tei':'油麻地', 'Shek Tong Tsui':'石塘咀', 'Causeway Bay':'铜锣湾', 'Quarry Bay':'鲗鱼涌', 'Sha Tin':'沙田', 'Chai Wan':'柴湾', 'Sham Shui Po':'深水埗', 'Mong Kok':'旺角', 'Kowloon Bay':'九龙湾', 'Tsuen Wan':'荃湾', 'Wan Chai':'湾仔', 'Tuen Mun':'屯门', Aberdeen:'香港仔', 'Shek O':'石澳', '94 To Kwa Wan Road':'土瓜湾道94号'};
const genreName = name => lang === 'zh' ? genresZh[name] || name : name;
const districtName = place => lang === 'zh' ? place.districtZh || districtsZh[place.districtEn] || place.districtEn || '' : place.districtEn || '';
const tours = {central:[3,18,64,67], kowloon:[16,62,9]};
let notebook;
let data, movies, places, map, markers, routeLine, selectedFilm = null, selectedPlace = null, view = 'films', route = [], storageMessage = '';
const isMapped = scene => scene.isMapVisible === 1 && Number.isFinite(places.get(scene.placeId).latitude) && Number.isFinite(places.get(scene.placeId).longitude);
const poster = (movie, className = '') => `<img class="${className}" src="${escape(movie.poster)}" alt="${escape(local(movie, 'title'))}" loading="lazy" width="48" height="69">`;

function filteredScenes() {
  if (view === 'notes') return data.scenes;
  return matchingScenes(data.scenes, movies, places, {query:$('search').value, genre:$('genre').value, district:$('district').value});
}

function translate() {
  document.documentElement.lang = lang === 'zh' ? 'zh-CN' : 'en';
  document.querySelectorAll('[data-i18n]').forEach(node => { node.innerHTML = t(node.dataset.i18n); });
  $('language').textContent = lang === 'zh' ? 'English' : '中文';
  $('language').lang = lang === 'zh' ? 'en' : 'zh';
  $('search').placeholder = t('search'); $('search').setAttribute('aria-label', t('search'));
  const genre = $('genre').value, district = $('district').value;
  $('genre').innerHTML = `<option value="">${t('allGenres')}</option>` + [...new Set(data.movies.map(m => m.genreGroup))].map(g => `<option value="${escape(g)}">${genreName(g)}</option>`).join('');
  $('district').innerHTML = `<option value="">${t('allDistricts')}</option>` + [...new Set(data.places.map(p => p.districtEn).filter(Boolean))].sort().map(d => `<option value="${escape(d)}">${escape(districtName({districtEn:d}))}</option>`).join('');
  $('genre').value = genre; $('district').value = district;
  $('catalogue-count').textContent = lang === 'zh' ? `${data.movies.length} 部电影 · ${data.places.length} 个地点记录 · ${data.scenes.length} 个场景` : `${data.movies.length} films · ${data.places.length} place records · ${data.scenes.length} scenes`;
  render(); renderRoute(); renderDetail();
}

function render() {
  $('notebook-actions').hidden = view !== 'notes';
  $('note-status').hidden = view !== 'notes';
  $('genre').disabled = $('district').disabled = view === 'notes';
  document.querySelectorAll('[data-view]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.view === view)));
  if (view === 'notes') { notebook.render($('search').value); renderMarkers(); return; }
  const scenes = filteredScenes();
  const movieIds = new Set(scenes.map(s => s.movieId));
  const visiblePlaces = [...new Set(scenes.filter(isMapped).map(s => s.placeId))];
  const filmList = data.movies.filter(m => movieIds.has(m.movieId)).sort((a,b) => b.isFeatured-a.isFeatured || a.titleEn.localeCompare(b.titleEn));
  const placeList = visiblePlaces.map(id => places.get(id)).sort((a,b) => local(a,'name').localeCompare(local(b,'name')));
  $('result-count').textContent = `${view === 'films' ? filmList.length : placeList.length} ${t('results')}`;
  if (view === 'films') {
    $('results').innerHTML = filmList.map(movie => {
      const count = new Set(scenes.filter(s => s.movieId === movie.movieId && isMapped(s)).map(s => s.placeId)).size;
      return `<button class="film-row" data-film="${movie.movieId}" aria-pressed="${selectedFilm === movie.movieId}">${poster(movie)}<span><strong>${escape(local(movie,'title'))}</strong><small>${movie.year || '—'} · ${escape(genreName(movie.genreGroup))}</small><span class="film-locations">${count ? `${count} ${t('mapped')}` : t('notMapped')}</span></span><span class="row-arrow" aria-hidden="true">↗</span></button>`;
    }).join('');
  } else {
    $('results').innerHTML = placeList.map(p => `<button class="place-row" data-place="${p.placeId}" aria-pressed="${selectedPlace === p.placeId}"><span class="place-number" aria-hidden="true">⌖</span><span><strong>${escape(local(p,'name'))}</strong><small>${escape(districtName(p))}</small></span></button>`).join('');
  }
  if (!(view === 'films' ? filmList.length : placeList.length)) $('results').innerHTML = `<div class="empty">${t('noResults')}<button id="empty-reset">${t('resetFilters')}</button></div>`;
  renderMarkers();
}

function renderMarkers() {
  const scenes = filteredScenes();
  const mapped = selectedFilm ? scenes.filter(s => s.movieId === selectedFilm && isMapped(s)) : scenes.filter(isMapped);
  const ids = [...new Set(mapped.map(s => s.placeId))];
  $('map-count').textContent = `${ids.length} ${t('onMap')}`;
  markers.clearLayers();
  const groups = new Map();
  [...new Set([...ids, ...route, ...(selectedPlace ? [selectedPlace] : [])])].forEach(id => {
    const p = places.get(id), point = map.latLngToContainerPoint([p.latitude,p.longitude]);
    const key = map.getZoom() < 15 && !route.includes(id) && selectedPlace !== id ? `${Math.floor(point.x/44)},${Math.floor(point.y/44)}` : `place-${id}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(id);
  });
  groups.forEach(group => {
    if (group.length > 1) {
      const points = group.map(id => places.get(id));
      const centre = [points.reduce((sum,p)=>sum+p.latitude,0)/points.length, points.reduce((sum,p)=>sum+p.longitude,0)/points.length];
      L.marker(centre, {icon:L.divIcon({className:'map-pin cluster',html:`<span><b>${group.length}</b></span>`,iconSize:[34,34],iconAnchor:[17,17]}),title:`${group.length} ${t('mapped')}`,alt:`${group.length} ${t('mapped')}`}).addTo(markers).on('click',()=>fitPlaces(group));
      return;
    }
    const id = group[0];
    const p = places.get(id), order = route.indexOf(id);
    const scene = mapped.find(s => s.placeId === id) || data.scenes.find(s => s.placeId === id);
    const thumbnail = order < 0 && map.getZoom() >= 13;
    const icon = L.divIcon({className:`map-pin ${thumbnail ? 'poster-pin' : ''} ${order >= 0 ? 'is-route' : ''} ${selectedPlace === id ? 'is-selected' : ''}`, html:thumbnail ? `${poster(movies.get(scene.movieId))}<i></i>` : `<span><b>${order >= 0 ? order+1 : '•'}</b></span>`, iconSize:thumbnail ? [40,56] : [28,28], iconAnchor:thumbnail ? [20,56] : [14,28]});
    const marker = L.marker([p.latitude,p.longitude], {icon, title:local(p,'name'), alt:local(p,'name'), riseOnHover:true}).addTo(markers);
    marker.getElement().setAttribute('aria-label',local(p,'name'));
    marker.bindTooltip(escape(local(p,'name')), {direction:'top',offset:[0,-26]});
    marker.on('click', () => selectPlace(id));
  });
}

function fitPlaces(ids) {
  if (!ids.length) return;
  map.fitBounds(ids.map(id => {const p=places.get(id);return [p.latitude,p.longitude];}), {paddingTopLeft:[innerWidth > 1050 ? 365 : innerWidth > 700 && !selectedPlace && !selectedFilm ? 316 : 30,85],paddingBottomRight:[innerWidth > 700 && (selectedPlace || selectedFilm || !$('itinerary').hidden) ? 385 : 45,innerWidth <= 700 ? 300 : 70],maxZoom:15,animate:false});
}

function selectPlace(id) {
  selectedPlace = id;
  render(); renderDetail();
  fitPlaces([id]);
}

function renderDetail() {
  const detail = $('detail');
  detail.hidden = !selectedPlace && !selectedFilm;
  if (detail.hidden) return;
  $('itinerary').hidden = true; $('route-toggle').setAttribute('aria-expanded','false');
  const close = `<button class="close-detail" data-close aria-label="${t('close')}">×</button>`;
  if (!selectedPlace) {
    const movie = movies.get(selectedFilm);
    const scenes = filteredScenes().filter(s => s.movieId === selectedFilm);
    detail.innerHTML = `<div class="detail-top"><div><p class="detail-meta">${movie.year || '—'} · ${escape(movie.director)}</p><h2>${escape(local(movie,'title'))}</h2></div>${close}</div><div class="film-cover">${poster(movie)}<p>${escape(local(movie,'title'))}<small>${escape(genreName(movie.genreGroup))} · ${scenes.filter(isMapped).length} ${t('mapped')}</small></p></div><p class="detail-meta">${t('filmPlaces')}</p><div>${scenes.map(s => `<button class="place-row" data-place="${s.placeId}" ${isMapped(s) ? '' : 'disabled'}><span><strong>${escape(local(places.get(s.placeId),'name'))}</strong><small>${isMapped(s) ? escape(local(s,'description')) : t('unavailable')}</small></span><span>↗</span></button>`).join('')}</div>${scenes.some(isMapped) ? '' : `<p class="detail-meta">${t('noFilmMap')}</p>`}`;
    return;
  }
  const place = places.get(selectedPlace), inRoute = route.includes(selectedPlace);
  const scenes = data.scenes.filter(s => s.placeId === selectedPlace && (!selectedFilm || s.movieId === selectedFilm));
  const visibleScenes = scenes.length ? scenes : data.scenes.filter(s => s.placeId === selectedPlace);
  const placeLink = `https://www.google.com/maps/search/?api=1&query=${place.latitude},${place.longitude}`;
  detail.innerHTML = `<div class="detail-top"><div><p class="eyebrow">${escape(districtName(place))}</p><h2>${escape(local(place,'name'))}</h2><p class="detail-meta">${escape(local(place,'address'))}</p></div>${close}</div><div class="scene-list">${visibleScenes.map(s => `<div class="scene">${poster(movies.get(s.movieId))}<div><strong>${escape(local(movies.get(s.movieId),'title'))} · ${movies.get(s.movieId).year || '—'}</strong><p>${escape(local(s,'description'))}</p></div></div>`).join('')}</div><div class="detail-actions"><button class="primary" data-add="${selectedPlace}" ${inRoute ? 'disabled' : ''}>${inRoute ? t('added') : t('add')}</button><a href="${placeLink}" target="_blank" rel="noopener">${t('viewMap')}</a></div><button class="note-place-action" data-note-place="${selectedPlace}">${lang === 'en' ? '＋ Write a note about this place' : '＋ 为这个地点写笔记'}</button><p class="tiny">${t(place.coordStatus === 'verified' ? 'verified' : 'geocoded')} · ${t('historical')}</p>`;
}

function renderRoute() {
  $('route-badge').textContent = route.length;
  $('stop-count').textContent = `${route.length} / 8 ${t('stops')}`;
  $('clear-route').disabled = route.length === 0;
  $('route-empty').hidden = route.length > 0;
  $('route-list').innerHTML = route.map((id,index) => {
    const p = places.get(id);
    return `<li class="route-stop"><span class="stop-number">${index+1}</span><button class="stop-title" data-place="${id}">${escape(local(p,'name'))}</button><br><small>${escape(districtName(p))}</small><div class="stop-actions"><button data-up="${index}" aria-label="${t('up')}: ${escape(local(p,'name'))}" ${index === 0 ? 'disabled' : ''}>↑</button><button data-down="${index}" aria-label="${t('down')}: ${escape(local(p,'name'))}" ${index === route.length-1 ? 'disabled' : ''}>↓</button><button data-remove="${id}" aria-label="${t('remove')}: ${escape(local(p,'name'))}">${t('remove')}</button></div></li>`;
  }).join('');
  const stops = route.map(id => places.get(id));
  $('route-summary').hidden = route.length < 2;
  $('route-summary').innerHTML = route.length < 2 ? '' : `<strong>${routeDistance(stops).toFixed(1)} <small>${t('km')}</small></strong><p>${t('distanceNote')}</p><button class="optimise" data-optimise ${route.length < 3 ? 'disabled' : ''}>${lang === 'en' ? 'Shorten route · keep first stop' : '优化顺序 · 保留首站'}</button><a class="primary" href="${escape(directionsUrl(stops))}" target="_blank" rel="noopener">${t('directions')}</a>`;
  routeLine.setLatLngs(stops.map(p => [p.latitude,p.longitude]));
  $('save-status').textContent = storageMessage ? t(storageMessage) : route.length ? t('saved') : '';
}

function saveRoute() {
  try { localStorage.setItem('hkfilmmap.route.v1',JSON.stringify(route)); storageMessage = ''; }
  catch { storageMessage = 'saveFailed'; }
  render(); renderRoute(); renderDetail();
}

function showAbout() {
  $('about-content').innerHTML = lang === 'en' ? `<p class="eyebrow">A FILM ATLAS, IN YOUR BROWSER</p><h2>Hong Kong, through cinema.</h2><p>HKFilmMap connects films, their scenes and the places behind them. This desktop edition brings the Android coursework project to the web: explore the catalogue, find a place, and put together a city itinerary.</p><p><a href="project.html">Project story & course report ↗</a></p><h3>One catalogue, two experiences</h3><p>The packaged dataset contains ${data.movies.length} films, ${data.places.length} place records and ${data.scenes.length} scenes. Of these, ${data.scenes.filter(isMapped).length} scenes link to ${new Set(data.scenes.filter(isMapped).map(s=>s.placeId)).size} map-ready places. Unmapped records remain in the film catalogue.</p><p>This web edition supports search, filters, movie/place details personal place notes (create, edit, delete, search, import and export), and a browser-saved itinerary with exact straight-line order optimisation (first stop fixed). Android account sign-in, check-ins, food recommendations and automatic half-day route generation remain in the Android app.</p><h3>Before you go</h3><p>These are film-location records, not a guarantee of present-day access. Some venues are historic, approximate, closed or private. The route line connects stops in your chosen order; it is not a pedestrian path. Google Maps opens separately for directions.</p><h3>Data & credits</h3><p>Film and scene records come from the project's bundled SQLite catalogue. Film posters and metadata belong to their respective rights holders; TMDB-derived materials retain their original terms. This product uses the TMDB API but is not endorsed or certified by TMDB.</p><p>Map data © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>. Map interface: <a href="https://leafletjs.com">Leaflet</a>. Type: Newsreader & IBM Plex Sans (SIL Open Font License).</p><p>PolyU LSGI541 · 2025/26 Semester 2 · Coursework project.</p><p><a href="https://github.com/Fwrog/HKFilmMap">Source & documentation ↗</a> · <a href="https://github.com/Fwrog/HKFilmMap/blob/master/THIRD_PARTY_NOTICES.md">Third-party notices ↗</a> · <a href="https://fwrog.github.io/">Yikai Wu ↗</a></p>` : `<p class="eyebrow">浏览器里的香港电影地图</p><h2>沿着电影，走进香港。</h2><p>HKFilmMap 连接电影、场景和真实城市地点。这是 Android 课程项目的桌面 Web 延伸：搜索电影、寻找取景地，再安排一条自己的城市路线。</p><p><a href="project.html?lang=zh">项目介绍与课程报告 ↗</a></p><h3>同一份数据，两种体验</h3><p>项目数据包含 ${data.movies.length} 部电影、${data.places.length} 个地点记录和 ${data.scenes.length} 个场景。其中 ${data.scenes.filter(isMapped).length} 个场景对应 ${new Set(data.scenes.filter(isMapped).map(s=>s.placeId)).size} 个可在地图展示的地点。尚未定位的记录仍保留在电影列表中。</p><p>Web 版提供搜索、筛选、电影与地点详情，以及保存在浏览器中的行程；路线可按直线距离精确优化顺序（固定首站）。账号登录、打卡、美食推荐和自动半日行程生成仍属于 Android 版功能。</p><h3>出发之前</h3><p>数据记录电影取景关联，不代表当前可进入；部分地点已变更、关闭、仅为估计位置或属于私人空间。地图虚线仅表示所选地点的顺序，不是步行路径。步行导航会在 Google Maps 中打开。</p><h3>数据与鸣谢</h3><p>电影与场景来自项目已打包的 SQLite 数据库。海报与电影元数据权利归各自权利人所有，TMDB 衍生内容遵循其原有条款。本产品使用 TMDB API，但未经 TMDB 背书或认证。</p><p>地图数据 © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap 贡献者</a>；地图组件：<a href="https://leafletjs.com">Leaflet</a>。字体：Newsreader、IBM Plex Sans（SIL Open Font License）。</p><p>香港理工大学 LSGI541 · 2025/26 第二学期 · 课程项目。</p><p><a href="https://github.com/Fwrog/HKFilmMap">源码与文档 ↗</a> · <a href="https://github.com/Fwrog/HKFilmMap/blob/master/THIRD_PARTY_NOTICES.md">第三方声明 ↗</a> · <a href="https://fwrog.github.io/zh/">吴艺楷的主页 ↗</a></p>`;
  $('about-dialog').showModal();
}

async function init() {
  const response = await fetch('catalogue.json');
  if (!response.ok) throw new Error(`Catalogue HTTP ${response.status}`);
  data = await response.json();
  movies = new Map(data.movies.map(m=>[m.movieId,m])); places = new Map(data.places.map(p=>[p.placeId,p]));
  const available = new Set(data.scenes.filter(isMapped).map(s=>s.placeId));
  try {
    const saved = JSON.parse(localStorage.getItem('hkfilmmap.route.v1') || '[]');
    route = Array.isArray(saved) ? [...new Set(saved.filter(id=>available.has(id)))].slice(0,8) : [];
  } catch { storageMessage = 'noStorage'; }
  map = L.map('map',{zoomControl:false,minZoom:10,maxZoom:18,zoomAnimation:false,fadeAnimation:false,markerZoomAnimation:false}).setView([22.294,114.168],13);
  L.control.zoom({position:'bottomright'}).addTo(map);
  const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'}).addTo(map);
  tiles.on('tileerror',()=>{$('map-warning').hidden=false;});
  markers = L.layerGroup().addTo(map);
  map.on('moveend',renderMarkers);
  routeLine = L.polyline([],{color:getComputedStyle(document.documentElement).getPropertyValue('--color-route').trim(),weight:3,dashArray:'5 8',interactive:false}).addTo(map);
  notebook = initNotebook({places, available, local, language:()=>lang, selectPlace, onChange:()=>{view='notes';render();}});
  translate();
  map.setView([22.296,114.15],13,{animate:false});
  $('route-toggle').onclick=()=>{const show=$('itinerary').hidden;selectedFilm=null;selectedPlace=null;renderDetail();$('itinerary').hidden=!show;$('route-toggle').setAttribute('aria-expanded',String(show));renderMarkers();};
  $('route-close').onclick=()=>{$('itinerary').hidden=true;$('route-toggle').setAttribute('aria-expanded','false');};
  $('search').addEventListener('input',()=>{selectedFilm=null;selectedPlace=null;render();renderDetail();});
  ['genre','district'].forEach(id=>$(id).addEventListener('change',()=>{selectedFilm=null;selectedPlace=null;render();renderDetail();fitPlaces([...new Set(filteredScenes().filter(isMapped).map(s=>s.placeId))]);}));
  $('reset-map').onclick=()=>{$('search').value='';$('genre').value='';$('district').value='';selectedFilm=null;selectedPlace=null;render();renderDetail();fitPlaces([...new Set(filteredScenes().filter(isMapped).map(s=>s.placeId))]);};
  $('language').onclick=()=>{lang=lang==='en'?'zh':'en'; const url=new URL(location.href);url.searchParams.set('lang',lang);history.replaceState(null,'',url);translate();};
  $('clear-route').onclick=()=>{route=[];saveRoute();};
  $('about-open').onclick=showAbout; $('credits-open').onclick=showAbout;
  document.querySelector('.dialog-close').onclick=()=>$('about-dialog').close();
  document.addEventListener('keydown',event=>{if(event.key==='/' && !['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName) && !$('about-dialog').open){event.preventDefault();$('search').focus();}});
  document.querySelector('.workspace').addEventListener('click',event=>{
    const button=event.target.closest('button'); if(!button || button.disabled)return;
    const d=button.dataset;
    if(d.film){selectedFilm=Number(d.film);selectedPlace=null;render();renderDetail();fitPlaces([...new Set(filteredScenes().filter(s=>s.movieId===selectedFilm && isMapped(s)).map(s=>s.placeId))]);}
    if(d.place)selectPlace(Number(d.place));
    if(d.view){view=d.view;$('search').value='';render();}
    if('close' in d){selectedPlace=null;selectedFilm=null;render();renderDetail();}
    if(button.id==='empty-reset'){$('search').value='';$('genre').value='';$('district').value='';selectedFilm=null;render();}
    if(d.notePlace)notebook.open(Number(d.notePlace));
    if(d.add){if(route.length===8){$('save-status').textContent=t('maxStops');return;}route=addStops(route,[Number(d.add)]);saveRoute();selectedFilm=null;selectedPlace=null;renderDetail();$('itinerary').hidden=false;$('route-toggle').setAttribute('aria-expanded','true');}
    if(d.tour){const ids=tours[d.tour];const combined=new Set([...route,...ids]);if(combined.size>8){$('save-status').textContent=t('maxStops');return;}route=addStops(route,ids);saveRoute();$('itinerary').hidden=false;$('route-toggle').setAttribute('aria-expanded','true');fitPlaces(route);}
    if(d.remove){route=route.filter(id=>id!==Number(d.remove));saveRoute();}
    if('optimise' in d){route=optimiseStops(route.map(id=>places.get(id))).map(p=>p.placeId);saveRoute();fitPlaces(route);}
    if('up' in d || 'down' in d){const index=Number(d.up ?? d.down);const next=index+('up' in d?-1:1);[route[index],route[next]]=[route[next],route[index]];saveRoute();}
  });
  new ResizeObserver(()=>map.invalidateSize({pan:false})).observe($('map'));
}

init().catch(error=>{
  console.error(error);
  $('results').innerHTML=`<div class="empty">${t('loadError')}<button onclick="location.reload()">${t('reload')}</button></div>`;
});
