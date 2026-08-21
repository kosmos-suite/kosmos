import { useState } from "react";
import { useLocation, useParams } from "react-router-dom";
import { api } from "../../api/client";
import { DiscoverGrid } from "../../components/DiscoverGrid";
import { LanguageFilterDropdown } from "../../components/LanguageFilterDropdown";

export default function NetworkDiscoverPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const name = (location.state as { name?: string } | null)?.name ?? "Network";
  const networkId = Number(id);
  const [hiddenLanguages, setHiddenLanguages] = useState<Set<string>>(new Set());

  const excludeLanguages = Array.from(hiddenLanguages);

  return (
    <DiscoverGrid
      title={name}
      depKey={`${id}-${excludeLanguages.join(",")}`}
      fetcher={(page) => api.discoverTvByNetwork(networkId, page, excludeLanguages)}
      filters={<LanguageFilterDropdown hidden={hiddenLanguages} onChange={setHiddenLanguages} />}
    />
  );
}
