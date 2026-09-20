import { describe, expect, it } from "vitest";
import { insertBlock, prefixLines, wrapSelection } from "./markdownEditing";

describe("wrapSelection", () => {
  it("선택한 글을 감싼다", () => {
    const result = wrapSelection("가나다", 1, 2, "**", "**", "굵은 글");

    expect(result.value).toBe("가**나**다");
    expect(result.selectionStart).toBe(3);
    expect(result.selectionEnd).toBe(4);
  });

  it("선택이 없으면 자리표시 글을 넣고 선택해 둔다", () => {
    const result = wrapSelection("", 0, 0, "**", "**", "굵은 글");

    expect(result.value).toBe("**굵은 글**");
    expect(result.value.slice(result.selectionStart, result.selectionEnd)).toBe("굵은 글");
  });
});

describe("prefixLines", () => {
  it("선택이 걸친 모든 줄에 붙이고 이미 붙은 줄은 건드리지 않는다", () => {
    const value = "첫째\n- 둘째\n셋째";
    const result = prefixLines(value, 0, value.length, "- ");

    expect(result.value).toBe("- 첫째\n- 둘째\n- 셋째");
  });

  it("커서만 있어도 그 줄에 붙는다", () => {
    expect(prefixLines("가\n나", 3, 3, "# ").value).toBe("가\n# 나");
  });
});

describe("insertBlock", () => {
  it("앞뒤에 빈 줄을 두고 넣는다", () => {
    const result = insertBlock("앞글뒷글", 2, 2, "![a](image:1)");

    expect(result.value).toBe("앞글\n\n![a](image:1)\n\n뒷글");
    expect(result.selectionStart).toBe("앞글\n\n![a](image:1)\n\n".length);
  });

  it("이미 빈 줄이 있으면 더 늘리지 않는다", () => {
    expect(insertBlock("앞글\n\n", 4, 4, "---").value).toBe("앞글\n\n---");
  });
});
