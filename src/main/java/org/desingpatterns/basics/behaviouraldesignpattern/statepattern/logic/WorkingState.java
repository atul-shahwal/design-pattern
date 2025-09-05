package org.desingpatterns.basics.behaviouraldesignpattern.statepattern.logic;

public class WorkingState implements VendingState{

    @Override
    public void insertCoin(VendingMachine product) {
        System.out.println("Inside Working state");
        //not doing anything here
    }

    @Override
    public void dispenseItem(VendingMachine product) {
        //dispensing state
        System.out.println("WorkingState Product dispensed");
        //set any other state if applicable.
    }
}
