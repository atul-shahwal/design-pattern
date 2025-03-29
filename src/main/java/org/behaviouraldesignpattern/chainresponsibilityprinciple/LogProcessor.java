package org.behaviouraldesignpattern.chainresponsibilityprinciple;

public class LogProcessor {

    LogProcessor nextLogProcessor;

    public LogProcessor(LogProcessor logProcessor) {
        this.nextLogProcessor = logProcessor;
    }
    void log(String message){
        if(nextLogProcessor != null){
            nextLogProcessor.log(message);
        }
    }

}
