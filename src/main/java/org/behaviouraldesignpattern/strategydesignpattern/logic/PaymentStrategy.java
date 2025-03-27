package org.behaviouraldesignpattern.strategydesignpattern.logic;

import org.behaviouraldesignpattern.strategydesignpattern.model.user.User;

public interface PaymentStrategy {

    boolean pay(String userId,int amount);
}
