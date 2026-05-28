"use client";

import { useState } from "react";
import AdminUserTable from "./AdminUserTable";
import AdminPostTable from "./AdminPostTable";
import AdminCommentTable from "./AdminCommentTable";

type AdminTab = "users" | "posts" | "comments";

const TABS: { key: AdminTab; label: string }[] = [
  { key: "users", label: "유저 관리" },
  { key: "posts", label: "게시글 관리" },
  { key: "comments", label: "댓글 관리" },
];

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<AdminTab>("users");

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-8">
        <h1 className="mb-6 text-2xl font-bold text-gray-900">관리자 대시보드</h1>

        <nav className="mb-6 flex border-b border-gray-200">
          {TABS.map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={`px-5 py-3 text-sm font-medium transition-colors ${
                activeTab === key
                  ? "border-b-2 border-gray-900 text-gray-900"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              {label}
            </button>
          ))}
        </nav>

        <article>
          {activeTab === "users" && <AdminUserTable />}
          {activeTab === "posts" && <AdminPostTable />}
          {activeTab === "comments" && <AdminCommentTable />}
        </article>
      </div>
    </div>
  );
}
