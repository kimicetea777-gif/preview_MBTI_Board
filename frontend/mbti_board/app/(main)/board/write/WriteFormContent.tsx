"use client";

import { useState } from "react";
import Link from "next/link";
import { POST_TYPE, POST_TYPE_LABELS, GUEST_PASSWORD_MIN_LENGTH, GUEST_PASSWORD_MAX_LENGTH, MAX_POST_IMAGES } from "@/types/post";
import type { PostType } from "@/types/post";
import { useWriteForm } from "@/hooks/useWriteForm";
import PostWriteForm from "@/components/post/PostWriteForm";
import InfoBanner, { BANNER_VARIANT } from "@/components/common/InfoBanner";
import FloatingLabelInput from "@/components/common/FloatingLabelInput";
import { ALLOWED_IMAGE_TYPES_LABEL, MAX_IMAGE_BYTES_LABEL } from "@/lib/imageUtils";
import VoteOptionsField from "@/components/post/VoteOptionsField";
import DebateLabelsField from "@/components/post/DebateLabelsField";
import { ROUTES } from "@/constants/routes";
import { toast } from "@/components/common/Toast";

interface Props {
  initialType: PostType;
}

export default function WriteFormContent({ initialType }: Props) {
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);

  const {
    postType, title, targetMbti, guestPassword, isMbtiMode, options,
    endsAt, isNoDeadline, proLabel, conLabel, mbtiList, isLoggedIn,
    hasImage, uploadingCount, isPending,
    setTitle, setTargetMbti, setContent, setGuestPassword, setEndsAt,
    setProLabel, setConLabel,
    handleTypeChange, handleNoDeadlineToggle, toggleMbtiMode, addOption, removeOption,
    updateOptionText, handleSubmit, handleUpload, setUploadingCount,
  } = useWriteForm(initialType);

  const contentLabel = postType !== POST_TYPE.FREE ? "내용 (선택)" : undefined;
  const contentPlaceholder =
    postType === POST_TYPE.VOTE ? "투표에 대한 설명을 입력하세요."
    : postType === POST_TYPE.DEBATE ? "토론 주제에 대한 설명을 입력하세요."
    : "내용을 입력하세요.";

  return (
    <section className="max-w-180 mx-auto px-4 py-8 pb-20">
      {/* HEADER */}
      <div className="mb-7 text-center">
        <h1 className="text-3xl font-bold text-primary mb-2">새로운 소식 전하기</h1>
        <p className="text-sm text-on-surface-variant">자유롭게 생각을 나누고 소통해보세요.</p>
      </div>
      {/* Post Type Switch */}
      <div className="flex justify-center mb-2">
        <div className="inline-flex bg-surface-container p-1.5 rounded-2xl">
          {(Object.values(POST_TYPE) as PostType[]).map((type) => (
            <button
              key={type}
              type="button"
              onClick={() => handleTypeChange(type)}
              disabled={isPending}
              className={`px-8 py-2.5 rounded-xl text-sm font-semibold transition-all disabled:opacity-50 ${
                postType === type
                  ? "bg-card text-primary shadow-sm border border-surface-variant"
                  : "text-on-surface-variant hover:text-primary"
              }`}
            >
              {POST_TYPE_LABELS[type]}
            </button>
          ))}
        </div>
      </div>
      {/* 게시글 공통 입력창 (title + mbti target + content)  */}
      <PostWriteForm
        isPending={isPending}
        isUploading={uploadingCount > 0}
        onSubmit={(e) => {
          if (!isLoggedIn && postType === POST_TYPE.FREE && (!termsAgreed || !privacyAgreed)) {
            e.preventDefault();
            toast.warning("약관 동의를 읽고 체크해주세요.");
            return;
          }
          handleSubmit(e);
        }}
        title={title}
        onTitleChange={setTitle}
        targetMbti={targetMbti}
        onTargetMbtiChange={setTargetMbti}
        onContentChange={setContent}
        onUpload={handleUpload}
        onUploadingChange={setUploadingCount}
        mbtiList={mbtiList}
        contentLabel={contentLabel}
        contentPlaceholder={contentPlaceholder}
      >
        {/* Image Info */}
        {hasImage && (
          <InfoBanner
            legend="이미지 업로드"
            items={[
              { variant: BANNER_VARIANT.WARNING, text: `크기 제한 ${MAX_IMAGE_BYTES_LABEL}` },
              { variant: BANNER_VARIANT.WARNING, text: `${ALLOWED_IMAGE_TYPES_LABEL}만 허용` },
              { variant: BANNER_VARIANT.WARNING, text: `최대 ${MAX_POST_IMAGES}개까지 가능` },
            ]}
          />
        )}

        {postType === POST_TYPE.FREE && !isLoggedIn && (
          <div className="space-y-3">
            <FloatingLabelInput
              id="guestPassword"
              label="비회원 비밀번호 (4~72자)"
              type="password"
              value={guestPassword}
              onChange={(e) => setGuestPassword(e.target.value)}
              minLength={GUEST_PASSWORD_MIN_LENGTH}
              maxLength={GUEST_PASSWORD_MAX_LENGTH}
              autoComplete="off"
              disabled={isPending}
            />
            <div className="space-y-2">
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={termsAgreed}
                  onChange={(e) => setTermsAgreed(e.target.checked)}
                  disabled={isPending}
                  className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
                />
                <span className="text-xs text-on-surface-variant leading-relaxed">
                  <Link href={ROUTES.terms} target="_blank" className="font-medium text-primary underline underline-offset-2 hover:opacity-80">이용약관</Link>에 동의합니다. <span className="text-red-500">*</span>
                </span>
              </label>
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={privacyAgreed}
                  onChange={(e) => setPrivacyAgreed(e.target.checked)}
                  disabled={isPending}
                  className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
                />
                <span className="text-xs text-on-surface-variant leading-relaxed">
                  <Link href={ROUTES.privacy} target="_blank" className="font-medium text-primary underline underline-offset-2 hover:opacity-80">개인정보처리방침</Link>에 동의합니다. <span className="text-red-500">*</span>
                </span>
              </label>
            </div>
          </div>
        )}

        {postType === POST_TYPE.VOTE && (
          <VoteOptionsField
            options={options}
            isMbtiMode={isMbtiMode}
            mbtiList={mbtiList}
            isPending={isPending}
            endsAt={endsAt}
            isNoDeadline={isNoDeadline}
            onToggleMbtiMode={toggleMbtiMode}
            onAddOption={addOption}
            onRemoveOption={removeOption}
            onUpdateOptionText={updateOptionText}
            onEndsAtChange={setEndsAt}
            onNoDeadlineToggle={handleNoDeadlineToggle}
          />
        )}

        {postType === POST_TYPE.DEBATE && (
          <DebateLabelsField
            proLabel={proLabel}
            conLabel={conLabel}
            isPending={isPending}
            endsAt={endsAt}
            isNoDeadline={isNoDeadline}
            onProLabelChange={setProLabel}
            onConLabelChange={setConLabel}
            onEndsAtChange={setEndsAt}
            onNoDeadlineToggle={handleNoDeadlineToggle}
          />
        )}
      </PostWriteForm>
    </section>
  );
}
