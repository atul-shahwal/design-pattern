package org.structural.adapter;

import org.structural.adapter.logic.WeightMachineImpl;
import org.structural.adapter.logic.adaptor.WeightMachineAdaptor;
import org.structural.adapter.logic.adaptor.WeightMachineAdaptorImpl;

public class AdapterTest {

    public static void main(String[] args) {
        WeightMachineAdaptor weightMachineAdaptor = new WeightMachineAdaptorImpl(new WeightMachineImpl());
        System.out.println(weightMachineAdaptor.getWeightInKg());
    }
}
