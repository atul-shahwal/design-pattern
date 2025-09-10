package org.desingpatterns.question_generic.socialnetworkingservice.service;

import org.desingpatterns.question_generic.socialnetworkingservice.model.Comment;
import org.desingpatterns.question_generic.socialnetworkingservice.model.Post;
import org.desingpatterns.question_generic.socialnetworkingservice.model.User;
import org.desingpatterns.question_generic.socialnetworkingservice.observer.PostObserver;
import org.desingpatterns.question_generic.socialnetworkingservice.repository.PostRepository;

import java.util.ArrayList;
import java.util.List;

public class PostService {
    private final PostRepository postRepository = PostRepository.getInstance();
    private final List<PostObserver> observers = new ArrayList<>();

    public void addObserver(PostObserver observer) { observers.add(observer); }

    public Post createPost(User author, String content) {
        Post post = new Post(author, content);
        postRepository.save(post);
        author.addPost(post);
        observers.forEach(observer -> observer.onPostCreated(post)); // Notify observers
        return post;
    }

    public void likePost(User user, String postId) {
        Post post = postRepository.findById(postId);
        post.addLike(user);
        observers.forEach(observer -> observer.onLike(post, user));
    }

    public void addComment(User author, String commentableId, String content) {
        Comment comment = new Comment(author, content);
        Post post = postRepository.findById(commentableId);
        post.addComment(comment);
        observers.forEach(observer -> observer.onComment(post, comment));
    }
}
