package com.pryvn.audiophile.code.utils.lrc

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedWord(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val isBackground: Boolean = false
)

data class ParsedLine(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val words: List<ParsedWord> = emptyList(),
    val isBackground: Boolean = false,
    val agent: String? = null,
    val providerRomanizedText: String? = null,
    val providerRomanizedWords: List<String>? = null,
    val providerRomanizedLanguage: String? = null,
    val providerTranslationText: String? = null,
)

object TTMLParser {

    private data class TimingContext(val tickRate: Double, val frameRate: Double)

    private data class ParsedTransliteration(val text: String, val words: List<String>, val language: String?)

    private val whitespaceRegex = Regex("\\s+")

    fun isTtml(lyrics: String): Boolean {
        val trimmed = lyrics.trimStart()
        return trimmed.startsWith("<") && (trimmed.contains("<tt") || trimmed.contains("ttml"))
    }

    fun isLineSyncedLrc(lyrics: String): Boolean {
        val timeRegex = Regex("""\[\d{2}:\d{2}(\.\d{2,3})?\]""")
        return timeRegex.containsMatchIn(lyrics)
    }

    fun isCjk(text: String): Boolean = text.any { c ->
        Character.UnicodeBlock.of(c) in setOf(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT,
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA,
            Character.UnicodeBlock.HANGUL_SYLLABLES,
            Character.UnicodeBlock.HANGUL_JAMO,
            Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
        )
    }

    fun parseTTML(ttml: String): List<ParsedLine> {
        if (!isTtml(ttml)) return emptyList()
        val lines = mutableListOf<ParsedLine>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(ttml.toByteArray()))
            doc.documentElement.normalize()
            val timingContext = readTimingContext(doc.documentElement)
            val transliterations = parseTransliterations(doc.documentElement)
            val translations = parseTranslations(doc.documentElement)

            val divElements = doc.getElementsByTagName("*")
            for (divIdx in 0 until divElements.length) {
                val divElement = divElements.item(divIdx) as? Element ?: continue
                if (!divElement.tagName.endsWith("div", ignoreCase = true)) continue
                val pElements = divElement.getElementsByTagName("*")
                for (pIdx in 0 until pElements.length) {
                    val pElement = pElements.item(pIdx) as? Element ?: continue
                    if (!pElement.tagName.endsWith("p", ignoreCase = true)) continue
                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    val dur = pElement.getAttribute("dur")
                    if (begin.isNullOrEmpty()) continue
                    val startTime = parseTime(begin, timingContext)
                    val endTime = when {
                        end.isNotEmpty() -> parseTime(end, timingContext)
                        dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
                        else -> startTime + 5.0
                    }
                    val agent = pElement.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
                        ?: pElement.attributes?.let { attrs ->
                            (0 until attrs.length).map { attrs.item(it) }
                                .firstOrNull { it.nodeName.endsWith("agent", ignoreCase = true) }
                                ?.nodeValue?.takeIf { it.isNotEmpty() }
                        }
                    val lineKey = readAttributeBySuffix(pElement, "key")
                    val transliteration = lineKey?.let(transliterations::get)
                    val translation = lineKey?.let(translations::get)
                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()
                    parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)

                    if (words.isEmpty() && lineText.isNotEmpty()) {
                        splitWordsWithCjk(lineText.toString(), startTime, endTime).forEach { (word, ws, we) ->
                            words.add(ParsedWord(word, ws, we, false))
                        }
                    } else if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                            splitWordsWithCjk(directText, startTime, endTime).forEach { (word, ws, we) ->
                                words.add(ParsedWord(word, ws, we, false))
                            }
                        }
                    }
                    if (lineText.isNotEmpty()) {
                        lines.add(ParsedLine(
                            text = lineText.toString().trim(),
                            startTime = startTime,
                            endTime = endTime,
                            words = words,
                            isBackground = false,
                            agent = agent,
                            providerRomanizedText = transliteration?.text,
                            providerRomanizedWords = transliteration?.words,
                            providerRomanizedLanguage = transliteration?.language,
                            providerTranslationText = translation,
                        ))
                    }
                }
            }

            if (lines.isEmpty()) {
                val pElements = doc.getElementsByTagName("*")
                for (i in 0 until pElements.length) {
                    val pElement = pElements.item(i) as? Element ?: continue
                    if (!pElement.tagName.endsWith("p", ignoreCase = true)) continue
                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    val dur = pElement.getAttribute("dur")
                    if (begin.isNullOrEmpty()) continue
                    val startTime = parseTime(begin, timingContext)
                    val endTime = when {
                        end.isNotEmpty() -> parseTime(end, timingContext)
                        dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
                        else -> startTime + 5.0
                    }
                    val agent = pElement.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
                        ?: pElement.attributes?.let { attrs ->
                            (0 until attrs.length).map { attrs.item(it) }
                                .firstOrNull { it.nodeName.endsWith("agent", ignoreCase = true) }
                                ?.nodeValue?.takeIf { it.isNotEmpty() }
                        }
                    val lineKey = readAttributeBySuffix(pElement, "key")
                    val transliteration = lineKey?.let(transliterations::get)
                    val translation = lineKey?.let(translations::get)
                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()
                    parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)

                    if (words.isEmpty() && lineText.isNotEmpty()) {
                        splitWordsWithCjk(lineText.toString(), startTime, endTime).forEach { (word, ws, we) ->
                            words.add(ParsedWord(word, ws, we, false))
                        }
                    } else if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                            splitWordsWithCjk(directText, startTime, endTime).forEach { (word, ws, we) ->
                                words.add(ParsedWord(word, ws, we, false))
                            }
                        }
                    }
                    if (lineText.isNotEmpty()) {
                        lines.add(ParsedLine(
                            text = lineText.toString().trim(),
                            startTime = startTime,
                            endTime = endTime,
                            words = words,
                            isBackground = false,
                            agent = agent,
                            providerRomanizedText = transliteration?.text,
                            providerRomanizedWords = transliteration?.words,
                            providerRomanizedLanguage = transliteration?.language,
                            providerTranslationText = translation,
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return parseTtmlRegex(ttml)
        }
        return lines.sortedBy { it.startTime }
    }

    private fun splitWordsWithCjk(text: String, startTime: Double, endTime: Double): List<Triple<String, Double, Double>> {
        val isCjkText = isCjk(text)
        val splitWords = if (isCjkText) {
            val chars = mutableListOf<String>()
            var currentWord = StringBuilder()
            text.forEach { char ->
                if (char.isWhitespace()) {
                    if (currentWord.isNotEmpty()) { chars.add(currentWord.toString()); currentWord.clear() }
                    chars.add(char.toString())
                } else if (isCjk(char.toString())) {
                    if (currentWord.isNotEmpty()) { chars.add(currentWord.toString()); currentWord.clear() }
                    chars.add(char.toString())
                } else {
                    currentWord.append(char)
                }
            }
            if (currentWord.isNotEmpty()) chars.add(currentWord.toString())
            val grouped = mutableListOf<String>()
            chars.forEach { c ->
                if (c.isBlank()) {
                    if (grouped.isNotEmpty()) grouped[grouped.lastIndex] = grouped.last() + c
                } else grouped.add(c)
            }
            grouped
        } else {
            text.split(Regex("\\s+"))
        }
        val totalDuration = endTime - startTime
        val totalLength = splitWords.sumOf { it.length }.toDouble()
        val result = mutableListOf<Triple<String, Double, Double>>()
        var currentWordStart = startTime
        splitWords.forEachIndexed { index, word ->
            val wordLen = word.length.toDouble()
            val wordDuration = if (totalLength > 0) (wordLen / totalLength) * totalDuration else totalDuration / splitWords.size
            val wordEnd = currentWordStart + wordDuration
            val wordText = if (index < splitWords.size - 1 && !isCjkText) "$word " else word
            result.add(Triple(wordText, currentWordStart, wordEnd))
            currentWordStart = wordEnd
        }
        return result
    }

    private fun parseTransliterations(root: Element): Map<String, ParsedTransliteration> {
        val latinTransliterations = linkedMapOf<String, ParsedTransliteration>()
        val fallbackTransliterations = linkedMapOf<String, ParsedTransliteration>()
        val elements = root.getElementsByTagName("*")
        for (i in 0 until elements.length) {
            val el = elements.item(i) as? Element ?: continue
            if (!el.tagName.endsWith("transliteration", ignoreCase = true)) continue
            val language = readAttributeBySuffix(el, "lang")
            val target = if (language?.contains("Latn", ignoreCase = true) == true) latinTransliterations else fallbackTransliterations
            val textElements = el.getElementsByTagName("*")
            for (j in 0 until textElements.length) {
                val textEl = textElements.item(j) as? Element ?: continue
                if (!textEl.tagName.endsWith("text", ignoreCase = true)) continue
                val lineKey = readAttributeBySuffix(textEl, "for") ?: continue
                val parsed = parseTransliterationLine(textEl, language)
                if (parsed.text.isNotBlank() && lineKey !in target) target[lineKey] = parsed
            }
        }
        return fallbackTransliterations.apply { putAll(latinTransliterations) }
    }

    private fun parseTranslations(root: Element): Map<String, String> {
        val translations = linkedMapOf<String, String>()
        val elements = root.getElementsByTagName("*")
        for (i in 0 until elements.length) {
            val el = elements.item(i) as? Element ?: continue
            if (!el.tagName.endsWith("translation", ignoreCase = true)) continue
            val textElements = el.getElementsByTagName("*")
            for (j in 0 until textElements.length) {
                val textEl = textElements.item(j) as? Element ?: continue
                if (!textEl.tagName.endsWith("text", ignoreCase = true)) continue
                val lineKey = readAttributeBySuffix(textEl, "for") ?: continue
                val translatedText = normalizeRomanization(textEl.textContent.orEmpty()) ?: continue
                translations[lineKey] = translatedText
            }
        }
        return translations
    }

    private fun parseTransliterationLine(element: Element, language: String?): ParsedTransliteration {
        val lineText = StringBuilder()
        val words = mutableListOf<String>()
        parseTransliterationNodes(element, lineText, words)
        return ParsedTransliteration(
            text = normalizeRomanization(lineText.toString()).orEmpty(),
            words = words.mapNotNull(::normalizeRomanization),
            language = language,
        )
    }

    private fun parseTransliterationNodes(element: Element, lineText: StringBuilder, words: MutableList<String>) {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    val childEl = node as Element
                    if (childEl.tagName.endsWith("span", ignoreCase = true)) {
                        val rawText = childEl.textContent.orEmpty()
                        lineText.append(rawText)
                        normalizeRomanization(rawText)?.let(words::add)
                    } else {
                        parseTransliterationNodes(childEl, lineText, words)
                    }
                }
                Node.TEXT_NODE -> lineText.append(node.textContent.orEmpty())
            }
        }
    }

    private fun normalizeRomanization(text: String): String? = text.replace(whitespaceRegex, " ").trim().takeIf { it.isNotEmpty() }

    private fun parseSpanElements(
        element: Element, words: MutableList<ParsedWord>, lineText: StringBuilder,
        lineStartTime: Double, lineEndTime: Double, isBackground: Boolean, timingContext: TimingContext,
    ) {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    val childEl = node as Element
                    if (childEl.tagName.endsWith("span", ignoreCase = true)) {
                        val role = childEl.getAttribute("role").takeIf { it.isNotEmpty() }
                            ?: childEl.getAttribute("ttm:role")
                        val isBgSpan = role == "x-bg" || isBackground
                        val wBegin = childEl.getAttribute("begin")
                        val wEnd = childEl.getAttribute("end")
                        val wDur = childEl.getAttribute("dur")
                        val nestedSpans = childEl.getElementsByTagName("*")
                        if (nestedSpans.length > 0 && hasDirectSpanChildren(childEl)) {
                            parseSpanElements(childEl, words, lineText, lineStartTime, lineEndTime, isBgSpan, timingContext)
                        } else {
                            val wordText = getDirectTextContent(childEl)
                            if (wordText.isNotEmpty()) {
                                val isSyllableContinuation = words.isNotEmpty() && !words.last().text.endsWith(" ")
                                lineText.append(wordText)
                                val rawWordStart = wBegin.takeIf { it.isNotEmpty() }?.let { parseTime(it, timingContext) }
                                val rawWordEnd = when {
                                    wEnd.isNotEmpty() -> parseTime(wEnd, timingContext)
                                    wDur.isNotEmpty() && rawWordStart != null -> rawWordStart + parseTime(wDur, timingContext)
                                    else -> null
                                }
                                val wordStartTime = normalizeChildTime(rawWordStart, lineStartTime, lineEndTime, lineStartTime)
                                val wordEndTime = normalizeChildTime(rawWordEnd, lineStartTime, lineEndTime, lineEndTime).coerceAtLeast(wordStartTime)
                                val trimmedText = wordText.trim()
                                val newWord = ParsedWord(trimmedText, wordStartTime, wordEndTime, isBgSpan)
                                val lastWord = words.lastOrNull()
                                if (isSyllableContinuation && lastWord != null && !lastWord.text.endsWith(" ")
                                    && lastWord.isBackground == isBgSpan && !isCjk(lastWord.text.trim()) && !isCjk(trimmedText) && trimmedText.isNotEmpty()
                                ) {
                                    words[words.lastIndex] = lastWord.copy(text = lastWord.text + trimmedText, endTime = wordEndTime)
                                } else if (trimmedText.isNotEmpty()) {
                                    words.add(newWord)
                                }
                            }
                        }
                    }
                }
                Node.TEXT_NODE -> {
                    val text = node.textContent
                    if (text.isNotBlank()) lineText.append(text)
                    else if (text.isNotEmpty() && !text.contains('\n')) {
                        if (words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                            lineText.append(" ")
                            val lastWord = words.last()
                            words[words.lastIndex] = lastWord.copy(text = lastWord.text + " ")
                        }
                    }
                }
            }
        }
    }

    private fun hasDirectSpanChildren(element: Element): Boolean {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE && (node as Element).tagName.endsWith("span", ignoreCase = true)) return true
        }
        return false
    }

    private fun getDirectTextContent(element: Element): String {
        val textBuilder = StringBuilder()
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.TEXT_NODE) textBuilder.append(node.textContent)
        }
        return textBuilder.toString()
    }

    private fun normalizeChildTime(raw: Double?, lineStartTime: Double, lineEndTime: Double, fallback: Double): Double {
        if (raw == null || raw.isNaN() || raw.isInfinite()) return fallback
        val lineDuration = (lineEndTime - lineStartTime).coerceAtLeast(0.0)
        val isProbablyRelative = raw < (lineStartTime - 0.25) && raw <= (lineDuration + 1.0)
        val adjusted = if (isProbablyRelative) lineStartTime + raw else raw
        return adjusted.coerceIn(lineStartTime.coerceAtLeast(0.0), lineEndTime.coerceAtLeast(lineStartTime))
    }

    private fun readTimingContext(root: Element): TimingContext {
        fun getAttrBySuffix(suffix: String): String? {
            val attrs = root.attributes ?: return null
            for (i in 0 until attrs.length) {
                val node = attrs.item(i) ?: continue
                if (node.nodeName.endsWith(suffix, ignoreCase = true)) {
                    val v = node.nodeValue?.trim()
                    if (!v.isNullOrEmpty()) return v
                }
            }
            return null
        }
        val baseFrameRate = getAttrBySuffix("frameRate")?.toDoubleOrNull() ?: 30.0
        val frameRateMultiplierRaw = getAttrBySuffix("frameRateMultiplier")
        val frameRateMultiplier = frameRateMultiplierRaw?.split(Regex("\\s+"))
            ?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == 2 && it[1] != 0.0 }
            ?.let { it[0] / it[1] } ?: 1.0
        val frameRate = (baseFrameRate * frameRateMultiplier).coerceAtLeast(1.0)
        val tickRate = getAttrBySuffix("tickRate")?.toDoubleOrNull() ?: (frameRate * 1.0).coerceAtLeast(1.0)
        return TimingContext(tickRate = tickRate, frameRate = frameRate)
    }

    private fun readAttributeBySuffix(element: Element, suffix: String): String? {
        val directValue = element.getAttribute(suffix).takeIf { it.isNotBlank() }
        if (directValue != null) return directValue
        val attrs = element.attributes ?: return null
        for (i in 0 until attrs.length) {
            val node = attrs.item(i) ?: continue
            val name = node.nodeName ?: continue
            if (name.equals(suffix, ignoreCase = true) || name.endsWith(":$suffix", ignoreCase = true)) {
                val value = node.nodeValue?.trim()
                if (!value.isNullOrEmpty()) return value
            }
        }
        return null
    }

    private fun parseTime(timeStr: String, timingContext: TimingContext): Double {
        return try {
            val raw = timeStr.trim()
            if (raw.isEmpty()) return 0.0
            val offsetRegex = Regex("""^([0-9]+(?:\.[0-9]+)?)(h|ms|m|s|f|t)$""", RegexOption.IGNORE_CASE)
            offsetRegex.matchEntire(raw)?.let { m ->
                val value = m.groupValues[1].toDoubleOrNull() ?: return 0.0
                return when (m.groupValues[2].lowercase()) {
                    "h" -> value * 3600.0; "m" -> value * 60.0; "s" -> value; "ms" -> value / 1000.0
                    "f" -> value / timingContext.frameRate; "t" -> value / timingContext.tickRate; else -> value
                }
            }
            val cleanClock = raw.replace(';', ':').trimEnd { it.isLetter() }
            if (cleanClock.contains(":")) {
                val parts = cleanClock.split(":")
                return when (parts.size) {
                    2 -> (parts[0].toDoubleOrNull() ?: 0.0) * 60.0 + (parts[1].toDoubleOrNull() ?: 0.0)
                    3 -> (parts[0].toDoubleOrNull() ?: 0.0) * 3600.0 + (parts[1].toDoubleOrNull() ?: 0.0) * 60.0 + (parts[2].toDoubleOrNull() ?: 0.0)
                    4 -> (parts[0].toDoubleOrNull() ?: 0.0) * 3600.0 + (parts[1].toDoubleOrNull() ?: 0.0) * 60.0 + (parts[2].toDoubleOrNull() ?: 0.0) + ((parts[3].toDoubleOrNull() ?: 0.0) / timingContext.frameRate)
                    else -> cleanClock.toDoubleOrNull() ?: 0.0
                }
            }
            raw.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) { 0.0 }
    }

    private fun parseTtmlRegex(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        val lineRegex = Regex("""<p\s+[^>]*begin="([^"]+)"[^>]*(?:end="([^"]+)")?[^>]*>(.*?)</p>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        for (match in lineRegex.findAll(ttml)) {
            val begin = match.groupValues[1]
            val end = match.groupValues[2]
            val content = match.groupValues[3]
            val startTime = parseTtmlTime(begin)
            val endTime = if (end.isNotBlank()) parseTtmlTime(end) else startTime + 3.0
            val isBackground = content.contains("role=\"x-bg\"") || content.contains("class=\"x-bg\"")
            val agent = Regex("""ttm:agent="([^"]+)"""").find(content)?.groupValues?.get(1)
            val spanRegex = Regex("""<span[^>]*(?:begin="([^"]+)")?[^>]*(?:end="([^"]+)")?[^>]*>(.*?)</span>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            var plainText = content.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
            val words = mutableListOf<ParsedWord>()
            var hasWordTiming = false
            for (span in spanRegex.findAll(content)) {
                val wBegin = span.groupValues[1]
                val wEnd = span.groupValues[2]
                val wText = span.groupValues[3].replace(Regex("<[^>]+>"), "").trim()
                if (wText.isNotBlank() && wBegin.isNotBlank()) {
                    hasWordTiming = true
                    words.add(ParsedWord(wText, parseTtmlTime(wBegin), if (wEnd.isNotBlank()) parseTtmlTime(wEnd) else parseTtmlTime(wBegin) + 0.3, isBackground))
                } else if (wText.isNotBlank()) {
                    plainText = wText
                }
            }
            if (!hasWordTiming) {
                val chars = mutableListOf<ParsedWord>()
                val charList = plainText.toList()
                val charDuration = (endTime - startTime) / charList.size.coerceAtLeast(1)
                charList.forEachIndexed { i, c ->
                    if (c != ' ') chars.add(ParsedWord(c.toString(), startTime + i * charDuration, startTime + (i + 1) * charDuration, isBackground))
                }
                if (chars.isNotEmpty()) lines.add(ParsedLine(plainText, startTime, endTime, chars, isBackground, agent))
                else if (plainText.isNotBlank()) lines.add(ParsedLine(plainText, startTime, endTime, emptyList(), isBackground, agent))
            } else {
                lines.add(ParsedLine(plainText, startTime, endTime, words, isBackground, agent))
            }
        }
        return lines.sortedBy { it.startTime }
    }

    fun parseTtmlTime(value: String): Double {
        val clean = value.trim().removeSuffix("s").removeSuffix("ms")
        if (clean.endsWith("t")) return clean.removeSuffix("t").toDoubleOrNull() ?: 0.0
        if (clean.endsWith("f")) return (clean.removeSuffix("f").toDoubleOrNull() ?: 0.0) / 30.0
        val parts = clean.split(":", ";")
        return when (parts.size) {
            3 -> (parts[0].toDoubleOrNull() ?: 0.0) * 3600.0 + (parts[1].toDoubleOrNull() ?: 0.0) * 60.0 + (parts[2].toDoubleOrNull() ?: 0.0)
            2 -> (parts[0].toDoubleOrNull() ?: 0.0) * 60.0 + (parts[1].toDoubleOrNull() ?: 0.0)
            else -> clean.toDoubleOrNull() ?: 0.0
        }
    }

    private val lrcTimeRegex = Regex("""\[(\d{2}):(\d{2}(?:\.\d{2,3})?)\]""")

    fun parseSyncedLrc(lrcText: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        val rawLines = lrcText.lines().filter { it.isNotBlank() }
        for (rawLine in rawLines) {
            val timeMatches = lrcTimeRegex.findAll(rawLine)
            val times = timeMatches.map { match ->
                val mins = match.groupValues[1].toIntOrNull() ?: 0
                val secs = match.groupValues[2].toFloatOrNull() ?: 0f
                mins * 60.0 + secs.toDouble()
            }.toList()
            if (times.isEmpty()) continue
            val textPart = rawLine.replace(lrcTimeRegex, "").trim()
            if (textPart.isBlank()) continue
            val lineStart = times.first()
            val lineEnd = times.last() + 2.0
            val wordRegex = Regex("""<(\d{2}:\d{2}(?:\.\d{2,3})?)>""")
            val wordMatches = wordRegex.findAll(textPart).toList()
            if (wordMatches.isNotEmpty()) {
                val words = mutableListOf<ParsedWord>()
                var lastEnd = lineStart
                var lastIndex = 0
                for (wm in wordMatches) {
                    val before = textPart.substring(lastIndex, wm.range.first).trim()
                    if (before.isNotBlank()) {
                        words.add(ParsedWord(before, lastEnd, parseLrcTimeToSec(wm.groupValues[1])))
                        lastEnd = parseLrcTimeToSec(wm.groupValues[1])
                    }
                    lastIndex = wm.range.last + 1
                }
                val remaining = textPart.substring(lastIndex).trim()
                if (remaining.isNotBlank()) words.add(ParsedWord(remaining, lastEnd, lineEnd))
                if (words.isNotEmpty()) lines.add(ParsedLine(textPart, lineStart, lineEnd, words))
            } else {
                lines.add(ParsedLine(textPart, lineStart, lineEnd, emptyList()))
            }
        }
        return lines
    }

    fun insertInstrumentalBreaks(entries: List<ParsedLine>, songDurationMs: Long): List<ParsedLine> {
        if (entries.isEmpty()) return entries
        val result = mutableListOf<ParsedLine>()
        val gapThreshold = 5.0
        val songDurationSec = songDurationMs / 1000.0
        val firstStart = entries.first().startTime
        if (firstStart > gapThreshold) result.add(ParsedLine("", 0.0, firstStart, emptyList()))
        for (i in entries.indices) {
            result.add(entries[i])
            if (i < entries.size - 1) {
                val gap = entries[i + 1].startTime - entries[i].endTime
                if (gap > gapThreshold) result.add(ParsedLine("", entries[i].endTime, entries[i + 1].startTime, emptyList()))
            }
        }
        val lastEnd = entries.last().endTime
        if (songDurationSec > 0 && lastEnd < songDurationSec - gapThreshold) {
            result.add(ParsedLine("", lastEnd, songDurationSec, emptyList()))
        }
        return result
    }

    private fun parseLrcTimeToSec(time: String): Double {
        val parts = time.split(":")
        if (parts.size != 2) return 0.0
        val mins = parts[0].toIntOrNull() ?: 0
        val secs = parts[1].toFloatOrNull() ?: 0f
        return mins * 60.0 + secs.toDouble()
    }

    fun ttmlToLrc(text: String): String {
        if (!text.trimStart().startsWith("<")) return text
        val lineRegex = Regex("""<p[^>]*begin="([^"]+)"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        return lineRegex.findAll(text).joinToString("\n") { match ->
            val timestamp = ttmlTimeToLrc(match.groupValues[1])
            val line = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
            "[$timestamp]$line"
        }
    }

    private fun ttmlTimeToLrc(value: String): String {
        val parts = value.removeSuffix("s").split(":")
        val seconds = when (parts.size) {
            3 -> parts[0].toFloat() * 3600 + parts[1].toFloat() * 60 + parts[2].toFloat()
            2 -> parts[0].toFloat() * 60 + parts[1].toFloat()
            else -> parts.firstOrNull()?.toFloatOrNull() ?: 0f
        }
        val minutes = (seconds / 60).toInt()
        val remaining = seconds - minutes * 60
        return "%02d:%05.2f".format(minutes, remaining)
    }
}
