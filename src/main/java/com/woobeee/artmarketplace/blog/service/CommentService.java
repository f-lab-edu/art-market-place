package com.woobeee.artmarketplace.blog.service;

import com.woobeee.artmarketplace.blog.api.request.PostCommentRequest;
import com.woobeee.artmarketplace.blog.api.response.GetCommentResponse;

import java.util.List;

public interface CommentService {
    void saveComment(PostCommentRequest request, String loginId);
    void deleteComment(Long commentId, String loginId);
    List<GetCommentResponse> getAllCommentsFromPost(Long postId, String loginId);
}
