export const EMPTY_TIPTAP = '{"type":"doc","content":[{"type":"paragraph"}]}';

export function countTiptapImages(node: Record<string, unknown>): number {
  if (node.type === "image") return 1;
  const children = node.content as Record<string, unknown>[] | undefined;
  return children?.reduce((sum, child) => sum + countTiptapImages(child), 0) ?? 0;
}

export function parseTiptapContent(json?: string) {
  if (!json) return undefined;
  try { return JSON.parse(json); } catch { return undefined; }
}

const LEAF_NODES = new Set(["image", "youtube"]);

export function isTiptapEmpty(json: string): boolean {
  if (!json) return true;
  try {
    const doc = JSON.parse(json);
    const hasContent = (node: { type: string; text?: string; content?: unknown[] }): boolean => {
      if (node.type === "text") return (node.text ?? "").trim().length > 0;
      if (LEAF_NODES.has(node.type)) return true;
      return (node.content ?? []).some((n) => hasContent(n as typeof node));
    };
    return !hasContent(doc);
  } catch {
    return true;
  }
}
