package org.desingpatterns.basics.structural.bridge.solution.breathing;

public class AnimalBreadth implements BreadthImplementor{

    @Override
    public void breadth() {
        System.out.println("Logic for Animal Breathing");
    }
}
