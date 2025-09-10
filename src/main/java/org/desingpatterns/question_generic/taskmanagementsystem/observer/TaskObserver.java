package org.desingpatterns.question_generic.taskmanagementsystem.observer;

import org.desingpatterns.question_generic.taskmanagementsystem.models.Task;

public interface TaskObserver {
    void update(Task task, String changeType);
}
