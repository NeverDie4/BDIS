import { HerbResourceClient } from "@/components/herbs/HerbResourceClient";
import { SiteLayout } from "@/components/layout/SiteLayout";

type HerbsSearchParams = {
  keyword?: string | string[];
};

export default async function HerbsPage({
  searchParams,
}: {
  searchParams: Promise<HerbsSearchParams>;
}) {
  const params = await searchParams;
  const rawKeyword = Array.isArray(params.keyword) ? params.keyword[0] : params.keyword;
  const keyword = rawKeyword?.trim() || undefined;

  return (
    <SiteLayout contentMode="fluid">
      <HerbResourceClient initialKeyword={keyword} />
    </SiteLayout>
  );
}
