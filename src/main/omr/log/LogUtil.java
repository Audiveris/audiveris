//----------------------------------------------------------------------------//
//                                                                            //
//                                L o g U t i l                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class {@code LogUtil} handles logging features that depend on
 * underlying LogBack binding.
 *
 * @author Hervé Bitteur
 */
public class LogUtil
{
    //~ Static fields/initializers ---------------------------------------------

    /** System property for LogBack configuration. */
    private static final String LOGBACK_LOGGING_KEY = "logback.configurationFile";

    /** File name for LogBack configuration. */
    private static final String LOGBACK_FILE_NAME = "logback.xml";

    //~ Methods ----------------------------------------------------------------
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
     * @param TEMP_FOLDER   Temporary folder where log file should be written
     */
    public static void initialize (File CONFIG_FOLDER,
                                   File TEMP_FOLDER)
    {
        // 1/ Check if system property is set and points to a real file
        final String loggingProp = System.getProperty(LOGBACK_LOGGING_KEY);

        if (loggingProp != null) {
            File configFile = new File(loggingProp);

            if (configFile.exists()) {
                // Everything seems OK, let LogBack use the config file
                System.out.println("Using " + configFile.getAbsolutePath());

                return;
            } else {
                System.out.println(
                        "File " + configFile.getAbsolutePath()
                        + " does not exist.");
            }
        } else {
            System.out.println(
                    "Property " + LOGBACK_LOGGING_KEY + " not defined.");
        }

        // 2/ Look for well-known location
        File configFile = new File(CONFIG_FOLDER, LOGBACK_FILE_NAME);

        if (configFile.exists()) {
            System.out.println("Using " + configFile.getAbsolutePath());

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
        consoleEncoder.setPattern("%-5level %caller{1} - %msg%ex%n");
        consoleEncoder.start();
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.start();
        root.addAppender(consoleAppender);

        // FILE (located in default temp directory)
        File logFile;
        FileAppender fileAppender = new FileAppender();
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileAppender.setName("FILE");
        fileAppender.setContext(loggerContext);
        fileAppender.setAppend(false);

        String now = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(
                new Date());
        logFile = Paths.get(
                System.getProperty("java.io.tmpdir"),
                "audiveris-" + now + ".log")
                .toFile();
        fileAppender.setFile(logFile.getAbsolutePath());
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern("%date %level \\(%file:%line\\) - %msg%ex%n");
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

        // STEP
        Appender stepAppender = new LogStepAppender();
        stepAppender.setName("STEP");
        stepAppender.setContext(loggerContext);
        stepAppender.start();
        root.addAppender(stepAppender);

        // Levels
        root.setLevel(Level.INFO);

        // OPTIONAL: print logback internal status messages
        StatusPrinter.print(loggerContext);

        root.info("Logging to file {}", logFile.getAbsolutePath());
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
