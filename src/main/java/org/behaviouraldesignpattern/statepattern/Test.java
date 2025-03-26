package org.behaviouraldesignpattern.statepattern;

import org.behaviouraldesignpattern.statepattern.logic.IdealState;
import org.behaviouraldesignpattern.statepattern.logic.VendingMachine;

public class Test {

    public static void main(String[] args) {
        VendingMachine vendingMachine = new VendingMachine();
        vendingMachine.setMachineState(new IdealState());
        vendingMachine.getMachineState().insertCoin(vendingMachine);
        vendingMachine.getMachineState().dispenseItem(vendingMachine);
    }
}
