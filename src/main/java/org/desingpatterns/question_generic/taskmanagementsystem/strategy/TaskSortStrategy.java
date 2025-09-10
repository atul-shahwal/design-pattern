package org.desingpatterns.question_generic.taskmanagementsystem.strategy;

import org.desingpatterns.question_generic.taskmanagementsystem.models.Task;

import java.util.List;

public interface TaskSortStrategy {
    void sort(List<Task> tasks);
}
