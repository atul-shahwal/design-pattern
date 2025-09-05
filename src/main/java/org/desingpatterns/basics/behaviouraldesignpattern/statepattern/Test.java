package org.desingpatterns.basics.behaviouraldesignpattern.statepattern;

import org.desingpatterns.basics.behaviouraldesignpattern.statepattern.logic.IdealState;
import org.desingpatterns.basics.behaviouraldesignpattern.statepattern.logic.VendingMachine;

public class Test {

    public static void main(String[] args) {
        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setMachineState(new IdealState());
        vendingMachine.getMachineState().insertCoin(vendingMachine);
        vendingMachine.getMachineState().dispenseItem(vendingMachine);
    }
}
