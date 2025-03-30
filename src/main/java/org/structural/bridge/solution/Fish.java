package org.structural.bridge.solution;

import org.structural.bridge.solution.breathing.BreadthImplementor;

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
