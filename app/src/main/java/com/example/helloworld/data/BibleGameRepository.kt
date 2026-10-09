package com.example.helloworld.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth

enum class BibleGameCategory(val label: String) {
    ALL("All"),
    OLD_TESTAMENT("Old Testament"),
    NEW_TESTAMENT("New Testament"),
    PEOPLE("People"),
    PLACES("Places"),
    FAITH_AND_LIFE("Faith & Life")
}

data class BibleGameQuestion(
    val id: String,
    val category: BibleGameCategory,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val reference: String,
    val gameType: String = "quiz"
)

data class BibleGameRound(
    val questions: List<BibleGameQuestion>,
    val timed: Boolean
)

data class BibleGamePlayerStats(
    val xp: Int = 0,
    val gamesPlayed: Int = 0,
    val questionsAnswered: Int = 0,
    val correctAnswers: Int = 0,
    val bestScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

@Serializable
private data class BibleGamePlayerStatsRow(
    @SerialName("user_id") val userId: String,
    val xp: Int = 0,
    @SerialName("games_played") val gamesPlayed: Int = 0,
    @SerialName("questions_answered") val questionsAnswered: Int = 0,
    @SerialName("correct_answers") val correctAnswers: Int = 0,
    @SerialName("best_score") val bestScore: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("last_played_on") val lastPlayedOn: String? = null
)

@Serializable
private data class BibleGameQuestionRow(
    val id: String,
    val category: String,
    val question: String,
    val options: List<String>,
    @SerialName("correct_answer_index") val correctAnswerIndex: Int,
    @SerialName("game_type") val gameType: String = "quiz",
    val explanation: String = "",
    val reference: String = ""
)

class BibleGameRepository {
    suspend fun loadGameQuestions(gameType: String, limit: Int = 10): List<BibleGameQuestion> {
        val remote = withTimeoutOrNull(8_000L) {
            runCatching {
                SupabaseProvider.client.from("bible_game_questions")
                    .select().decodeList<BibleGameQuestionRow>()
                    .filter { it.gameType == gameType && it.options.size >= 2 && it.correctAnswerIndex in it.options.indices }
                    .sortedBy { it.id }
            }.getOrDefault(emptyList())
        }.orEmpty()
        if (remote.isNotEmpty()) return remote.shuffled().take(limit).map {
            BibleGameQuestion(
                id = it.id,
                category = BibleGameCategory.entries.firstOrNull { value -> value.name == it.category } ?: BibleGameCategory.PEOPLE,
                question = it.question,
                options = it.options,
                correctAnswerIndex = it.correctAnswerIndex,
                explanation = it.explanation,
                reference = it.reference,
                gameType = it.gameType
            )
        }
        delay(120)
        return when (gameType) {
            "guess_character" -> CHARACTER_BANK.shuffled().take(limit)
            "fill_blank" -> FILL_BLANK_BANK.shuffled().take(limit)
            else -> emptyList()
        }
    }

    suspend fun loadQuestions(
        category: BibleGameCategory = BibleGameCategory.ALL,
        limit: Int = 10
    ): List<BibleGameQuestion> {
        // Never let the games screen remain on a loading state because a remote
        // database request is slow or unavailable. The built-in bank is a
        // deliberate offline fallback so the game is always playable.
        val remote = withTimeoutOrNull(8_000L) {
            runCatching {
                SupabaseProvider.client
                    .from("bible_game_questions")
                    .select()
                    .decodeList<BibleGameQuestionRow>()
                    .filter {
                        it.gameType == "quiz" &&
                        (category == BibleGameCategory.ALL || it.category == category.name) &&
                            it.options.size >= 2 &&
                            it.correctAnswerIndex in it.options.indices
                    }
                    .sortedBy { it.id }
            }.getOrDefault(emptyList())
        }.orEmpty()

        if (remote.isNotEmpty()) {
            return remote.shuffled().take(limit.coerceAtMost(remote.size)).map {
                BibleGameQuestion(
                    id = it.id,
                    category = BibleGameCategory.entries.firstOrNull { value -> value.name == it.category }
                        ?: BibleGameCategory.FAITH_AND_LIFE,
                    question = it.question,
                    options = it.options,
                    correctAnswerIndex = it.correctAnswerIndex,
                    explanation = it.explanation,
                    reference = it.reference,
                    gameType = it.gameType
                )
            }
        }

        delay(120)
        val source = if (category == BibleGameCategory.ALL) {
            QUESTION_BANK
        } else {
            QUESTION_BANK.filter { it.category == category }
        }
        return source.shuffled().take(limit.coerceAtMost(source.size))
    }

    suspend fun loadPlayerStats(): BibleGamePlayerStats? {
        if (!SupabaseProvider.ensureSession()) return null
        val userId = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return runCatching {
            SupabaseProvider.client
                .from("bible_game_player_stats")
                .select()
                .decodeList<BibleGamePlayerStatsRow>()
                .firstOrNull { it.userId == userId }
                ?.let {
                    BibleGamePlayerStats(
                        xp = it.xp,
                        gamesPlayed = it.gamesPlayed,
                        questionsAnswered = it.questionsAnswered,
                        correctAnswers = it.correctAnswers,
                        bestScore = it.bestScore,
                        currentStreak = it.currentStreak,
                        bestStreak = it.bestStreak
                    )
                }
        }.getOrNull()
    }

    suspend fun recordQuizResult(score: Int, total: Int): BibleGamePlayerStats? {
        if (!SupabaseProvider.ensureSession()) return null
        val userId = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id ?: return null
        if (total <= 0) return null

        return runCatching {
            val existing = SupabaseProvider.client
                .from("bible_game_player_stats")
                .select()
                .decodeList<BibleGamePlayerStatsRow>()
                .firstOrNull { it.userId == userId }

            val today = java.time.LocalDate.now().toString()
            val currentStreak = when {
                existing == null -> 1
                existing.lastPlayedOn == today -> existing.currentStreak.coerceAtLeast(1)
                existing.lastPlayedOn == runCatching {
                    java.time.LocalDate.parse(today).minusDays(1).toString()
                }.getOrNull() -> existing.currentStreak + 1
                else -> 1
            }
            val xpEarned = (score.coerceAtLeast(0) * 10) + 20
            val next = BibleGamePlayerStatsRow(
                userId = userId,
                xp = (existing?.xp ?: 0) + xpEarned,
                gamesPlayed = (existing?.gamesPlayed ?: 0) + 1,
                questionsAnswered = (existing?.questionsAnswered ?: 0) + total,
                correctAnswers = (existing?.correctAnswers ?: 0) + score.coerceAtMost(total),
                bestScore = maxOf(existing?.bestScore ?: 0, score),
                currentStreak = currentStreak,
                bestStreak = maxOf(existing?.bestStreak ?: 0, currentStreak),
                lastPlayedOn = today
            )

            if (existing == null) {
                SupabaseProvider.client.from("bible_game_player_stats").insert(next)
            } else {
                SupabaseProvider.client
                    .from("bible_game_player_stats")
                    .update(next) {
                        filter { this.eq("user_id", userId) }
                    }
            }

            BibleGamePlayerStats(
                xp = next.xp,
                gamesPlayed = next.gamesPlayed,
                questionsAnswered = next.questionsAnswered,
                correctAnswers = next.correctAnswers,
                bestScore = next.bestScore,
                currentStreak = next.currentStreak,
                bestStreak = next.bestStreak
            )
        }.getOrNull()
    }

    suspend fun recordMemoryVerseCompleted(): BibleGamePlayerStats? {
        if (!SupabaseProvider.ensureSession()) return null
        val userId = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id ?: return null

        return runCatching {
            val existing = SupabaseProvider.client
                .from("bible_game_player_stats")
                .select()
                .decodeList<BibleGamePlayerStatsRow>()
                .firstOrNull { it.userId == userId }

            val today = java.time.LocalDate.now().toString()
            val yesterday = java.time.LocalDate.now().minusDays(1).toString()
            val streak = when {
                existing == null -> 1
                existing.lastPlayedOn == today -> existing.currentStreak.coerceAtLeast(1)
                existing.lastPlayedOn == yesterday -> existing.currentStreak + 1
                else -> 1
            }
            val next = BibleGamePlayerStatsRow(
                userId = userId,
                xp = (existing?.xp ?: 0) + 15,
                gamesPlayed = (existing?.gamesPlayed ?: 0) + 1,
                questionsAnswered = existing?.questionsAnswered ?: 0,
                correctAnswers = existing?.correctAnswers ?: 0,
                bestScore = existing?.bestScore ?: 0,
                currentStreak = streak,
                bestStreak = maxOf(existing?.bestStreak ?: 0, streak),
                lastPlayedOn = today
            )

            if (existing == null) {
                SupabaseProvider.client.from("bible_game_player_stats").insert(next)
            } else {
                SupabaseProvider.client
                    .from("bible_game_player_stats")
                    .update(next) {
                        filter { eq("user_id", userId) }
                    }
            }

            BibleGamePlayerStats(
                xp = next.xp,
                gamesPlayed = next.gamesPlayed,
                questionsAnswered = next.questionsAnswered,
                correctAnswers = next.correctAnswers,
                bestScore = next.bestScore,
                currentStreak = next.currentStreak,
                bestStreak = next.bestStreak
            )
        }.getOrNull()
    }

        private val CHARACTER_BANK = listOf(
            BibleGameQuestion("character-noah", BibleGameCategory.PEOPLE, "I built an ark before a great flood. Who am I?", listOf("Noah", "Moses", "David", "Joshua"), 0, "Noah obeyed God and built the ark before the flood.", "Genesis 6–9", "guess_character"),
            BibleGameQuestion("character-david", BibleGameCategory.PEOPLE, "I defeated a giant with a sling and a stone. Who am I?", listOf("Jonathan", "David", "Saul", "Samuel"), 1, "David trusted God and defeated Goliath.", "1 Samuel 17", "guess_character"),
            BibleGameQuestion("character-daniel", BibleGameCategory.PEOPLE, "I was thrown into a lions' den because I continued praying to God. Who am I?", listOf("Daniel", "Jeremiah", "Joseph", "Elijah"), 0, "Daniel remained faithful to God despite the royal decree.", "Daniel 6", "guess_character"),
            BibleGameQuestion("character-jonah", BibleGameCategory.PEOPLE, "I was swallowed by a great fish after running from God's call. Who am I?", listOf("Jonah", "Amos", "Elisha", "Isaiah"), 0, "Jonah eventually went to Nineveh after God called him.", "Jonah 1–4", "guess_character"),
            BibleGameQuestion("character-moses", BibleGameCategory.PEOPLE, "I led Israel out of Egypt and received the Law from God. Who am I?", listOf("Aaron", "Joshua", "Moses", "Caleb"), 2, "Moses led Israel out of Egypt and received God's commandments.", "Exodus 3–20", "guess_character"),
            BibleGameQuestion("character-solomon", BibleGameCategory.PEOPLE, "I was known for great wisdom and built the temple in Jerusalem. Who am I?", listOf("David", "Solomon", "Samuel", "Hezekiah"), 1, "Solomon asked God for wisdom and later built the temple.", "1 Kings 3–8", "guess_character")
        )

    private val FILL_BLANK_BANK = listOf(
        BibleGameQuestion("blank-psalm-23-1", BibleGameCategory.FAITH_AND_LIFE, "The Lord is my shepherd; I shall not ____. ", listOf("want", "fear", "sleep", "wander"), 0, "Psalm 23 describes God as a shepherd who provides for His people.", "Psalm 23:1", "fill_blank"),
        BibleGameQuestion("blank-proverbs-3-5", BibleGameCategory.FAITH_AND_LIFE, "Trust in the Lord with all your ____. ", listOf("strength", "heart", "wisdom", "wealth"), 1, "Proverbs teaches wholehearted trust in the Lord.", "Proverbs 3:5", "fill_blank"),
        BibleGameQuestion("blank-joshua-1-9", BibleGameCategory.OLD_TESTAMENT, "Be strong and of good ____. ", listOf("fortune", "cheer", "courage", "health"), 2, "God encouraged Joshua to be strong and courageous.", "Joshua 1:9", "fill_blank"),
        BibleGameQuestion("blank-john-3-16", BibleGameCategory.NEW_TESTAMENT, "For God so loved the world that He gave His only ____. ", listOf("prophet", "Son", "angel", "servant"), 1, "John 3:16 describes God's love and the gift of His Son.", "John 3:16", "fill_blank"),
        BibleGameQuestion("blank-philippians-4-13", BibleGameCategory.NEW_TESTAMENT, "I can do all things through Christ who ____. ", listOf("calls me", "strengthens me", "guides others", "created me"), 1, "Paul describes receiving strength through Christ.", "Philippians 4:13", "fill_blank"),
        BibleGameQuestion("blank-galatians-5-22", BibleGameCategory.FAITH_AND_LIFE, "The fruit of the Spirit includes love, joy, peace, and ____. ", listOf("patience", "pride", "envy", "fear"), 0, "Patience is included in the fruit of the Spirit.", "Galatians 5:22–23", "fill_blank")
    )

    companion object {
        private val QUESTION_BANK = listOf(
            BibleGameQuestion(
                "genesis-01", BibleGameCategory.OLD_TESTAMENT,
                "Who built the ark?",
                listOf("Abraham", "Noah", "Moses", "David"), 1,
                "God instructed Noah to build the ark before the flood.",
                "Genesis 6–7"
            ),
            BibleGameQuestion(
                "exodus-01", BibleGameCategory.OLD_TESTAMENT,
                "Who led the Israelites out of Egypt?",
                listOf("Joshua", "Aaron", "Moses", "Joseph"), 2,
                "Moses was called to lead Israel out of slavery in Egypt.",
                "Exodus 3–14"
            ),
            BibleGameQuestion(
                "samuel-01", BibleGameCategory.PEOPLE,
                "Who defeated Goliath?",
                listOf("Saul", "David", "Jonathan", "Samuel"), 1,
                "David faced Goliath and defeated him with a sling and a stone.",
                "1 Samuel 17"
            ),
            BibleGameQuestion(
                "solomon-01", BibleGameCategory.PEOPLE,
                "What did Solomon ask God for?",
                listOf("Wealth", "Long life", "Wisdom", "Military power"), 2,
                "Solomon asked for an understanding heart so he could govern God's people wisely.",
                "1 Kings 3"
            ),
            BibleGameQuestion(
                "jonah-01", BibleGameCategory.PEOPLE,
                "Which prophet was sent to Nineveh?",
                listOf("Jonah", "Elijah", "Isaiah", "Jeremiah"), 0,
                "God sent Jonah to preach to the people of Nineveh.",
                "Jonah 1–4"
            ),
            BibleGameQuestion(
                "daniel-01", BibleGameCategory.PEOPLE,
                "Where was Daniel thrown because of his faithfulness to God?",
                listOf("A furnace", "A lion's den", "A prison ship", "A cave"), 1,
                "Daniel was thrown into the lions' den after continuing to pray to God.",
                "Daniel 6"
            ),
            BibleGameQuestion(
                "bethlehem-01", BibleGameCategory.PLACES,
                "In which town was Jesus born?",
                listOf("Nazareth", "Jerusalem", "Bethlehem", "Capernaum"), 2,
                "Jesus was born in Bethlehem of Judea.",
                "Matthew 2; Luke 2"
            ),
            BibleGameQuestion(
                "disciples-01", BibleGameCategory.NEW_TESTAMENT,
                "How many apostles did Jesus appoint?",
                listOf("10", "12", "40", "70"), 1,
                "Jesus appointed twelve apostles to be with Him and to preach.",
                "Mark 3:13–19"
            ),
            BibleGameQuestion(
                "peter-01", BibleGameCategory.PEOPLE,
                "Which disciple walked on water toward Jesus?",
                listOf("John", "Peter", "Andrew", "Thomas"), 1,
                "Peter stepped out of the boat and walked toward Jesus on the water.",
                "Matthew 14:22–33"
            ),
            BibleGameQuestion(
                "parable-01", BibleGameCategory.NEW_TESTAMENT,
                "Who told the parable of the Good Samaritan?",
                listOf("Peter", "Paul", "Jesus", "James"), 2,
                "Jesus used the parable to teach about loving and showing mercy to our neighbor.",
                "Luke 10:25–37"
            ),
            BibleGameQuestion(
                "paul-01", BibleGameCategory.NEW_TESTAMENT,
                "What was Paul's name before his conversion?",
                listOf("Saul", "Simon", "Stephen", "Silas"), 0,
                "Paul was known as Saul before his encounter with Jesus on the road to Damascus.",
                "Acts 9"
            ),
            BibleGameQuestion(
                "resurrection-01", BibleGameCategory.NEW_TESTAMENT,
                "On which day did Jesus rise from the dead?",
                listOf("The first day of the week", "The second day", "The Sabbath", "The fifth day"), 0,
                "The Gospel accounts describe Jesus rising on the first day of the week.",
                "Matthew 28; Mark 16; Luke 24; John 20"
            ),
            BibleGameQuestion(
                "love-01", BibleGameCategory.FAITH_AND_LIFE,
                "According to Jesus, what is the greatest commandment?",
                listOf(
                    "Build a large temple",
                    "Love God with all your heart, soul, and mind",
                    "Become wealthy",
                    "Never make a mistake"
                ), 1,
                "Jesus identified wholehearted love for God as the greatest commandment.",
                "Matthew 22:34–40"
            ),
            BibleGameQuestion(
                "faith-01", BibleGameCategory.FAITH_AND_LIFE,
                "What does faith involve according to Hebrews 11?",
                listOf(
                    "Trusting what God has promised",
                    "Knowing everything",
                    "Avoiding every difficulty",
                    "Being famous"
                ), 0,
                "Hebrews 11 presents faith as confidence in what is hoped for and conviction about what is not seen.",
                "Hebrews 11:1"
            ),
            BibleGameQuestion(
                "fruit-01", BibleGameCategory.FAITH_AND_LIFE,
                "Which of these is listed as a fruit of the Spirit?",
                listOf("Jealousy", "Patience", "Pride", "Greed"), 1,
                "Patience is one of the qualities Paul lists as fruit produced by the Spirit.",
                "Galatians 5:22–23"
            ),
            BibleGameQuestion(
                "forgive-01", BibleGameCategory.FAITH_AND_LIFE,
                "What does Jesus teach His followers to do toward those who wrong them?",
                listOf("Seek revenge", "Refuse all mercy", "Forgive", "Ignore everyone"), 2,
                "Jesus repeatedly taught His followers to forgive and show mercy.",
                "Matthew 6:14–15; Matthew 18:21–35"
            )
        )
    }
}
