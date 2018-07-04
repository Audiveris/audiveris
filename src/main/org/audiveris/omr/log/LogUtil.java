//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          L o g U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;

import org.apache.commons.io.FileUtils;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.UriUtil;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    /** Initial messages before logging is fully set. */
    private static final List<String> initialMessages = new ArrayList<String>();

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // addAppender //
    //-------------//
    /**
     * Start a specific file logging, typically for the processing of a given book.
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

    //-----------------//
    // addFileAppender //
    //-----------------//
    /**
     * Add a specific appender meant for FILE.
     */
    public static void addFileAppender ()
    {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                Logger.ROOT_LOGGER_NAME);
        FileAppender fileAppender = new FileAppender();
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileAppender.setName("FILE");
        fileAppender.setContext(loggerContext);
        fileAppender.setAppend(false);

        String now = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
        Path logPath = WellKnowns.LOG_FOLDER.resolve(now + ".log").toAbsolutePath();
        fileAppender.setFile(logPath.toString());
        fileEncoder.setContext(loggerContext);
        fileEncoder.setPattern(
                "%date %-5level [%X{BOOK}%X{SHEET}] %25replace(%file){'\\.java$',''} %-4line | %msg%n%ex");
        fileEncoder.start();
        fileAppender.setEncoder(fileEncoder);
        fileAppender.start();
        root.addAppender(fileAppender);

        initMessage("LogUtil. Logging to " + logPath);
    }

    //----------------//
    // addGuiAppender //
    //----------------//
    /**
     * Add a specific appender meant for GUI log pane.
     * To be called only when running with a GUI.
     */
    public static void addGuiAppender ()
    {
        // GUI (filtered in LogGuiAppender)
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                Logger.ROOT_LOGGER_NAME);
        Appender guiAppender = new LogGuiAppender();
        guiAppender.setName("GUI");
        guiAppender.setContext(loggerContext);
        guiAppender.start();
        root.addAppender(guiAppender);
    }

    //--------------------//
    // allInitialMessages //
    //--------------------//
    /**
     * Report messages recorded before logging was fully set.
     *
     * @return initial messages concatenated into one string
     */
    public static String allInitialMessages ()
    {
        StringBuilder sb = new StringBuilder();

        for (String str : initialMessages) {
            sb.append(str).append("\n");
        }

        return sb.toString();
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
     * @param RES_URI       uri to 'res' folder (within .jar or development hierarchy)
     */
    public static void initialize (Path CONFIG_FOLDER,
                                   URI RES_URI)
    {
        // 1/ Check if system property is set and points to a real file
        final String loggingProp = System.getProperty(LOGBACK_LOGGING_KEY);

        if (loggingProp != null) {
            Path configPath = Paths.get(loggingProp).toAbsolutePath();

            if (Files.exists(configPath)) {
                // Everything seems OK, let LogBack use the config file
                initMessage("LogUtil. Configuration found " + configPath);

                return;
            } else {
                initMessage("LogUtil. File " + configPath + " does not exist.");
            }
        } else {
            initMessage("LogUtil. Property " + LOGBACK_LOGGING_KEY + " not defined, skipped.");
        }

        // 2/ Look for well-known location (user Audiveris config folder)
        Path configPath = CONFIG_FOLDER.resolve(LOGBACK_FILE_NAME).toAbsolutePath();

        if (Files.exists(configPath)) {
            initMessage("LogUtil. Configuration found " + configPath);
            System.setProperty(LOGBACK_LOGGING_KEY, configPath.toString());

            return;
        } else {
            initMessage("LogUtil. No " + configPath + ", skipped.");
        }

        // 3/ Look for suitable file within 'res' folder or resource
        try {
            final URI configUri = UriUtil.toURI(RES_URI, LOGBACK_FILE_NAME);
            final Path localPath;

            if (configUri.toString().startsWith("jar:")) {
                // Make a temporary copy off .jar archive
                File tmpFile = File.createTempFile("logback-", ".xml");
                tmpFile.deleteOnExit();

                InputStream is = configUri.toURL().openStream();
                FileUtils.copyInputStreamToFile(is, tmpFile);
                is.close();
                localPath = tmpFile.toPath();
            } else {
                localPath = Paths.get(configUri);
            }

            if (Files.exists(localPath)) {
                initMessage("LogUtil. Configuration found " + configUri);
                System.setProperty(LOGBACK_LOGGING_KEY, localPath.toString());

                return;
            } else {
                initMessage("LogUtil. No " + localPath + ", skipped.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 4/ We need a default configuration
        initMessage("LogUtil. Building a minimal Logging configuration");

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

        // Levels
        root.setLevel(Level.INFO);

        // OPTIONAL: print logback internal status messages
        StatusPrinter.print(loggerContext);
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
            String str = book.getAlias();

            if (str == null) {
                str = book.getRadix();
            }

            MDC.put(BOOK, str);
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

    //-------------//
    // initMessage //
    //-------------//
    /**
     * Record a message before logging is fully set.
     *
     * @param str the message to record
     */
    private static void initMessage (String str)
    {
        initialMessages.add(str);
    }
}
