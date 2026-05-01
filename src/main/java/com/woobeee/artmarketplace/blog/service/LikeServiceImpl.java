package com.woobeee.artmarketplace.blog.service;

import com.woobeee.artmarketplace.blog.entity.Likes;
import com.woobeee.artmarketplace.blog.exception.CustomAuthenticationException;
import com.woobeee.artmarketplace.blog.exception.CustomNotFoundException;
import com.woobeee.artmarketplace.blog.exception.ErrorCode;
import com.woobeee.artmarketplace.blog.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class LikeServiceImpl implements LikeService{
    private final LikeRepository likeRepository;
    private final AuthMemberResolver authMemberResolver;

    @Override
    public void saveLike(Long postId, String loginId) {

        if (loginId == null) {
            throw new CustomAuthenticationException(ErrorCode.like_needAuthentication);
        }

        AuthMemberResolver.MemberIdentity memberIdentity = authMemberResolver.requireByLoginId(loginId);

        if (likeRepository.existsByMemberIdAndMemberRoleAndPostId(
                memberIdentity.memberId(),
                memberIdentity.role(),
                postId
        )) {
            return;
        }

        Likes like = new Likes(memberIdentity.memberId(), memberIdentity.role(), postId);
        likeRepository.save(like);
    }

    @Override
    public void deleteLike(Long postId, String loginId) {
        if (loginId == null) {
            throw new CustomAuthenticationException(ErrorCode.like_needAuthentication);
        }

        AuthMemberResolver.MemberIdentity memberIdentity = authMemberResolver.requireByLoginId(loginId);

        Likes like = likeRepository
                .findByMemberIdAndMemberRoleAndPostId(memberIdentity.memberId(), memberIdentity.role(), postId)
                .orElseThrow();

        likeRepository.delete(like);
    }
}
