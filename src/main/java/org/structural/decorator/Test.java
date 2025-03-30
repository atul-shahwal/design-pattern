package org.structural.decorator;

import org.structural.decorator.decologic.ExtraCheese;
import org.structural.decorator.decologic.MushRoom;
import org.structural.decorator.existinglogic.BasePizza;
import org.structural.decorator.existinglogic.FarmHouse;
import org.structural.decorator.existinglogic.MargaritaPizza;

public class Test {

    public static void main(String[] args) {
        BasePizza order1 = new MushRoom(new ExtraCheese(new FarmHouse()));
        System.out.println(order1.cost());
        BasePizza order2 = new ExtraCheese(new MargaritaPizza());
        System.out.println(order2.cost());
    }
}
