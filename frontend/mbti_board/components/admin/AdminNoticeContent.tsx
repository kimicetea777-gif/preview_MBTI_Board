"use client";

import { useState } from "react";
import { MdAdd, MdEdit, MdDelete, MdClose, MdCheck } from "react-icons/md";
import {
  useAdminNotices,
  useCreateAdminNotice,
  useUpdateAdminNotice,
  useDeleteAdminNotice,
} from "@/hooks/useAdmin";
import type { AdminNoticeResponse, NoticeCreateRequest } from "@/types/notice";

const INPUT_CLS =
  "w-full rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

const EMPTY_FORM: NoticeCreateRequest = {
  title: "",
  content: "",
  isActive: true,
  priority: 0,
  startsAt: null,
  endsAt: null,
};

export default function AdminNoticeContent() {
  const { data: notices } = useAdminNotices();
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<AdminNoticeResponse | null>(null);
  const [form, setForm] = useState<NoticeCreateRequest>(EMPTY_FORM);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  const create = useCreateAdminNotice(() => closeForm());
  const update = useUpdateAdminNotice(() => closeForm());
  const remove = useDeleteAdminNotice(() => setConfirmDeleteId(null));

  function openCreate() {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
  }

  function openEdit(notice: AdminNoticeResponse) {
    setEditTarget(notice);
    setForm({
      title: notice.title,
      content: notice.content,
      isActive: notice.isActive,
      priority: notice.priority,
      startsAt: notice.startsAt,
      endsAt: notice.endsAt,
    });
    setFormOpen(true);
  }

  function closeForm() {
    setFormOpen(false);
    setEditTarget(null);
    setForm(EMPTY_FORM);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (editTarget) {
      update.mutate({ id: editTarget.id, data: form });
    } else {
      create.mutate(form);
    }
  }

  const isPending = create.isPending || update.isPending;

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-foreground">공지사항 관리</h1>
        <button
          onClick={openCreate}
          className="flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
        >
          <MdAdd size={18} />
          공지 등록
        </button>
      </div>

      {/* 공지 목록 */}
      <section className="rounded-2xl border border-outline-variant bg-card divide-y divide-outline-variant">
        {notices.length === 0 && (
          <p className="px-5 py-8 text-center text-sm text-on-surface-variant">등록된 공지사항이 없습니다.</p>
        )}
        {notices.map((notice) => (
          <div key={notice.id} className="flex items-start justify-between gap-4 px-5 py-4">
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap mb-1">
                <span className="text-sm font-medium text-on-surface truncate">{notice.title}</span>
                <span className={`shrink-0 text-xs px-2 py-0.5 rounded-full font-medium ${
                  notice.isActive
                    ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900 dark:text-emerald-300"
                    : "bg-surface-variant text-on-surface-variant"
                }`}>
                  {notice.isActive ? "활성" : "비활성"}
                </span>
                {notice.priority > 0 && (
                  <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                    우선순위 {notice.priority}
                  </span>
                )}
              </div>
              <p className="text-xs text-on-surface-variant line-clamp-2 whitespace-pre-wrap">
                {notice.content}
              </p>
            </div>
            <div className="flex shrink-0 gap-1">
              <button
                onClick={() => openEdit(notice)}
                className="rounded-lg p-1.5 text-on-surface-variant hover:bg-accent transition-colors"
                aria-label="수정"
              >
                <MdEdit size={16} />
              </button>
              {confirmDeleteId === notice.id ? (
                <div className="flex gap-1">
                  <button
                    onClick={() => remove.mutate(notice.id)}
                    disabled={remove.isPending}
                    className="rounded-lg p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-950 transition-colors disabled:opacity-50"
                    aria-label="삭제 확인"
                  >
                    <MdCheck size={16} />
                  </button>
                  <button
                    onClick={() => setConfirmDeleteId(null)}
                    className="rounded-lg p-1.5 text-on-surface-variant hover:bg-accent transition-colors"
                    aria-label="취소"
                  >
                    <MdClose size={16} />
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setConfirmDeleteId(notice.id)}
                  className="rounded-lg p-1.5 text-on-surface-variant hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950 transition-colors"
                  aria-label="삭제"
                >
                  <MdDelete size={16} />
                </button>
              )}
            </div>
          </div>
        ))}
      </section>

      {/* 등록/수정 폼 모달 */}
      {formOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/30" onClick={closeForm} />
          <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-lg -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-outline-variant bg-surface-container-lowest shadow-xl">
            <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
              <h2 className="text-sm font-semibold text-on-surface">
                {editTarget ? "공지사항 수정" : "공지사항 등록"}
              </h2>
              <button onClick={closeForm} className="rounded-full p-1 text-on-surface-variant hover:bg-accent transition-colors">
                <MdClose size={20} />
              </button>
            </header>

            <form onSubmit={handleSubmit} className="px-5 py-4 flex flex-col gap-3">
              <div>
                <label className="mb-1 block text-xs font-medium text-on-surface-variant">제목</label>
                <input
                  className={INPUT_CLS}
                  value={form.title}
                  onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                  maxLength={200}
                  required
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-medium text-on-surface-variant">내용</label>
                <textarea
                  className={`${INPUT_CLS} resize-none`}
                  rows={5}
                  value={form.content}
                  onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                  required
                />
              </div>

              <div className="flex gap-4">
                <div className="flex items-center gap-2">
                  <input
                    id="isActive"
                    type="checkbox"
                    checked={form.isActive}
                    onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))}
                    className="h-4 w-4 rounded border-outline-variant accent-primary"
                  />
                  <label htmlFor="isActive" className="text-sm text-on-surface">활성화</label>
                </div>
                <div className="flex items-center gap-2">
                  <label className="text-sm text-on-surface-variant">우선순위</label>
                  <input
                    type="number"
                    className="w-20 rounded-md border border-outline-variant bg-surface-container-lowest px-2 py-1 text-sm text-on-surface focus:border-outline focus:outline-none"
                    value={form.priority}
                    min={0}
                    onChange={(e) => setForm((f) => ({ ...f, priority: Number(e.target.value) }))}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-xs font-medium text-on-surface-variant">
                    노출 시작 <span className="font-normal">(비워두면 즉시)</span>
                  </label>
                  <input
                    type="datetime-local"
                    className={INPUT_CLS}
                    value={form.startsAt ? form.startsAt.slice(0, 16) : ""}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, startsAt: e.target.value ? new Date(e.target.value).toISOString() : null }))
                    }
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-on-surface-variant">
                    노출 종료 <span className="font-normal">(비워두면 무기한)</span>
                  </label>
                  <input
                    type="datetime-local"
                    className={INPUT_CLS}
                    value={form.endsAt ? form.endsAt.slice(0, 16) : ""}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, endsAt: e.target.value ? new Date(e.target.value).toISOString() : null }))
                    }
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-1">
                <button
                  type="button"
                  onClick={closeForm}
                  className="rounded-xl border border-outline-variant px-4 py-2 text-sm text-on-surface-variant hover:bg-accent transition-colors"
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  className="rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity disabled:opacity-50"
                >
                  {isPending ? "저장 중..." : editTarget ? "수정" : "등록"}
                </button>
              </div>
            </form>
          </div>
        </>
      )}
    </div>
  );
}
