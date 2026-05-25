export const ROUTES = {
  login: "/login",
  signup: "/signup",
  signupVerify: "/signup/verify",
  my: "/my",
  board: {
    free: "/board/free",
    vote: "/board/vote",
    debate: "/board/debate",
    freeWrite: "/board/free/write",
    voteWrite: "/board/vote/write",
    debateWrite: "/board/debate/write",
    freePost: (id: number) => `/board/free/${id}`,
    votePost: (id: number) => `/board/vote/${id}`,
  },
} as const;
