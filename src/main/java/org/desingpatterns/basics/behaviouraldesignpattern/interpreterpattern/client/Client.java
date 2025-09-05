package org.desingpatterns.basics.behaviouraldesignpattern.interpreterpattern.client;

import org.desingpatterns.basics.behaviouraldesignpattern.interpreterpattern.logic.AbstractExpression;
import org.desingpatterns.basics.behaviouraldesignpattern.interpreterpattern.logic.Context;
import org.desingpatterns.basics.behaviouraldesignpattern.interpreterpattern.logic.MultiplyTerminalExpression;
import org.desingpatterns.basics.behaviouraldesignpattern.interpreterpattern.logic.NumberTerminalExpression;

public class Client {

    public static void main(String[] args) {
        Context context = new Context();
        context.put("a",2);
        context.put("b",4);
        //a*b
        AbstractExpression expression1 = new MultiplyTerminalExpression(
                new NumberTerminalExpression("a"),
                new NumberTerminalExpression("b")
        );
        System.out.println(expression1.interpret(context));
    }
}
