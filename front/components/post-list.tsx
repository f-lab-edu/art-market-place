"use client"

import { formatDistanceToNow } from "date-fns"
import { ko } from "date-fns/locale"
import { Eye, Heart, MessageCircle, Loader2, RefreshCw, AlertCircle, User } from "lucide-react"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription } from "@/components/ui/alert"
import Link from "next/link"
import { useInfinitePosts } from "@/hooks/use-infinite-posts"
import MinimalScrollToTop from "@/components/minimal-scroll-to-top"
import {Post} from "@/lib/types";

interface PostListProps {
    selectedCategoryId?: number | null
    selectedCategoryName?: string
    isSearchResult?: boolean
    searchQuery?: string
    enableInfiniteScroll?: boolean
    itemsPerPage?: number
    onPostSelect?: (post: Post) => void   // ✅ 추가
}

export default function PostList({
                                     selectedCategoryId,
                                     selectedCategoryName,
                                     isSearchResult,
                                     searchQuery,
                                     enableInfiniteScroll = true,
                                     itemsPerPage = 5,
                                     onPostSelect,
                                 }: PostListProps) {
    const { posts, loading, error, hasMore, loadMore, loadMoreRef, refresh } =
        useInfinitePosts({
            categoryId: selectedCategoryId || undefined,
            search: searchQuery || undefined,
            pageSize: itemsPerPage,
            enabled: enableInfiniteScroll,
        })

    const getTitle = () => {
        if (searchQuery && selectedCategoryName) {
            return `"${searchQuery}" 검색 결과 (카테고리: ${selectedCategoryName})`
        }
        if (searchQuery) {
            return `"${searchQuery}" 검색 결과`
        }
        if (selectedCategoryName) {
            return selectedCategoryName
        }
        return "전체 글"
    }

    // 에러 상태
    if (error && posts.length === 0) {
        return (
            <div className="space-y-4">
                <div className="flex items-center justify-between">
                    <h1 className="text-2xl font-bold">{getTitle()}</h1>
                </div>

                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription className="flex items-center justify-between">
                        <span>{error}</span>
                        <Button variant="outline" size="sm" onClick={refresh} className="ml-4 bg-transparent">
                            <RefreshCw className="h-4 w-4 mr-2" />
                            다시 시도
                        </Button>
                    </AlertDescription>
                </Alert>
            </div>
        )
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">{getTitle()}</h1>
                <div className="flex items-center gap-2">
                    <Button variant="ghost" size="sm" onClick={refresh} disabled={loading}>
                        <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
                    </Button>
                </div>
            </div>

            {/* TOP 버튼 */}
            <MinimalScrollToTop threshold={200} />

            {/* 초기 로딩 상태 */}
            {loading && (
                <div className="flex items-center justify-center py-12">
                    <div className="flex items-center gap-2 text-muted-foreground">
                        <Loader2 className="h-5 w-5 animate-spin" />
                        <span>포스트를 불러오는 중...</span>
                    </div>
                </div>
            )}

            {/* 포스트 목록 */}
            <div className="space-y-3">
                {posts.map((post, index) => (
                    // <Card key={`${post.id}-${index}`} className="cursor-pointer hover:shadow-md transition-shadow">
                    <Card
                        key={`${post.id}-${index}`}
                        className="cursor-pointer hover:shadow-md transition-shadow"
                        onClick={() => onPostSelect?.(post)}   // ✅ 부모 콜백 호출
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                            if (e.key === "Enter" || e.key === " ") {
                                e.preventDefault()
                                onPostSelect?.(post)
                            }
                        }}
                    >
                        <Link href={`/post/${post.id}`}>
                            <CardHeader className="pb-3">
                                <div className="flex items-start justify-between gap-4">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-2">
                                            <span className="text-sm font-medium text-muted-foreground">#{index + 1}</span>
                                            <Badge variant="outline" className="text-xs">
                                                {post.categoryName}
                                            </Badge>

                                        </div>
                                        <h3 className="font-semibold text-lg hover:text-primary transition-colors">{post.title}</h3>

                                        {/* 작성자 정보 */}
                                        <div className="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
                                            <User className="h-3 w-3" />
                                            <span>{post.authorName}</span>
                                        </div>
                                    </div>
                                </div>
                            </CardHeader>

                            <CardContent className="pt-0">
                                <div className="flex items-center justify-between text-sm text-muted-foreground">
                                    <div className="flex items-center gap-4">
                                        <div className="flex items-center gap-1">
                                            <Eye className="h-4 w-4" />
                                            <span>{post.views?.toLocaleString() || 0}</span>
                                        </div>
                                        <div className="flex items-center gap-1">
                                            <Heart className="h-4 w-4" />
                                            <span>{post.likes || 0}</span>
                                        </div>
                                        <div className="flex items-center gap-1">
                                            <MessageCircle className="h-4 w-4" />
                                            {/*<span>{post.commentCount || 0}</span>*/}
                                        </div>
                                    </div>
                                    <span>
                    {post.createdAt &&
                        formatDistanceToNow(new Date(post.createdAt), {
                            addSuffix: true,
                            locale: ko,
                        })}
                  </span>
                                </div>
                            </CardContent>
                        </Link>
                    </Card>
                ))}
            </div>

            {/* 무한 스크롤 로딩 영역 */}
            {enableInfiniteScroll && hasMore && (
                <div ref={loadMoreRef} className="flex flex-col items-center py-8 space-y-4">
                    {loading ? (
                        <div className="flex items-center gap-2 text-muted-foreground">
                            <Loader2 className="h-4 w-4 animate-spin" />
                            <span>더 많은 글을 불러오는 중...</span>
                        </div>
                    ) : (
                        <Button variant="outline" onClick={loadMore} className="w-full max-w-xs bg-transparent">
                            더 보기
                        </Button>
                    )}
                </div>
            )}

            {/* 에러 상태 (일부 데이터가 있는 경우) */}
            {error && posts.length > 0 && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription className="flex items-center justify-between">
                        <span>추가 데이터를 불러오는데 실패했습니다: {error}</span>
                        <Button variant="outline" size="sm" onClick={loadMore} disabled={loading}>
                            <RefreshCw className="h-4 w-4 mr-2" />
                            다시 시도
                        </Button>
                    </AlertDescription>
                </Alert>
            )}

            {/* 모든 글을 다 보여준 경우 */}
            {enableInfiniteScroll && !hasMore && posts.length > 0 && (
                <div className="text-center py-8">
                    <p className="text-muted-foreground">모든 글을 확인했습니다! 🎉</p>
                    <p className="text-sm text-muted-foreground mt-1">총 {posts.length}개의 글</p>
                </div>
            )}

            {/* 글이 없는 경우 */}
            {!loading && posts.length === 0 && !error && (
                <div className="text-center py-12">
                    <p className="text-muted-foreground">{isSearchResult ? "검색 결과가 없습니다." : "글이 없습니다."}</p>
                </div>
            )}
        </div>
    )
}
