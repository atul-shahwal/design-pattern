package org.desingpatterns.basics.behaviouraldesignpattern.commandpattern.solution;
//sender
public class ClientAirConditionerSolution {

    public static void main(String[] args) {
        AirConditioner airConditioner = new AirConditioner();

        MyRemoteControl control = new MyRemoteControl();
        control.setCommand(new TurnOnCommand(airConditioner));
        control.pressButton();
        control.setCommand(new TurnOfCommand(airConditioner));
        control.pressButton();
    }
}
