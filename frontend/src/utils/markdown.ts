import { extractYoutubeId } from "./youtube";

// 이 사이트 전용의 작은 마크다운이다. HTML을 만들지 않고 화면 요소가 될 트리만 만든다 — 입력한
// HTML 태그는 글자 그대로 보인다(CLAUDE.md §3.6 XSS 이유와 같다). 지원: 소제목(#, ##), 굵게, 기울임,
// 목록, 인용, 구분선, 링크, 우리가 올린 이미지(image:번호), 유튜브(@[youtube](주소)).

export type Inline =
  | { type: "text"; text: string }
  | { type: "strong"; children: Inline[] }
  | { type: "em"; children: Inline[] }
  | { type: "link"; href: string; children: Inline[] };

export type Block =
  | { type: "heading"; level: 2 | 3; children: Inline[] }
  | { type: "question"; children: Inline[] }
  | { type: "paragraph"; lines: Inline[][] }
  | { type: "list"; ordered: boolean; items: Inline[][] }
  | { type: "quote"; lines: Inline[][] }
  | { type: "hr" }
  | { type: "image"; id: number; alt: string; width?: number }
  | { type: "youtube"; id: string };

/** 회원 글처럼 기능을 좁혀야 하는 곳에서 끈다(스팸 방지). 꺼진 기능은 글자로만 남는다. */
export type MarkdownOptions = { links?: boolean; images?: boolean; youtube?: boolean };

// `![설명](image:번호)` 또는 가로 픽셀을 함께 적는 `![설명](image:번호|400)`.
const IMAGE_LINE = /^!\[([^\]]*)\]\(image:(\d+)(?:\|(\d{2,4}))?\)$/;
export const IMAGE_MIN_WIDTH = 50;
export const IMAGE_MAX_WIDTH = 1600; // 가장 큰 파생본(1600w)보다 크게 늘려도 더 선명해지지 않는다
const YOUTUBE_LINE = /^@\[youtube\]\(([^)\s]+)\)$/;
const HEADING = /^(#{1,3})\s+(.+)$/;
const HR = /^(-{3,}|\*{3,}|_{3,})$/;
const UNORDERED = /^[-*]\s+(.+)$/;
const ORDERED = /^\d+\.\s+(.+)$/;
const QUOTE = /^>\s?(.*)$/;
const QUESTION = /^\*\*([^*\n]+)\*\*$/;

function isSafeHref(href: string): boolean {
  return /^https?:\/\//i.test(href) || /^mailto:/i.test(href);
}

// 가장 앞에 나오는 서식 하나를 찾아 그 앞은 글자로, 안쪽은 재귀로 처리한다.
export function parseInline(text: string, allowLinks = true): Inline[] {
  const patterns: { type: "link" | "strong" | "em"; regex: RegExp }[] = [
    { type: "link", regex: /\[([^\]]+)\]\(([^)\s]+)\)/ },
    { type: "strong", regex: /\*\*(.+?)\*\*/ },
    { type: "em", regex: /(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)|(?<![\w])_(.+?)_(?![\w])/ },
  ];

  let best: { type: "link" | "strong" | "em"; match: RegExpExecArray } | null = null;
  for (const { type, regex } of patterns) {
    const match = regex.exec(text);
    if (match && (best === null || match.index < best.match.index)) {
      best = { type, match };
    }
  }
  if (!best) {
    return text === "" ? [] : [{ type: "text", text }];
  }

  const { type, match } = best;
  const before = text.slice(0, match.index);
  const after = text.slice(match.index + match[0].length);
  const nodes: Inline[] = before === "" ? [] : [{ type: "text", text: before }];

  if (type === "link") {
    const [, label, href] = match;
    nodes.push(...(allowLinks && isSafeHref(href) ? [{ type: "link" as const, href, children: parseInline(label, false) }] : parseInline(label, false)));
  } else if (type === "strong") {
    nodes.push({ type: "strong", children: parseInline(match[1], allowLinks) });
  } else {
    nodes.push({ type: "em", children: parseInline(match[1] ?? match[2], allowLinks) });
  }
  return [...nodes, ...parseInline(after, allowLinks)];
}

function startsBlock(line: string): boolean {
  return (
    HEADING.test(line) ||
    HR.test(line) ||
    IMAGE_LINE.test(line) ||
    YOUTUBE_LINE.test(line) ||
    UNORDERED.test(line) ||
    ORDERED.test(line) ||
    QUOTE.test(line)
  );
}

export function parseMarkdown(source: string | null | undefined, options: MarkdownOptions = {}): Block[] {
  const { links = true, images = true, youtube = true } = options;
  if (!source) return [];

  const lines = source.replace(/\r\n?/g, "\n").split("\n").map((line) => line.trim());
  const blocks: Block[] = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];
    if (line === "") {
      i += 1;
      continue;
    }

    const heading = HEADING.exec(line);
    if (heading) {
      blocks.push({ type: "heading", level: heading[1].length === 1 ? 2 : 3, children: parseInline(heading[2], links) });
      i += 1;
      continue;
    }
    if (HR.test(line)) {
      blocks.push({ type: "hr" });
      i += 1;
      continue;
    }
    const image = IMAGE_LINE.exec(line);
    if (image && images) {
      const width = image[3] ? Math.min(IMAGE_MAX_WIDTH, Math.max(IMAGE_MIN_WIDTH, Number(image[3]))) : undefined;
      blocks.push({ type: "image", id: Number(image[2]), alt: image[1], ...(width ? { width } : {}) });
      i += 1;
      continue;
    }
    const video = YOUTUBE_LINE.exec(line);
    const videoId = video ? extractYoutubeId(video[1]) : null;
    if (videoId && youtube) {
      blocks.push({ type: "youtube", id: videoId });
      i += 1;
      continue;
    }

    if (QUOTE.test(line)) {
      const quoted: Inline[][] = [];
      while (i < lines.length && QUOTE.test(lines[i])) {
        quoted.push(parseInline((QUOTE.exec(lines[i]) as RegExpExecArray)[1], links));
        i += 1;
      }
      blocks.push({ type: "quote", lines: quoted });
      continue;
    }

    const listRegex = UNORDERED.test(line) ? UNORDERED : ORDERED.test(line) ? ORDERED : null;
    if (listRegex) {
      const items: Inline[][] = [];
      while (i < lines.length && listRegex.test(lines[i])) {
        items.push(parseInline((listRegex.exec(lines[i]) as RegExpExecArray)[1], links));
        i += 1;
      }
      blocks.push({ type: "list", ordered: listRegex === ORDERED, items });
      continue;
    }

    // 문단: 빈 줄이나 다른 블록이 시작될 때까지 이어 붙인다. 줄바꿈은 그대로 보존한다.
    const paragraph: string[] = [];
    while (i < lines.length && lines[i] !== "" && (paragraph.length === 0 || !startsBlock(lines[i]))) {
      paragraph.push(lines[i]);
      i += 1;
    }
    const question = paragraph.length === 1 ? QUESTION.exec(paragraph[0]) : null;
    if (question) {
      blocks.push({ type: "question", children: parseInline(question[1].trim(), links) });
    } else {
      blocks.push({ type: "paragraph", lines: paragraph.map((p) => parseInline(p, links)) });
    }
  }

  return blocks;
}

/** 본문이 참조한 이미지 번호들 — 페이지가 본문에 넣은 사진을 아래 갤러리에서 빼는 데 쓴다. */
export function referencedImageIds(source: string | null | undefined): Set<number> {
  return new Set(
    parseMarkdown(source)
      .filter((block): block is Extract<Block, { type: "image" }> => block.type === "image")
      .map((block) => block.id),
  );
}
