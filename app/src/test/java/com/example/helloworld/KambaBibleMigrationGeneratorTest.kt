package com.example.helloworld

import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KambaBibleMigrationGeneratorTest {

    @Test
    fun generateKambaBibleMigrationSqlParts() {
        val xmlFile = File("src/main/assets/bible/kamba/KambaMBIVLIABible.xml")
        if (!xmlFile.exists()) {
            println("XML file not found at ${xmlFile.absolutePath}")
            return
        }

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val standardBookIds = listOf(
            "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL",
            "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
        )

        val verses = mutableListOf<Triple<String, Int, Pair<Int, String>>>()

        val bookNodes = doc.getElementsByTagName("book")
        for (i in 0 until bookNodes.length) {
            val bookElement = bookNodes.item(i) as? Element ?: continue
            val bookNumStr = bookElement.getAttribute("number").ifEmpty { bookElement.getAttribute("bnumber") }
            val bookNum = bookNumStr.toIntOrNull() ?: (i + 1)
            val bookId = if (bookNum in 1..standardBookIds.size) standardBookIds[bookNum - 1] else "BK$bookNum"

            val chapterNodes = bookElement.getElementsByTagName("chapter")
            for (j in 0 until chapterNodes.length) {
                val chapElement = chapterNodes.item(j) as? Element ?: continue
                val chapNum = chapElement.getAttribute("number").toIntOrNull() ?: (j + 1)

                val verseNodes = chapElement.getElementsByTagName("verse")
                for (k in 0 until verseNodes.length) {
                    val verseElement = verseNodes.item(k) as? Element ?: continue
                    val verseNum = verseElement.getAttribute("number").toIntOrNull() ?: (k + 1)
                    val verseText = verseElement.textContent?.trim() ?: ""
                    if (verseText.isNotBlank()) {
                        verses.add(Triple(bookId, chapNum, Pair(verseNum, verseText)))
                    }
                }
            }
        }

        val migrationsDir = File("../supabase/migrations")
        migrationsDir.mkdirs()

        // 1. Generate translation insert file with cleanup for abbreviation unique constraint conflicts
        val translationFile = File(migrationsDir, "20261025_add_kamba_mbivilia_translation.sql")
        translationFile.writeText(
            """
            -- Clean up any existing Kamba translation record to avoid unique constraint conflicts
            delete from public.bible_translations where id = 'kam' or abbreviation = 'KAM';

            -- Add Kamba Bible (Mbivilia) translation record
            insert into public.bible_translations (id, abbreviation, name, language_code, language_name, license_name, source_name, is_enabled, sort_order)
            values ('kam', 'KAM', 'Kamba Bible (Mbivilia)', 'kam', 'Kamba', 'God''s word open for all ©Bible Society of Kenya, 1956', 'Bible Society of Kenya', true, 25);
            """.trimIndent()
        )

        // 2. Split verses into parts of ~4000 verses each (approx 600KB each)
        val chunkSize = 4000
        verses.chunked(chunkSize).forEachIndexed { partIndex, chunk ->
            val partNum = partIndex + 1
            val partFile = File(migrationsDir, "20261025_add_kamba_mbivilia_bible_part$partNum.sql")
            val sb = StringBuilder()
            sb.append("-- Kamba Bible (Mbivilia) Verses - Part $partNum\n")
            sb.append("insert into public.bible_verses (translation_id, book_id, chapter, verse, text)\n")
            sb.append("values\n")

            chunk.forEachIndexed { index, (bId, chap, vPair) ->
                val (vNum, vText) = vPair
                val escapedText = vText.replace("'", "''")
                val isLast = index == chunk.size - 1
                sb.append("('kam', '$bId', $chap, $vNum, '$escapedText')${if (isLast) "" else ","}\n")
            }
            sb.append("on conflict (translation_id, book_id, chapter, verse) do update set text = EXCLUDED.text;\n")
            partFile.writeText(sb.toString())
            println("Generated ${partFile.name} with ${chunk.size} verses.")
        }
    }
}
