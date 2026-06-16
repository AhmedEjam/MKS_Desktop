package com.ahmedyejam.mks.desktop

import com.ahmedyejam.mks.data.importer.detector.ImportFormatDetector
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.parser.CsvParser
import com.ahmedyejam.mks.data.importer.parser.GenericImageExtractor
import com.ahmedyejam.mks.data.importer.parser.HtmlQuestionParser
import com.ahmedyejam.mks.data.importer.parser.JsonLibraryParser
import com.ahmedyejam.mks.data.importer.parser.JsonQuestionParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetHeaderMapper
import com.ahmedyejam.mks.data.importer.parser.TextQuestionParser
import com.ahmedyejam.mks.data.importer.parser.ZipLibraryParser
import com.ahmedyejam.mks.data.importer.xlsx.XlsxLibraryCompiler
import com.ahmedyejam.mks.data.local.entity.QuestionType
import java.io.File

/**
 * Desktop compiler — parses imported files into ParsedQuestions
 * without Android dependencies. Mirrors CompilerViewModel logic.
 */
class DesktopCompiler {
    private val formatDetector = ImportFormatDetector()
    private val csvParser = CsvParser()
    private val headerMapper = SpreadsheetHeaderMapper()
    private val imageExtractor = GenericImageExtractor()
    private val textParser = TextQuestionParser(imageExtractor)
    private val jsonQuestionParser = JsonQuestionParser(imageExtractor)
    private val htmlParser = HtmlQuestionParser(jsonQuestionParser)
    private val jsonLibraryParser = JsonLibraryParser()
    private val zipParser = ZipLibraryParser(jsonLibraryParser)
    private val xlsxCompiler = XlsxLibraryCompiler()

    data class CompileResult(
        val questions: List<ParsedQuestion>,
        val format: ImportFormat,
        val fileName: String,
        val error: String? = null
    )

    fun compile(filePath: String): CompileResult {
        val file = File(filePath)
        if (!file.exists()) return CompileResult(emptyList(), ImportFormat.UNKNOWN, file.name, "File not found")

        val format = formatDetector.detectFormat(filePath)
        return try {
            when (format) {
                ImportFormat.CSV_TSV -> compileCsv(file)
                ImportFormat.XLSX -> compileXlsx(filePath)
                ImportFormat.TEXT -> compileText(file)
                ImportFormat.JSON -> compileJson(file)
                ImportFormat.HTML -> compileHtml(file)
                ImportFormat.ZIP -> compileZip(file)
                ImportFormat.UNKNOWN -> CompileResult(emptyList(), format, file.name, "Unknown file format")
            }
        } catch (e: Exception) {
            CompileResult(emptyList(), format, file.name, e.message ?: "Parse error")
        }
    }

    private fun compileCsv(file: File): CompileResult {
        val content = file.readText()
        val rows = csvParser.parse(content)
        if (rows.size < 2) return CompileResult(emptyList(), ImportFormat.CSV_TSV, file.name, "File has no data rows")

        val header = rows[0].map { it.trim().lowercase() }
        val qIdx = header.indexOfFirst { it in listOf("question", "text", "stem", "سؤال") }
        val optIdx = header.indexOfFirst { it in listOf("options", "choices", "answers", "خيارات") }
        val correctIdx = header.indexOfFirst { it in listOf("correct_answer", "answer", "correct", "الإجابة") }
        if (qIdx < 0) return CompileResult(emptyList(), ImportFormat.CSV_TSV, file.name, "No question column found in header")

        val questions = mutableListOf<ParsedQuestion>()
        for (i in 1 until rows.size) {
            val cols = rows[i]
            if (cols.size <= qIdx) continue
            val text = cols.getOrElse(qIdx) { "" }.trim()
            if (text.isBlank()) continue

            val options = if (optIdx >= 0 && optIdx < cols.size) {
                cols[optIdx].split("|", ";").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()

            val correctAnswers = if (correctIdx >= 0 && correctIdx < cols.size) {
                val answer = cols[correctIdx].trim()
                if (options.isNotEmpty()) {
                    options.mapIndexedNotNull { idx, opt ->
                        if (opt.equals(answer, ignoreCase = true) || answer.contains(('A' + idx).toString())) idx else null
                    }
                } else {
                    answer.split(",", ";", "|").mapNotNull { it.trim().toIntOrNull()?.minus(1) }
                }
            } else emptyList()

            if (options.isEmpty() && correctAnswers.isEmpty()) {
                // No options column — parse as text question
                questions.add(ParsedQuestion(stem = text, type = QuestionType.SINGLE_CHOICE))
            } else {
                val parsedOptions = options.mapIndexed { idx, opt ->
                    ParsedOption(id = "opt_${'A' + idx}", text = opt)
                }
                questions.add(
                    ParsedQuestion(
                        stem = text,
                        options = parsedOptions,
                        correctAnswers = correctAnswers.map { "opt_${'A' + it}" },
                        type = if (correctAnswers.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
                        sourceLine = i + 1
                    )
                )
            }
        }
        return CompileResult(questions, ImportFormat.CSV_TSV, file.name)
    }

    /** Re-parse CSV with user-selected header row and column mapping. */
    fun reparseCsv(rows: List<List<String>>, headerIdx: Int, mapping: Map<String, Int>): CompileResult {
        if (rows.isEmpty() || headerIdx >= rows.size) return CompileResult(emptyList(), ImportFormat.CSV_TSV, "", "Invalid header row")

        val qIdx = mapping["question"] ?: -1
        val optIdx = mapping["options"] ?: -1
        val ansIdx = mapping["answer"] ?: -1
        val explIdx = mapping["explanation"] ?: -1
        val hintIdx = mapping["hint"] ?: -1
        val refIdx = mapping["reference"] ?: -1
        val catIdx = mapping["categories"] ?: -1

        if (qIdx < 0) return CompileResult(emptyList(), ImportFormat.CSV_TSV, "", "No question column assigned")

        val questions = mutableListOf<ParsedQuestion>()
        for (i in (headerIdx + 1) until rows.size) {
            val cols = rows[i]
            if (cols.size <= qIdx) continue
            val text = cols.getOrElse(qIdx) { "" }.trim()
            if (text.isBlank()) continue

            val options = if (optIdx >= 0 && optIdx < cols.size) {
                cols[optIdx].split("|", ";").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()

            val correctAnswers = if (ansIdx >= 0 && ansIdx < cols.size) {
                val answer = cols[ansIdx].trim()
                if (options.isNotEmpty()) {
                    options.mapIndexedNotNull { idx, opt ->
                        if (opt.equals(answer, ignoreCase = true) || answer.contains(('A' + idx).toString())) idx else null
                    }
                } else answer.split(",", ";", "|").mapNotNull { it.trim().toIntOrNull()?.minus(1) }
            } else emptyList()

            val explanation = if (explIdx >= 0 && explIdx < cols.size) cols[explIdx].trim().takeIf { it.isNotEmpty() } else null
            val hint = if (hintIdx >= 0 && hintIdx < cols.size) cols[hintIdx].trim().takeIf { it.isNotEmpty() } else null
            val reference = if (refIdx >= 0 && refIdx < cols.size) cols[refIdx].trim().takeIf { it.isNotEmpty() } else null
            val categories = if (catIdx >= 0 && catIdx < cols.size) cols[catIdx].split(",", ";").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()

            val parsedOptions = options.mapIndexed { idx, opt -> ParsedOption(id = "opt_${'A' + idx}", text = opt) }
            questions.add(ParsedQuestion(
                stem = text, options = parsedOptions,
                correctAnswers = correctAnswers.map { "opt_${'A' + it}" },
                explanation = explanation, hint = hint, reference = reference, categories = categories,
                type = if (correctAnswers.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
                sourceLine = i + 1
            ))
        }
        return CompileResult(questions, ImportFormat.CSV_TSV, "")
    }


    private fun compileXlsx(filePath: String): CompileResult {
        return try {
            val compiler = XlsxLibraryCompiler()
            val bundle = compiler.compile(filePath)
            val questions = bundle.quizzes.flatMap { quiz ->
                (quiz.questions ?: emptyList()).map { q ->
                    ParsedQuestion(
                        stem = q.stem,
                        externalId = q.id,
                        options = q.options?.mapIndexed { i, o -> ParsedOption(id = "opt_${'A' + i}", text = o.text) } ?: emptyList(),
                        correctAnswers = q.correct ?: emptyList(),
                        explanation = q.explanation,
                        hint = q.hint,
                        reference = q.reference,
                        imageDataUrl = q.imageDataUrl,
                        categories = q.categories ?: emptyList(),
                        type = if ((q.answerMode ?: "single") == "multiple") QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
                        sourceLine = q.sourceLine ?: 0
                    )
                }
            }
            val fileName = File(filePath).name
            CompileResult(questions, ImportFormat.XLSX, fileName)
        } catch (e: Exception) {
            CompileResult(emptyList(), ImportFormat.XLSX, File(filePath).name, e.message ?: "XLSX parse error")
        }
    }

    private fun compileText(file: File): CompileResult {
        val content = file.readText()
        val questions = textParser.parse(content) ?: emptyList()
        return CompileResult(questions, ImportFormat.TEXT, file.name)
    }

    private fun compileJson(file: File): CompileResult {
        return try {
            val bundle = jsonLibraryParser.parse(file.inputStream())
            val questions = bundle.quizzes.flatMap { quiz ->
                (quiz.questions ?: emptyList()).map { q ->
                    ParsedQuestion(
                        stem = q.stem, externalId = q.id,
                        options = q.options?.mapIndexed { i, o -> ParsedOption(id = "opt_${'A' + i}", text = o.text) } ?: emptyList(),
                        correctAnswers = q.correct ?: emptyList(),
                        explanation = q.explanation, hint = q.hint, reference = q.reference,
                        imageDataUrl = q.imageDataUrl, categories = q.categories ?: emptyList(),
                        type = if ((q.answerMode ?: "single") == "multiple") QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE
                    )
                }
            }
            CompileResult(questions, ImportFormat.JSON, file.name)
        } catch (e: Exception) { CompileResult(emptyList(), ImportFormat.JSON, file.name, e.message) }
    }

    private fun compileHtml(file: File): CompileResult {
        return try {
            val questions = htmlParser.parse(file.readText())
            CompileResult(questions, ImportFormat.HTML, file.name)
        } catch (e: Exception) { CompileResult(emptyList(), ImportFormat.HTML, file.name, e.message) }
    }

    private fun compileZip(file: File): CompileResult {
        return try {
            val result = file.inputStream().use { zipParser.parse(it) }
            val questions = result.bundle.quizzes.flatMap { quiz ->
                (quiz.questions ?: emptyList()).map { q ->
                    ParsedQuestion(
                        stem = q.stem, externalId = q.id,
                        options = q.options?.mapIndexed { i, o -> ParsedOption(id = "opt_${'A' + i}", text = o.text) } ?: emptyList(),
                        correctAnswers = q.correct ?: emptyList(),
                        explanation = q.explanation, hint = q.hint, reference = q.reference,
                        imageDataUrl = q.imageDataUrl, categories = q.categories ?: emptyList(),
                        type = if ((q.answerMode ?: "single") == "multiple") QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE
                    )
                }
            }
            CompileResult(questions, ImportFormat.ZIP, file.name)
        } catch (e: Exception) { CompileResult(emptyList(), ImportFormat.ZIP, file.name, e.message) }
    }
}
