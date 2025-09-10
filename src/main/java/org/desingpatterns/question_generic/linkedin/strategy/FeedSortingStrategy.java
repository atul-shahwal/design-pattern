package org.desingpatterns.question_generic.linkedin.strategy;

import org.desingpatterns.question_generic.linkedin.entities.Post;

import java.util.List;

public interface FeedSortingStrategy {
    List<Post> sort(List<Post> posts);
}
