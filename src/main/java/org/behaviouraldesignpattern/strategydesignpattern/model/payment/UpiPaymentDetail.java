package org.behaviouraldesignpattern.strategydesignpattern.model.payment;

public class UpiPaymentDetail {
    private String upiId;
    private String bankName;

    public UpiPaymentDetail(String upiId, String bankName) {
        this.upiId = upiId;
        this.bankName = bankName;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    @Override
    public String toString() {
        return "UpiPaymentDetail{" +
                "upiId='" + upiId + '\'' +
                ", bankName='" + bankName + '\'' +
                '}';
    }
}
