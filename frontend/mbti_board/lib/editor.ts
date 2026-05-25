export const EMPTY_TIPTAP = '{"type":"doc","content":[{"type":"paragraph"}]}';

export function isTiptapEmpty(json: string): boolean {
  if (!json) return true;
  try {
    const doc = JSON.parse(json);
    const extractText = (node: { type: string; text?: string; content?: unknown[] }): string => {
      if (node.type === "text") return node.text ?? "";
      return (node.content ?? []).map((n) => extractText(n as typeof node)).join("");
    };
    return !extractText(doc).trim();
  } catch {
    return true;
  }
}
