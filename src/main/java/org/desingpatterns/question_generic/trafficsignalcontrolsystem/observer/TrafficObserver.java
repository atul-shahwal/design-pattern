package org.desingpatterns.question_generic.trafficsignalcontrolsystem.observer;

import org.desingpatterns.question_generic.trafficsignalcontrolsystem.enums.Direction;
import org.desingpatterns.question_generic.trafficsignalcontrolsystem.enums.LightColor;

public interface TrafficObserver {
    void update(int intersectionId, Direction direction, LightColor color);
}
