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

  // Resolve chapter-only, verse, same-chapter range, and cross-chapter range
  // references, while enforcing an exact and bounded set of retrieved verses.
  for (const candidate of candidates) {
    const pattern = new RegExp(
      "(?:^|[^a-z0-9])" +
      escapeRegex(candidate.alias) +
      "\\s*(?:chapter\\s*)?(\\d{1,3})(?:\\s*(?::|[;,]|\\s+verse\\s+)\\s*(\\d{1,3})(?:\\s*[-–]\\s*(?:(\\d{1,3}):)?(\\d{1,3}))?)?(?=$|[^0-9])",
      "i"
    );
    const match = bibleSearchQuestion.match(pattern);
    if (!match) continue;

    const chapterStart = Number(match[1]);
    const chapterOnly = !match[2];
    const verseStart = chapterOnly ? 1 : Number(match[2]);
    const chapterEnd = match[3] ? Number(match[3]) : chapterStart;
    const verseEnd = chapterOnly ? null : Number(match[4] || match[2]);
    const canonicalReference = chapterOnly
      ? `${candidate.book.name} ${chapterStart}`
      : `${candidate.book.name} ${chapterStart}:${verseStart}` +
        (chapterEnd !== chapterStart
          ? `-${chapterEnd}:${verseEnd}`
          : (verseEnd !== verseStart ? `-${verseEnd}` : ""));
    const invalidRange = chapterStart < 1 || verseStart < 1 ||
      chapterEnd < chapterStart || chapterEnd - chapterStart > 2 ||
      (!chapterOnly && (verseEnd! < 1 ||
        (chapterEnd === chapterStart && verseEnd! < verseStart)));

    if (invalidRange) {
      return {
        books: books || [], translation,
        translationName: translationRecord.name,
        translationAbbreviation: translationRecord.abbreviation,
        translationLanguageCode: translationRecord.language_code,
        reference: canonicalReference, referenceNotFound: true,
        references: [], verses: []
      };
    }

    const rowsByChapter: any[][] = [];
    let queryFailed = false;
    for (let currentChapter = chapterStart; currentChapter <= chapterEnd; currentChapter++) {
      let query = supabase.from("bible_verses")
        .select("book_id,chapter,verse,text")
        .eq("translation_id", translation)
        .eq("book_id", candidate.book.id)
        .eq("chapter", currentChapter)
        .order("verse");
      if (!chapterOnly && currentChapter === chapterStart) query = query.gte("verse", verseStart);
      if (!chapterOnly && currentChapter === chapterEnd) query = query.lte("verse", verseEnd);
      const { data, error } = await query.limit(41);
      if (error) {
        queryFailed = true;
        console.error("Bible passage lookup failed", { reference: canonicalReference, error });
        break;
      }
      rowsByChapter.push(data || []);
    }

    const rows = rowsByChapter.flat();
    const tooLarge = rows.length > 40 || (!chapterOnly && rows.length > 30);
    const exactRangeFound = !queryFailed && rows.length > 0 && !tooLarge &&
      rowsByChapter.every((chapterRows: any[], index: number) => {
        if (!chapterRows.length) return false;
        const expectedStart = !chapterOnly && index === 0 ? verseStart : 1;
        const expectedEnd = !chapterOnly && index === rowsByChapter.length - 1
          ? verseEnd!
          : Number(chapterRows[chapterRows.length - 1].verse);
        return chapterRows.every((verse: any, rowIndex: number) =>
          Number(verse.verse) === expectedStart + rowIndex &&
          Boolean(String(verse.text || "").trim())
        ) && (chapterOnly || index !== rowsByChapter.length - 1 ||
          Number(chapterRows[chapterRows.length - 1].verse) === expectedEnd);
      }) &&
      (chapterOnly || rowsByChapter[rowsByChapter.length - 1].some((verse: any) =>
        Number(verse.verse) === verseEnd));

    return {
      books: books || [], translation,
      translationName: translationRecord.name,
      translationAbbreviation: translationRecord.abbreviation,
      translationLanguageCode: translationRecord.language_code,
      reference: canonicalReference,
      referenceNotFound: !exactRangeFound,
      chapterTooLong: chapterOnly && rows.length > 40,
      passageTooLong: !chapterOnly && rows.length > 30,
      references: exactRangeFound ? [canonicalReference] : [],
      verses: exactRangeFound ? rows : []
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
    love: ["love", "loveth", "loved", "loving", "lovely", "charity"],
    forgiveness: ["forgive", "forgiven", "forgiveness", "forgiving", "forgave", "mercy", "reconcile", "reconciliation"],
    hope: ["hope", "hopeth", "hoped", "hopeful", "trust"],
    peace: ["peace", "peaceable", "reconcile", "reconciliation", "contentment"],
    wisdom: ["wisdom", "wise", "understanding", "knowledge", "discernment"],
    obedience: ["obey", "obeyed", "obedience", "keep", "commandments"],
    salvation: ["save", "saved", "salvation", "redeemed", "redemption"],
    temptation: ["tempt", "tempted", "temptation", "sin", "resist"],
    fasting: ["fast", "fasted", "fasting"],
    worship: ["worship", "worshipped", "praise", "praises", "praising", "thanksgiving"],
    anxiety: ["anxiety", "anxious", "careful", "worry", "worrying", "worried", "trouble", "cares"],
    fear: ["fear", "afraid", "fearing", "frightened", "courage"],
    strength: ["strength", "strong", "strengthen", "weakness", "weary"],
    healing: ["heal", "healed", "healing", "sick", "sickness", "illness"],
    patience: ["patience", "patient", "wait", "waiting", "longsuffering", "endure"],
    humility: ["humble", "humility", "meek", "meekness"],
    repentance: ["repent", "repented", "repentance", "turn", "confess", "confession"],
    grief: ["grief", "grieve", "grieving", "mourning", "mourn", "sorrow", "sorrowful", "comfort"],
    anger: ["anger", "angry", "wrath", "rage", "resentment", "gentle", "slow to anger"],
    relationships: ["relationship", "relationships", "marriage", "married", "husband", "wife", "friendship", "friend", "neighbor"],
    guidance: ["guidance", "guide", "direction", "decision", "decisions", "counsel", "path"],
    gratitude: ["gratitude", "grateful", "thankful", "thanks", "thanksgiving"],
    generosity: ["generosity", "generous", "give", "giving", "charity", "alms"],
    justice: ["justice", "just", "fairness", "oppression", "righteousness"],
    suffering: ["suffering", "suffer", "affliction", "afflicted", "pain", "endure"],
    doubt: ["doubt", "doubting", "unbelief", "unbelieving", "question"],
    loneliness: ["lonely", "loneliness", "alone", "forsaken", "comfort"],
    courage: ["courage", "brave", "bold", "boldness", "fearless"]
  };

  // Normalize common inflections and everyday wording to a concept before
  // expanding it. This makes "forgiving", "worried", and "grieving" retrieve
  // the same topic family as "forgiveness", "anxiety", and "grief".
  const conceptAliases: Record<string, string> = {
    pray: "prayer", praying: "prayer", prayed: "prayer", supplication: "prayer",
    believe: "faith", believeth: "faith", believed: "faith", believing: "faith", trust: "faith",
    loving: "love", loved: "love", loveth: "love",
    forgive: "forgiveness", forgiven: "forgiveness", forgiving: "forgiveness", forgave: "forgiveness",
    reconcile: "forgiveness", reconciliation: "forgiveness", mercy: "forgiveness",
    hopeful: "hope", hopeth: "hope", hoped: "hope",
    anxious: "anxiety", worry: "anxiety", worrying: "anxiety", worried: "anxiety", cares: "anxiety",
    afraid: "fear", fearing: "fear", frightened: "fear",
    strong: "strength", strengthen: "strength", weakness: "strength", weary: "strength",
    heal: "healing", healed: "healing", sick: "healing", sickness: "healing", illness: "healing",
    patient: "patience", waiting: "patience", endure: "patience",
    humble: "humility", meek: "humility", meekness: "humility",
    repent: "repentance", repented: "repentance", confess: "repentance", confession: "repentance",
    grieve: "grief", grieving: "grief", mourning: "grief", mourn: "grief", sorrow: "grief", sorrowful: "grief",
    angry: "anger", wrath: "anger", rage: "anger", resentment: "anger",
    married: "relationships", husband: "relationships", wife: "relationships", friendship: "relationships", friend: "relationships", neighbor: "relationships",
    guide: "guidance", direction: "guidance", decision: "guidance", decisions: "guidance", counsel: "guidance",
    grateful: "gratitude", thankful: "gratitude", thanks: "gratitude",
    generous: "generosity", alms: "generosity",
    fair: "justice", fairness: "justice", oppression: "justice",
    suffer: "suffering", afflicted: "suffering", pain: "suffering",
    doubting: "doubt", unbelief: "doubt", unbelieving: "doubt",
    lonely: "loneliness", alone: "loneliness", forsaken: "loneliness",
    brave: "courage", bold: "courage", boldness: "courage", fearless: "courage"
  };

  const canonicalTerms = rawTerms.map(term => conceptAliases[term] || term);
  const expandedTerms = canonicalTerms.flatMap(term => conceptExpansions[term] || [term]);
  const terms = [...new Set([...expandedTerms, ...rawTerms])].slice(0, 12);

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

  // Search terms independently and merge results. A single broad OR query can
  // fill its row limit with early database matches, hiding relevant verses for
  // other concepts. Per-term retrieval gives each synonym a chance to contribute.
  const discoveryTranslation = translation === "kam" ? "kjv" : translation;
  const searchTerms = terms.slice(0, 8);
  const searchResults = await Promise.all(searchTerms.map(async (term) => {
    const safeTerm = term.replace(/[%_]/g, "").trim();
    if (!safeTerm) return [];
    const { data, error } = await supabase
      .from("bible_verses")
      .select("book_id,chapter,verse,text")
      .eq("translation_id", discoveryTranslation)
      .ilike("text", `%${safeTerm}%`)
      .limit(20);
    if (error) {
      console.error("Bible topic search error", { term: safeTerm, error });
      return [];
    }
    return data ?? [];
  }));
  const candidateMap = new Map<string, any>();
  for (const verse of searchResults.flat()) {
    const key = `${verse.book_id}:${verse.chapter}:${verse.verse}`;
    if (!candidateMap.has(key)) candidateMap.set(key, verse);
  }
  const rawCandidates = [...candidateMap.values()];
  const primaryPhrase = rawTerms.join(" ").toLowerCase().trim();

  const candidatesWithScore = rawCandidates.map((verse: any) => {
    const text = String(verse.text || "").toLowerCase();
    let score = 0;
    let matchedTerms = 0;
    for (const term of terms) {
      const occurrences = text.split(term.toLowerCase()).length - 1;
      if (occurrences > 0) {
        matchedTerms++;
        score += Math.min(occurrences, 3) * 2;
      }
    }

    // Reward passages that match several topic terms or the user's phrase,
    // rather than passages that mention one incidental word.
    score += Math.max(0, matchedTerms - 1) * 2;
    if (primaryPhrase.length >= 5 && text.includes(primaryPhrase)) score += 8;

    // "I pray thee" is a historical idiom, not necessarily a passage about prayer.
    if (/\bi pray thee\b/.test(text) &&
      /\bprayer\b|\bpraying\b|\bsupplication\b|\bintercession\b/.test(primaryPhrase)) {
      score -= 4;
    }

    return { verse, score, matchedTerms };
  }).sort((a: any, b: any) =>
    b.score - a.score ||
    b.matchedTerms - a.matchedTerms ||
    String(a.verse.book_id).localeCompare(String(b.verse.book_id)) ||
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
  const contextual = Array.isArray(bible?.contextVerses) ? bible.contextVerses.map((v: any) => String(v.reference || "")).filter(Boolean) : [];
  const candidates = [...available, ...(requested ? [requested] : []), ...contextual];
  return [...new Set(candidates.filter(reference => answerCitesReference(answer, String(reference))))].slice(0, 12);
}

function unsupportedBibleReferences(answer: string, bible: any): string[] {
  if (!answer.trim() || !bible || bible.translationNotAvailable || bible.referenceNotFound) return [];

  const books = Array.isArray(bible.books) ? bible.books : [];
  const escapeRegex = (value: string) => value.replace(/[.*+?^$()|[\]\\{}]/g, "\\$&");
  const supplied = new Set<string>(
    [
      ...(Array.isArray(bible.references) ? bible.references : []),
      ...(String(bible.reference || "").trim() ? [String(bible.reference).trim()] : []),
      ...(Array.isArray(bible.contextVerses) ? bible.contextVerses.map((v: any) => String(v.reference || "")).filter(Boolean) : [])
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

async function enrichBibleContext(bible: any): Promise<any> {
  if (!bible || bible.translationNotAvailable || bible.referenceNotFound ||
      !bible.translation || !Array.isArray(bible.verses) || !bible.verses.length) return bible;

  const first = bible.verses[0];
  const last = bible.verses[bible.verses.length - 1];
  if (!first?.book_id || !Number.isFinite(Number(first.chapter)) ||
      !Number.isFinite(Number(first.verse)) || !Number.isFinite(Number(last.chapter))) {
    return { ...bible, contextVerses: [] };
  }

  // Search adjacent chapters too, so passages near chapter boundaries and
  // ranges spanning chapters are interpreted with nearby text where available.
  const fromChapter = Math.max(1, Number(first.chapter) - 1);
  const toChapter = Number(last.chapter) + 1;
  const { data, error } = await supabase.from("bible_verses")
    .select("book_id,chapter,verse,text")
    .eq("translation_id", bible.translation)
    .eq("book_id", first.book_id)
    .gte("chapter", fromChapter)
    .lte("chapter", toChapter)
    .order("chapter", { ascending: true })
    .order("verse", { ascending: true })
    .limit(160);

  if (error || !Array.isArray(data)) {
    console.warn("Could not load surrounding Bible context", error);
    return { ...bible, contextVerses: [] };
  }

  const key = (v: any) => `${v.book_id}:${Number(v.chapter)}:${Number(v.verse)}`;
  const requestedKeys = new Set(bible.verses.map(key));
  const validRows = data.filter((v: any) => String(v.text || "").trim());
  const preceding = validRows.filter((v: any) =>
    !requestedKeys.has(key(v)) &&
    (Number(v.chapter) < Number(first.chapter) ||
      (Number(v.chapter) === Number(first.chapter) && Number(v.verse) < Number(first.verse)))
  );
  const following = validRows.filter((v: any) =>
    !requestedKeys.has(key(v)) &&
    (Number(v.chapter) > Number(last.chapter) ||
      (Number(v.chapter) === Number(last.chapter) && Number(v.verse) > Number(last.verse)))
  );
  const nearby = [...preceding.slice(-4), ...following.slice(0, 4)];
  const books = Array.isArray(bible.books) ? bible.books : [];
  const bookName = books.find((book: any) => String(book.id) === String(first.book_id))?.name || "Bible";
  const contextVerses = nearby.map((v: any) => ({
    ...v,
    reference: bookName + " " + v.chapter + ":" + v.verse,
    translation: bible.translationAbbreviation || String(bible.translation).toUpperCase()
  }));

  return { ...bible, contextVerses };
}

function localBibleExplanation(message: string, bible: any): string {
  const q = normalize(message);
  const requestedLanguage =
    /\b(in|use|reply|answer|explain)\s+(kiswahili|swahili)\b|\b(kiswahili|swahili)\s+(language|please)\b|\b(habari|tafadhali|nisaidie|maana yake|ueleze|eleza|mstari|biblia|upendo|msamaha|woga|wasiwasi|maombi|imani|ninahitaji|naomba|asante|kwa nini|jinsi ya|nini maana)\b/i.test(message)
      ? "sw"
      : /\b(in|use|reply|answer|explain)\s+(kikamba|kamba|kimakamba)\b/i.test(message)
        ? "kam"
        : "en";

  // Offline mode explains common references in English or Kiswahili even when
  // the selected Scripture text is Kamba. A request for Kikamba is not a reason
  // to return a language limitation message; English is the safe fallback.
  const reference = normalize(String(bible?.reference || bible?.references?.[0] || ""))
    .replace(/\./g, "")
    .replace(/\s+/g, " ");
  const referenceExplanations: Array<{ pattern: RegExp; en: string; sw: string }> = [
    { pattern: /\bjohn 3:16\b/, en: "John 3:16 teaches that God's love is expressed through giving his Son so that people may receive eternal life through faith. It emphasizes God's initiative in loving the world, not that people must earn his love. In daily life, respond with trust in God and show others generous, active love.", sw: "Yohana 3:16 inafundisha kwamba Mungu alionyesha upendo wake kwa kutoa Mwanawe ili watu wapate uzima wa milele kwa kumwamini. Inasisitiza kwamba Mungu ndiye aliyeanza kutupenda; hatununui upendo wake kwa matendo yetu. Katika maisha ya kila siku, mtumaini Mungu na uwaonyeshe wengine upendo kwa matendo." },
    { pattern: /\b(psalm|psalms) 23:1\b/, en: "Psalm 23:1 pictures God as a shepherd who cares for and guides his people. It encourages trust in God's care, while not promising a life without difficulty. Practically, bring your needs to God and seek his guidance for the next step.", sw: "Zaburi 23:1 inamwonyesha Mungu kama mchungaji anayewatunza na kuwaongoza watu wake. Inatualika kumtumaini Mungu, ingawa haimaanishi kwamba maisha hayatakuwa na changamoto. Kwa vitendo, mweleze Mungu mahitaji yako na utafute mwongozo wake kwa hatua inayofuata." },
    { pattern: /\bphilippians 4:6(?:-7)?\b/, en: "Philippians 4:6–7 encourages believers to bring their worries to God in prayer with gratitude. It does not mean ignoring problems; it invites you to seek God's peace while taking wise, practical steps and asking trusted people for support when needed.", sw: "Wafilipi 4:6–7 inawahimiza waamini kumweleza Mungu mahangaiko yao kwa maombi na shukrani. Haimaanishi kupuuza matatizo; inatualika kutafuta amani ya Mungu huku tukichukua hatua za busara na kuomba msaada wa watu tunaowaamini inapohitajika." },
    { pattern: /\bproverbs 3:5(?:-6)?\b/, en: "Proverbs 3:5–6 calls you to trust God rather than relying only on your own limited understanding. This does not remove the need to think carefully; it encourages prayer, humility, and choices guided by God's wisdom instead of pride or impulse.", sw: "Mithali 3:5–6 inatualika kumtumaini Mungu badala ya kutegemea ufahamu wetu mdogo pekee. Hii haiondoi umuhimu wa kufikiri kwa makini; inahimiza maombi, unyenyekevu na kufanya maamuzi kwa hekima badala ya kiburi au pupa." },
    { pattern: /\b(joshua|jos) 1:9\b/, en: "Joshua 1:9 calls God's people to be courageous because God is with them. Courage does not mean feeling no fear; it means taking the right next step without letting fear make every decision. Seek support and act responsibly as you face a challenge.", sw: "Yoshua 1:9 inawahimiza watu wa Mungu kuwa hodari kwa sababu Mungu yuko pamoja nao. Ujasiri haumaanishi kutokuwa na hofu kabisa; unamaanisha kuchukua hatua inayofaa bila kuruhusu hofu iamue kila kitu. Tafuta msaada na tenda kwa uwajibikaji unapokabili changamoto." },
    { pattern: /\bjeremiah 29:11\b/, en: "Jeremiah 29:11 was spoken to people living through exile and a long wait, so it should not be treated as a promise that every personal plan will happen immediately. It offers hope in God's purposes and invites patience, faithfulness, and perseverance during uncertainty.", sw: "Yeremia 29:11 ilielekezwa kwa watu waliokuwa uhamishoni na waliopaswa kusubiri kwa muda mrefu. Haipaswi kuchukuliwa kama ahadi kwamba kila mpango wa mtu utatimia mara moja. Inatoa tumaini katika makusudi ya Mungu na kutualika kuwa na subira, uaminifu na uvumilivu wakati wa kutokuwa na uhakika." },
    { pattern: /\b(romans 8:28|rom 8:28)\b/, en: "Romans 8:28 teaches that God can work through circumstances for the good of those who love him and are called according to his purpose. It does not say every event is good in itself. In hardship, seek support and keep taking wise steps while trusting that suffering need not have the final word.", sw: "Warumi 8:28 inafundisha kwamba Mungu anaweza kufanya mambo yote yatimize mema kwa wale wanaompenda na walioitwa kulingana na kusudi lake. Haimaanishi kwamba kila tukio lenyewe ni jema. Wakati wa shida, tafuta msaada na endelea kuchukua hatua za busara huku ukitumaini kwamba mateso si lazima yawe mwisho wa hadithi yako." },
    { pattern: /\b(matthew 6:33|matt 6:33)\b/, en: "Matthew 6:33 calls people to put God's kingdom and righteousness first instead of letting worry about material needs control their lives. Practically, make choices shaped by faith, justice, and care for others while still responsibly meeting everyday needs.", sw: "Mathayo 6:33 inatualika kutanguliza ufalme wa Mungu na haki yake badala ya kuruhusu wasiwasi wa mahitaji ya maisha ututawale. Kwa vitendo, fanya maamuzi yanayoongozwa na imani, haki na kuwajali wengine huku ukiendelea kushughulikia mahitaji ya kila siku kwa uwajibikaji." },
    { pattern: /\b(romans 12:2|rom 12:2)\b/, en: "Romans 12:2 encourages inner renewal rather than simply copying the values and habits around us. Reflect on what shapes your thinking, compare choices with biblical principles, and build habits that help you act with wisdom, compassion, and integrity.", sw: "Warumi 12:2 inahimiza kufanywa upya kwa ndani badala ya kuiga tu maadili na tabia zinazotuzunguka. Tafakari mambo yanayoathiri mawazo yako, linganisha maamuzi yako na kanuni za Biblia, na jenga tabia zinazokusaidia kutenda kwa hekima, huruma na uadilifu." }
  ];
  const knownReference = referenceExplanations.find(item => item.pattern.test(reference));
  if (knownReference) {
    return requestedLanguage === "sw"
      ? "Maelezo: " + knownReference.sw
      : "Explanation: " + knownReference.en;
  }

  const topics: Array<{ pattern: RegExp; en: string; sw: string }> = [
    { pattern: /forgiv|forgive|forgiven|forgiveness|msamaha|samehe|kusamehe|nimesamehe|hurt me|wronged me|betray/, en: "The central idea is forgiveness: choosing not to repay wrong with revenge, while being honest about the harm. Forgiveness does not mean pretending the hurt never happened or allowing ongoing abuse. Where it is safe, seek peace and wise counsel; boundaries and accountability can still be necessary.", sw: "Wazo kuu ni msamaha: kuchagua kutolipiza ubaya kwa ubaya, huku ukikubali ukweli wa maumivu yaliyotokea. Kusamehe hakumaanishi kujifanya kwamba hakuna kilichotokea au kuruhusu unyanyasaji uendelee. Ikiwa ni salama, tafuta amani na ushauri wenye hekima; mipaka na uwajibikaji bado vinaweza kuhitajika." },
    { pattern: /anx|worr|worry|fear|afraid|stress|panic|wasiwasi|hofu|ogopa|kuogopa|msongo/, en: "The passage can be applied by bringing your concerns honestly to God and taking the next manageable step instead of trying to solve everything at once. Prayer can support you, and seeking practical help from a trusted person is also appropriate.", sw: "Unaweza kutumia ujumbe huu kwa kumweleza Mungu wasiwasi wako kwa uaminifu na kuchukua hatua moja inayowezekana badala ya kujaribu kutatua kila kitu mara moja. Maombi yanaweza kukutia moyo, na pia ni sawa kutafuta msaada wa vitendo kutoka kwa mtu unayemwamini." },
    { pattern: /pray|prayer|praying|maombi|omba|kuomba/, en: "The passage points toward honest communication with God, not merely repeating words. You can pray about what is happening, ask for wisdom, and remain open to taking responsible action.", sw: "Ujumbe huu unatukumbusha kuzungumza na Mungu kwa uaminifu, si kurudia maneno tu. Unaweza kuomba kuhusu hali unayopitia, kuomba hekima, na kuwa tayari kuchukua hatua zinazofaa." },
    { pattern: /faith|believ|trust|imani|kuamini|tumaini kwa mungu/, en: "Faith means relying on God while responding to life with trust and faithfulness. It does not require pretending that doubts or difficulties do not exist; bring them honestly into prayer and seek wise guidance.", sw: "Imani ni kumtegemea Mungu huku ukiendelea kutenda kwa uaminifu. Haimaanishi kujifanya kwamba mashaka au matatizo hayapo; mweleze Mungu ukweli wa moyo wako katika maombi na utafute mwongozo wenye hekima." },
    { pattern: /love|loving|upendo|penda|kupenda/, en: "The practical focus is to show love through truthful, patient, and helpful actions, not words alone. Love does not require accepting harm; it can include clear boundaries and speaking honestly.", sw: "Jambo la kufanya ni kuonyesha upendo kwa matendo ya kweli, uvumilivu na msaada, si kwa maneno pekee. Upendo haukulazimishi kukubali kuumizwa; unaweza pia kuhusisha kuweka mipaka iliyo wazi na kusema ukweli." },
    { pattern: /grief|griev|mourning|sorrow|loss|bereav|huzuni|majonzi|msiba|kuomboleza/, en: "The passage can offer language for grief without requiring you to hide your sadness. Give yourself time, seek support from people you trust, and take grief one day at a time.", sw: "Ujumbe huu unaweza kukupa nafasi ya kueleza huzuni bila kujilazimisha kuificha. Jipe muda, tafuta msaada wa watu unaowaamini, na pitia kipindi hiki hatua kwa hatua." },
    { pattern: /anger|angry|rage|hasira|ghadhabu|kukasirika/, en: "A practical response is to pause before speaking or acting, name what has hurt or angered you, and respond truthfully without revenge. If someone is unsafe, create distance and seek help.", sw: "Hatua ya vitendo ni kutulia kabla ya kusema au kutenda, kutambua kilichokuumiza au kukukasirisha, na kujibu kwa ukweli bila kulipiza kisasi. Ikiwa kuna hatari, ondoka mahali hapo na utafute msaada." },
    { pattern: /guidance|decision|direction|wisdom|mwongozo|uamuzi|hekima|nifanye nini/, en: "Use this as an invitation to seek wisdom, consider the facts, pray, and ask trustworthy people for counsel. A decision should also be assessed by its consequences and whether it treats others justly.", sw: "Chukulia hili kama mwaliko wa kutafuta hekima, kuchunguza ukweli, kuomba, na kushauriana na watu waaminifu. Pia tathmini matokeo ya uamuzi na kama unawatendea wengine kwa haki." },
    { pattern: /hope|hopeless|despair|tumaini|kukata tamaa/, en: "The message encourages you not to treat the present difficulty as the whole story. Hold on to hope while also taking practical steps and accepting support from others.", sw: "Ujumbe huu unakutia moyo usione ugumu wa sasa kuwa ndio mwisho wa kila kitu. Shikilia tumaini huku ukichukua hatua za vitendo na kukubali msaada wa wengine." }
  ];
  const topic = topics.find(item => item.pattern.test(q));
  if (topic) {
    if (requestedLanguage === "sw") return "Maelezo: " + topic.sw;
    return "Explanation: " + topic.en;
  }

  if (requestedLanguage === "sw") {
    return "Maelezo: Soma mstari huu pamoja na mistari inayouzunguka ili kuelewa ujumbe mkuu, watu wanaozungumziwa, na kile kifungu kinachofundisha kuhusu Mungu na maisha ya kila siku. Jiulize: kifungu hiki kinafundisha nini, kinanialika kuamini au kubadilisha nini, na ninawezaje kukitumia kwa njia yenye hekima leo?";
  }
  return "Explanation: The verse quoted above is from your selected Bible translation. Read it together with the surrounding verses to understand the author's main point, who is being addressed, and what the passage says about God and how people should live. A practical starting point is to ask: what does this passage teach, what does it invite me to trust or change, and how can I apply that responsibly today?";
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
    const scripture = verses.map((v: any) => `${v.verse}. ${v.text}`).join("\n");

    if (reference) {
      return `Mbivilia (Kamba): ${reference}\n\n${translationName}:\n${scripture}\n\n${localBibleExplanation(message, bible)}`;
    }

    const references = verses.map((v: any) => {
      const name = (bible.books || []).find((book: any) => book.id === v.book_id)?.name;
      return name ? `${name} ${v.chapter}:${v.verse}` : "";
    }).filter(Boolean);

    const heading = bible.translation === "kam"
      ? "Kamba Bible (Mbivilia) — selected passages:"
      : "Scripture Answers:";
    const selectedPassages = verses.map((v: any, i: number) =>
      `${references[i] ? references[i] + " — " : ""}${v.text}`
    ).join("\n\n");
    return `${heading}\n\n${selectedPassages}\n\n${localBibleExplanation(message, bible)}`;
  }

  if (/help|what can you do|how can you help/.test(q)) {
    return `Hello! I am ${assistantName}, your AI church & Bible companion. I can help you with ${churchName} service times, upcoming events, media and sermons, giving, contact information, and exploring Bible passages. How may I assist you today?`;
  }

  return `I am here to help with ${churchName} details and Bible questions. Feel free to ask about our service times, upcoming events, giving, or ask for Scripture passages on topics like prayer, faith, hope, or love!`;
}
function buildPrompt(context: any, bible: any, message: string, history: any[]) {
  const church = JSON.stringify(context);
  const scripture = JSON.stringify({ ...bible, verses: bible?.verses || [], surroundingContext: bible?.contextVerses || [] });
  const recent = JSON.stringify((history || []).slice(-8));
  const languageInstruction = (text: string): string | null => {
    if (/\b(in|use|reply|answer|explain)\s+(kikamba|kamba|kimakamba)\b|\b(kikamba|kamba|kimakamba)\s+(language|please)\b/i.test(text)) return "kam";
    if (/\b(in|use|reply|answer|explain)\s+(kiswahili|swahili)\b|\b(kiswahili|swahili)\s+(language|please)\b/i.test(text)) return "sw";
    if (/\b(in|use|reply|answer|explain)\s+english\b|\benglish\s+(language|please)\b/i.test(text)) return "en";
    return null;
  };
  const languageOfEverydayText = (text: string): string | null => {
    if (/\b(habari|tafadhali|nisaidie|maana yake|ueleze|eleza|mstari|biblia|upendo|msamaha|woga|wasiwasi|maombi|imani|ninahitaji|naomba|asante|kwa nini|jinsi ya|nini maana)\b/i.test(text)) return "sw";
    return null;
  };
  // Honor a language request made now; otherwise preserve a prior explicit
  // language preference for short follow-ups such as "explain more" or "why?".
  const priorUserMessages = (history || [])
    .filter(item => item.role === "user")
    .slice(-8)
    .map(item => String(item.content || ""));
  const requestedLanguage = languageInstruction(message) ||
    [...priorUserMessages].reverse().map(languageInstruction).find(Boolean) ||
    languageOfEverydayText(message) ||
    [...priorUserMessages].reverse().map(languageOfEverydayText).find(Boolean) ||
    "en";
  const responseLanguageRule = requestedLanguage === "kam"
    ? "RESPONSE LANGUAGE: Explain the Scripture and answer the question in clear Kikamba only where you can do so reliably. If a phrase or idea is beyond your reliable Kikamba, explain that part in plain English or Kiswahili rather than inventing words. Never return only the Scripture quotation. The selected Bible translation controls the quotation, not the explanation language. Keep quoted Scripture exactly as supplied."
    : requestedLanguage === "sw"
      ? "RESPONSE LANGUAGE: Explain the Scripture and answer the question in Kiswahili. The selected Bible translation controls the quotation, not the explanation language. Keep quoted Scripture exactly as supplied, even if it is in English or Kikamba. Never return only the quotation."
      : "RESPONSE LANGUAGE: Explain the Scripture and answer the question in clear English, even when the selected Bible translation is in Kikamba or another language you do not speak fluently. The selected Bible translation controls the quotation, not the explanation language. Preserve the verse text exactly, then explain its meaning separately in English. Never omit the explanation merely because the quotation is non-English. If the user explicitly requests another language, follow that request only to the extent you can do so reliably.";

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
- Bible quotations/references must come from the supplied Bible context. Never invent a Bible reference or quote. The surroundingContext verses help interpret the requested passage; quote them only if useful and preserve their supplied wording exactly.
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
- The Bible translation language and the explanation language are separate settings. Do not infer the response language from BIBLE CONTEXT.translationLanguageCode. Follow RESPONSE LANGUAGE above, which is based on the user's message or explicit language request.
- A non-English Scripture quotation is never a reason to skip explanation. Clearly separate the exact quoted text from commentary, then explain the passage's meaning and practical relevance in the response language specified above. If you understand the verse language only partially, be transparent about uncertainty and explain what can be responsibly explained; do not pretend that the translation itself is an explanation.
- For every Bible question with relevant retrieved verses, include all three where possible: (1) the exact requested verse text, (2) a plain-language explanation grounded in the passage and surrounding context, and (3) one practical application that directly answers the user's question. Avoid generic filler reflections that could fit any verse.\n- Interpret passages in literary and historical context where supported by the supplied text. Explain the main point, key words or contrasts, who is speaking and to whom when evident, and how the passage fits the surrounding paragraph or chapter. Distinguish what the text explicitly says from reasonable interpretation. Do not claim historical details or original-language meanings without reliable support. Mention when Christians interpret a passage differently rather than presenting a disputed view as certain.
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

  const contextualBible = await enrichBibleContext(bible);

  if (settings?.enabled === false) {
    return json({ error: "The Kanisa Assistant is currently disabled." }, 403);
  }

  const usableBible = settings?.bible_enabled === false
    ? { verses: [], contextVerses: [], reference: "", references: [] }
    : contextualBible;

  const finalize = async (
    answer: string,
    providerName: string,
    modelName: string | null
  ) => {
    // Only expose references that the answer actually cites, and only from
    // passages retrieved and verified in the selected translation.
    const references = citedVerifiedReferences(answer, usableBible);
    const allBibleQuotes = [...(usableBible?.verses || []), ...(usableBible?.contextVerses || [])].slice(0, 28).map((verse: any) => {
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
