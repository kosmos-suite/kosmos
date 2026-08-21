import { useLocation, useParams } from "react-router-dom";
import { api } from "../../api/client";
import { DiscoverGrid } from "../../components/DiscoverGrid";

export default function GenreDiscoverPage() {
  const { mediaType, id } = useParams<{ mediaType: string; id: string }>();
  const location = useLocation();
  const name = (location.state as { name?: string } | null)?.name ?? "Genre";
  const genreId = Number(id);

  return (
    <DiscoverGrid
      title={`${name} ${mediaType === "tv" ? "Series" : "Movies"}`}
      depKey={`${mediaType}-${id}`}
      fetcher={() => (mediaType === "tv" ? api.discoverTvByGenre(genreId) : api.discoverMoviesByGenre(genreId))}
    />
  );
}
