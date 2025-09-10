package org.desingpatterns.questions.stackoverflow.strategy;

import org.desingpatterns.questions.stackoverflow.entities.Question;

import java.util.List;

public interface SearchStrategy {
    List<Question> filter(List<Question> questions);
}
