package org.behaviouraldesignpattern.templetepattern;

public class PayToMerchantFlow extends PaymentFlow {
    @Override
    public void validateRequest() {
        System.out.println("validate logic of PayToMerchant");
    }

    @Override
    public void calculateFee() {
        System.out.println("Calculate Fee Logic some amount");
    }

    @Override
    public void debitAmount() {
        System.out.println("Debit Amount Logic to debit money From Customer");
    }

    @Override
    public void creditAmount() {
        System.out.println("Credit Amount Logic to credit money to Merchant");
    }
}
