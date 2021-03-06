package com.hadihariri.markcode

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

val EMPTY_SOURCE_METADATA = SourceMetadata(null, null, emptyList(), false, null, false, false, false, emptyList())

fun captionToFilename(caption: String, captionIndex: Int): String {
    val name = StringBuilder()
    var capitalize = true
    caption.forEach {
        if (it == ' ') {
            capitalize = true
        } else if (it.isLetterOrDigit()) {
            name.append(if (capitalize) it.toUpperCase() else it)
            capitalize = false
        }
    }
    return name.toString() + (if (captionIndex == 0) "" else "$captionIndex")
}



val annotationRegex = Regex("<(\\d+)>")

fun String.startsWithAny(vararg prefixes: String) = prefixes.any { startsWith(it) }

fun extractChapterNumber(filename: String): Int {
    if (filename.startsWith("ch")) {
        return Integer.parseInt(filename.removePrefix("ch").substringBeforeLast("."))
    }
    return 0
}

fun writeVerifyAllSamples(chapters: List<Chapter>, outputDir: File) {
    BufferedWriter(FileWriter(File(outputDir, "VerifyAllSamples.kt"))).use { outputFile ->
        outputFile.write("import com.hadihariri.markcode.OutputVerifier\n")

        val examples = mutableListOf<ExampleOutput>()

        for (chapter in chapters) {
            for (example in chapter.examples.values.filter { !it.skip && it.language is KotlinLanguage && it.hasOutputToVerify }) {
                val fqName = example.packageName ?: continue
                val import = fqName.replace('.', '_')
                outputFile.write("import $fqName.main as $import\n")
                examples.add(ExampleOutput(
                        import,
                        "${chapter.chapterCodeDir.name}/${example.expectedOutputFileName}",
                        "${example.chapter.chapterFile.name}:${example.expectedOutputStartLine ?: example.mainExample?.expectedOutputStartLine}"))
            }
        }

        // TODO - make this actually read the file so we don't have to keep in sync
        // (distZip right now needs proper path for this so that's why hacked right now)
        outputFile.write("\n\nfun main(args: Array<String>) {\n")
        outputFile.write("    val verifier = OutputVerifier()\n")
        for ((function, expectedOutput, location) in examples) {
            outputFile.write("    verifier.verifySample(::$function, \"$outputDir/$expectedOutput\", \"$location\")\n")
        }
        outputFile.write("    verifier.report()\n}\n")
    }
}

