package org.behaviouraldesignpattern.statepattern.logic;

public class IdealState implements VendingState{
    @Override
    public void insertCoin(VendingMachine product) {
        //inset coin logic
        System.out.println("IdealState Coin Inserted");
        WorkingState workingState = new WorkingState();
        product.setMachineState(workingState);

    }

    @Override
    public void dispenseItem(VendingMachine product) {
        //not doing anything here
        System.out.println("IdealState dispenseItem");
    }
}
