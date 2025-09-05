package org.desingpatterns.basics.behaviouraldesignpattern.commandpattern.solution;


public class TurnOfCommand implements Command {

    AirConditioner airConditioner;

    public TurnOfCommand(AirConditioner airConditioner) {
        this.airConditioner = airConditioner;
    }

    @Override
    public void execute() {
        airConditioner.turnOfAc();
    }
}
