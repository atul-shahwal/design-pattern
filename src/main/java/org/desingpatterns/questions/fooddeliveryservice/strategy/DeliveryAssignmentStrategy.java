package org.desingpatterns.questions.fooddeliveryservice.strategy;

import org.desingpatterns.questions.fooddeliveryservice.entity.DeliveryAgent;
import org.desingpatterns.questions.fooddeliveryservice.order.Order;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentStrategy {
    Optional<DeliveryAgent> findAgent(Order order, List<DeliveryAgent> agents);
}
