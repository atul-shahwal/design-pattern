package org.desingpatterns.question_generic.trafficsignalcontrolsystem.states.light;

import org.desingpatterns.question_generic.trafficsignalcontrolsystem.TrafficLight;
import org.desingpatterns.question_generic.trafficsignalcontrolsystem.enums.LightColor;

public class YellowState implements SignalState {
    @Override
    public void handle(TrafficLight context) {
        context.setColor(LightColor.YELLOW);
        // After being yellow, the next state is red.
        context.setNextState(new RedState());
    }
}
