import type { IconType } from "react-icons";
import {
  MdHome, MdChatBubbleOutline, MdHowToVote, MdForum,
  MdPerson, MdArticle, MdPsychology, MdQuiz, MdManageAccounts, MdBarChart,
  MdDashboard, MdPeople, MdComment,
} from "react-icons/md";
import { ROUTES } from "./routes";
import { POST_TYPE, type PostType } from "@/types/post";

export type BoardTab = { label: string; href: string; type: PostType | null; icon: IconType };
export type NavTab   = { label: string; href: string; icon: IconType };


/* --------------------------------------------------일반---------------------------------------------------------------- */
export const BOARD_TABS: BoardTab[] = [
  { label: "전체",        href: ROUTES.board.all,    type: null,              icon: MdHome },
  { label: "자유 게시판", href: ROUTES.board.free,   type: POST_TYPE.FREE,   icon: MdChatBubbleOutline },
  { label: "투표 게시판", href: ROUTES.board.vote,   type: POST_TYPE.VOTE,   icon: MdHowToVote },
  { label: "토론 게시판", href: ROUTES.board.debate, type: POST_TYPE.DEBATE, icon: MdForum },
];

export const EXTRA_TABS: NavTab[] = [
  { label: "MBTI 검사", href: ROUTES.mbti.test, icon: MdQuiz },
  { label: "MBTI 유형", href: ROUTES.mbti.list, icon: MdPsychology },
];

export const STATS_TABS: NavTab[] = [
  { label: "질문 통계", href: ROUTES.mbti.funStats, icon: MdBarChart },
];

/* --------------------------------------------------마이페이지---------------------------------------------------------------- */
export const MY_TABS: NavTab[] = [
  { label: "마이 페이지",  href: ROUTES.my,        icon: MdPerson },
  { label: "내 게시글",   href: ROUTES.myPosts,   icon: MdArticle },
  { label: "계정 관리",   href: ROUTES.myAccount, icon: MdManageAccounts },
];

/* --------------------------------------------------어드민---------------------------------------------------------------- */
export const ADMIN_DASHBOARD_TABS: NavTab[] = [
  { label: "대시보드", href: ROUTES.admin, icon: MdDashboard },
];

export const ADMIN_USER_TABS: NavTab[] = [
  { label: "유저 관리", href: ROUTES.adminUsers,     icon: MdPeople },
  { label: "유저 통계", href: ROUTES.adminUserStats, icon: MdBarChart },
];

export const ADMIN_POST_TABS: NavTab[] = [
  { label: "게시글 관리", href: ROUTES.adminPosts,     icon: MdArticle },
  { label: "게시글 통계", href: ROUTES.adminPostStats, icon: MdBarChart },
];

export const ADMIN_COMMENT_TABS: NavTab[] = [
  { label: "댓글 관리", href: ROUTES.adminComments,     icon: MdComment },
  { label: "댓글 통계", href: ROUTES.adminCommentStats, icon: MdBarChart },
];

export const ADMIN_MBTI_TABS: NavTab[] = [
  { label: "MBTI 문제 관리", href: ROUTES.adminMbti,     icon: MdQuiz },
  { label: "MBTI 검사 통계", href: ROUTES.adminMbtiStats, icon: MdBarChart },
];
