package org.desingpatterns.basics.structural.bridge.problem;

public class Fish implements LivingThings{
    @Override
    public void breadthProcess() {
        System.out.println("Fish Breadth Logic");
    }
}
