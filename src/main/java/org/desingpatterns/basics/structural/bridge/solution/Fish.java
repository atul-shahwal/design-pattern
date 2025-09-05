package org.desingpatterns.basics.structural.bridge.solution;

import org.desingpatterns.basics.structural.bridge.solution.breathing.BreadthImplementor;

public class Fish implements LivingThings {

    BreadthImplementor breadthImplementor;

    public Fish(BreadthImplementor breadthImplementor) {
        this.breadthImplementor = breadthImplementor;
    }
    @Override
    public void breadthProcess() {
        breadthImplementor.breadth();
    }
}
