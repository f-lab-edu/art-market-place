package com.woobeee.artmarketplace.blog.service;

import com.woobeee.artmarketplace.blog.api.request.PostCategoryRequest;
import com.woobeee.artmarketplace.blog.api.response.GetCategoryResponse;

import java.util.List;

public interface CategoryService {
    void saveCategory(PostCategoryRequest request, Long parentId);
    void deleteCategory(Long categoryId);
    List<GetCategoryResponse> getCategoryList(String locale);
}
