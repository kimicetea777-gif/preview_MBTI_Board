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
    mbtiHistory: "/api/users/me/mbti/history",
  },
  mbti: {
    list: "/api/mbti",
    byId: (id: string) => `/api/mbti/${id}`,
    testQuestions: "/api/mbti/test/questions",
    testSubmit: "/api/mbti/test/submit",
  },
  posts: {
    list: "/api/posts",
    byId: (id: number) => `/api/posts/${id}`,
    voteCreate: "/api/posts/vote",
    debateCreate: "/api/posts/debate",
  },
  comments: {
    byPostId: (postId: number) => `/api/posts/${postId}/comments`,
    byId: (postId: number, commentId: number) => `/api/posts/${postId}/comments/${commentId}`,
    reactions: (commentId: number) => `/api/comments/${commentId}/reactions`,
  },
  votes: {
    cast: (postId: number) => `/api/posts/${postId}/vote`,
    result: (postId: number) => `/api/posts/${postId}/vote/result`,
  },
  admin: {
    users: "/api/admin/users",
    userState: (id: string) => `/api/admin/users/${id}/state`,
    userRole: (id: string) => `/api/admin/users/${id}/role`,
    posts: "/api/admin/posts",
    postVisibility: (id: number) => `/api/admin/posts/${id}/visibility`,
    comments: "/api/admin/comments",
    commentVisibility: (id: number) => `/api/admin/comments/${id}/visibility`,
  },
} as const;
