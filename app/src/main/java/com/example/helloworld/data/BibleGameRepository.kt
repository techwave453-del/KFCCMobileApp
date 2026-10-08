package com.example.helloworld.data

import kotlinx.coroutines.delay

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
    val reference: String
)

data class BibleGameRound(
    val questions: List<BibleGameQuestion>,
    val timed: Boolean
)

class BibleGameRepository {
    suspend fun loadQuestions(
        category: BibleGameCategory = BibleGameCategory.ALL,
        limit: Int = 10
    ): List<BibleGameQuestion> {
        delay(120)
        val source = if (category == BibleGameCategory.ALL) {
            QUESTION_BANK
        } else {
            QUESTION_BANK.filter { it.category == category }
        }
        return source.shuffled().take(limit.coerceAtMost(source.size))
    }

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
