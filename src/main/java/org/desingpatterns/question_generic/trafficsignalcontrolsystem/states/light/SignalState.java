package org.desingpatterns.question_generic.trafficsignalcontrolsystem.states.light;

import org.desingpatterns.question_generic.trafficsignalcontrolsystem.TrafficLight;

public interface SignalState {
    void handle(TrafficLight context);
}
