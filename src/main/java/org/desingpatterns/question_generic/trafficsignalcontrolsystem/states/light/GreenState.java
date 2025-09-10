package org.desingpatterns.question_generic.trafficsignalcontrolsystem.states.light;

import org.desingpatterns.question_generic.trafficsignalcontrolsystem.TrafficLight;
import org.desingpatterns.question_generic.trafficsignalcontrolsystem.enums.LightColor;

public class GreenState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.GREEN);
        // After being green, the next state is yellow.
        context.setNextState(new YellowState());
    }
}
