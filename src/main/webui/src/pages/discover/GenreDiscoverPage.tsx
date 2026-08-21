import { useState } from "react";
import { useLocation, useParams } from "react-router-dom";
import { api } from "../../api/client";
import { DiscoverGrid } from "../../components/DiscoverGrid";
import { LanguageFilterDropdown } from "../../components/LanguageFilterDropdown";

export default function GenreDiscoverPage() {
  const { mediaType, id } = useParams<{ mediaType: string; id: string }>();
  const location = useLocation();
  const name = (location.state as { name?: string } | null)?.name ?? "Genre";
  const genreId = Number(id);
  const [hiddenLanguages, setHiddenLanguages] = useState<Set<string>>(new Set());

  const excludeLanguages = Array.from(hiddenLanguages);

  return (
    <DiscoverGrid
      title={`${name} ${mediaType === "tv" ? "Series" : "Movies"}`}
      depKey={`${mediaType}-${id}-${excludeLanguages.join(",")}`}
      fetcher={(page) =>
        mediaType === "tv"
          ? api.discoverTvByGenre(genreId, page, excludeLanguages)
          : api.discoverMoviesByGenre(genreId, page, excludeLanguages)
      }
      filters={<LanguageFilterDropdown hidden={hiddenLanguages} onChange={setHiddenLanguages} />}
    />
  );
}
