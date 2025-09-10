package org.desingpatterns.question_generic.taskmanagementsystem.strategy;

import org.desingpatterns.question_generic.taskmanagementsystem.models.Task;

import java.util.Comparator;
import java.util.List;

public class SortByDueDate implements TaskSortStrategy {
    @Override
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparing(Task::getDueDate));
    }
}