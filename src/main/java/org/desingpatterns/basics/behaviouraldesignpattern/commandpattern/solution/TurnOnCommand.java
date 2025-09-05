package org.desingpatterns.basics.behaviouraldesignpattern.commandpattern.solution;

public class TurnOnCommand implements Command{

    AirConditioner airConditioner;

    public TurnOnCommand(AirConditioner airConditioner) {
        this.airConditioner = airConditioner;
    }

    @Override
    public void execute() {
        airConditioner.turnOnAc();
    }
}
