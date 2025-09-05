package org.desingpatterns.basics.structural.adapter;

import org.desingpatterns.basics.structural.adapter.logic.WeightMachineImpl;
import org.desingpatterns.basics.structural.adapter.logic.adaptor.WeightMachineAdaptor;
import org.desingpatterns.basics.structural.adapter.logic.adaptor.WeightMachineAdaptorImpl;

public class AdapterTest {

    public static void main(String[] args) {
        WeightMachineAdaptor weightMachineAdaptor = new WeightMachineAdaptorImpl(new WeightMachineImpl());
        System.out.println(weightMachineAdaptor.getWeightInKg());
    }
}
