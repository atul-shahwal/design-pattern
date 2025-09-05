package org.desingpatterns.basics.behaviouraldesignpattern.interpreterpattern.logic;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private Map<String,Integer> contextMap = new HashMap<>();

    public void put(String stringValue, int intValue){
        contextMap.put(stringValue, intValue);
    }

    public int get(String stringValue){
        return contextMap.get(stringValue);
    }

}
