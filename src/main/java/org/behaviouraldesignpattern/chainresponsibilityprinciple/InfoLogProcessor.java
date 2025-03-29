package org.behaviouraldesignpattern.chainresponsibilityprinciple;

public class InfoLogProcessor extends LogProcessor{

    private boolean canIHandle = false;

    public InfoLogProcessor(LogProcessor logProcessor,boolean canIHandle) {
        super(logProcessor);
        this.canIHandle = true;
    }

    @Override
    void log(String message){
        if(canIHandle){
            //process the message
        }else {
            super.log(message);
        }
    }
}
