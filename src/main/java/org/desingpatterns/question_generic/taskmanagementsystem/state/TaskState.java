package org.desingpatterns.question_generic.taskmanagementsystem.state;

import org.desingpatterns.question_generic.taskmanagementsystem.models.Task;
import org.desingpatterns.question_generic.taskmanagementsystem.enums.TaskStatus;

public interface TaskState {
    void startProgress(Task task);
    void completeTask(Task task);
    void reopenTask(Task task);
    TaskStatus getStatus();
}
