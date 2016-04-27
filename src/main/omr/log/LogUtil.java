//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          L o g U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import omr.sheet.Book;
import omr.sheet.SheetStub;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.SwingUtilities;

/**
 * Class {@code LogUtil} handles logging features based on underlying LogBack binding.
 *
 * @author Hervé Bitteur
 */
public abstract class LogUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** MDC key for book context. */
    public static final String BOOK = "BOOK";

    /** MDC key for sheet/stub context. */
    public static final String SHEET = "SHEET";

    /** System property for LogBack configuration. */
    private static final String LOGBACK_LOGGING_KEY = "logback.configurationFile";

    /** File name for LogBack configuration. */
    private static final String LOGBACK_FILE_NAME = "logback.xml";

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // addAppender //
    //-------------//
    /**
     * Start a specific file logging, typically for the processing of a given project.
     *
     * @param name      appender name (typically the book radix)
     * @param logFolder target folder where the log file is to be written
     */
    public static void addAppender (String name,
                                    Path logFolder)
    {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                Logger.ROOT_LOGGER_NAME);
        FileAppender fileAppender = new FileAppender();
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileAppender.setName(name);
        fileAppender.setContext(loggerContext);
        fileAppender.setAppend(false);

        String now = new SimpleDateFormat("yyyyMMdd'T'HHmm").format(new Date());
        Path logFile = logFolder.resolve(name + "-" + now + ".log");
        fileAppender.setFile(logFile.toAbsolutePath().toString());
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%date %level [%X{BOOK}%X{SHEET}] %25file:%-4line | %msg%n%ex");
        fileEncoder.start();
        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();
        root.addAppender(fileAppender);
    }

    //------------//
    // initialize //
    //------------//
    /**
     * Check for (BackLog) logging configuration, and if not found,
     * define a minimal configuration.
     * This method should be called at the very beginning of the program before
     * any logging request is sent.
     *
     * @param CONFIG_FOLDER Config folder which may contain a logback.xml file
     */
    public static void initialize (Path CONFIG_FOLDER)
    {
        // 1/ Check if system property is set and points to a real file
        final String loggingProp = System.getProperty(LOGBACK_LOGGING_KEY);

        if (loggingProp != null) {
            Path configFile = Paths.get(loggingProp);

            if (Files.exists(configFile)) {
                // Everything seems OK, let LogBack use the config file
                System.out.println("Using " + configFile.toAbsolutePath());

                return;
            } else {
                System.out.println("File " + configFile.toAbsolutePath() + " does not exist.");
            }
        } else {
            System.out.println("Property " + LOGBACK_LOGGING_KEY + " not defined.");
        }

        // 2/ Look for well-known location
        Path configFile = CONFIG_FOLDER.resolve(LOGBACK_FILE_NAME);

        if (Files.exists(configFile)) {
            System.out.println("Using " + configFile.toAbsolutePath());

            // Set property for logback
            System.setProperty(LOGBACK_LOGGING_KEY, configFile.toString());

            return;
        } else {
            System.out.println("Could not find " + configFile);
        }

        // 3/ We need a default configuration
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                Logger.ROOT_LOGGER_NAME);

        // CONSOLE
        ConsoleAppender consoleAppender = new ConsoleAppender();
        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleAppender.setName("CONSOLE");
        consoleAppender.setContext(loggerContext);
        consoleEncoder.setContext(loggerContext);
        consoleEncoder.setPattern("%-5level %caller{1} [%X{BOOK}%X{SHEET}] %msg%n%ex");
        consoleEncoder.start();
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();
        root.addAppender(consoleAppender);

        // FILE (located in default temp directory)
        Path logFile;
        FileAppender fileAppender = new FileAppender();
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileAppender.setName("FILE");
        fileAppender.setContext(loggerContext);
        fileAppender.setAppend(false);

        String now = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
        logFile = Paths.get(System.getProperty("user.home"), "audiveris-" + now + ".log");
        fileAppender.setFile(logFile.toAbsolutePath().toString());
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%date %level \\(%file:%line\\) [%X{BOOK}%X{SHEET}] %msg%ex%n");
        fileEncoder.start();
        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();
        root.addAppender(fileAppender);

        // GUI (filtered in LogGuiAppender)
        Appender guiAppender = new LogGuiAppender();
        guiAppender.setName("GUI");
        guiAppender.setContext(loggerContext);
        guiAppender.start();
        root.addAppender(guiAppender);

        // Levels
        root.setLevel(Level.INFO);

        // OPTIONAL: print logback internal status messages
        StatusPrinter.print(loggerContext);

        root.info("Logging to file {}", logFile.toAbsolutePath());
    }

    //----------------//
    // removeAppender //
    //----------------//
    /**
     * Terminate the specific file logging.
     *
     * @param name appender name (typically the book radix)
     */
    public static void removeAppender (String name)
    {
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                Logger.ROOT_LOGGER_NAME);
        root.detachAppender(name);
    }

    //-------//
    // start //
    //-------//
    /**
     * In the calling thread, start log annotation with stub ID.
     *
     * @param stub the sheet/stub related to processing
     */
    public static void start (SheetStub stub)
    {
        start(stub.getBook());

        if (!SwingUtilities.isEventDispatchThread()) {
            MDC.put(SHEET, stub.getNum());

            ///System.out.println("startSheet '" + stub.getNum() + "' on " + Thread.currentThread());
        }
    }

    //-------//
    // start //
    //-------//
    /**
     * In the calling thread, start log annotation with book ID.
     *
     * @param book the book related to processing
     */
    public static void start (Book book)
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            MDC.put(BOOK, book.getRadix());

            ///System.out.println("startBook '" + book.getRadix() + "' on " + Thread.currentThread());
        }
    }

    //----------//
    // stopBook //
    //----------//
    /**
     * In the calling thread, stop book log annotation.
     */
    public static void stopBook ()
    {
        stopStub();

        if (!SwingUtilities.isEventDispatchThread()) {
            MDC.remove(BOOK);

            ///System.out.println("stopBook on " + Thread.currentThread());
        }
    }

    //----------//
    // stopStub //
    //----------//
    /**
     * In the calling thread, stop sheet stub log annotation.
     */
    public static void stopStub ()
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            MDC.remove(SHEET);

            ///System.out.println("stopSheet on " + Thread.currentThread());
        }
    }

    //---------//
    // toLevel //
    //---------//
    public static Level toLevel (final String str)
    {
        switch (str.toUpperCase()) {
        case "ALL":
            return Level.ALL;

        case "TRACE":
            return Level.TRACE;

        case "DEBUG":
            return Level.DEBUG;

        case "INFO":
            return Level.INFO;

        case "WARN":
            return Level.WARN;

        case "ERROR":
            return Level.ERROR;

        default:
        case "OFF":
            return Level.OFF;
        }
    }
}
