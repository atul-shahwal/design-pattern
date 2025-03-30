package org.structural.bridge;

import org.structural.bridge.solution.Dog;
import org.structural.bridge.solution.Fish;
import org.structural.bridge.solution.LivingThings;
import org.structural.bridge.solution.breathing.AnimalBreadth;
import org.structural.bridge.solution.breathing.AquaticBreadth;

public class BridgePatternTest {
    public static void main(String[] args) {
        LivingThings fish = new Fish(new AquaticBreadth());
        LivingThings dog = new Dog(new AnimalBreadth());
        fish.breadthProcess();
        dog.breadthProcess();
    }
}
