export interface Category {
  id: number
  name: string
  count: number
  children?: Category[]
}

export interface Comment {
  id: number
  author: string
  isEditable: boolean
  content: string
  createdAt: Date
  replies: Comment[]
}


// 사용자 관련 타입 추가
export interface User {
  id: string
  email: string
  name: string
  profileImage?: string
  createdAt: Date
}

export interface AuthResponse {
  user: User
  accessToken: string
  refreshToken: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}


export interface ApiResponse<T> {
  header: ApiHeader
  data: T
}

export interface ApiHeader {
  successful: boolean
  message: string
  resultCode: number
}

export interface Post {
  id: number
  title: string
  content: string
  categoryName: string
  categoryId: number
  authorName: string
  views: number
  likes: number
  createdAt: Date
}

export interface GetPostsResponse {
  contents: Post[]
  hasNext: boolean
}

export interface PostsParams {
  page?: number
  size?: number
  categoryId?: number
  q?: string // 검색어
}

export interface GetPostResponse {
  id: number
  title: string
  content: string
  categoryName : string
  categoryId : number
  views: number
  likes: number
  isLiked: boolean
  createdAt: Date
}

export interface GetCommentResponse {
  id: number
  author: string
  content: string
  createdAt: Date
  replies?: GetCommentResponse[]
}

export interface PostCommentRequest {
  postId : number
  parentId : number | null
  content : string
}
