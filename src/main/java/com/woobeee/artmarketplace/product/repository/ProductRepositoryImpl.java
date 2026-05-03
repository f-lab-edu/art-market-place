package com.woobeee.artmarketplace.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.woobeee.artmarketplace.auth.entity.QSeller;
import com.woobeee.artmarketplace.product.entity.Product;
import com.woobeee.artmarketplace.product.entity.QProduct;
import com.woobeee.artmarketplace.product.entity.QProductTag;
import com.woobeee.artmarketplace.product.entity.QTag;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

public class ProductRepositoryImpl implements ProductRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public ProductRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<Product> findActiveProductsByFilters(String tagName, String artist, Pageable pageable) {
        QProduct product = QProduct.product;
        BooleanBuilder condition = new BooleanBuilder(product.active.isTrue());

        if (StringUtils.hasText(tagName)) {
            QProductTag productTag = QProductTag.productTag;
            QTag tag = QTag.tag;
            condition.and(JPAExpressions
                    .selectOne()
                    .from(productTag, tag)
                    .where(
                            productTag.productId.eq(product.id),
                            tag.id.eq(productTag.tagId),
                            tag.name.equalsIgnoreCase(tagName)
                    )
                    .exists());
        }

        if (StringUtils.hasText(artist)) {
            QSeller seller = QSeller.seller;
            condition.and(JPAExpressions
                    .selectOne()
                    .from(seller)
                    .where(
                            seller.id.eq(product.sellerId),
                            seller.nickname.containsIgnoreCase(artist)
                    )
                    .exists());
        }

        List<Product> contents = queryFactory
                .selectFrom(product)
                .where(condition)
                .orderBy(product.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(contents, pageable, total == null ? 0L : total);
    }
}
