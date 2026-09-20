// 새 이야기를 쓰는 동안에는 글 번호가 없어서 사진을 아직 올릴 수 없다. 그래서 본문에는 임시 표시
// `![설명](image:new1)`(첨부 목록의 n번째 파일)를 두고, 글을 등록한 뒤 사진을 올려 실제 번호로 바꾼다.

const TOKEN_LINE = /^!\[[^\]]*\]\(image:new(\d+)(?:\|\d+)?\)$/;

export function newImageToken(index: number, alt: string, width: number | null): string {
  return `![${alt.replace(/[[\]]/g, "")}](image:new${index}${width ? `|${width}` : ""})`;
}

function tidy(lines: string[]): string {
  return lines.join("\n").replace(/\n{3,}/g, "\n\n").trim();
}

/** 올린 사진의 실제 번호로 임시 표시를 바꾼다. 올리지 못한 사진의 줄은 지운다. */
export function resolveNewImageTokens(body: string, idsByIndex: (number | null)[]): string {
  const lines: string[] = [];
  for (const line of body.split("\n")) {
    const match = TOKEN_LINE.exec(line.trim());
    if (!match) {
      lines.push(line);
      continue;
    }
    const id = idsByIndex[Number(match[1]) - 1];
    if (id) lines.push(line.replace(`image:new${match[1]}`, `image:${id}`));
  }
  return tidy(lines);
}

/** 첨부 목록에서 n번째 파일을 뺐을 때 — 그 사진의 줄을 지우고 뒤 번호를 하나씩 당긴다. */
export function removeNewImage(body: string, index: number): string {
  const lines: string[] = [];
  for (const line of body.split("\n")) {
    const match = TOKEN_LINE.exec(line.trim());
    if (!match) {
      lines.push(line);
      continue;
    }
    const n = Number(match[1]);
    if (n === index) continue;
    lines.push(n > index ? line.replace(`image:new${n}`, `image:new${n - 1}`) : line);
  }
  return tidy(lines);
}
