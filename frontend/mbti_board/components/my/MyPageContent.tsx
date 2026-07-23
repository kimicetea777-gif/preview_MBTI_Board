import MyProfileCard from "./MyProfileCard";
import MyActivityStats from "./MyActivityStats";
import MyPostsPreview from "./MyPostsPreview";
import MyRecentlyViewedPreview from "./MyRecentlyViewedPreview";
import MyMbtiHistorySection from "./MyMbtiHistorySection";

export default function MyPageContent() {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
      <section className="lg:col-span-4 flex flex-col gap-5">
        <MyProfileCard />
        <MyActivityStats />
      </section>
      <section className="lg:col-span-8 flex flex-col gap-3.5">
        <MyRecentlyViewedPreview />
        <MyPostsPreview />
        <MyMbtiHistorySection />
      </section>
    </div>
  );
}
