package org.desingpatterns.question_generic.socialnetworkingservice.model;

public class Post extends CommentableEntity {
    public Post(User author, String content) {
        super(author, content);
    }
}