package org.desingpatterns.basics.behaviouraldesignpattern.commandpattern.solution;

public class MyRemoteControl {

    Command command;

    public MyRemoteControl() {
    }

    public void setCommand(Command command){
        this.command = command;
    }

    public void pressButton(){
        command.execute();
    }
}
