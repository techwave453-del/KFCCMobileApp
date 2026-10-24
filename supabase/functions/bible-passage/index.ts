const API_BIBLE_KEY = Deno.env.get("API_BIBLE_KEY");
if (!API_BIBLE_KEY) throw new Error("API_BIBLE_KEY is required");

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};
function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}
const validId = (value: unknown): value is string =>
  typeof value === "string" && /^[A-Za-z0-9._-]{1,128}$/.test(value);

async function apiBible(path: string): Promise<{ ok: boolean; status: number; body: any }> {
  const response = await fetch(`https://api.scripture.api.bible/v1${path}`, {
    headers: { "api-key": API_BIBLE_KEY!, "accept": "application/json" },
  });
  const body = await response.json().catch(() => ({}));
  return { ok: response.ok, status: response.status, body };
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return jsonResponse({ error: "Use POST" }, 405);
  let body: any;
  try { body = await req.json(); }
  catch { return jsonResponse({ error: "Request body must be valid JSON" }, 400); }
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return jsonResponse({ error: "Request body must be a JSON object" }, 400);
  }
  const action = typeof body.action === "string" ? body.action : "verse";

  try {
    if (action === "catalog") {
      const language = typeof body.language === "string" && (/^[a-z]{3}$/.test(body.language) || body.language === "all") ? body.language : "eng";
      const page = Number.isInteger(body.page) && body.page >= 1 && body.page <= 100 ? body.page : 1;
      const languageFilter = language === "all" ? "" : `language=${encodeURIComponent(language)}&`;
      const result = await apiBible(`/bibles?${languageFilter}limit=100&page=${page}`);
      if (!result.ok) return jsonResponse({ error: "Unable to list Bible versions from API.Bible", upstreamStatus: result.status }, 502);
      return jsonResponse({ data: result.body?.data ?? [], meta: result.body?.meta ?? {}, page, language });
    }
    if (action === "books" || action === "chapters") {
      const bibleId = body.bibleId;
      if (!validId(bibleId)) return jsonResponse({ error: "A valid bibleId is required" }, 400);
      if (action === "chapters" && !validId(body.bookId)) return jsonResponse({ error: "A valid bookId is required" }, 400);
      const suffix = action === "books"
        ? `/bibles/${encodeURIComponent(bibleId)}/books`
        : `/bibles/${encodeURIComponent(bibleId)}/books/${encodeURIComponent(body.bookId)}/chapters`;
      const result = await apiBible(suffix);
      if (!result.ok) return jsonResponse({ error: `API.Bible ${action} request failed`, upstreamStatus: result.status }, 502);
      return jsonResponse({ data: result.body?.data ?? [], meta: result.body?.meta ?? {} });
    }
    if (action === "chapter") {
      const { bibleId, chapterId } = body;
      if (!validId(bibleId) || !validId(chapterId)) return jsonResponse({ error: "Valid bibleId and chapterId are required" }, 400);
      const query = new URLSearchParams({
        "content-type": "json", "include-verse-numbers": "true",
        "include-notes": "false", "include-titles": "true", "include-chapter-numbers": "false",
      });
      const result = await apiBible(`/bibles/${encodeURIComponent(bibleId)}/chapters/${encodeURIComponent(chapterId)}?${query.toString()}`);
      if (!result.ok) return jsonResponse({ error: "API.Bible chapter download failed", upstreamStatus: result.status }, 502);
      return jsonResponse({ data: result.body?.data ?? {} });
    }
    // Backwards-compatible passage/verse lookup.
    const { bibleId, verseId } = body;
    if (!validId(bibleId) || !validId(verseId)) {
      return jsonResponse({ error: "Provide bibleId and verseId, or a supported action." }, 400);
    }
    const result = await apiBible(`/bibles/${encodeURIComponent(bibleId)}/verses/${encodeURIComponent(verseId)}`);
    if (!result.ok) return jsonResponse({ error: "API.Bible request failed", upstreamStatus: result.status }, 502);
    return jsonResponse(result.body);
  } catch (error) {
    console.error("API.Bible proxy error", error);
    return jsonResponse({ error: "Unable to reach API.Bible" }, 502);
  }
});
