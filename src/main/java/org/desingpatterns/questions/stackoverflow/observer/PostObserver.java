package org.desingpatterns.questions.stackoverflow.observer;

import org.desingpatterns.questions.stackoverflow.entities.Event;

public interface PostObserver {
    void onPostEvent(Event event);
}
