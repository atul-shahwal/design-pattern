package org.desingpatterns.basics.structural.bridge.solution;

import org.desingpatterns.basics.structural.bridge.solution.breathing.BreadthImplementor;

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
