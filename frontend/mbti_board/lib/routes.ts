export const ROUTES = {
  login: "/login",
  signup: "/signup",
  signupVerify: "/signup/verify",
  my: "/my",
  admin: "/admin",
  mbti: {
    list: "/mbti",
    test: "/mbti/test",
  },
  board: {
    free: "/board/free",
    vote: "/board/vote",
    debate: "/board/debate",
    freeWrite: "/board/write",
    voteWrite: "/board/write?type=VOTE",
    debateWrite: "/board/write?type=DEBATE",
    freePost: (id: number) => `/board/free/${id}`,
    votePost: (id: number) => `/board/vote/${id}`,
    debatePost: (id: number) => `/board/debate/${id}`,
  },
} as const;
