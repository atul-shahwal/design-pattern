package org.structural.decorator.decologic;

import org.structural.decorator.existinglogic.BasePizza;

public class MushRoom implements ToppingDecorator {

    BasePizza basePizza;

    public MushRoom(BasePizza basePizza) {
        this.basePizza = basePizza;
    }

    @Override
    public int cost() {
        return basePizza.cost() + 15;
    }
}
