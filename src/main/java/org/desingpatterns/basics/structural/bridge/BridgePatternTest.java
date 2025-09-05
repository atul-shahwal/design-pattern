package org.desingpatterns.basics.structural.bridge;

import org.desingpatterns.basics.structural.bridge.solution.Dog;
import org.desingpatterns.basics.structural.bridge.solution.Fish;
import org.desingpatterns.basics.structural.bridge.solution.LivingThings;
import org.desingpatterns.basics.structural.bridge.solution.breathing.AnimalBreadth;
import org.desingpatterns.basics.structural.bridge.solution.breathing.AquaticBreadth;

public class BridgePatternTest {
    public static void main(String[] args) {
        LivingThings fish = new Fish(new AquaticBreadth());
        LivingThings dog = new Dog(new AnimalBreadth());
        fish.breadthProcess();
        dog.breadthProcess();
    }
}
