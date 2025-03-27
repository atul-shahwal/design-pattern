package org.behaviouraldesignpattern.strategydesignpattern.model.payment;

public class DebitCardPaymentDetail implements PaymentDetail {
    private int debitCardNumber;
    private String userName;
    private String bank;

    public DebitCardPaymentDetail(int debitCardNumber, String userName, String bank) {
        this.debitCardNumber = debitCardNumber;
        this.userName = userName;
        this.bank = bank;
    }

    public int getDebitCardNumber() {
        return debitCardNumber;
    }

    public void setDebitCardNumber(int debitCardNumber) {
        this.debitCardNumber = debitCardNumber;
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
        return "DebitCardPaymentDetail{" +
                "debitCardNumber=" + debitCardNumber +
                ", userName='" + userName + '\'' +
                ", bank='" + bank + '\'' +
                '}';
    }
}
