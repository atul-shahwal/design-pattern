package org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern;

import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.logic.CreditCardStrategy;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.logic.PaymentStrategy;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.PaymentType;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.payment.CreditCardPaymentDetail;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.payment.PaymentDetail;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.user.User;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.repository.PaymentDetailsRepository;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.repository.UserRepository;

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
