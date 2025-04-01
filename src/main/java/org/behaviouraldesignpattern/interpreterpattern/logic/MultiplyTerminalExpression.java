package org.behaviouraldesignpattern.interpreterpattern.logic;

public class MultiplyTerminalExpression implements AbstractExpression {

    AbstractExpression leftTerminalExpression;

    AbstractExpression rightTerminalExpression;

    public MultiplyTerminalExpression(AbstractExpression leftTerminalExpression, AbstractExpression rightTerminalExpression) {
        this.leftTerminalExpression = leftTerminalExpression;
        this.rightTerminalExpression = rightTerminalExpression;
    }

    @Override
    public int interpret(Context context) {
        return (leftTerminalExpression.interpret(context) * rightTerminalExpression.interpret(context));
    }
}
