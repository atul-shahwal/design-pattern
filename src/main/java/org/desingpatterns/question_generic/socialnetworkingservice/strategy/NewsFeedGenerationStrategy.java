package org.desingpatterns.question_generic.socialnetworkingservice.strategy;

import org.desingpatterns.question_generic.socialnetworkingservice.model.Post;
import org.desingpatterns.question_generic.socialnetworkingservice.model.User;

import java.util.List;

public interface NewsFeedGenerationStrategy {
    List<Post> generateFeed(User user);
}
