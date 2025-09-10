package org.desingpatterns.question_generic.trafficsignalcontrolsystem.states.intersection;

import org.desingpatterns.question_generic.trafficsignalcontrolsystem.IntersectionController;

public interface IntersectionState {
    void handle(IntersectionController context) throws InterruptedException;
}
