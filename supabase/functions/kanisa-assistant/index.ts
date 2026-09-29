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
  room_id?: string;
  reply_to_message_id?: string;
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
  const translation = /\b(web|world english bible)\b/i.test(q) ? "web" : "kjv";

  const { data: books } = await supabase
    .from("bible_books")
    .select("id,name,abbreviation")
    .order("id");

  const escapeRegex = (value: string) =>
    value.replace(/[.*+?^$\{\}()|[\]\\]/g, "\\$&");

  const addAlias = (set: Set<string>, value: unknown) => {
    const raw = String(value || "")
      .toLowerCase()
      .replace(/\./g, "")
      .replace(/\s+/g, " ")
      .trim();
    if (!raw) return;
    set.add(raw);

    const numbered = raw.match(/^(1|2|3)\s+(.+)$/);
    if (numbered) {
      const n = numbered[1];
      const name = numbered[2];
      const first = name.split(/\s+/)[0];
      const short = first.slice(0, 3);
      set.add(`${n} ${name}`);
      set.add(`${n} ${first}`);
      set.add(`${n} ${short}`);
      set.add(`${n}st ${name}`);
      set.add(`${n}nd ${name}`);
      set.add(`${n}rd ${name}`);
      set.add(`${n}st ${first}`);
      set.add(`${n}nd ${first}`);
      set.add(`${n}rd ${first}`);
      set.add(`${n}st ${short}`);
      set.add(`${n}nd ${short}`);
      set.add(`${n}rd ${short}`);

      // Common Roman-numeral forms: I Corinthians, II Timothy, III John.
      const roman = n === "1" ? "i" : n === "2" ? "ii" : "iii";
      set.add(`${roman} ${name}`);
      set.add(`${roman} ${first}`);
      set.add(`${roman} ${short}`);
    }
  };

  const candidates = (books || [])
    .flatMap((book: any) => {
      const aliases = new Set<string>();
      addAlias(aliases, book.name);
      addAlias(aliases, book.abbreviation);
      return [...aliases].map(alias => ({ book, alias }));
    })
    .sort((a, b) => b.alias.length - a.alias.length);

  // Match a real Bible book followed by chapter:verse. The book name must come
  // from bible_books, so question words such as "does", "say", or "mean" can
  // never accidentally become part of the book name.
  for (const candidate of candidates) {
    const pattern = new RegExp(
      "(?:^|[^a-z0-9])" +
      escapeRegex(candidate.alias) +
      "\\s*(?:chapter\\s*)?(\\d{1,3})\\s*[:;,]\\s*(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?(?=$|[^0-9])",
      "i"
    );
    const match = q.match(pattern);
    if (!match) continue;

    const chapter = Number(match[1]);
    const verseStart = Number(match[2]);
    const verseEnd = match[3] ? Number(match[3]) : verseStart;

    const { data: verses } = await supabase
      .from("bible_verses")
      .select("book_id,chapter,verse,text")
      .eq("translation_id", translation)
      .eq("book_id", candidate.book.id)
      .eq("chapter", chapter)
      .gte("verse", verseStart)
      .lte("verse", verseEnd)
      .order("verse")
      .limit(30);

    const canonicalReference =
      `${candidate.book.name} ${chapter}:${verseStart}` +
      (verseEnd !== verseStart ? `-${verseEnd}` : "");

    return {
      translation,
      reference: canonicalReference,
      references: [canonicalReference],
      verses: verses ?? []
    };
  }

  // Only perform topic/concept Bible retrieval when the user actually asks
  // for Bible/Scripture material. Otherwise ordinary church questions such as
  // "what are the service times?" must not silently inject Bible context.
  const explicitBibleRequest =
    /\bbible\b|\bscripture\b|\bverse\b|\bverses\b|\bpassage\b|\baccording to (the )?bible\b|\bwhat does (the )?bible say\b|\bwhat do (the )?scriptures say\b|\bshow me (a )?(bible )?verse\b|\bmore scriptures\b|\bscripts? about\b/i.test(q);

  if (!explicitBibleRequest) {
    return {
      books: books || [],
      translation,
      reference: "",
      references: [],
      verses: []
    };
  }

  // Topic/concept search: retrieve Bible text from Supabase first, then let the
  // response layer explain only what was actually retrieved. We expand concepts
  // lexically rather than hard-coding Scripture references.
  const stopWords = new Set([
    "what", "does", "about", "tell", "show", "give", "with", "from",
    "this", "that", "bible", "verse", "verses", "says", "say", "please",
    "can", "you", "mean", "means", "explain", "explanation", "according",
    "teach", "teaches", "teaching", "scripture", "passage", "chapter",
    "the", "are", "is", "of", "on", "for", "to", "in", "how", "why"
  ]);

  const rawTerms = q
    .replace(/[^a-z0-9\s'-]/gi, " ")
    .split(/\s+/)
    .filter(x => x.length >= 3 && !stopWords.has(x));

  const conceptExpansions: Record<string, string[]> = {
    prayer: ["prayer", "pray", "praying", "prayed", "supplication", "intercession"],
    faith: ["faith", "believe", "believeth", "believed", "believing", "trust"],
    love: ["love", "loveth", "loved", "charity"],
    forgiveness: ["forgive", "forgiven", "forgiveness", "forgiving", "mercy"],
    hope: ["hope", "hopeth", "hoped", "trust"],
    peace: ["peace", "peaceable", "reconcile", "reconciliation"],
    wisdom: ["wisdom", "wise", "understanding", "knowledge"],
    obedience: ["obey", "obeyed", "obedience", "keep", "commandments"],
    salvation: ["save", "saved", "salvation", "redeemed", "redemption"],
    temptation: ["tempt", "tempted", "temptation", "sin"],
    fasting: ["fast", "fasted", "fasting"],
    worship: ["worship", "worshipped", "praise", "praises", "praising"],
    anxiety: ["anxiety", "careful", "worry", "worrying", "trouble"],
    fear: ["fear", "afraid", "fearing"],
    strength: ["strength", "strong", "strengthen"],
    healing: ["heal", "healed", "healing", "sick", "sickness"],
    patience: ["patience", "patient", "wait", "waiting", "longsuffering"],
    humility: ["humble", "humility", "meek", "meekness"],
    repentance: ["repent", "repented", "repentance", "turn", "confess"]
  };

  const terms = [...new Set(rawTerms.flatMap(term =>
    conceptExpansions[term] || [term]
  ))].slice(0, 12);

  if (!terms.length) return { books: books || [], translation, reference: "", references: [], verses: [] };

  const filters = terms
    .map(term => `text.ilike.%${term.replace(/[%_]/g, "")}%`)
    .join(",");

  const { data: rawCandidates, error: bibleSearchError } = await supabase
    .from("bible_verses")
    .select("book_id,chapter,verse,text")
    .eq("translation_id", translation)
    .or(filters)
    .limit(80);

  if (bibleSearchError) {
    console.error("Bible topic search error", bibleSearchError);
  }

  const candidatesWithScore = (rawCandidates ?? []).map((verse: any) => {
    const text = String(verse.text || "").toLowerCase();
    let score = 0;
    for (const term of terms) {
      const occurrences = text.split(term.toLowerCase()).length - 1;
      if (occurrences > 0) score += Math.min(occurrences, 3);
    }

    // Prefer passages that contain the concept directly rather than incidental
    // uses of "pray" as the expression "I pray thee".
    if (/\bprayer\b|\bpraying\b|\bsupplication\b|\bintercession\b/.test(text)) score += 4;
    if (/\bi pray thee\b/.test(text)) score -= 3;

    return { verse, score };
  }).sort((a: any, b: any) =>
    b.score - a.score ||
    Number(a.verse.book_id) - Number(b.verse.book_id) ||
    Number(a.verse.chapter) - Number(b.verse.chapter) ||
    Number(a.verse.verse) - Number(b.verse.verse)
  );

  const verses = candidatesWithScore.slice(0, 20).map((item: any) => item.verse);
  const bookNames = new Map((books || []).map((book: any) => [book.id, book.name]));
  const references = [...new Set(
    verses.map((verse: any) => {
      const name = bookNames.get(verse.book_id);
      return name ? `${name} ${verse.chapter}:${verse.verse}` : "";
    }).filter(Boolean)
  )].slice(0, 12);

  return { books: books || [], translation, reference: "", references, verses };
}
function verifiedUserId(req: Request): string | null {
  const header = req.headers.get("Authorization") || "";
  const token = header.replace(/^Bearer\\s+/i, "").trim();
  if (!token) return null;

  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - normalized.length % 4) % 4);
    const claims = JSON.parse(atob(padded));
    return typeof claims.sub === "string" && claims.sub ? claims.sub : null;
  } catch {
    return null;
  }
}

async function persistRoomMessage(
  req: Request,
  body: Body,
  answer: string,
  bibleReferences: string[],
  bibleQuotes: Array<{ reference: string; text: string; translation: string }>
): Promise<string | null> {
  const roomId = String(body.room_id || "").trim();
  if (!roomId) return null;

  const userId = verifiedUserId(req);
  if (!userId) return null;

  const { data: membership, error: membershipError } = await supabase
    .from("chat_room_members")
    .select("room_id")
    .eq("room_id", roomId)
    .eq("user_id", userId)
    .maybeSingle();

  if (membershipError || !membership) {
    console.error("Kanisa room persistence: user is not a room member", membershipError);
    return null;
  }

  let replyToId: string | null = null;
  const requestedReplyId = String(body.reply_to_message_id || "").trim();
  if (requestedReplyId) {
    const { data: replyTarget } = await supabase
      .from("chat_messages")
      .select("id,room_id")
      .eq("id", requestedReplyId)
      .eq("room_id", roomId)
      .maybeSingle();
    if (replyTarget) replyToId = replyTarget.id;
  }

  const messageId = crypto.randomUUID();
  const { error } = await supabase.from("kanisa_room_messages").insert({
    id: messageId,
    room_id: roomId,
    user_id: userId,
    message: answer.trim().slice(0, 4000),
    bible_references: bibleReferences.slice(0, 12),
    bible_quotes: bibleQuotes.slice(0, 12),
    reply_to_message_id: replyToId
  });

  if (error) {
    console.error("Kanisa room persistence failed", error);
    return null;
  }

  return messageId;
}

function firstValue(obj: any, keys: string[]) {
  for (const key of keys) {
    const value = obj?.[key];
    if (value !== undefined && value !== null && String(value).trim()) return String(value).trim();
  }
  return "";
}

function localAnswer(context: any, bible: any, message: string, settings: any) {
  const assistantName = settings?.assistant_name || "Kanisa Assistant";
  const q = normalize(message);
  const churchName =
    firstValue(context.identity, ["church_name", "official_name"]) ||
    "the church";
  const site = context.site_content || {};
  const events = Array.isArray(context.events) ? context.events : [];
  const media = Array.isArray(context.media) ? context.media : [];
  const pages = Array.isArray(context.pages) ? context.pages : [];

  const page = (slug: string) => pages.find((item: any) => item.slug === slug);
  const pageText = (slug: string) => {
    const item = page(slug);
    if (!item) return "";
    const sections = Array.isArray(item.cms_sections) ? item.cms_sections : [];
    return sections
      .sort((a: any, b: any) => Number(a.position || 0) - Number(b.position || 0))
      .map((section: any) => {
        const content = section.content;
        if (typeof content === "string") return content;
        try { return JSON.stringify(content); } catch { return ""; }
      })
      .filter(Boolean)
      .join("\n");
  };

  if (/^(hi|hello|hey|habari|shalom)\\b/.test(q)) {
    return `Hello! 👋 I’m ${assistantName}. I can help you with ${churchName}, service times, events, media, giving, contact information and Bible questions.`;
  }

  if (/who (are|is) you|what (are|is) you|your name/.test(q)) {
    return `I’m ${assistantName}, the digital assistant for ${churchName}. I use information published in Kanisa and the Bible data available to the app.`;
  }

  if (/church name|name of (our|the) church|which church/.test(q)) {
    return `The church name currently published in Kanisa is ${churchName}.`;
  }

  if (/about (the )?church|who (are|is) (we|the church)|what is the church about|mission|vision/.test(q)) {
    const about = firstValue(site, ["aboutText", "about", "church_description"]) || pageText("about");
    return about
      ? `Here is the published information about ${churchName}:\\n\\n${about}`
      : `I couldn't find published information about ${churchName} in the current church data.`;
  }

  if (/contact|phone|telephone|call|email|e-mail|reach (the )?church/.test(q)) {
    const phone = firstValue(site, ["phone", "contactPhone", "contact_phone"]);
    const email = firstValue(site, ["email", "contactEmail", "contact_email"]);
    const address = firstValue(site, ["address", "contactAddress", "location", "churchAddress"]);
    const lines = [
      phone ? `Phone: ${phone}` : "",
      email ? `Email: ${email}` : "",
      address ? `Address: ${address}` : ""
    ].filter(Boolean);
    return lines.length
      ? `Here is the published church contact information:\\n\\n${lines.join("\\n")}`
      : `I couldn't find published contact details in the current church data.`;
  }

  if (/give|giving|offering|tithe|tithes|donat|contribut/.test(q)) {
    const give = pageText("give");
    const givingKeys = Object.entries(site).filter(([key, value]) =>
      /give|giving|offering|tithe|donat|mpesa|paybill|account|bank/i.test(key) && String(value).trim()
    );
    if (give) return `Here is the published giving information:\\n\\n${give}`;
    if (givingKeys.length) {
      return `Here is the published giving information:\\n\\n${givingKeys.slice(0, 12).map(([key, value]) =>
        `${key.replace(/[_-]+/g, " ")}: ${value}`
      ).join("\\n")}`;
    }
    return "I couldn't find published giving instructions in the current church data. Please check the Giving section in Kanisa.";
  }

  if (/visit|where (is|are)|location|address|directions|find (the )?church/.test(q)) {
    const visit = pageText("visit-us");
    const address = firstValue(site, ["address", "contactAddress", "location", "churchAddress"]);
    if (visit) return `Here is the published visitor information:\\n\\n${visit}`;
    if (address) return `The published church location is: ${address}`;
    return "I couldn't find a published church address or location in the current church data.";
  }

  if (/youth|young people|teen|teenager|young adult/.test(q)) {
    const youth = pageText("youth");
    return youth
      ? `Here is the published youth information:\\n\\n${youth}`
      : "I couldn't find published youth-ministry information in the current church data.";
  }

  if (/event|upcoming|what('s| is) happening|calendar/.test(q)) {
    if (!events.length) return "There are no published upcoming events in the church database right now.";
    const lines = events.slice(0, 8).map((event: any) => {
      const date = event.start_at ? new Date(event.start_at).toLocaleString("en-KE", {
        dateStyle: "medium",
        timeStyle: "short"
      }) : "Date not published";
      const location = event.location || event.address ? ` — ${event.location || event.address}` : "";
      return `• ${event.title || "Untitled event"} — ${date}${location}`;
    });
    return `Here are the published events I can find:\\n\\n${lines.join("\\n")}`;
  }

  if (/service|worship time|church time|when (do|does) (we|the church) meet/.test(q)) {
    const raw = firstValue(site, ["services", "service_times", "serviceTimes", "worship_times"]);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length) {
          return `Here are the published service times:\\n\\n${parsed.map((item: any) =>
            `• ${item.title || "Service"} — ${item.time || "Time not published"}`
          ).join("\\n")}`;
        }
      } catch {
        // Fall through to ordinary site-content fields.
      }
    }
    const candidates = Object.entries(site).filter(([key, value]) =>
      /service|worship|sunday|saturday|meeting|time/i.test(key) && String(value).trim()
    );
    if (candidates.length) {
      return candidates.slice(0, 8).map(([key, value]) => `${key.replace(/[_-]+/g, " ")}: ${value}`).join("\\n");
    }
    return "I couldn't find published service times in the current church data.";
  }

  if (/media|video|sermon|livestream|live stream/.test(q)) {
    if (!media.length) return "There are no published media items available right now.";
    const lines = media.slice(0, 8).map((item: any) =>
      `• ${item.title || "Untitled media"}${item.type ? ` — ${item.type}` : ""}`
    );
    return `Here are some published media items:\\n\\n${lines.join("\\n")}`;
  }

  if (/kiswahili|swahili|kikamba|kamba bible|kitui bible/.test(q)) {
    const requested = /kikamba|kamba|kitui/.test(q) ? "Kikamba" : "Kiswahili";
    return `${requested} Bible data is not currently enabled in the assistant's Bible database. The currently enabled translations are KJV and WEB. I won't pretend to quote a translation that isn't available.`;
  }

  if (bible?.verses?.length) {
    const verses = bible.verses.slice(0, 8);
    const reference = bible.reference || "";
    const scripture = verses.map((v: any) => `${v.verse}. ${v.text}`).join("\n");
    if (reference) {
      const translationName = bible.translation === "web" ? "WEB" : "KJV";
      return `Here is ${translationName} Scripture for ${reference}:\\n\\n${scripture}`;
    }
    return `I found these relevant Bible passages in the Kanisa database:\\n\\n${verses.map((v: any) => `• ${v.text}`).join("\\n")}\\n\\nAsk me for a specific reference, such as John 3:16, for a precise result.`;
  }

  if (/help|what can you do|how can you help/.test(q)) {
    return `I can help with ${churchName}, service times, events, media, giving, contact and visitor information, youth information, and Bible passages available in Kanisa. Ask me a specific question and I’ll use the published data available to me.`;
  }

  return `I’m currently operating in local mode. I can answer from the church and Bible information available in Kanisa, but I don’t have enough published data for that question yet. Try asking about service times, events, giving, contact details, media, or a specific Bible passage.`;
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
- Bible quotations/references must come from the supplied Bible context. Never invent a Bible reference or quote.
- For Bible concept/topic questions such as "what does the Bible say about prayer?", use the supplied BIBLE CONTEXT as the source. Select the most relevant passages from it and cite their exact supplied references in the answer.
- When BIBLE CONTEXT contains relevant references, do not answer a Bible concept question from general model memory alone.
- Distinguish Scripture from explanation or interpretation.
- When relevant Scripture is supplied, place the explanation immediately before or after the relevant passage. Do not put all references in a separate list.
- Never paraphrase a supplied Scripture passage while presenting it as a quotation.
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


  // Supabase is configured with verify_jwt=true for this function. The Edge gateway
  // validates the bearer token before this handler runs. Do not call auth.getUser()
  // again with the client service-role key here: that duplicate validation can reject
  // otherwise-valid ES256 sessions and turn a healthy request into a 401.
  const authHeader = req.headers.get("Authorization") || "";
  if (!/^Bearer\s+\S+/i.test(authHeader)) {
    return json({ error: "Authentication required." }, 401);
  }

  let body: Body;
  try { body = await req.json(); } catch { return json({ error: "Invalid request body." }, 400); }

  const message = String(body.message || "").trim().slice(0, 2000);
  if (!message) return json({ error: "Ask a question to continue." }, 400);

  const provider = body.provider || (openAiKey ? "cloud" : "local");

  const [{ data: settings }, context, bible] = await Promise.all([
    supabase.from("ai_assistant_settings").select("enabled,assistant_name,welcome_message,cloud_ai_enabled,bible_enabled,model").eq("id", 1).maybeSingle(),
    churchContext(),
    bibleContext(message)
  ]);

  if (settings?.enabled === false) {
    return json({ error: "The Kanisa Assistant is currently disabled." }, 403);
  }

  const usableBible = settings?.bible_enabled === false
    ? { verses: [], reference: "", references: [] }
    : bible;

  const finalize = async (
    answer: string,
    providerName: string,
    modelName: string | null
  ) => {
    const references = usableBible.references?.length
      ? usableBible.references
      : usableBible.reference
        ? [usableBible.reference]
        : [];
    const allBibleQuotes = (usableBible?.verses || []).slice(0, 20).map((verse: any) => {
      const name = (usableBible?.books || []).find((book: any) => book.id === verse.book_id)?.name;
      return name && verse.text
        ? { reference: name + " " + verse.chapter + ":" + verse.verse, text: String(verse.text), translation: usableBible.translation === "web" ? "WEB" : "KJV" }
        : null;
    }).filter(Boolean);
    const bibleQuotes = allBibleQuotes
      .filter((quote: any) => answer.includes(quote.reference) || answer.includes(quote.reference.replace("-", "–")))
      .slice(0, 6);
    // Never attach Bible cards merely because Bible context was available.
    // A Scripture card is shown only when the answer itself cites that passage.
    const selectedBibleQuotes = bibleQuotes;
    const roomMessageId = await persistRoomMessage(req, body, answer, references, selectedBibleQuotes);

    return json({
      assistant_name: settings?.assistant_name || "Kanisa Assistant",
      answer,
      bible_references: references,
      bible_quotes: selectedBibleQuotes,
      provider: providerName,
      model: modelName,
      room_message_id: roomMessageId
    });
  };

  // Local mode is the guaranteed baseline. It uses Supabase church/Bible data and
  // does not require an external AI provider or API key.
  if (provider === "local" || !openAiKey || settings?.cloud_ai_enabled === false) {
    return await finalize(
      localAnswer(context, usableBible, message, settings),
      "local",
      null
    );
  }

  if (provider === "personal") {
    return await finalize(
      localAnswer(context, usableBible, message, settings),
      "local",
      null
    );
  }

  const prompt = buildPrompt(context, usableBible, message, body.conversation || []);
  const model = settings?.model || defaultModel;

  try {
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
      console.error("OpenAI error; falling back to local mode", response.status, payload);
      return await finalize(
        localAnswer(context, usableBible, message, settings),
        "local",
        null
      );
    }

    const answer = String(payload.output_text || payload.output?.flatMap((item: any) => item.content || []).map((part: any) => part.text || "").join("") || "").trim();
    if (!answer) {
      return await finalize(
        localAnswer(context, usableBible, message, settings),
        "local",
        null
      );
    }

    return await finalize(answer, "cloud", model);
  } catch (error) {
    console.error("OpenAI request failed; falling back to local mode", error);
    return await finalize(
      localAnswer(context, usableBible, message, settings),
      "local",
      null
    );
  }
});
