package org.behaviouraldesignpattern.statepattern.logic;

public class VendingMachine {

    VendingState machineState;

    public VendingState getMachineState() {
        return machineState;
    }

    public void setMachineState(VendingState machineState) {
        this.machineState = machineState;
    }
}
