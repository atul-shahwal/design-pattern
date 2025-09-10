package org.desingpatterns.questions.fooddeliveryservice.search;

import org.desingpatterns.questions.fooddeliveryservice.entity.Restaurant;

import java.util.List;

public interface RestaurantSearchStrategy {
    List<Restaurant> filter(List<Restaurant> allRestaurants);
}
