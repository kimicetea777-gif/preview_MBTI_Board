export const API = {
  auth: {
    login: "/api/auth/login",
    signup: "/api/auth/signup",
    reissue: "/api/auth/reissue",
    logout: "/api/auth/logout",
    emailVerifySend: "/api/auth/email-verify/send",
    emailVerifyConfirm: "/api/auth/email-verify/confirm",
  },
  users: {
    me: "/api/users/me",
    mePassword: "/api/users/me/password",
    meWithdrawalVerify: "/api/users/me/withdrawal-verify",
    byId: (id: string) => `/api/users/${id}`,
  },
  mbti: {
    list: "/api/mbti",
  },
  posts: {
    list: "/api/posts",
    byId: (id: number) => `/api/posts/${id}`,
  },
  comments: {
    byPostId: (postId: number) => `/api/posts/${postId}/comments`,
    byId: (postId: number, commentId: number) => `/api/posts/${postId}/comments/${commentId}`,
    reactions: (commentId: number) => `/api/comments/${commentId}/reactions`,
  },
} as const;
