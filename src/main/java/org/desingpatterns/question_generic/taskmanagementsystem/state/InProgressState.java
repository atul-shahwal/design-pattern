package org.desingpatterns.question_generic.taskmanagementsystem.state;

import org.desingpatterns.question_generic.taskmanagementsystem.models.Task;
import org.desingpatterns.question_generic.taskmanagementsystem.enums.TaskStatus;

public class InProgressState implements TaskState {
    @Override
    public void startProgress(Task task) {
        System.out.println("Task is already in progress.");
    }
    @Override
    public void completeTask(Task task) {
        task.setState(new DoneState());
    }
    @Override
    public void reopenTask(Task task) {
        task.setState(new TodoState());
    }
    @Override
    public TaskStatus getStatus() { return TaskStatus.IN_PROGRESS; }
}
