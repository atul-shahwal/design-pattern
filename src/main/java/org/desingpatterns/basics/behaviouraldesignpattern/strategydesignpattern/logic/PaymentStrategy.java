package org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.logic;

public interface PaymentStrategy {

    boolean pay(String userId,int amount);
}
