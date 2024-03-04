package org.behavioral.observer;

public class Subscriber implements Observer{

    private String name;

    public Subscriber(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public void notified(String title) {
        System.out.println("Hello "+ this.name + " new video uploaded "+ "title "+title);
    }
}
