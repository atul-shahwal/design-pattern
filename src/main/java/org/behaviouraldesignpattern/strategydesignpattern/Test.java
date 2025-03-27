package org.behaviouraldesignpattern.strategydesignpattern;

import org.behaviouraldesignpattern.strategydesignpattern.logic.CreditCardStrategy;
import org.behaviouraldesignpattern.strategydesignpattern.logic.PaymentStrategy;
import org.behaviouraldesignpattern.strategydesignpattern.model.PaymentType;
import org.behaviouraldesignpattern.strategydesignpattern.model.payment.CreditCardPaymentDetail;
import org.behaviouraldesignpattern.strategydesignpattern.model.payment.PaymentDetail;
import org.behaviouraldesignpattern.strategydesignpattern.model.user.User;
import org.behaviouraldesignpattern.strategydesignpattern.repository.PaymentDetailsRepository;
import org.behaviouraldesignpattern.strategydesignpattern.repository.UserRepository;

public class Test {
    public static void main(String[] args) {

        User user1 = new User("12345","Ajeet");
        User user2 = new User("12346","Amit");
        User user3 = new User("12347","Raghu");
        UserRepository.addUser(user1);
        UserRepository.addUser(user2);
        UserRepository.addUser(user3);
        PaymentDetail paymentDetailuser1 = new CreditCardPaymentDetail(18309465,"Ajeeet","AXIS");
        PaymentDetailsRepository.addUserDetailsForPaymentType("12345",paymentDetailuser1, PaymentType.CREDIT);
        PaymentStrategy paymentStrategy = new CreditCardStrategy();
        paymentStrategy.pay(user1.getUserId(),20);

    }
}
