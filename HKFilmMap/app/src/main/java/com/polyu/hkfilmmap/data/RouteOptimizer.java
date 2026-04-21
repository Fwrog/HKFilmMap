package com.polyu.hkfilmmap.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteOptimizer {
    private RouteOptimizer() {
    }

    public static List<RouteSceneItem> optimize(double originLat, double originLng, List<RouteSceneItem> input) {
        List<RouteSceneItem> items = new ArrayList<>();
        List<RouteSceneItem> unresolved = new ArrayList<>();
        for (RouteSceneItem item : input) {
            if (item.latitude != null && item.longitude != null) {
                items.add(item);
            } else {
                unresolved.add(item);
            }
        }
        if (items.size() <= 1) {
            List<RouteSceneItem> ordered = new ArrayList<>(items);
            ordered.addAll(unresolved);
            return ordered;
        }
        List<RouteSceneItem> ordered;
        if (items.size() <= 10) {
            ordered = optimizeExact(originLat, originLng, items);
        } else {
            ordered = optimizeHeuristic(originLat, originLng, items);
        }
        ordered.addAll(unresolved);
        return ordered;
    }

    private static List<RouteSceneItem> optimizeExact(double originLat, double originLng, List<RouteSceneItem> items) {
        int n = items.size();
        int fullMask = 1 << n;
        double[][] dist = buildDistanceMatrix(items);
        double[] originDist = buildOriginDistances(originLat, originLng, items);
        double[][] dp = new double[fullMask][n];
        int[][] parent = new int[fullMask][n];
        double inf = Double.MAX_VALUE / 4d;

        for (int mask = 0; mask < fullMask; mask++) {
            for (int end = 0; end < n; end++) {
                dp[mask][end] = inf;
                parent[mask][end] = -1;
            }
        }
        for (int i = 0; i < n; i++) {
            dp[1 << i][i] = originDist[i];
        }
        for (int mask = 1; mask < fullMask; mask++) {
            for (int end = 0; end < n; end++) {
                if ((mask & (1 << end)) == 0) {
                    continue;
                }
                int prevMask = mask ^ (1 << end);
                if (prevMask == 0) {
                    continue;
                }
                for (int prev = 0; prev < n; prev++) {
                    if ((prevMask & (1 << prev)) == 0) {
                        continue;
                    }
                    double candidate = dp[prevMask][prev] + dist[prev][end];
                    if (candidate < dp[mask][end]) {
                        dp[mask][end] = candidate;
                        parent[mask][end] = prev;
                    }
                }
            }
        }

        int last = 0;
        double best = inf;
        int mask = fullMask - 1;
        for (int end = 0; end < n; end++) {
            if (dp[mask][end] < best) {
                best = dp[mask][end];
                last = end;
            }
        }

        List<RouteSceneItem> ordered = new ArrayList<>();
        while (last != -1) {
            ordered.add(items.get(last));
            int next = parent[mask][last];
            mask ^= 1 << last;
            last = next;
        }
        Collections.reverse(ordered);
        return ordered;
    }

    private static List<RouteSceneItem> optimizeHeuristic(double originLat, double originLng, List<RouteSceneItem> items) {
        List<RouteSceneItem> remaining = new ArrayList<>(items);
        List<RouteSceneItem> route = new ArrayList<>();
        double currentLat = originLat;
        double currentLng = originLng;

        while (!remaining.isEmpty()) {
            int bestIndex = 0;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                RouteSceneItem candidate = remaining.get(i);
                double distance = haversine(currentLat, currentLng, candidate.latitude, candidate.longitude);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }
            RouteSceneItem next = remaining.remove(bestIndex);
            route.add(next);
            currentLat = next.latitude;
            currentLng = next.longitude;
        }

        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < route.size() - 2; i++) {
                for (int j = i + 1; j < route.size() - 1; j++) {
                    double before = edgeDistance(originLat, originLng, route, i - 1, i)
                            + edgeDistance(originLat, originLng, route, j, j + 1);
                    reverse(route, i, j);
                    double after = edgeDistance(originLat, originLng, route, i - 1, i)
                            + edgeDistance(originLat, originLng, route, j, j + 1);
                    if (after + 0.001d < before) {
                        improved = true;
                    } else {
                        reverse(route, i, j);
                    }
                }
            }
        }
        return route;
    }

    private static void reverse(List<RouteSceneItem> route, int start, int end) {
        while (start < end) {
            RouteSceneItem temp = route.get(start);
            route.set(start, route.get(end));
            route.set(end, temp);
            start++;
            end--;
        }
    }

    private static double edgeDistance(double originLat, double originLng, List<RouteSceneItem> route, int fromIndex, int toIndex) {
        double fromLat = originLat;
        double fromLng = originLng;
        if (fromIndex >= 0) {
            fromLat = route.get(fromIndex).latitude;
            fromLng = route.get(fromIndex).longitude;
        }
        RouteSceneItem to = route.get(toIndex);
        return haversine(fromLat, fromLng, to.latitude, to.longitude);
    }

    private static double[][] buildDistanceMatrix(List<RouteSceneItem> items) {
        int n = items.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = haversine(items.get(i).latitude, items.get(i).longitude, items.get(j).latitude, items.get(j).longitude);
            }
        }
        return matrix;
    }

    private static double[] buildOriginDistances(double originLat, double originLng, List<RouteSceneItem> items) {
        double[] distances = new double[items.size()];
        for (int i = 0; i < items.size(); i++) {
            distances[i] = haversine(originLat, originLng, items.get(i).latitude, items.get(i).longitude);
        }
        return distances;
    }

    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double earth = 6371.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earth * Math.asin(Math.sqrt(a));
    }
}
