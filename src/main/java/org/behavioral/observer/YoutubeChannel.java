package org.behavioral.observer;

import java.util.ArrayList;
import java.util.List;

public class YoutubeChannel implements Subject{

    List<Observer> subscribers = new ArrayList<>();

    @Override
    public void subscribe(Observer observer) {
        this.subscribers.add(observer);
    }

    @Override
    public void unSubscribe(Observer observer) {
        this.subscribers.remove(observer);
    }

    @Override
    public void notifyChanges(String title) {
        for(Observer ob: this.subscribers){
            ob.notified(title);
        }

    }
}
