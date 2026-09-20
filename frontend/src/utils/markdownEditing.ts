export type EditResult = { value: string; selectionStart: number; selectionEnd: number };

/** 선택한 글을 before/after로 감싼다. 선택이 없으면 placeholder를 넣고 그 글자를 선택해 둔다. */
export function wrapSelection(
  value: string,
  start: number,
  end: number,
  before: string,
  after: string,
  placeholder: string,
): EditResult {
  const selected = value.slice(start, end) || placeholder;
  const next = value.slice(0, start) + before + selected + after + value.slice(end);
  return { value: next, selectionStart: start + before.length, selectionEnd: start + before.length + selected.length };
}

/** 선택이 걸친 모든 줄 앞에 prefix를 붙인다(소제목·목록·인용). 이미 붙어 있으면 그대로 둔다. */
export function prefixLines(value: string, start: number, end: number, prefix: string): EditResult {
  const lineStart = value.lastIndexOf("\n", start - 1) + 1;
  const newlineAfter = value.indexOf("\n", end);
  const lineEnd = newlineAfter === -1 ? value.length : newlineAfter;
  const block = value.slice(lineStart, lineEnd);
  const prefixed = block
    .split("\n")
    .map((line) => (line.startsWith(prefix) ? line : prefix + line))
    .join("\n");
  const next = value.slice(0, lineStart) + prefixed + value.slice(lineEnd);
  return { value: next, selectionStart: lineStart, selectionEnd: lineStart + prefixed.length };
}

/** 커서 위치에 한 블록(이미지·유튜브·구분선)을 앞뒤 빈 줄과 함께 넣는다. */
export function insertBlock(value: string, start: number, end: number, block: string): EditResult {
  const before = value.slice(0, start);
  const after = value.slice(end);
  const lead = before === "" || before.endsWith("\n\n") ? "" : before.endsWith("\n") ? "\n" : "\n\n";
  const trail = after === "" || after.startsWith("\n\n") ? "" : after.startsWith("\n") ? "\n" : "\n\n";
  const insertion = lead + block + trail;
  const position = before.length + insertion.length;
  return { value: before + insertion + after, selectionStart: position, selectionEnd: position };
}
