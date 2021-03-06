package io.github.ksmirenko.kotlin.visualizer

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import org.apache.commons.csv.CSVFormat
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) = mainBody {
    val argParser = ArgParser(args)
    val parsedArgs = argParser.parseInto(::CommandLineArgs)
    argParser.force()

    File(parsedArgs.outFolder).mkdirs()

    val strategy: RecordProcessingStrategy = when (parsedArgs.mode) {
        CommandLineArgs.Mode.Seek -> SeekingStrategy(parsedArgs.inFolder, parsedArgs.outFolder,
                parsedArgs.importantFeaturesPath)
        CommandLineArgs.Mode.BinaryMark -> BinaryMarkStrategy(parsedArgs.inFolder, parsedArgs.outFolder)
        CommandLineArgs.Mode.Copy -> CopyingStrategy(parsedArgs.inFolder, parsedArgs.outFolder)
        CommandLineArgs.Mode.CategoryCopy -> CategoryCopyingStrategy(parsedArgs.inFolder, parsedArgs.outFolder)
    }

    var counter = 0
    var successCounter = 0

    try {
        parsedArgs.inputCsvPaths.forEach {
            for (csvFile in File(it).walkTopDown()) {
                if (csvFile.isDirectory || csvFile.extension != "csv") {
                    continue
                }
                println(csvFile.path)

                val reader = FileReader(csvFile)
                val parser = CSVFormat.EXCEL.parse(reader)
                val records = if (parsedArgs.numRecordsPicked > 0) {
                    parser.records.shuffled().take(parsedArgs.numRecordsPicked)
                } else {
                    parser.records
                }

                for (record in records) {
                    val success = strategy.process(record)
                    if (success) {
                        successCounter++
                    }
                    counter++
                }
                reader.close()
            }
        }
        println("Done. Successfully processed $counter files.")
        strategy.printFooter()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        strategy.close()
    }
}

private class CommandLineArgs(parser: ArgParser) {
    val mode by parser.mapping(
            "--seek" to Mode.Seek,
            "--binary-mark" to Mode.BinaryMark,
            "--copy" to Mode.Copy,
            "--categ-copy" to Mode.CategoryCopy,
            help = """mode: 'seek' - just seek Kotlin functions in the repo and print into separate files
                |'binary-mark' - mark useless/useful
                |'copy' - just copy files with ID-based names from inFolder to outFolder
                |'categ-copy' - copy files from inFolder to outFolder/<categId>/<id>.kt (last CSV value is categId)
            """.trimMargin())

    val numRecordsPicked by parser.storing("--pick",
            help = "max number of records picked randomly for analysis") { toInt() }
            .default(-1)
    val inFolder by parser.storing("--in", "-i",
            help = "path to input folder or repo root (for 'seek' mode)", argName = "IN-FOLDER")
    val outFolder by parser.storing("--out", "-o", help = "path to output folder", argName = "OUT-FOLDER")

    val importantFeaturesPath by parser.storing("--important-only",
            help = "path to CSV with 'important' features to filter the anomaly report (for seek mode)",
            argName = "IMPORTANT-FEATURES-CSV")
            .default<String?>(null)

    val inputCsvPaths by parser.positionalList("CSV", sizeRange = 1..Int.MAX_VALUE,
            help = "CSV files or folders with anomaly reports")

    enum class Mode { Seek, Copy, CategoryCopy, BinaryMark }
}
