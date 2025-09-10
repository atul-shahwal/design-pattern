package org.desingpatterns.question_generic.socialnetworkingservice;

import org.desingpatterns.question_generic.socialnetworkingservice.model.Post;
import org.desingpatterns.question_generic.socialnetworkingservice.model.User;
import org.desingpatterns.question_generic.socialnetworkingservice.observer.UserNotifier;
import org.desingpatterns.question_generic.socialnetworkingservice.service.NewsFeedService;
import org.desingpatterns.question_generic.socialnetworkingservice.service.PostService;
import org.desingpatterns.question_generic.socialnetworkingservice.service.UserService;

import java.util.List;

public class SocialNetworkFacade {
    private final UserService userService;
    private final PostService postService;
    private final NewsFeedService newsFeedService;

    public SocialNetworkFacade() {
        this.userService = new UserService();
        this.postService = new PostService();
        this.newsFeedService = new NewsFeedService();
        // Wire up the observer
        postService.addObserver(new UserNotifier());
    }

    public User createUser(String name, String email) {
        return userService.createUser(name, email);
    }

    public void addFriend(String userId1, String userId2) {
        userService.addFriend(userId1, userId2);
    }

    public Post createPost(String authorId, String content) {
        User author = userService.getUserById(authorId);
        return postService.createPost(author, content);
    }

    public void addComment(String userId, String postId, String content) {
        User user = userService.getUserById(userId);
        postService.addComment(user, postId, content);
    }

    public void likePost(String userId, String postId) {
        User user = userService.getUserById(userId);
        postService.likePost(user, postId);
    }

    public List<Post> getNewsFeed(String userId) {
        User user = userService.getUserById(userId);
        return newsFeedService.getNewsFeed(user);
    }
}

