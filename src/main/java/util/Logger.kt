package util

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.PrintStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale


/**
 * Logger class, which handles both normal logs, as well as error messages.
 * Both types of logs can be written to the terminal or to a file.
 */
object Logger {

    private val DATE_FORMAT = SimpleDateFormat("hh;mm;ss_dd-MM-yyyy", Locale.ENGLISH)
    private val TIME_FORMAT = SimpleDateFormat("hh:mm:ss", Locale.ENGLISH)

    private var errOutStream = System.err
    private var fileOutStream = System.out
    private var loggerOutStream = System.out

    private val errors = ArrayList<String>()

    private var printTag = true
    private var printColored = true
    private var printTimeStamp = true
    private var writeErrorToFile: Boolean = false

    /**
     * Sets whether of not the error messages should be written to a file.
     * @param writeErrorToFile whether or not the error messages should be written to a file.
     */
    fun setWriteErrorToFile(writeErrorToFile: Boolean) {
        Logger.writeErrorToFile = writeErrorToFile
    }

    /**
     * Setter for the printColored variable.
     * @param printColored determines whether or not the Logger should print colored text or not.
     */
    fun setPrintColored(printColored: Boolean) {
        Logger.printColored = printColored
    }

    /**
     * Setter for the printTimeStamp variable.
     * @param printTimeStamp determines whether or not the Logger should print a timestamp in front of each line.
     */
    fun setPrintTimeStamp(printTimeStamp: Boolean) {
        Logger.printTimeStamp = printTimeStamp
    }

    /**
     * Setter for the printTag variable.
     * @param printTag determines whether or not the Logger should print the tag that corresponds to the given severity.
     */
    fun setPrintTag(printTag: Boolean) {
        Logger.printTag = printTag
    }

    /**
     * Set the print stream of the Logger.
     * @param printStream the print stream for the Logger.
     */
    fun setOut(printStream: PrintStream) {
        loggerOutStream = printStream
    }

    /**
     * Set the print stream for the specified type.
     * @param printStream the print stream.
     * @param type the type of the print stream.
     */
    fun setOut(printStream: PrintStream, type: PrintStreamType) {
        if (type === PrintStreamType.FILE) {
            fileOutStream = printStream
        } else {
            loggerOutStream = printStream
        }
    }

    /**
     * Set the print stream of the Logger to write to a file, specified by the given file name.
     * @param fileName the file name to which the Logger should write.
     * @param overWrite whether or not the Logger can overwrite, if the given file already exists.
     * @param type the type of the print stream that needs to be set.
     */
    fun setOut(fileName: String?, overWrite: Boolean, type: PrintStreamType) {
        if (fileName == null || fileName.isEmpty()) {
            return
        }
        val file = File(fileName)

        if (!file.exists() || overWrite) {
            try {
                if (!file.exists() && !file.createNewFile()) {
                    err("Could not create output file: $fileName")
                    return
                }

                if (type === PrintStreamType.FILE) {
                    fileOutStream = PrintStream(file)
                } else {
                    loggerOutStream = PrintStream(file)
                }
            } catch (e: IOException) {
                err("Could not create output file: $fileName")
            }

        } else {
            err("Could not write output to file: " + fileName + " because it already exists.\n"
                    + "The output is written to the console instead.\n"
                    + "Use -x to allow for overwriting of existing files.")
        }
    }

    /**
     * Standard log method, which simply prints a message to the terminal.
     * @param message the message to be written to the terminal.
     */
    fun log(message: String?) {
        if (message == null) {
            err("Cannot print null string!")
            return
        }

        if (loggerOutStream === System.out && fileOutStream === System.out) {
            fileOutStream.print(Color.DEFAULT.code)
        }

        if (printTimeStamp) {
            fileOutStream.print(createTimeStamp())
        }

        if (printTag) {
            fileOutStream.print(createTag(Severity.INFO))
        }

        fileOutStream.print(message + "\n")
    }

    /**
     * Log a message with certain severity.
     * @param message the message to be logged.
     * @param severity the severity of the message.
     */
    private fun log(message: String, severity: Severity) {
        if (printColored) {
            loggerOutStream.print(severity.color.code)
        }

        if (printTimeStamp) {
            loggerOutStream.print(createTimeStamp())
        }

        if (printTag) {
            loggerOutStream.print(createTag(severity))
        }

        loggerOutStream.print(message + "\n")
    }

    /**
     * Helper method that creates a tag for the given severity.
     * @param severity the severity for which a tag must be created.
     * @return the string representation of the given severity.
     */
    private fun createTag(severity: Severity): String {
        return "[" + severity.type + "] "
    }

    /**
     * Helper method that creates a timestamp.
     * @return the string representation of the timestamp.
     */
    private fun createTimeStamp(): String {
        val date = Date()

        synchronized(TIME_FORMAT) {
            val timeStamp = TIME_FORMAT.format(date)
            return "[$timeStamp] "
        }
    }

    /**
     * Overload log method. Automatically prints with INFO severity.
     * @param message the message to be logged.
     */
    fun info(message: String) {
        log(message, Severity.INFO)
    }

    /**
     * Overload log method. Automatically prints with WARNING severity.
     * @param message the message to be logged.
     */
    fun warn(message: String) {
        log(message, Severity.WARNING)
    }

    /**
     * Overload log method. Automatically prints with DEBUG severity.
     * @param message the message to be logged.
     */
    fun debug(message: String) {
        log(message, Severity.DEBUG)
    }

    /**
     * Prints an error message to either the terminal, or to a file, depending on the writeErrorToFile parameter.
     * If the message should be written to a file, create a crash report file, with the title of the file being
     * the current date and time.
     * @param message the error message to be printed to either the terminal, or to a file.
     */
    fun err(message: String?) {
        if (message == null) {
            err("Cannot print a null string!")
            return
        }

        errors.add(message)
    }

    /**
     * Sets the PrintStream for the errorOutStream.
     * @param outStream the new PrintStream.
     */
    fun setErr(outStream: PrintStream) {
        errOutStream = outStream
    }

    /**
     * Prints the errors to the console or crash report file.
     */
    fun printErrors() {
        if (!errors.isEmpty()) {
            val output = StringBuilder()

            if (printColored) {
                output.append(Color.RED.code)
            }

            if (printTimeStamp) {
                output.append(createTimeStamp())
            }

            if (printTag) {
                output.append("[ERROR] ")
            }

            val errorMessage = StringBuilder()

            for (error in errors) {
                errorMessage.append(error).append("\n\n\n")
            }

            output.append(errorMessage.toString().trim { it <= ' ' })

            if (writeErrorToFile) {
                val date = Date()
                synchronized(DATE_FORMAT) {
                    val fileName = "crash_report_" + DATE_FORMAT.format(date) + ".txt"
                    setErrorStream(fileName)
                }
            }

            errOutStream.print(output.toString() + "\n")
            errors.clear()
        }
    }

    /**
     * Sets the errOutStream to the file at the given fileName.
     * @param fileName name of the error file.
     */
    internal fun setErrorStream(fileName: String) {
        try {
            if (writeErrorToFile) {
                errOutStream.print(Color.RED.code
                        + "An error occurred! Check the file to see what went wrong: " + fileName + '\n'.toString())
            }
            errOutStream = PrintStream(File(fileName))
        } catch (e: FileNotFoundException) {
            err("Could not create error file: " + fileName + ". " + e.message)
        }

    }

    /**
     * Flushes the output stream.
     */
    fun flush() {
        fileOutStream.flush()
        loggerOutStream.flush()
        errOutStream.flush()
    }

    /**
     * First flushes the output stream, then closes it.
     */
    fun close() {
        fileOutStream.flush()
        if (fileOutStream !== System.out) {
            fileOutStream.close()
        }

        loggerOutStream.flush()
        if (loggerOutStream !== System.out) {
            loggerOutStream.close()
        }

        errors.clear()
        errOutStream.flush()
        errOutStream.close()
    }

}
/**
 * Private constructor, since the Logger should never be instantiated.
 */
