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
  translation?: string;
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

async function bibleContext(question: string, history: Array<{ role: "user" | "assistant"; content: string }> = [], requestedTranslation?: string) {
  const q = normalize(question);
  const lastAssistant = [...history].reverse().find(item => item.role === "assistant")?.content || "";
  const contextualFollowUp =
    /\b(this|that|these|those|it|the passage|the verse|the scripture|the scriptures)\b/i.test(q) ||
    /\b(explain|expand|elaborate|more scriptures|another verse|another passage)\b/i.test(q);
  const bibleSearchQuestion = contextualFollowUp && lastAssistant
    ? q + " " + lastAssistant
    : q;
  const translation = requestedTranslation && requestedTranslation.trim().length > 0
    ? requestedTranslation.toLowerCase().trim()
    : /\b(web|world english bible)\b/i.test(q) ? "web" : "kjv";

  const [
    { data: books },
    { data: translationRecord, error: translationError },
    { data: translationProbe, error: translationProbeError }
  ] = await Promise.all([
    supabase.from("bible_books").select("id,name,abbreviation").order("id"),
    supabase.from("bible_translations")
      .select("id,abbreviation,name,language_code,language_name")
      .eq("id", translation)
      .eq("is_enabled", true)
      .maybeSingle(),
    supabase.from("bible_verses")
      .select("book_id")
      .eq("translation_id", translation)
      .limit(1)
  ]);

  // Never silently substitute KJV when the user has selected another translation.
  // Verify both its metadata and that at least some verse text is actually stored.
  if (translationError || !translationRecord || translationProbeError || !translationProbe?.length) {
    console.error("Selected Bible translation is unavailable", translation, translationError);
    return {
      books: books || [],
      translation,
      translationNotAvailable: true,
      reference: "",
      references: [],
      verses: []
    };
  }

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

  const kambaBookNames: Record<string, string> = {
    GEN: "Mwambĩlĩlyo", EXO: "Kuma", LEV: "Alivai", NUM: "Motalo",
    DEU: "Kũtũngĩlĩlwa Kwa Mĩao", JOS: "Yosua", JDG: "Asili", RUT: "Luthi",
    "1SA": "1 Samueli", "2SA": "2 Samueli", "1KI": "1 Asumbĩ", "2KI": "2 Asumbĩ",
    "1CH": "1 Mavinda", "2CH": "2 Mavinda", EZR: "Esala", NEH: "Neemia",
    EST: "Esita", JOB: "Yovu", PSA: "Savuli", PRO: "Nthimo", ECC: "Mũtavan'ya",
    SNG: "Wathi", ISA: "Isaia", JER: "Yelemai", LAM: "Iĩo", EZK: "Esekieli",
    DAN: "Ndanieli", HOS: "Osea", JOL: "Yoeli", AMO: "Amosi", OBA: "Ovatia",
    JON: "Yona", MIC: "Mika", NAM: "Naumu", HAB: "Avakuki", ZEP: "Nzevania",
    HAG: "Akai", ZEC: "Nzekalia", MAL: "Malaki", MAT: "Mathayo", MRK: "Maliko",
    LUK: "Luka", JHN: "Yoana", ACT: "Meko Ma Atũmwa", ROM: "Alomi",
    "1CO": "1 Akolintho", "2CO": "2 Akolintho", GAL: "Akalatia", EPH: "Aeveso",
    PHP: "Avilivi", COL: "Akolosai", "1TH": "1 Athesalonika", "2TH": "2 Athesalonika",
    "1TI": "1 Timotheo", "2TI": "2 Timotheo", TIT: "Tito", PHM: "Vilemona",
    HEB: "Aevelania", JAS: "Yakobo", "1PE": "1 Vetelo", "2PE": "2 Vetelo",
    "1JN": "1 Yoana", "2JN": "2 Yoana", "3JN": "3 Yoana", JUD: "Yuta", REV: "Ũvuan'yo"
  };

  const candidates = (books || [])
    .flatMap((book: any) => {
      const aliases = new Set<string>();
      addAlias(aliases, book.name);
      addAlias(aliases, book.abbreviation);
      if (translation === "kam") addAlias(aliases, kambaBookNames[String(book.id)] || "");
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
      "\\s*(?:chapter\\s*)?(\\d{1,3})\\s*(?::|[;,]|\\s+verse\\s+)\\s*(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?(?=$|[^0-9])",
      "i"
    );
    const match = bibleSearchQuestion.match(pattern);
    if (!match) continue;

    const chapter = Number(match[1]);
    const verseStart = Number(match[2]);
    const verseEnd = match[3] ? Number(match[3]) : verseStart;
    const canonicalReference =
      `${candidate.book.name} ${chapter}:${verseStart}` +
      (verseEnd !== verseStart ? `-${verseEnd}` : "");

    if (chapter < 1 || verseStart < 1 || verseEnd < verseStart || verseEnd - verseStart > 29) {
      return {
        books: books || [],
        translation,
        translationName: translationRecord.name,
        translationAbbreviation: translationRecord.abbreviation,
        translationLanguageCode: translationRecord.language_code,
        reference: canonicalReference,
        referenceNotFound: true,
        references: [],
        verses: []
      };
    }

    const { data: verses, error: verseError } = await supabase
      .from("bible_verses")
      .select("book_id,chapter,verse,text")
      .eq("translation_id", translation)
      .eq("book_id", candidate.book.id)
      .eq("chapter", chapter)
      .gte("verse", verseStart)
      .lte("verse", verseEnd)
      .order("verse")
      .limit(30);

    const exactRangeFound = !verseError &&
      (verses || []).length === verseEnd - verseStart + 1 &&
      (verses || []).every((verse: any, index: number) =>
        Number(verse.verse) === verseStart + index && Boolean(String(verse.text || "").trim())
      );

    return {
      books: books || [],
      translation,
      translationName: translationRecord.name,
      translationAbbreviation: translationRecord.abbreviation,
      translationLanguageCode: translationRecord.language_code,
      reference: canonicalReference,
      referenceNotFound: !exactRangeFound,
      references: exactRangeFound ? [canonicalReference] : [],
      verses: exactRangeFound ? (verses || []) : []
    };
  }

  // Only perform topic/concept Bible retrieval when the user actually asks
  // for Bible/Scripture material. Otherwise ordinary church questions such as
  // "what are the service times?" must not silently inject Bible context.
  const explicitBibleRequest =
    /\bbible\b|\bscripture\b|\bverse\b|\bverses\b|\bpassage\b|\baccording to (the )?bible\b|\bwhat does (the )?bible say\b|\bwhat do (the )?scriptures say\b|\bshow me (a )?(bible )?verse\b|\bmore scriptures\b|\bscripts? about\b/i.test(q);

  const contextualBibleRequest =
    contextualFollowUp &&
    lastAssistant &&
    /\b[A-Za-z]{2,}(?:\s+[A-Za-z]{2,})?\s+\d{1,3}:\d{1,3}/.test(lastAssistant);

  if (!explicitBibleRequest && !contextualBibleRequest) {
    return {
      books: books || [],
      translation,
      translationName: translationRecord.name,
      translationAbbreviation: translationRecord.abbreviation,
      translationLanguageCode: translationRecord.language_code,
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

  if (!terms.length) return {
    books: books || [],
    translation,
    translationName: translationRecord.name,
    translationAbbreviation: translationRecord.abbreviation,
    translationLanguageCode: translationRecord.language_code,
    reference: "",
    references: [],
    verses: []
  };

  const filters = terms
    .map(term => `text.ilike.%${term.replace(/[%_]/g, "")}%`)
    .join(",");

  // The Kamba text is not English, so English topic words cannot reliably find
  // Kamba verses. Discover candidate references from KJV, then retrieve the
  // actual verse text from the selected Kamba translation below.
  const discoveryTranslation = translation === "kam" ? "kjv" : translation;
  const { data: rawCandidates, error: bibleSearchError } = await supabase
    .from("bible_verses")
    .select("book_id,chapter,verse,text")
    .eq("translation_id", discoveryTranslation)
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

  const rankedCandidates = candidatesWithScore.slice(0, 20).map((item: any) => item.verse);
  let verses: any[] = rankedCandidates;

  if (translation === "kam") {
    const translated = await Promise.all(rankedCandidates.slice(0, 12).map(async (candidate: any) => {
      const { data, error } = await supabase
        .from("bible_verses")
        .select("book_id,chapter,verse,text")
        .eq("translation_id", translation)
        .eq("book_id", candidate.book_id)
        .eq("chapter", candidate.chapter)
        .eq("verse", candidate.verse)
        .maybeSingle();
      if (error) console.error("Kamba verse lookup failed", candidate, error);
      return data && String(data.text || "").trim() ? data : null;
    }));
    verses = translated.filter(Boolean);
  }

  const bookNames = new Map((books || []).map((book: any) => [book.id, book.name]));
  const references = [...new Set(
    verses.map((verse: any) => {
      const name = bookNames.get(verse.book_id);
      return name ? `${name} ${verse.chapter}:${verse.verse}` : "";
    }).filter(Boolean)
  )].slice(0, 12);

  return {
    books: books || [],
    translation,
    translationName: translationRecord.name,
    translationAbbreviation: translationRecord.abbreviation,
    translationLanguageCode: translationRecord.language_code,
    reference: "",
    references,
    verses
  };
}
function verifiedUserId(req: Request): string | null {
  const header = req.headers.get("Authorization") || "";
  const token = header.replace(/^Bearer\s+/i, "").trim();
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

function normalizeCitationText(value: string) {
  return value
    .normalize("NFKD")
    .replace(/[’']/g, "'")
    .replace(/[‐‑‒–—]/g, "-")
    .replace(/[^a-z0-9:-]+/gi, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function answerCitesReference(answer: string, reference: string): boolean {
  const answerText = normalizeCitationText(answer);
  const refText = normalizeCitationText(reference);
  if (!answerText || !refText) return false;
  if (answerText.includes(refText)) return true;

  // If the retrieved context is a range, also accept a citation to its first
  // verse (for example, "John 3:16" when context contains "John 3:16-18").
  const range = refText.match(/^(.*?)\s+(\d+):(\d+)-(\d+)$/);
  if (range) {
    const firstVerse = `${range[1]} ${range[2]}:${range[3]}`;
    return answerText.includes(firstVerse);
  }
  return false;
}

function citedVerifiedReferences(answer: string, bible: any): string[] {
  const available = Array.isArray(bible?.references) ? bible.references : [];
  const requested = String(bible?.reference || "").trim();
  const candidates = [...available, ...(requested ? [requested] : [])];
  return [...new Set(candidates.filter(reference => answerCitesReference(answer, String(reference))))].slice(0, 12);
}

function unsupportedBibleReferences(answer: string, bible: any): string[] {
  if (!answer.trim() || !bible || bible.translationNotAvailable || bible.referenceNotFound) return [];

  const books = Array.isArray(bible.books) ? bible.books : [];
  const escapeRegex = (value: string) => value.replace(/[.*+?^${}()|[\\]\\\\]/g, "\\\\$&");
  const supplied = new Set<string>(
    [
      ...(Array.isArray(bible.references) ? bible.references : []),
      ...(String(bible.reference || "").trim() ? [String(bible.reference).trim()] : [])
    ].map((reference: string) => normalizeCitationText(reference))
  );

  const detected: string[] = [];
  for (const book of books) {
    const name = String(book?.name || "").trim();
    if (!name) continue;
    const pattern = new RegExp(
      "(?:^|[^a-z0-9])" + escapeRegex(name) +
      "\\s+(\\d{1,3}):(\\d{1,3})(?:\\s*[-–—]\\s*(\\d{1,3}))?(?=$|[^0-9])",
      "gi"
    );
    for (const match of answer.matchAll(pattern)) {
      const chapter = Number(match[1]);
      const startVerse = Number(match[2]);
      const endVerse = match[3] ? Number(match[3]) : startVerse;
      const citation = `${name} ${chapter}:${startVerse}` +
        (endVerse !== startVerse ? `-${endVerse}` : "");
      const normalized = normalizeCitationText(citation);

      let verified = supplied.has(normalized);
      // Permit a combined range only when every verse in it was retrieved,
      // or the exact requested range was verified from the selected translation.
      if (!verified && endVerse >= startVerse && endVerse - startVerse <= 29) {
        verified = true;
        for (let verse = startVerse; verse <= endVerse; verse++) {
          if (!supplied.has(normalizeCitationText(`${name} ${chapter}:${verse}`))) {
            verified = false;
            break;
          }
        }
      }
      if (!verified) detected.push(citation);
    }
  }

  return [...new Set(detected)];
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

  if (/^(hi|hello|hey|habari|shalom)\b/.test(q)) {
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

  if (bible?.translationNotAvailable) {
    return `The selected Bible translation (${bible.translation || "unknown"}) is not available in the assistant's online Bible database yet. I won't substitute another translation or present another version's wording as the selected one. Please check the Bible translation sync in Kanisa.`;
  }

  if (bible?.referenceNotFound) {
    return `I couldn't verify ${bible.reference || "that Bible reference"} in the selected translation (${bible.translationName || bible.translationAbbreviation || bible.translation}). I haven't quoted a passage because the requested chapter and verse could not be confirmed. Please check the book, chapter, and verse number.`;
  }

  if (bible?.verses?.length) {
    // For a specific reference, show the complete verified range rather than
    // labelling a truncated subset with the full requested citation.
    const verses = bible.verses.slice(0, bible.reference ? 30 : 6);
    const reference = bible.reference || "";
    const translationName = bible.translationName || bible.translationAbbreviation || bible.translation || "selected translation";
    const scripture = verses.map((v: any) => `${v.verse}. ${v.text}`).join("\\n");

    if (reference) {
      if (bible.translation === "kam") {
        return `Mbivilia (Kamba): ${reference}\\n\\n${translationName}:\\n${scripture}\\n\\nNote: This quotation is taken from the selected Kamba (Mbivilia) text. The local fallback uses English for explanation where a verified Kikamba explanation is not available.`;
      }

      const contextualFollowUp =
        /\b(this|that|these|those|it|the passage|the verse|the scripture|the scriptures)\b/i.test(q) ||
        /\b(explain|expand|elaborate|meaning|application)\b/i.test(q);

      if (contextualFollowUp) {
        return `Here is the ${translationName} passage we were discussing, ${reference}:\\n\\n${scripture}\\n\\n💡 Spiritual Insight & Application:\nThis passage invites us to anchor our faith in God's unfailing promises. Reflect on how its timeless truth applies directly to your walk with Christ today, offering peace, encouragement, and strength.`;
      }

      return `Scripture Reference: ${reference}\\n\\n${translationName} Bible Text:\\n${scripture}\\n\\n🙏 Pastoral Reflection:\nThe passage above provides wonderful biblical insight and encouragement. Read it prayerfully in its surrounding context to experience its full spiritual depth.`;
    }

    const references = verses.map((v: any) => {
      const name = (bible.books || []).find((book: any) => book.id === v.book_id)?.name;
      return name ? `${name} ${v.chapter}:${v.verse}` : "";
    }).filter(Boolean);

    if (bible.translation === "kam") {
      return `Kamba Bible (Mbivilia) — selected passages:\\n\\n${verses.map((v: any, i: number) => `${references[i] ? references[i] + " — " : ""}${v.text}`).join("\\n\\n")}\\n\\nNote: The passages above are from the selected Kamba translation. The local fallback uses English for explanation where a verified Kikamba explanation is not available.`;
    }

    return `Scripture Answers:\\n\\n${verses.map((v: any, i: number) => `${references[i] ? references[i] + " — " : ""}${v.text}`).join("\\n\\n")}\\n\\n🙏 Pastoral Reflection:\nThese passages offer meaningful biblical guidance on your question. Take time to meditate on God's Word.`;
  }

  if (/help|what can you do|how can you help/.test(q)) {
    return `Hello! I am ${assistantName}, your AI church & Bible companion. I can help you with ${churchName} service times, upcoming events, media and sermons, giving, contact information, and exploring Bible passages. How may I assist you today?`;
  }

  return `I am here to help with ${churchName} details and Bible questions. Feel free to ask about our service times, upcoming events, giving, or ask for Scripture passages on topics like prayer, faith, hope, or love!`;
}
function buildPrompt(context: any, bible: any, message: string, history: any[]) {
  const church = JSON.stringify(context);
  const scripture = JSON.stringify(bible);
  const recent = JSON.stringify((history || []).slice(-8));
  const languageCode = String(bible?.translationLanguageCode || "").toLowerCase();
  const responseLanguageRule = languageCode === "kam"
    ? "RESPONSE LANGUAGE: Use Kikamba first for the explanation and conversation. Only use Kikamba wording you can produce reliably; for uncertain or unavailable terms, fall back to Kiswahili or English rather than inventing Kikamba words. Keep quoted Scripture exactly as supplied from Kamba Bible (Mbivilia)."
    : languageCode === "sw"
      ? "RESPONSE LANGUAGE: Reply in Kiswahili, while preserving the exact supplied Scripture wording from the selected Kiswahili translation."
      : "RESPONSE LANGUAGE: Reply in English unless the user explicitly asks for another language. Preserve the exact supplied Scripture wording from the selected translation.";

  return `You are Kanisa Assistant, the official AI assistant and spiritual companion for our Christian church mobile app.

PASTORAL & COMMUNICATION STYLE:
${responseLanguageRule}
- Be warm, deeply encouraging, pastorally sensitive, concise, and spiritually uplifting.
- Speak with grace and clarity, offering genuine Christian encouragement and biblical truth.
- Do not claim to be a human or a pastor, but serve as a faithful, knowledgeable Bible and church companion.

STRICT SOURCE RULES:
- Never use the church website as a source.
- Church facts must come only from the supplied Supabase church context.
- Do not invent service times, events, leaders, contact details, ministries, giving instructions, locations, or announcements.
- If a church fact is missing or ambiguous, say that the current published church data does not provide a reliable answer.
- Bible quotations/references must come from the supplied Bible context. Never invent a Bible reference or quote.
- The user's selected Bible translation is identified by BIBLE CONTEXT.translation and BIBLE CONTEXT.translationName. Quote only verse text supplied for that exact translation; do not silently replace it with KJV, WEB, GNB, or another version.
- If BIBLE CONTEXT says translationNotAvailable or referenceNotFound, explain that the passage could not be verified and do not invent a substitute quotation or reference.
- For Bible concept/topic questions such as "what does the Bible say about prayer?", use the supplied BIBLE CONTEXT as the source. Select the most relevant passages from it and cite their exact supplied references in the answer.
- When BIBLE CONTEXT contains relevant references, do not answer a Bible concept question from general model memory alone.
- Distinguish Scripture from explanation, interpretation, and practical application.
- For a Bible question that has relevant BIBLE CONTEXT, use this natural order when useful: brief encouraging insight -> Scripture -> spiritual meaning -> practical Christian application.
- Keep the structure conversational rather than turning every response into a rigid template.
- If the user asks for a specific passage, prioritize that passage instead of adding unrelated verses.
- If the user asks a contextual follow-up such as "explain this", "what does that mean?", "how do I apply this?", or "more scriptures", use the previous conversation plus the supplied BIBLE CONTEXT to stay on the same subject.
- When relevant Scripture is supplied, place the explanation immediately before or after the relevant passage.
- Never paraphrase a supplied Scripture passage while presenting it as a quotation.
- Keep exact Scripture quotations clearly identifiable as Scripture and do not mix commentary into the quotation.
- Practical application should be framed as thoughtful Christian encouragement.
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
    bibleContext(message, body.conversation || [], body.translation)
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
    // Only expose references that the answer actually cites, and only from
    // passages retrieved and verified in the selected translation.
    const references = citedVerifiedReferences(answer, usableBible);
    const allBibleQuotes = (usableBible?.verses || []).slice(0, 20).map((verse: any) => {
      const name = (usableBible?.books || []).find((book: any) => book.id === verse.book_id)?.name;
      const transAbbr = usableBible.translationAbbreviation ||
        (usableBible.translation === "gnb" ? "GNB" :
          usableBible.translation === "web" ? "WEB" :
            usableBible.translation === "kam" ? "KAM" :
              String(usableBible.translation || "KJV").toUpperCase());
      return name && verse.text
        ? { reference: name + " " + verse.chapter + ":" + verse.verse, text: String(verse.text), translation: transAbbr }
        : null;
    }).filter(Boolean);
    const bibleQuotes = allBibleQuotes
      .filter((quote: any) => answerCitesReference(answer, quote.reference))
      .filter((quote: any) => references.some((reference: string) =>
        answerCitesReference(reference, quote.reference) ||
        answerCitesReference(reference, quote.reference.replace(/:(\d+)$/, ":$1"))
      ))
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

    // Model output is not itself proof that a Bible citation is valid.
    // If it mentions a reference absent from the selected-translation retrieval,
    // discard the generated answer and use the database-backed local response.
    const unsupportedReferences = unsupportedBibleReferences(answer, usableBible);
    if (unsupportedReferences.length) {
      console.warn("Unsupported Bible citation in generated answer; using verified fallback", unsupportedReferences);
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
