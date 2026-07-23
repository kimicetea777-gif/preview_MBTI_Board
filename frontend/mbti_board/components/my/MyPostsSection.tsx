"use client";

import { useState } from "react";
import { useSuspenseQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchMyPosts, deletePost } from "@/lib/api/post";
import type { PostType } from "@/types/post";
import MyPostsHeader from "@/components/my/MyPostsHeader";
import MyPostsList from "@/components/my/MyPostsList";

const PAGE_SIZE = 10;

export default function MyPostsSection() {
  const queryClient = useQueryClient();
  const [selectedType, setSelectedType] = useState<PostType | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data } = useSuspenseQuery({
    queryKey: ["myPosts", selectedType, page],
    queryFn: () => fetchMyPosts({ type: selectedType, sort: "LATEST", page, size: PAGE_SIZE }),
  });

  const { mutate: remove, isPending: isDeleting } = useMutation({
    mutationFn: (id: number) => deletePost(id),
    onSuccess: () => {
      if (data.content.length === 1 && page > 0) {
        setPage((p) => p - 1);
      } else {
        queryClient.invalidateQueries({ queryKey: ["myPosts"] });
      }
    },
  });

  const handleTabChange = (type: PostType | undefined) => {
    setSelectedType(type);
    setPage(0);
  };

  const handleDelete = (id: number, title: string) => {
    if (!window.confirm(`"${title}" 게시글을 삭제하시겠습니까?`)) return;
    remove(id);
  };

  return (
    <section>
      <MyPostsHeader
        totalElements={data.totalElements}
        selectedType={selectedType}
        onTabChange={handleTabChange}
      />
      <MyPostsList
        data={data}
        page={page}
        onPageChange={setPage}
        onDelete={handleDelete}
        isDeleting={isDeleting}
      />
    </section>
  );
}
