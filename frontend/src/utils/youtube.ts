// 유튜브 주소(watch?v= / youtu.be / embed / shorts)에서 영상 ID를 꺼낸다. 유튜브가 아니면 null.
export function extractYoutubeId(url: string | null | undefined): string | null {
  if (!url) return null;
  try {
    const parsed = new URL(url);
    const host = parsed.hostname.replace(/^(www|m)\./, "");
    let id: string | null = null;
    if (host === "youtu.be") {
      id = parsed.pathname.split("/")[1] ?? null;
    } else if (host === "youtube.com") {
      const [, kind, second] = parsed.pathname.split("/");
      id = kind === "embed" || kind === "shorts" ? (second ?? null) : parsed.searchParams.get("v");
    }
    return id && /^[\w-]{6,}$/.test(id) ? id : null;
  } catch {
    return null;
  }
}
