package org.desingpatterns.questions.cabbookingsystem.not_good_for_interview;
import ch.hsr.geohash.GeoHash;
public class GeoHashingConceptForCaching {
    private static final int GEOHASH_PRECISION = 6; // ~1 km area

    public static String encode(double lat, double lon) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(lat, lon, GEOHASH_PRECISION);
        return geoHash.toBase32();
    }

    public static void main(String[] args) {
        double baseLat = 37.7749;    // San Francisco example
        double baseLon = -122.4194;

        System.out.println("Base location: " + baseLat + ", " + baseLon);
        System.out.println("Base geohash: " + encode(baseLat, baseLon));

        // Nearby points in latitude and longitude (approximately within a few hundred meters)
        double[][] nearbyPoints = {
                { baseLat + 0.001, baseLon },   // ~111 meters north
                { baseLat - 0.001, baseLon },   // ~111 meters south
                { baseLat, baseLon + 0.001 },   // ~88 meters east
                { baseLat, baseLon - 0.001 },   // ~88 meters west
                { baseLat + 0.005, baseLon },   // ~555 meters north
                { baseLat + 0.01, baseLon },    // ~1.1 km north
                { baseLat + 0.02, baseLon },    // ~2.2 km north
        };

        for (double[] point : nearbyPoints) {
            double lat = point[0];
            double lon = point[1];
            String geohash = encode(lat, lon);
            System.out.printf("Nearby point: %.6f, %.6f -> Geohash: %s%n", lat, lon, geohash);
        }
    }
}
