package org.behaviouraldesignpattern.templetepattern;

public class PayToCustomerFlow extends PaymentFlow {
    @Override
    public void validateRequest() {
        System.out.println("validate logic of PayToCustomer");
    }

    @Override
    public void calculateFee() {
        System.out.println("Calculate Fee Logic Usually 0%");
    }

    @Override
    public void debitAmount() {
        System.out.println("Debit Amount Logic to debit money From Merchant");
    }

    @Override
    public void creditAmount() {
        System.out.println("Credit Amount Logic to credit money to Customer");
    }
}
