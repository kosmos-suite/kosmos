export type DownloadState = "downloading" | "paused";

export interface ActiveDownloadSource {
  title: string;
  tag: string;
  release: string;
  /** Total size in GB. */
  sizeGb: number;
  /** Starting progress percent for the simulated tick animation. */
  base: number;
  /** Percent gained per simulated tick, while downloading. */
  rate: number;
  /** MB/s while downloading. */
  speedMbps: number;
  seeds: string;
  indexer: string;
  state: DownloadState;
}

/** No download client is wired to the UI yet. Progress is simulated client-side from this baseline. */
export const activeDownloadSources: ActiveDownloadSource[] = [
  {
    title: "The Substance",
    tag: "2160p",
    release: "The.Substance.2024.2160p.WEB-DL.DDP5.1.Atmos.H265-FLUX",
    sizeGb: 22.4,
    base: 41,
    rate: 0.42,
    speedMbps: 6.2,
    seeds: "38 seeds",
    indexer: "Nyaa · usenet",
    state: "downloading",
  },
  {
    title: "Frieren: Beyond Journey's End — S01E27",
    tag: "anime · abs #27",
    release: "[SubsPlease] Sousou no Frieren - 27 (1080p) [A4F3E01C].mkv",
    sizeGb: 1.4,
    base: 62,
    rate: 0.9,
    speedMbps: 11.4,
    seeds: "164 seeds",
    indexer: "AnimeTosho",
    state: "downloading",
  },
  {
    title: "Anora",
    tag: "1080p",
    release: "Anora.2024.1080p.WEB-DL.H264-NAISU",
    sizeGb: 8.1,
    base: 78,
    rate: 0.3,
    speedMbps: 3.1,
    seeds: "12 seeds",
    indexer: "Prowlarr · rarbg",
    state: "downloading",
  },
  {
    title: "Foundation — S02E04",
    tag: "2160p",
    release: "Foundation.S02E04.2160p.ATVP.WEB-DL.DV.HDR10-NTb",
    sizeGb: 14.8,
    base: 12,
    rate: 0,
    speedMbps: 0,
    seeds: "stalled",
    indexer: "Prowlarr · nzb",
    state: "paused",
  },
];

export type HistoryEventKind = "done" | "upgrade" | "grab" | "fail";

export interface DownloadHistoryItem {
  kind: HistoryEventKind;
  title: string;
  release: string;
  event: string;
  size: string;
  time: string;
}

export interface HistoryGroup {
  label: string;
  rows: DownloadHistoryItem[];
}

export const mockHistory: HistoryGroup[] = [
  {
    label: "Today",
    rows: [
      { kind: "done", title: "Dune: Part Two", release: "Dune.Part.Two.2024.2160p.BluRay.REMUX.HDR.TrueHD-KOSMOS", event: "Imported", size: "24.8 GB", time: "14:08" },
      { kind: "upgrade", title: "Blade Runner 2049", release: "Blade.Runner.2049.2017.2160p.UHD.BluRay.x265-TERMiNAL", event: "Upgraded from 1080p", size: "31.2 GB", time: "12:41" },
      { kind: "fail", title: "Nosferatu", release: "Nosferatu.2024.1080p.CAM.x264-RAWR", event: "Rejected — below cut-off", size: "2.1 GB", time: "11:55" },
      { kind: "done", title: "Cowboy Bebop — S01E14", release: "[Anime Time] Cowboy Bebop - 14 [BD 1080p][Dual Audio]", event: "Imported", size: "1.9 GB", time: "09:23" },
      { kind: "fail", title: "Conclave", release: "Conclave.2024.1080p.WEB-DL.H264-EVO", event: "Failed — download client error", size: "—", time: "08:47" },
    ],
  },
  {
    label: "Yesterday",
    rows: [
      { kind: "done", title: "Flow", release: "Flow.2024.1080p.WEB-DL.DDP5.1.H264-FLUX", event: "Imported", size: "3.6 GB", time: "23:12" },
      { kind: "done", title: "Perfect Blue", release: "Perfect.Blue.1997.1080p.BluRay.REMUX.AVC.FLAC-PBLUE", event: "Imported", size: "18.4 GB", time: "21:30" },
      { kind: "grab", title: "Challengers", release: "Challengers.2024.2160p.WEB-DL.DV.HDR10-NTb", event: "Grabbed", size: "19.4 GB", time: "19:04" },
      { kind: "fail", title: "Parasite", release: "Parasite.2019.1080p.BluRay.x264-DEPTH", event: "Rejected — wrong language", size: "9.4 GB", time: "17:52" },
      { kind: "done", title: "Millennium Actress", release: "Millennium.Actress.2001.1080p.BluRay.x264-JAP", event: "Imported", size: "5.8 GB", time: "15:19" },
    ],
  },
];

export const historyFilters = ["All", "Imported", "Upgraded", "Failed"] as const;
export type HistoryFilter = (typeof historyFilters)[number];

export const historyFilterKind: Record<HistoryFilter, HistoryEventKind | null> = {
  All: null,
  Imported: "done",
  Upgraded: "upgrade",
  Failed: "fail",
};
