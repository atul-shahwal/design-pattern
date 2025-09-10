package org.desingpatterns.question_generic.linkedin.observer;

import org.desingpatterns.question_generic.linkedin.entities.Notification;

public interface NotificationObserver {
    void update(Notification notification);
}
