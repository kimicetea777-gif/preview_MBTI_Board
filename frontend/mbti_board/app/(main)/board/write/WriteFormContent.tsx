"use client";

import { useState } from "react";
import { POST_TYPE, POST_TYPE_LABELS, GUEST_PASSWORD_MIN_LENGTH, GUEST_PASSWORD_MAX_LENGTH, MAX_POST_IMAGES } from "@/types/post";
import type { PostType } from "@/types/post";
import { useWriteForm } from "@/hooks/useWriteForm";
import PostWriteForm from "@/components/post/PostWriteForm";
import InfoBanner, { BANNER_VARIANT } from "@/components/common/InfoBanner";
import FloatingLabelInput from "@/components/common/FloatingLabelInput";
import { ALLOWED_IMAGE_TYPES_LABEL, MAX_IMAGE_BYTES_LABEL } from "@/constants/imageUpload";
import VoteOptionsField from "@/components/post/VoteOptionsField";
import DebateLabelsField from "@/components/post/DebateLabelsField";
import DeadlineField from "@/components/post/DeadlineField";
import TermsAgreementCheckboxes from "@/components/auth/TermsAgreementCheckboxes";
import { toast } from "@/components/common/Toast";

interface Props {
  initialType: PostType;
}

export default function WriteFormContent({ initialType }: Props) {
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [age14Agreed, setAge14Agreed] = useState(false);

  const {
    postType, title, targetMbti, guestPassword, isMbtiMode, options,
    endsAt, isDefaultDeadline, proLabel, conLabel, mbtiList, isLoggedIn,
    hasImage, uploadingCount, isPending,
    setTitle, setTargetMbti, setContent, setGuestPassword, setEndsAt,
    setProLabel, setConLabel,
    handleTypeChange, handleSetThreeMonths, toggleMbtiMode, addOption, removeOption,
    updateOptionText, handleSubmit, handleUpload, setUploadingCount,
  } = useWriteForm(initialType);

  const contentPlaceholder =
    postType === POST_TYPE.VOTE ? "투표에 대한 설명을 입력해요."
    : postType === POST_TYPE.DEBATE ? "토론 주제에 대한 설명을 입력해요."
    : "내용을 입력해요.";

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
          // 비로그인 사용자 전용
          if (!isLoggedIn && postType === POST_TYPE.FREE && (!termsAgreed || !privacyAgreed || !age14Agreed)) {
            e.preventDefault();
            toast.warning("약관 동의를 읽고 체크해주세요.");
            return;
          }
          handleSubmit(e, { termsAgreed, privacyAgreed, age14Agreed });
        }}
        title={title}
        onTitleChange={setTitle}
        targetMbti={targetMbti}
        onTargetMbtiChange={setTargetMbti}
        onContentChange={setContent}
        onUpload={handleUpload}
        onUploadingChange={setUploadingCount}
        contentPlaceholder={contentPlaceholder}
      >
        {/* Image Info Banner */}
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

        {/* 비회원 전용 - 비밀번호 + 약관 동의 체크 */}
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
            <TermsAgreementCheckboxes
              termsAgreed={termsAgreed}
              onTermsAgreedChange={setTermsAgreed}
              privacyAgreed={privacyAgreed}
              onPrivacyAgreedChange={setPrivacyAgreed}
              age14Agreed={age14Agreed}
              onAge14AgreedChange={setAge14Agreed}
              disabled={isPending}
            />
          </div>
        )}

        {postType === POST_TYPE.VOTE && (
          <VoteOptionsField
            options={options}
            isMbtiMode={isMbtiMode}
            mbtiList={mbtiList}
            isPending={isPending}
            onToggleMbtiMode={toggleMbtiMode}
            onAddOption={addOption}
            onRemoveOption={removeOption}
            onUpdateOptionText={updateOptionText}
          />
        )}

        {postType === POST_TYPE.DEBATE && (
          <DebateLabelsField
            proLabel={proLabel}
            conLabel={conLabel}
            isPending={isPending}
            onProLabelChange={setProLabel}
            onConLabelChange={setConLabel}
          />
        )}

        {(postType === POST_TYPE.VOTE || postType === POST_TYPE.DEBATE) && (
          <DeadlineField
            endsAt={endsAt}
            isDefaultDeadline={isDefaultDeadline}
            isPending={isPending}
            onEndsAtChange={setEndsAt}
            onSetThreeMonths={handleSetThreeMonths}
          />
        )}
      </PostWriteForm>
    </section>
  );
}
