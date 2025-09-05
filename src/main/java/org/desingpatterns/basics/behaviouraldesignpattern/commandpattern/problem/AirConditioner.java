package org.desingpatterns.basics.behaviouraldesignpattern.commandpattern.problem;
//Receiver
public class AirConditioner {
    boolean isOn;
    int temperature;

    public void turnOnAc(){
        isOn = true;
        System.out.println("AC is ON");
    }

    public void turnOfAc(){
        isOn = false;
        System.out.println("AC is OF");
    }

    public void setTemperature(int temperature){
        this.temperature = temperature;
        System.out.println("Temperature Changed to : " + this.temperature);
    }
}
