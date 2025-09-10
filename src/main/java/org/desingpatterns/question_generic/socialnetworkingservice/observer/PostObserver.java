package org.desingpatterns.question_generic.socialnetworkingservice.observer;

import org.desingpatterns.question_generic.socialnetworkingservice.model.Comment;
import org.desingpatterns.question_generic.socialnetworkingservice.model.Post;
import org.desingpatterns.question_generic.socialnetworkingservice.model.User;

public interface PostObserver {
    void onPostCreated(Post post);
    void onLike(Post post, User user);
    void onComment(Post post, Comment comment);
}