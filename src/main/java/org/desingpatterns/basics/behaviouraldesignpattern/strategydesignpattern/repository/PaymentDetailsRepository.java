package org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.repository;

import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.PaymentDetailsRepoException;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.PaymentType;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.payment.PaymentDetail;

import java.util.HashMap;
import java.util.Map;

public class PaymentDetailsRepository {

    private static final Map<String, PaymentDetail> creditCardDetails = new HashMap<>();

    private static final Map<String, PaymentDetail> debitCardDetails = new HashMap<>();

    private static final Map<String, PaymentDetail> upiDetails = new HashMap<>();

    public static PaymentDetail getUserDetailsForPaymentType(String userId, PaymentType paymentType) throws PaymentDetailsRepoException{
        if(paymentType == PaymentType.CREDIT){
            if(creditCardDetails.containsKey(userId)){
                return creditCardDetails.get(userId);
            }
            throw new PaymentDetailsRepoException("No CreditCard found userId :" + userId);
        }
        else if(paymentType == PaymentType.DEBIT) {
            if(debitCardDetails.containsKey(userId)){
                return debitCardDetails.get(userId);
            }
            throw new PaymentDetailsRepoException("No DebitCard found userId :" + userId);
        }
        else {
            if(upiDetails.containsKey(userId)){
                return upiDetails.get(userId);
            }
            throw new PaymentDetailsRepoException("No UPI found userId :" + userId);
        }
    }

    public static void addUserDetailsForPaymentType(String userId, PaymentDetail payment, PaymentType paymentType) {
       if(paymentType == PaymentType.CREDIT){
           creditCardDetails.put(userId, payment);
           return;
       }
       else if(paymentType == PaymentType.DEBIT){
            debitCardDetails.put(userId, payment);
            return;
       }
       upiDetails.put(userId, payment);
    }
}
