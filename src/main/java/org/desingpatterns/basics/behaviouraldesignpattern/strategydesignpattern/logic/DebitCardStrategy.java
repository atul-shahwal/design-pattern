package org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.logic;

import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.PaymentDetailsRepoException;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.PaymentType;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.repository.PaymentDetailsRepository;

import java.util.Arrays;

public class DebitCardStrategy implements PaymentStrategy{
    @Override
    public boolean pay(String userId,int amount) {
        System.out.println("Making payment Through Debit Card");
        try {
            PaymentDetailsRepository.getUserDetailsForPaymentType(userId, PaymentType.DEBIT);
            //logic to debit amount
            return true;
        } catch (PaymentDetailsRepoException exception){
            System.out.println(exception.getMessage());
            System.out.println(Arrays.toString(exception.getStackTrace()));
            return false;
        } catch (Exception exception){
            System.out.println(exception.getMessage());
        }
        return false;
    }
}
