import { useLocation, useParams } from "react-router-dom";
import { api } from "../../api/client";
import { DiscoverGrid } from "../../components/DiscoverGrid";

export default function StudioDiscoverPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const name = (location.state as { name?: string } | null)?.name ?? "Studio";
  const studioId = Number(id);

  return <DiscoverGrid title={name} depKey={id ?? ""} fetcher={() => api.discoverMoviesByStudio(studioId)} />;
}
