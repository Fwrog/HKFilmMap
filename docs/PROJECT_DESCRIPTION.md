# HKFilmMap Project Report

## Abstract

HKFilmMap is an Android application designed for exploring Hong Kong film locations through an interactive map and a route-planning workflow. The app connects film information, filming places, scene descriptions, map-based points of interest, half-day route generation, check-ins, achievements, and nearby food or coffee recommendations.

The main purpose of the project is to turn static film-location data into an experience that users can actually follow in the city. A user can register or log in, browse Hong Kong film places on a map, open details about movies and scenes, add preferred places to a personal route, generate a half-day film route, refine the route through search and list editing, and then use in-app route preview, check-ins, and recommendations during the trip.

The current packaged seed database contains 68 movies, 97 places, and 142 scene records. Among them, 85 scenes are ready for map display because they have usable coordinates. The app also includes 34 nearby food, coffee, and dessert recommendation records. Although the project is a coursework prototype, it demonstrates how film culture, city geography, and basic route-planning algorithms can be combined in a mobile application.

## 1. Introduction

Hong Kong cinema is strongly connected to real urban spaces. Streets, footbridges, piers, cafes, commercial districts, and residential areas often become part of the meaning of a film scene. However, these places are usually described in text, film reviews, or databases rather than experienced as a route through the city.

HKFilmMap addresses this gap by building a map-based application for film-location exploration. Instead of only listing movies or tourist attractions, the app helps users move between three connected ideas:

| Concept | In the app |
|---|---|
| Film | Movie catalog, poster, title, year, director, genre, and related scenes |
| Place | Map marker, address, district, coordinates, and related movies |
| Route | A personal half-day route made from selected film places |

The project is written as a mobile application rather than a static website because the key use case is location-based exploration. Users are expected to use the app before or during a city walk.

## 2. Project Objectives

The project has five main objectives:

1. To visualize Hong Kong film locations on an interactive map.
2. To allow users to browse film scenes from both movie and place perspectives.
3. To support a personal route list where users can add, remove, reorder, and optimize selected locations.
4. To generate a half-day route using distance, content richness, coordinate confidence, genre preference, and check-in history.
5. To provide check-ins, achievements, and nearby recommendations to make the route more engaging.

In simple terms, the app is designed to answer three user questions:

| User question | HKFilmMap feature |
|---|---|
| "Where were Hong Kong movies filmed?" | Explore map and movie-place markers |
| "What movies are connected to this place?" | Place detail and scene list |
| "How can I visit several of these places in half a day?" | Route Planner and route optimization |

## 3. Target Users and Use Cases

The app is intended for three main user groups.

| User group | Expected need |
|---|---|
| Film fans | They want to visit places connected to their favorite movies. |
| Visitors to Hong Kong | They want a cultural half-day route with a clear theme. |
| Local explorers | They want to rediscover familiar places through film history. |

The central user journey is:

```text
Register or log in
  -> Browse film places on the map
  -> Open movie or place details
  -> Add favorite places to a route
  -> Generate a half-day route
  -> Search for extra preferred places
  -> Edit the route list
  -> Preview navigation, check in, and view recommendations
```

This journey is important because it gives the app a clear structure. The user is not only reading information. They are gradually building an actual route that can be followed in the real city.

## 4. System Overview

HKFilmMap contains two main parts:

1. The Android app, which provides the user interface, map, authentication, route planning, check-ins, and recommendations.
2. The offline backend data pipeline, which prepares the film-location dataset and produces the SQLite seed database used by the app.

The high-level system flow is:

```text
Film-location spreadsheet
  + manual override files
  + optional TMDB metadata
        |
        v
Python data pipeline
        |
        v
SQLite seed database
        |
        v
Android app assets
        |
        v
Mobile app features:
  login, map exploration, movie catalog,
  route planning, check-ins, achievements,
  and nearby recommendations
```

The Android app uses:

| Technology | Role in the project |
|---|---|
| Firebase Authentication | User login, registration, and email-link sign-in |
| Google Maps | Main map interface and location display |
| Google Directions API | Walking polyline for route preview |
| Room / SQLite | Local storage for movies, places, scenes, routes, and check-ins |
| Python scripts | Data cleaning, metadata enrichment, and seed database generation |

## 5. Data Design

The project uses a local SQLite seed database packaged with the Android app. This design allows the core film-location data to be available without requiring a live backend server.

The main database tables are:

| Table | Purpose |
|---|---|
| `movies` | Stores movie titles, Chinese titles, year, director, genre, poster asset, and featured status. |
| `places` | Stores place names, address, district, coordinates, and coordinate status. |
| `scenes` | Connects movies and places through specific scene records. |
| `user_check_ins` | Stores the scenes that the user has checked in to. |
| `route_plans` | Stores route plans, including the default draft route named `My Route`. |
| `route_plan_stops` | Stores the stops in a route and their visit order. |

The most important design decision is that movies, places, and scenes are separated. A movie can have multiple places, and one place can be used by multiple movies. A scene is therefore the connection between one movie and one place.

This structure supports two different levels of interaction:

- Route planning is place-based, so the same place is not added repeatedly.
- Check-in is scene-based, so users can still record progress for specific film scenes.

## 6. Main Functions

### 6.1 Login and Registration

The app starts with a Firebase Authentication flow. Users can sign in using email and password, register a new account, or request an email sign-in link. The login page checks basic input conditions such as empty email, invalid email format, and password length.

After a successful login, the user enters the main map screen. If there is no logged-in user, the app redirects back to the login page. In the current prototype, authentication mainly protects the app entry and prepares the system for future user-specific features such as cloud route sync or shared route collections.

### 6.2 Explore Map

The Explore screen is the main entry point after login. It uses a full-screen Google Map with a bottom sheet. The map shows film-location markers, while the bottom sheet shows place lists, genre filters, progress information, and selected place details.

The user can:

- Browse mapped film places in Hong Kong.
- Filter places by movie genre.
- Tap a marker to open a place summary.
- View all film scenes connected to a place.
- Check in to a scene.
- Add a place to the route.
- Open the full place page or movie detail page.

This layout is suitable for non-linear exploration. The user can move around the map visually and then use the bottom sheet for structured information.

### 6.3 Movie Catalog and Movie Details

The Movie Catalog provides a movie-based browsing path. It lists movies with posters, titles, year, director, genre, and scene count. Genre chips allow the user to filter the catalog.

The Movie Detail page shows the selected movie and all related scenes. From this page, users can open a place detail or add a scene's place to the route. This creates a useful two-way connection:

```text
Place -> related movies
Movie -> related places
```

This is important for a film-location app because some users start from a place, while others start from a favorite movie.

### 6.4 Route Planner

The Route Planner is the working area for building a personal film route. It manages a draft route called `My Route`.

Users can build a route in three ways:

1. Add places from the Explore map.
2. Add places from the Search or Browse panels in the Route Planner.
3. Generate a half-day route automatically.

The route list supports removing a stop, clearing the whole route, long-press drag-and-drop reordering, route optimization, and in-app route preview. Manual order changes are saved back to the local database through the `visitOrder` field.

The Route Planner page is organized into:

| UI section | Purpose |
|---|---|
| Top toolbar | Return to Explore and show the page title |
| Route configuration card | Choose generation mode and run generation or optimization |
| Map preview | Display route start, stops, and route line |
| Tab panel | Switch between My Route, Search, and Browse |
| Fixed bottom buttons | Start navigation preview or clear the route |

### 6.5 Check-ins, Achievements, and Recommendations

Users can check in to film scenes. The check-in record is stored by `sceneId`, so the app can calculate total scene progress and movie coverage.

The achievement system uses simple progress conditions, such as:

- First check-in.
- 5 checked-in scenes.
- 15 checked-in scenes.
- 50 percent scene progress.
- 100 percent scene progress.
- Scenes from 3 different movies.
- 3 route stops.
- 8 route stops.

After a check-in, the app can also show nearby food, coffee, or dessert recommendations. These recommendations are ranked using distance, district match, time of day, price, movie mood, and descriptive keywords.

## 7. Layout and Interaction Design

The app follows a clear screen structure rather than a complex multi-window design.

| Screen | Layout choice | Reason |
|---|---|---|
| Login / Register | Form-based page | Users need a simple authentication entry. |
| Explore | Full map plus bottom sheet | The map gives spatial context, and the bottom sheet gives readable details. |
| Route Planner | Configuration card, map preview, tab list, fixed bottom actions | Route planning needs both automatic generation and manual editing. |
| Movie Catalog | Scrollable movie list with genre chips | Users can browse by movie and filter by genre. |
| Detail pages | Header information plus scene list and map preview | Users need focused information about one movie or place. |
| Achievements | Progress-style list | Users can understand their exploration progress. |

The overall interaction design is intentionally practical. The app does not behave like a marketing page. It behaves like a tool for browsing, selecting, planning, and checking in.

## 8. Algorithm Design

This section is more technical because the main intelligence of the app is in route generation, route optimization, map display, and nearby recommendation ranking.

### 8.1 Map Marker Display Algorithm

A direct display of all markers would create visual clutter, especially in dense urban areas. The app therefore changes the marker style according to the map zoom level.

| Zoom level | Display method |
|---|---|
| Low zoom | Cluster nearby places into grouped markers. |
| Medium zoom | Show one poster-stack marker for each place. |
| High zoom | Fan out movie poster markers for places that contain multiple movies. |

Pseudocode:

```text
Algorithm RenderMapMarkers
Input:
    places: filtered film places
    zoom: current map zoom level
Output:
    markers drawn on the map

1. Clear all existing markers.
2. If zoom < clusterThreshold:
       clusters <- BuildScreenGridClusters(places, zoom)
       For each cluster:
           If cluster has only one place:
               DrawPosterStackMarker(cluster.place)
           Else:
               posters <- SamplePosters(cluster, limit = 3)
               DrawClusterMarker(cluster.center, posters,
                                 cluster.movieCount, cluster.sceneCount)
3. Else if zoom < expandedThreshold:
       For each place in places:
           DrawPosterStackMarker(place)
4. Else:
       For each place in places:
           scenes <- GetUniqueMovieScenes(place)
           If scenes is empty:
               DrawPosterStackMarker(place)
           Else:
               positions <- BuildFanOutPositions(place.center, scenes.count)
               For each scene and matching position:
                   DrawPosterMarker(position, scene.poster)
```

The clustering is based on screen grid cells rather than only geographic distance. This is useful because the user's problem is visual overlap on the current screen.

### 8.2 Half-Day Route Generation Algorithm

The route generation algorithm selects a small set of route-ready film places. It is designed for a half-day trip, usually 4 to 6 stops. The app balances practical distance with film-related value.

The score of each candidate can be represented as:

```text
score =
    distanceScore
  + featuredMovieScore
  + richnessScore
  + coordinateConfidenceScore
  + freshnessScore
  + randomness
  + genreScore
  + themeScore
```

Explanation of the score components:

| Component | Meaning |
|---|---|
| `distanceScore` | Nearby places receive a higher score, especially in Prefer Nearby mode. |
| `featuredMovieScore` | Places connected to featured movies receive extra weight. |
| `richnessScore` | Places with more movies or scenes are considered richer. |
| `coordinateConfidenceScore` | Verified coordinates are preferred over weaker coordinate sources. |
| `freshnessScore` | Unvisited places are encouraged; already checked-in places are penalized. |
| `randomness` | Adds variety, especially in More Random mode. |
| `genreScore` | Rewards candidates matching the selected genre. |
| `themeScore` | Gives extra weight to featured movie places in movie-theme generation. |

Pseudocode:

```text
Algorithm GenerateHalfDayRoute
Input:
    origin: start location
    mode: Prefer Nearby or More Random
    genre: selected genre
    durationHours: planned route duration
    minStops, maxStops: stop count limits
Output:
    ordered route stops

1. previousRoute <- Save current route scene IDs.
2. Clear current draft route.
3. targetMinutes <- Clamp(durationHours * 60, 180, 300).
4. candidates <- SearchRouteCandidates(query = "", genre, limit = 240).
5. scoredCandidates <- empty list.
6. For each candidate in candidates:
       If candidate has no coordinates:
           Continue.
       If candidate is already in the route:
           Continue.
       score <- ScoreCandidate(candidate, origin, mode, genre).
       Add (candidate, score) to scoredCandidates.
7. Sort scoredCandidates by score in descending order.
8. selected <- ChooseCandidatesByTimeBudget(scoredCandidates,
                                            origin,
                                            targetMinutes,
                                            minStops,
                                            maxStops).
9. selected <- RemoveWeakCandidatesIfOverBudget(selected, targetMinutes).
10. If selected is empty:
        Restore(previousRoute).
        Return empty list.
11. ordered <- OptimizeRouteOrder(origin, selected).
12. Insert ordered stops into route_plan_stops with visitOrder.
13. Return ordered.
```

Each stop is assumed to have about 25 minutes of visit time. Transfer time is estimated from distance. Short trips are treated as walking trips, while longer trips use a rough public-transport-style estimate.

### 8.3 Route Order Optimization

After selecting stops, the app needs to decide the visiting order. This is similar to a simplified travelling salesman problem, but the route does not need to return to the start.

The distance between two points is estimated with the Haversine formula:

```text
distance = 2 * R * asin(
    sqrt(
        sin^2((lat2 - lat1) / 2)
        + cos(lat1) * cos(lat2) * sin^2((lng2 - lng1) / 2)
    )
)
```

where `R = 6371 km`.

For 10 or fewer stops, the app uses dynamic programming to find the exact shortest open path from the origin.

```text
State:
    dp[mask][end] =
        shortest distance from origin,
        after visiting the stops in mask,
        and ending at stop end

Transition:
    dp[mask][end] =
        min(dp[mask without end][prev] + distance(prev, end))
```

Pseudocode:

```text
Algorithm OptimizeExact
Input:
    origin: start coordinate
    stops: route stops, n <= 10
Output:
    stops in optimized order

1. Compute dist[i][j] for all pairs of stops.
2. Compute originDist[i] from origin to each stop.
3. Initialize dp[mask][end] to Infinity.
4. For each stop i:
       dp[1 << i][i] <- originDist[i].
5. For each mask:
       For each end included in mask:
           prevMask <- mask without end.
           For each prev included in prevMask:
               candidate <- dp[prevMask][prev] + dist[prev][end].
               If candidate < dp[mask][end]:
                   dp[mask][end] <- candidate.
                   parent[mask][end] <- prev.
6. Select the end with minimum dp[fullMask][end].
7. Backtrack through parent to rebuild the route order.
8. Return the ordered stops.
```

For more than 10 stops, exact dynamic programming becomes too expensive for smooth mobile interaction. The app then uses a heuristic method: nearest neighbor followed by 2-opt improvement.

```text
Algorithm OptimizeHeuristic
Input:
    origin: start coordinate
    stops: route stops, n > 10
Output:
    approximately optimized route order

1. route <- empty list.
2. remaining <- all stops.
3. current <- origin.
4. While remaining is not empty:
       next <- stop in remaining with minimum distance from current.
       Add next to route.
       Remove next from remaining.
       current <- next.
5. improved <- true.
6. While improved:
       improved <- false.
       For each pair of indices i, j:
           before <- distance of affected edges before reversing route[i..j].
           Reverse route[i..j].
           after <- distance of affected edges after reversal.
           If after < before:
               improved <- true.
           Else:
               Reverse route[i..j] again to undo the change.
7. Return route.
```

This design uses an exact method when the route is small and a faster approximate method when the route is larger.

### 8.4 Search and Route Deduplication

The route search function returns places rather than raw scenes. This avoids repeated stops when one place appears in multiple films.

Pseudocode:

```text
Algorithm SearchRouteCandidates
Input:
    query: user keyword
    genre: selected genre
    limit: maximum number of results
Output:
    candidate places

1. normalizedQuery <- Trim(query).
2. If normalizedQuery is empty:
       likeQuery <- "".
   Else:
       likeQuery <- "%" + normalizedQuery + "%".
3. Query movies, places, and scenes.
4. Match fields including:
       place name,
       district,
       address,
       movie English title,
       movie Chinese title,
       genre,
       scene title,
       scene description.
5. Group results by placeId.
6. Compute movieCount, sceneCount, hasFeaturedMovie,
   representative poster, top movie title, and isInRoute.
7. Sort by:
       isInRoute DESC,
       hasFeaturedMovie DESC,
       movieCount DESC,
       placeName ASC.
8. Return the first limit candidates.
```

When a place is added to the route, the app chooses one representative mappable scene for that place.

```text
Algorithm AddPlaceToRoute
Input:
    placeId
Output:
    true if added, false otherwise

1. If the route already contains this place:
       Return false.
2. sceneId <- FindRepresentativeMappableScene(placeId).
3. If sceneId does not exist:
       Return false.
4. visitOrder <- Current maximum visitOrder + 1.
5. Insert (planId, sceneId, visitOrder) into route_plan_stops.
6. Return true.
```

### 8.5 Nearby Recommendation Ranking

Nearby recommendations are ranked using several factors. The aim is not only to find the closest place, but also to suggest a useful break during a film route.

Pseudocode:

```text
Algorithm RecommendNearbyFood
Input:
    origin: current film place
    districtHint: district of the current place
    genreHint: movie genre
    limit: number of recommendations
Output:
    ranked recommendation list

1. candidates <- empty list.
2. For radius in [1.2km, 2.0km, 3.2km, 4.5km]:
       For each item in nearby_food.json:
           distance <- Haversine(origin, item.location).
           If distance <= radius and item is not duplicated:
               Add item to candidates.
       If candidates.count >= limit:
           Break.
3. If candidates are not enough:
       Add same-district fallback items.
4. If candidates are still not enough:
       Add nearest fallback items.
5. For each candidate:
       score <- DistanceScore(distance)
              + DistrictMatchBonus(candidate, districtHint)
              + TimeOfDayBonus(candidate.category, currentHour)
              + GenreMoodBonus(candidate.category, genreHint)
              + PriceBonus(candidate.priceRange)
              + EditorialBoost(candidate.description).
6. Sort candidates by score in descending order.
7. Diversify the list by applying a category penalty
   when too many selected items have the same category.
8. Assign rank values.
9. Return the ranked list.
```

The diversity step is important because a useful route recommendation should not return only coffee shops or only restaurants. It should provide a balanced set of possible breaks.

### 8.6 Achievement Checking

Achievements are checked after user progress changes. The conditions are simple and easy to explain.

```text
Algorithm CheckAchievements
Input:
    checkedInScenes,
    totalScenes,
    checkedInMovies,
    totalMovies,
    routeStops
Output:
    one newly unlocked achievement, if any

1. progressPercent <- checkedInScenes / totalScenes * 100.
2. Build achievement candidates:
       First Step: checkedInScenes >= 1
       Scene Rookie: checkedInScenes >= 5
       Scene Hunter: checkedInScenes >= 15
       Halfway There: progressPercent >= 50
       HK Film Pilgrim: progressPercent >= 100
       Movie Collector: checkedInMovies >= min(totalMovies, 3)
       Route Rookie: routeStops >= 3
       Route Master: routeStops >= 8
3. For each candidate:
       If condition is true and not previously unlocked:
           Mark as unlocked.
           Show achievement dialog.
           Stop checking for this round.
```

Only one achievement is shown at a time to avoid interrupting the user with too many popups.

## 9. Data Pipeline

The backend pipeline prepares the data before it is packaged into the Android app. The pipeline reads the spreadsheet, merges local override files, optionally enriches movie metadata from TMDB, creates database tables, validates the result, and copies the database into the Android assets folder.

Pseudocode:

```text
Algorithm RefreshSeedDatabase
Input:
    hk_movie_locations.xlsx
    movie_overrides.json
    place_overrides.json
    optional TMDB credentials
Output:
    hkfilmmap_seed.db copied to Android assets

1. If TMDB credentials are available:
       Fetch metadata and posters from TMDB.
   Else:
       Use cached or local fallback metadata.
2. Read and clean spreadsheet rows.
3. Deduplicate movies by normalized title.
4. Merge movie metadata:
       title, Chinese title, year, director, genre, poster.
5. Resolve places:
       name, address, district, latitude, longitude, coordStatus.
6. Create scene records connecting movieId and placeId.
7. Mark a scene as map-visible only if its place has usable coordinates.
8. Create SQLite tables.
9. Insert movies, places, scenes, and the initial route plan.
10. Validate:
       no broken movie links,
       no broken place links,
       no map-visible scenes without coordinates.
11. Copy the database to Android app assets.
```

This pipeline is useful because the app data is not hard-coded directly into the interface. The dataset can be refreshed and expanded while keeping the Android app logic mostly unchanged.

## 10. Testing and Verification

The current repository notes that automated tests are not yet included. Therefore, verification for this report is based on code inspection, database inspection, and feature-path checking.

Confirmed project facts include:

| Item | Confirmed value |
|---|---|
| Movies in seed database | 68 |
| Places in seed database | 97 |
| Scene records in seed database | 142 |
| Map-visible scene records | 85 |
| Nearby recommendation records | 34 |
| Authentication method | Firebase email/password and email link |
| Route optimization | Dynamic programming for up to 10 stops; nearest neighbor plus 2-opt above 10 stops |
| Map display | Google Maps markers, clusters, poster stacks, and fan-out display |
| Route drawing | Google Directions walking polyline, with fallback line drawing |

Recommended future tests:

- Database query tests for movie, place, route, and check-in queries.
- Unit tests for Haversine distance, exact route optimization, and heuristic route optimization.
- Tests for half-day route generation under different route modes and time budgets.
- UI smoke tests for login, map filtering, adding stops, check-ins, and route planning.
- Data pipeline validation tests for broken links and missing coordinates.

## 11. Significance of the App

### 11.1 Cultural significance

HKFilmMap gives film locations a spatial form. It helps users understand that films are not only stories on a screen, but also records of real urban spaces. This is especially meaningful for Hong Kong cinema, where the city often plays an important role in the atmosphere of a film.

### 11.2 Tourism and local exploration

The app can support a more meaningful form of tourism. Instead of visiting unrelated famous attractions, users can follow a route based on movies, genres, or districts. This gives the trip a clearer theme and narrative.

### 11.3 Educational value

For a coursework project, HKFilmMap demonstrates how a mobile application can connect cultural content, geographic data, user interaction, and algorithmic decision-making. It can be discussed in relation to GIS, digital humanities, urban culture, tourism studies, and mobile app design.

## 12. Potential Profit and Sustainability Model

HKFilmMap is currently a coursework prototype, not a commercial product. Therefore, the following models should be understood as future possibilities rather than existing revenue streams.

| Potential model | Description | Important concern |
|---|---|---|
| Cultural tourism partnership | Work with tourism boards, film festivals, or cultural organizations to provide official film routes. | The app should preserve cultural value and not become only an advertising tool. |
| Sponsored thematic routes | Offer curated routes such as classic Hong Kong cinema, director-based routes, or district-based routes. | Sponsored content should be clearly labelled. |
| Nearby merchant cooperation | Restaurants or cafes could provide route-based offers or updated information. | Ranking should not be fully sold as advertising, or user trust may decline. |
| Paid content packs | Provide deeper stories, director notes, offline guides, or advanced themed routes. | Copyright and content permissions must be handled carefully. |
| Data or API licensing | Provide structured film-location data to education, research, exhibitions, or tourism services. | Third-party metadata, posters, and film-related materials require legal review. |
| Education and research use | Reuse the project as a case study for GIS, mobile development, or digital humanities. | This may create sustainability through teaching or grants rather than direct user payment. |

A conservative view is that HKFilmMap is more suitable as a cultural technology project than as a purely advertising-based app. Its long-term value would likely come from partnerships, curated content, education, and cultural tourism rather than mass commercial traffic.

## 13. Limitations and Future Work

The current version has several limitations:

- User authentication is implemented, but check-ins and route data are mainly stored locally. Cross-device sync is not yet implemented.
- The repository currently does not include automated tests.
- Some Room database queries are allowed on the main thread, which may need improvement if the dataset grows.
- Nearby recommendations are based on a local JSON file and do not yet include real-time opening hours, ratings, or user feedback.
- Route time estimation is approximate and should not be treated as a full transport-planning system.
- The app provides route preview, but it is not a complete turn-by-turn navigation product.
- Any future profit model must consider copyright, privacy, advertising transparency, and fairness in recommendations.

Future improvements could include:

1. Cloud sync for user routes, check-ins, and achievements.
2. More route themes, such as director routes, genre routes, or district routes.
3. Better time estimation using real transport data.
4. More complete recommendation data with user feedback.
5. Automated tests for the database, algorithms, and main UI flows.
6. A stronger data governance process for film metadata, posters, and location sources.

## 14. Conclusion

HKFilmMap is a mobile app prototype that connects Hong Kong film culture with real city locations. It supports login and registration, map-based POI browsing, movie and place details, half-day route generation, route search and editing, route preview, check-ins, achievements, and nearby recommendations.

The project is not only a list of film places. It is a complete user journey from cultural interest to city movement. From a development perspective, it combines a local seed database, map interaction, route optimization, recommendation ranking, and an offline data pipeline. From a cultural perspective, it shows how film memories can be translated into a walkable and explorable urban experience.
