import MyPostsSection from "@/components/my/MyPostsSection";

export default function MyPostsPage() {
  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">내 게시글</h1>
      <MyPostsSection />
    </div>
  );
}
