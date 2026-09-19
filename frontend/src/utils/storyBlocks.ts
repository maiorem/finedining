export type StoryBlock = { type: "question" | "text"; text: string };

// 인터뷰 글 표기: 빈 줄로 문단을 나누고, 한 줄 전체를 **로 감싼 문단은 굵은 질문으로 그린다.
// 리치 에디터 없이 플레인 텍스트만 쓴다(HTML을 받지 않는다, CLAUDE.md §3.6과 같은 이유).
export function parseStoryBlocks(source: string | null | undefined): StoryBlock[] {
  if (!source) return [];
  return source
    .split(/\n\s*\n/)
    .map((chunk) => chunk.trim())
    .filter(Boolean)
    .map((chunk): StoryBlock => {
      const question = /^\*\*([^*\n]+)\*\*$/.exec(chunk);
      return question ? { type: "question", text: question[1].trim() } : { type: "text", text: chunk };
    });
}
