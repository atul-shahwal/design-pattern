package org.behavioral.observer;

import java.util.List;

public interface Subject {
    void subscribe(Observer observer);
    void unSubscribe(Observer observer);
    void notifyChanges(String title);
}
