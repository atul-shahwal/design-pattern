package org.behaviouraldesignpattern.templetepattern;

public abstract class PaymentFlow {

    public abstract void validateRequest();

    public abstract void calculateFee();

    public abstract void debitAmount();

    public abstract void creditAmount();

    //below is the template method which define order of execution.
    public final void sendMoney(){
        //step1
        validateRequest();
        //step2
        debitAmount();
        //step3
        calculateFee();
        //step4
        creditAmount();
    }
}
