package org.desingpatterns.basics.structural.decorator.decologic;

import org.desingpatterns.basics.structural.decorator.existinglogic.BasePizza;

public class ExtraCheese implements ToppingDecorator{

    BasePizza basePizza;

    public ExtraCheese(BasePizza basePizza) {
        this.basePizza = basePizza;
    }

    @Override
    public int cost() {
        return basePizza.cost() + 10;
    }
}
