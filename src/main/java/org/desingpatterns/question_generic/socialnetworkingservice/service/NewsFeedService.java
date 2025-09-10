package org.desingpatterns.question_generic.socialnetworkingservice.service;

import org.desingpatterns.question_generic.socialnetworkingservice.model.Post;
import org.desingpatterns.question_generic.socialnetworkingservice.model.User;
import org.desingpatterns.question_generic.socialnetworkingservice.strategy.ChronologicalStrategy;
import org.desingpatterns.question_generic.socialnetworkingservice.strategy.NewsFeedGenerationStrategy;

import java.util.List;

public class NewsFeedService {
    private NewsFeedGenerationStrategy strategy;

    public NewsFeedService() {
        this.strategy = new ChronologicalStrategy(); // Default strategy
    }

    public void setStrategy(NewsFeedGenerationStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Post> getNewsFeed(User user) {
        return strategy.generateFeed(user);
    }
}