package org.desingpatterns.basics.behaviouraldesignpattern.commandpattern.problem;
//sender
public class ClientAirConditioner {

    public static void main(String[] args) {
        // process of turning on,off ac are simple but if there is a complex process then user need to be aware of that
        // so sender and receiver are not decoupled
        AirConditioner ac = new AirConditioner();
        ac.turnOnAc();
        ac.setTemperature(25);
        ac.turnOfAc();
    }
}
