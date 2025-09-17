package org.desingpatterns.questions.cabbookingsystem.not_good_for_interview;

import ch.hsr.geohash.GeoHash;

public class GeoHashingConceptForCaching {

    private static final int GEOHASH_PRECISION = 4; // ~20 km area for ~10 km radius

    public static String encode(double lat, double lon) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(lat, lon, GEOHASH_PRECISION);
        return geoHash.toBase32();
    }

    // Haversine formula to calculate distance in km between base and new coordinate
    public static double distanceFromBase(double baseLat, double baseLon, double lat, double lon) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat - baseLat);
        double dLon = Math.toRadians(lon - baseLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(baseLat)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Determine direction relative to base point
    public static String getDirection(double baseLat, double baseLon, double lat, double lon) {
        boolean north = lat > baseLat;
        boolean south = lat < baseLat;
        boolean east = lon > baseLon;
        boolean west = lon < baseLon;

        if (north && east) return "NE";
        if (north && west) return "NW";
        if (south && east) return "SE";
        if (south && west) return "SW";
        if (north) return "N";
        if (south) return "S";
        if (east) return "E";
        if (west) return "W";
        return "Base";
    }

    public static void main(String[] args) {
        double baseLat = 12.9716; // Bangalore city center
        double baseLon = 77.5946;

        System.out.println("Base location: " + baseLat + ", " + baseLon);
        System.out.println("Base geohash: " + encode(baseLat, baseLon));
        System.out.println();

        double[][] testPoints = {
                { baseLat + 0.001, baseLon },       // ~111 m N
                { baseLat - 0.001, baseLon },       // ~111 m S
                { baseLat, baseLon + 0.001 },       // ~88 m E
                { baseLat, baseLon - 0.001 },       // ~88 m W
                { baseLat + 0.01, baseLon },        // ~1.1 km N
                { baseLat - 0.01, baseLon },        // ~1.1 km S
                { baseLat, baseLon + 0.01 },        // ~0.88 km E
                { baseLat, baseLon - 0.01 },        // ~0.88 km W
                { baseLat + 0.05, baseLon + 0.05 }, // ~5.5 km NE
                { baseLat - 0.05, baseLon - 0.05 }, // ~5.5 km SW
                { baseLat + 0.1, baseLon + 0.1 },   // ~11 km NE
                { baseLat - 0.1, baseLon - 0.1 }    // ~11 km SW
        };

        for (double[] point : testPoints) {
            double lat = point[0];
            double lon = point[1];
            String geohash = encode(lat, lon);
            String direction = getDirection(baseLat, baseLon, lat, lon);
            double distance = distanceFromBase(baseLat, baseLon, lat, lon);
            System.out.printf(
                    "Point: %.6f, %.6f -> Direction: %-3s, Distance from base: %.2f km, Geohash: %s%n",
                    lat, lon, direction, distance, geohash
            );
        }
    }
}
