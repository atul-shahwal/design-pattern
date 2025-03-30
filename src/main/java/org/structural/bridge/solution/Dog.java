package org.structural.bridge.solution;

import org.structural.bridge.solution.breathing.BreadthImplementor;

public class Dog implements LivingThings {

    BreadthImplementor breadthImplementor;

    public Dog(BreadthImplementor breadthImplementor) {
        this.breadthImplementor = breadthImplementor;
    }

    @Override
    public void breadthProcess() {
        breadthImplementor.breadth();
    }
}
