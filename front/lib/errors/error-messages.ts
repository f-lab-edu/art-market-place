
export const errorMessageMap = {
    ko: {
        "comment_needAuthentication": "댓글을 작성,삭제하려면 로그인해야 합니다.",
        "like_needAuthentication": "좋아요를 누르려면 로그인해주세요.",
        "post_imageUploadError" : "게시글 이미지가 업로드가 안되었습니다.",
        "post_notFound": "해당 게시글을 찾을 수 없습니다.",
        "signIn_GoogleTokenNotValid": "유효하지 않은 Google 토큰입니다.",
        "signIn_userConflict": "이미 가입된 사용자입니다.",
        "login_userNotFound": "존재하지 않는 사용자입니다.",
        "login_passwordNotMatch": "비밀번호가 일치하지 않습니다.",

    },
    en: {
        "comment_needAuthentication": "You must be logged in to post or delete a comment.",
        "like_needAuthentication": "Please log in to like this post.",
        "post_imageUploadError" : "The requested image was not uploaded in post",
        "post_notFound": "The requested post was not found.",
        "signIn_GoogleTokenNotValid": "Invalid Google token.",
        "signIn_userConflict": "User already registered.",
        "login_userNotFound": "User not found.",
        "login_passwordNotMatch": "Password does not match.",
    },
} as const