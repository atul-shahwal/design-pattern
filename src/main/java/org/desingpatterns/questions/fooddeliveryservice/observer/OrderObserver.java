package org.desingpatterns.questions.fooddeliveryservice.observer;


import org.desingpatterns.questions.fooddeliveryservice.order.Order;

public interface OrderObserver {
    void onUpdate(Order order);
}
