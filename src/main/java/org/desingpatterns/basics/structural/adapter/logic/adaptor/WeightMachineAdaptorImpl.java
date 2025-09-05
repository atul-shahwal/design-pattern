package org.desingpatterns.basics.structural.adapter.logic.adaptor;

import org.desingpatterns.basics.structural.adapter.logic.WeightMachine;

public class WeightMachineAdaptorImpl implements WeightMachineAdaptor {

    WeightMachine weightMachine;

    public WeightMachineAdaptorImpl(WeightMachine weightMachine) {
        this.weightMachine = weightMachine;
    }

    @Override
    public int getWeightInKg() {
        return (weightMachine.getWeightInPounds() * 2);
    }
}
