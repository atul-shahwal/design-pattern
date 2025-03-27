package org.behaviouraldesignpattern.strategydesignpattern.model.payment;

public class CreditCardPaymentDetail implements PaymentDetail {
    private int creditCardNumber;
    private String userName;
    private String bank;

    public CreditCardPaymentDetail(int creditCardNumber, String userName, String bank) {
        this.creditCardNumber = creditCardNumber;
        this.userName = userName;
        this.bank = bank;
    }

    public int getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(int creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    @Override
    public String toString() {
        return "CreditCardDetail{" +
                "creditCardNumber=" + creditCardNumber +
                ", userName='" + userName + '\'' +
                ", bank='" + bank + '\'' +
                '}';
    }
}
