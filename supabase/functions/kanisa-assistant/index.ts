import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const openAiKey = Deno.env.get("OPENAI_API_KEY");
const defaultModel = Deno.env.get("OPENAI_MODEL") || "gpt-5.6-luna";
const supabase = createClient(supabaseUrl, serviceRoleKey);

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json"
};

type Body = {
  message?: string;
  provider?: "cloud" | "personal" | "local";
  conversation?: Array<{ role: "user" | "assistant"; content: string }>;
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: cors });
}

function normalize(value: string) {
  return value.toLowerCase().replace(/[’']/g, "'").replace(/\s+/g, " ").trim();
}

async function churchContext() {
  const [identity, site, events, cms, media] = await Promise.all([
    supabase.from("church_identity").select("church_name,official_name,logo_url,official_logo,registration_details").eq("id", 1).maybeSingle(),
    supabase.from("site_content").select("key,value"),
    supabase.from("events").select("title,category,short_description,description,start_at,end_at,location,address,attendance_type,registration_url,contact,livestream_url").eq("status", "published").order("start_at").limit(30),
    supabase.from("cms_pages").select("slug,internal_name,menu_label,status,cms_sections(section_type,content,position)").eq("status", "published").order("slug"),
    supabase.from("media_items").select("title,type,category,description,url,thumbnail_url,featured").eq("published", true).order("created_at", { ascending: false }).limit(30)
  ]);

  return {
    identity: identity.data ?? {},
    site_content: Object.fromEntries((site.data ?? []).map((row: any) => [row.key, row.value])),
    events: events.data ?? [],
    pages: cms.data ?? [],
    media: media.data ?? []
  };
}

async function bibleContext(question: string) {
  const q = normalize(question);
  const explicit = q.match(/\b((?:1|2|3)\s+)?[a-z]+\s+\d{1,3}(?::\d{1,3}(?:[-–]\d{1,3})?)?\b/i);
  const reference = explicit?.[0] ?? "";
  const translation = "kjv";
  if (reference) {
    const m = reference.match(/^((?:1|2|3)\s+)?(.+?)\s+(\d+)(?::(\d+)(?:[-–](\d+))?)?$/i);
    if (m) {
      const bookName = (m[1] || "") + m[2];
      const chapter = Number(m[3]);
      const verseStart = m[4] ? Number(m[4]) : null;
      const verseEnd = m[5] ? Number(m[5]) : verseStart;
      const { data: books } = await supabase.from("bible_books").select("id,name,abbreviation").or(
        `name.ilike.%${bookName.trim()}%,abbreviation.ilike.%${bookName.trim()}%`
      ).limit(5);
      const book = books?.[0];
      if (book) {
        let query = supabase.from("bible_verses").select("book_id,chapter,verse,text").eq("translation_id", translation).eq("book_id", book.id).eq("chapter", chapter).order("verse");
        if (verseStart) query = query.gte("verse", verseStart);
        if (verseEnd) query = query.lte("verse", verseEnd);
        const { data: verses } = await query.limit(30);
        return { translation, reference, verses: verses ?? [] };
      }
    }
  }

  const terms = q.split(/\s+/).filter(x => x.length >= 4).slice(0, 5);
  if (!terms.length) return { translation, reference: "", verses: [] };
  const filters = terms.map(term => `text.ilike.%${term.replace(/[%_]/g, "")}%`).join(",");
  const { data: verses } = await supabase.from("bible_verses")
    .select("book_id,chapter,verse,text")
    .eq("translation_id", translation)
    .or(filters)
    .limit(8);
  return { translation, reference: "", verses: verses ?? [] };
}

function buildPrompt(context: any, bible: any, message: string, history: any[]) {
  const church = JSON.stringify(context);
  const scripture = JSON.stringify(bible);
  const recent = JSON.stringify((history || []).slice(-8));
  return `You are Kanisa Assistant, the official AI assistant for a Christian church mobile app.

STRICT SOURCE RULES:
- Never use the church website as a source.
- Church facts must come only from the supplied Supabase church context.
- Do not invent service times, events, leaders, contact details, ministries, giving instructions, locations, or announcements.
- If a church fact is missing or ambiguous, say that the current published church data does not provide a reliable answer.
- Bible quotations/references must come from the supplied Bible context. Never invent a Bible reference.
- Distinguish Scripture from explanation or interpretation.
- You may answer general Christian questions, but do not present personal theological interpretation as an official church doctrine unless the supplied church context says so.
- Be warm, concise and useful. Do not claim to be a pastor or human.
- If the user asks for something private about another member, refuse to expose it.

CHURCH CONTEXT:
${church}

BIBLE CONTEXT:
${scripture}

RECENT CONVERSATION:
${recent}

USER QUESTION:
${message}

Return only the answer text. When Bible references are relevant, include them naturally in formats such as Matthew 6:14–15. Do not fabricate references.`;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return json({ error: "Method not allowed." }, 405);
  if (!openAiKey) return json({ error: "Cloud AI is not configured yet. Add OPENAI_API_KEY to the Supabase function secrets." }, 503);

  const authHeader = req.headers.get("Authorization") || "";
  const token = authHeader.replace(/^Bearer\s+/i, "");
  if (!token) return json({ error: "Authentication required." }, 401);

  const { data: userData, error: userError } = await supabase.auth.getUser(token);
  if (userError || !userData.user) return json({ error: "Your session is not valid. Please sign in again." }, 401);

  let body: Body;
  try { body = await req.json(); } catch { return json({ error: "Invalid request body." }, 400); }

  const message = String(body.message || "").trim().slice(0, 2000);
  if (!message) return json({ error: "Ask a question to continue." }, 400);

  const provider = body.provider || "cloud";
  if (provider !== "cloud") {
    return json({
      error: provider === "local"
        ? "Local AI is not connected in this build yet."
        : "Personal API providers are not connected in this build yet."
    }, 501);
  }

  const [{ data: settings }, context, bible] = await Promise.all([
    supabase.from("ai_assistant_settings").select("enabled,assistant_name,welcome_message,cloud_ai_enabled,bible_enabled,model").eq("id", 1).maybeSingle(),
    churchContext(),
    bibleContext(message)
  ]);

  if (settings && (!settings.enabled || !settings.cloud_ai_enabled)) {
    return json({ error: "The Kanisa Assistant is currently disabled." }, 403);
  }

  const prompt = buildPrompt(context, settings?.bible_enabled === false ? { verses: [] } : bible, message, body.conversation || []);
  const model = settings?.model || defaultModel;

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${openAiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model,
      input: prompt,
      max_output_tokens: 700
    })
  });

  const payload = await response.json();
  if (!response.ok) {
    console.error("OpenAI error", response.status, payload);
    return json({ error: "The AI service is temporarily unavailable. Please try again." }, 502);
  }

  const answer = String(payload.output_text || payload.output?.flatMap((item: any) => item.content || []).map((part: any) => part.text || "").join("") || "").trim();
  if (!answer) return json({ error: "The assistant returned an empty response." }, 502);

  return json({
    assistant_name: settings?.assistant_name || "Kanisa Assistant",
    answer,
    bible_references: bible.reference ? [bible.reference] : [],
    provider: "cloud",
    model
  });
});
