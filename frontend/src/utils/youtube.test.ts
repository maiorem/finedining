import { describe, expect, it } from "vitest";
import { extractYoutubeId } from "./youtube";

describe("extractYoutubeId", () => {
  it.each([
    ["https://www.youtube.com/watch?v=abc123XYZ_-", "abc123XYZ_-"],
    ["https://youtu.be/abc123XYZ_-?si=zz", "abc123XYZ_-"],
    ["https://m.youtube.com/embed/abc123XYZ_-", "abc123XYZ_-"],
    ["https://www.youtube.com/shorts/abc123XYZ_-", "abc123XYZ_-"],
  ])("%s → %s", (url, id) => {
    expect(extractYoutubeId(url)).toBe(id);
  });

  it("유튜브가 아니거나 비어 있으면 null", () => {
    expect(extractYoutubeId("https://vimeo.com/123456")).toBeNull();
    expect(extractYoutubeId("not a url")).toBeNull();
    expect(extractYoutubeId(null)).toBeNull();
  });
});
