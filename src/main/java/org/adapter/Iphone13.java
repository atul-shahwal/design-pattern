package org.adapter;

public class Iphone13 {

    private AppleCharger appleCharger;

    public Iphone13(AppleCharger appleCharger) {
        this.appleCharger = appleCharger;
    }

    public void chargePhone(){
        appleCharger.chargePhone();
    }
}
