export interface CustomFormat {
  id: string;
  name: string;
  note: string;
  field: "Release title" | "Source" | "Audio" | "Release group" | "Subtitles";
  score: number;
  enabled: boolean;
}

export interface QualityLadderEntry {
  label: string;
  enabled: boolean;
}

export interface QualityProfile {
  id: string;
  name: string;
  minScore: number;
  usedBy: string;
  blurb: string;
  cutoff: string;
  sample: string;
  ladder: QualityLadderEntry[];
  formats: CustomFormat[];
}

const ALL_QUALITIES = [
  "2160p Remux",
  "2160p BluRay",
  "2160p WEB",
  "1080p Remux",
  "1080p BluRay",
  "1080p WEB",
  "720p BluRay",
  "SDTV",
];

function ladder(on: string[]): QualityLadderEntry[] {
  return ALL_QUALITIES.map((label) => ({ label, enabled: on.includes(label) }));
}

let seq = 0;
function format(
  name: string,
  note: string,
  field: CustomFormat["field"],
  score: number,
  enabled: boolean,
): CustomFormat {
  seq += 1;
  return { id: `fmt-${seq}`, name, note, field, score, enabled };
}

/** Sample data — not currently referenced by any page; a real /quality-profiles API exists in api/client.ts. */
export const mockQualityProfiles: QualityProfile[] = [
  {
    id: "any",
    name: "Any",
    minScore: 0,
    usedBy: "used by 1 library",
    blurb: "Grabs the first thing that works. Kept for the throwaway library.",
    cutoff: "1080p WEB",
    sample: "Some.Movie.2019.720p.WEBRip.x264-GHOST",
    ladder: ladder(ALL_QUALITIES),
    formats: [
      format("BluRay source", "disc, not stream", "Source", 20, true),
      format("CAM / TS", "never, under any score", "Source", -1000, true),
    ],
  },
  {
    id: "hd-bluray",
    name: "HD Bluray",
    minScore: 90,
    usedBy: "used by 2 libraries",
    blurb: "The everyday profile — 1080p disc sources, English audio, no upscales or re-encodes.",
    cutoff: "1080p BluRay",
    sample: "Dune.Part.Two.2024.1080p.BluRay.x264.DTS-HD.MA.5.1-KOSMOS",
    ladder: ladder(["1080p Remux", "1080p BluRay", "1080p WEB", "720p BluRay"]),
    formats: [
      format("BluRay source", "disc, not stream", "Source", 60, true),
      format("x264", "widely compatible", "Release title", 25, true),
      format("DTS-HD MA", "lossless audio", "Audio", 30, true),
      format("EN audio", "English track present", "Audio", 20, true),
      format("Trusted groups", "14 groups in list", "Release group", 25, true),
      format("x265", "re-encode", "Release title", -20, true),
      format("WEBRip", "screen capture", "Source", -50, true),
      format("CAM / TS", "never, under any score", "Source", -1000, true),
      format("Hybrid", "mixed sources", "Release title", -15, false),
      format("HDR", "no HDR display yet", "Release title", 0, false),
      format("Dual audio", "two full audio tracks", "Audio", 10, false),
      format("Repack / Proper", "fixed release", "Release title", 5, true),
    ],
  },
  {
    id: "4k-remux",
    name: "4K Remux",
    minScore: 210,
    usedBy: "used by 1 library",
    blurb: "Untouched UHD discs with lossless audio. Big files, no compromises.",
    cutoff: "2160p Remux",
    sample: "Dune.Part.Two.2024.2160p.UHD.BluRay.REMUX.DV.HDR.TrueHD.7.1.Atmos-FraMeSToR",
    ladder: ladder(["2160p Remux", "2160p BluRay"]),
    formats: [
      format("Remux", "untouched disc stream", "Release title", 100, true),
      format("Dolby Vision", "DV profile 7 or 8", "Release title", 60, true),
      format("HDR10+", "dynamic metadata", "Release title", 40, true),
      format("TrueHD Atmos", "lossless object audio", "Audio", 50, true),
      format("FraMeSToR", "trusted remux group", "Release group", 30, true),
      format("x265", "re-encode, not remux", "Release title", -40, true),
      format("WEB-DL", "streaming source", "Source", -30, true),
      format("Upscaled", "fake 2160p", "Release title", -1000, true),
      format("EN subs", "forced subtitles present", "Subtitles", 10, false),
      format("Dual audio", "two full audio tracks", "Audio", 15, false),
    ],
  },
  {
    id: "anime-dual-audio",
    name: "Anime Dual Audio",
    minScore: 140,
    usedBy: "used by 1 library",
    blurb: "Prefers dual-audio BD releases from known fansub groups, absolute numbering safe.",
    cutoff: "1080p BluRay",
    sample: "[SubsPlease] Sousou no Frieren - 27 (1080p) [Dual Audio] [A4F3E01C].mkv",
    ladder: ladder(["1080p Remux", "1080p BluRay", "1080p WEB"]),
    formats: [
      format("Dual audio", "JP + EN full tracks", "Audio", 100, true),
      format("BD source", "disc, not stream", "Source", 60, true),
      format("SubsPlease", "trusted weekly group", "Release group", 35, true),
      format("Anime BD tier 1", "curated group list", "Release group", 40, true),
      format("Absolute numbering", "no season in filename", "Release title", 20, true),
      format("Hardsubbed", "burned-in subtitles", "Release title", -600, true),
      format("x265", "smaller, slightly softer", "Release title", -10, true),
      format("Multi-sub", "more than 3 sub tracks", "Subtitles", 10, false),
      format("Remux", "full BD stream", "Release title", 45, false),
    ],
  },
  {
    id: "space-saver",
    name: "Space Saver",
    minScore: 40,
    usedBy: "unused",
    blurb: "x265 WEB releases under 6 GB for the titles nobody rewatches.",
    cutoff: "1080p WEB",
    sample: "Enemy.2013.1080p.WEB-DL.x265.10bit-SPACE",
    ladder: ladder(["1080p WEB", "720p BluRay"]),
    formats: [
      format("WEB-DL", "streaming source", "Source", 20, true),
      format("x265", "smaller footprint", "Release title", 15, true),
      format("CAM / TS", "never, under any score", "Source", -1000, true),
    ],
  },
];
