package org.behaviouraldesignpattern.statepattern.logic;

public interface VendingState {

    void insertCoin(VendingMachine product);

    void dispenseItem(VendingMachine product);

}
