package org.desingpatterns.questions.fooddeliveryservice.search;

import org.desingpatterns.questions.fooddeliveryservice.entity.Address;
import org.desingpatterns.questions.fooddeliveryservice.entity.Restaurant;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SearchByProximityStrategy implements RestaurantSearchStrategy {
    private final Address userLocation;
    private final double maxDistance;

    public SearchByProximityStrategy(Address userLocation, double maxDistance) {
        this.userLocation = userLocation;
        this.maxDistance = maxDistance;
    }

    @Override
    public List<Restaurant> filter(List<Restaurant> allRestaurants) {
        return allRestaurants.stream()
                // Filter restaurants within the max distance
                .filter(r -> userLocation.distanceTo(r.getAddress()) <= maxDistance)
                // Sort the filtered list by distance (nearest first)
                .sorted(Comparator.comparingDouble(r -> userLocation.distanceTo(r.getAddress())))
                .collect(Collectors.toList());
    }
}
