package org.desingpatterns.basics.structural.decorator;

import org.desingpatterns.basics.structural.decorator.decologic.ExtraCheese;
import org.desingpatterns.basics.structural.decorator.decologic.MushRoom;
import org.desingpatterns.basics.structural.decorator.existinglogic.BasePizza;
import org.desingpatterns.basics.structural.decorator.existinglogic.FarmHouse;
import org.desingpatterns.basics.structural.decorator.existinglogic.MargaritaPizza;

public class Test {

    public static void main(String[] args) {
        BasePizza order1 = new MushRoom(new ExtraCheese(new FarmHouse()));
        System.out.println(order1.cost());
        BasePizza order2 = new ExtraCheese(new MargaritaPizza());
        System.out.println(order2.cost());
    }
}
