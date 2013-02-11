//----------------------------------------------------------------------------//
//                                                                            //
//                                L o g g e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import omr.WellKnowns;

import omr.constant.UnitManager;

import omr.step.LogStepMonitorHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.GregorianCalendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Class {@code Logger} is a specific subclass of standard java Logger,
 * augmented to fit the needs of Audiveris project.
 *
 * <p>The levels in descending order are:
 * <ol>
 * <li>SEVERE (highest value)</li>
 * <li>WARNING</li>
 * <li>INFO</li>
 * <li>CONFIG</li>
 * <li>FINE</li>
 * <li>FINER</li>
 * <li>FINEST (lowest value)</li>
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class Logger
        extends java.util.logging.Logger
{
    //~ Static fields/initializers ---------------------------------------------

    /** Flag set once configuration has been made. */
    private static boolean configured = false;

    /** Temporary mail box for logged messages. */
    private static ArrayBlockingQueue<FormattedRecord> logMbx;

    /**
     * Size of the mail box.
     * (This cannot be an application Constant, for elaboration dependencies)
     */
    private static final int LOG_MBX_SIZE = 10_000;

    /** Name of default log file. */
    private static final String LOG_FILE_NAME = "audiveris.log";

    /** Logger at the top of the loggers hierarchy. */
    private static final java.util.logging.Logger topLogger = java.util.logging.Logger.getLogger("");

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Logger // Not meant to be instantiated from outside
    //--------//
    private Logger (String name)
    {
        super(name, null);
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // fine //
    //------//
    public void fine (String msg,
                      Object... params)
    {
        super.log(Level.FINE, msg, params);
    }

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Report (and possibly create) the logger related to the provided
     * class.
     *
     * @param cl the related class
     * @return the logger
     */
    public static Logger getLogger (Class<?> cl)
    {
        return getLogger(cl.getName());
    }

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Report (and possibly create) the logger related to the provided
     * name (usually the full class name).
     *
     * @param name the logger name
     * @return the logger found or created
     */
    public static synchronized Logger getLogger (String name)
    {
        // Lazy initialization if needed
        if (!configured) {
            configured = true;
            setGlobalParameters();
        }

        // Reuse existing logger, if any with this name
        Logger result = (Logger) LogManager.getLogManager().getLogger(name);

        if (result == null) {
            result = new Logger(name);
            LogManager.getLogManager().addLogger(result);
            result = (Logger) LogManager.getLogManager().getLogger(name);

            // Insert in the hierarchy of units with logger
            UnitManager.getInstance().addLogger(result);
        }

        return result;
    }

    //------------//
    // getMailbox //
    //------------//
    /**
     * Report the mailbox used as a buffer for log messages before they
     * get displayed by a GUI.
     *
     * @return the GUI mailbox
     */
    public static synchronized BlockingQueue<FormattedRecord> getMailbox ()
    {
        if (logMbx == null) {
            logMbx = new ArrayBlockingQueue<>(LOG_MBX_SIZE);
        }

        return logMbx;
    }

    //-------------------//
    // getEffectiveLevel //
    //-------------------//
    /**
     * Report the resulting level for the logger, which may be
     * inherited from parents higher in the hierarchy.
     *
     * @return The effective logging level for this logger
     */
    public Level getEffectiveLevel ()
    {
        java.util.logging.Logger logger = this;
        Level level = getLevel();

        while (level == null) {
            logger = logger.getParent();

            if (logger == null) {
                return null;
            }

            level = logger.getLevel();
        }

        return level;
    }

    //------//
    // info //
    //------//
    /**
     * Log a message, with an array of object arguments.
     * <p>
     * If the logger is currently enabled for the given message
     * level then a corresponding LogRecord is created and forwarded
     * to all the registered output Handler objects.
     * <p>
     *
     * @param msg    The string message (or a key in the message catalog)
     * @param params array of parameters to the message
     */
    public void info (String msg,
                      Object... params)
    {
        super.log(Level.INFO, msg, params);
    }

    //---------------//
    // isFineEnabled //
    //---------------//
    /**
     * Check if a Debug (Fine) would actually be logged.
     *
     * @return true if to be logged
     */
    public boolean isFineEnabled ()
    {
        return isLoggable(Level.FINE);
    }

    //-----------//
    // logAssert //
    //-----------//
    /**
     * Assert the provided condition, and stop the application if the
     * condition is false, since this is supposed to detect a
     * programming error.
     *
     * @param exp the expression to check
     * @param msg the related error message
     */
    public void logAssert (boolean exp,
                           String msg)
    {
        if (!exp) {
            severe(msg);
        }
    }

    //----------//
    // setLevel //
    //----------//
    /**
     * Set the logger level, using a level name.
     *
     * @param levelStr the name of the level (case is irrelevant), such as Fine
     *                 or INFO
     */
    public void setLevel (String levelStr)
    {
        setLevel(Level.parse(levelStr.toUpperCase()));
    }

    //    //-----//
    //    // log //
    //    //-----//
    //    /**
    //     * Overridden version, to insert the name of the calling thread.
    //     * @param level A message level identifier
    //     * @param msg   The string message
    //     */
    //    @Override
    //    public void log (Level  level,
    //                     String msg)
    //    {
    //        StringBuilder sb = new StringBuilder();
    //
    //        if (constants.printThreadName.isSet()) {
    //            sb.append(Thread.currentThread().getName())
    //              .append(": ");
    //        }
    //
    //        sb.append(msg);
    //
    //        super.log(level, sb.toString());
    //    }
    //--------//
    // severe //
    //--------//
    /**
     * Log the provided message (and stop?).
     *
     * @param msg the (severe) message
     */
    @Override
    public void severe (String msg)
    {
        super.severe(msg);
        new Throwable().printStackTrace();
    }

    //--------//
    // severe //
    //--------//
    public void severe (String msg,
                        Object... params)
    {
        super.log(Level.SEVERE, msg, params);
        new Throwable().printStackTrace();
    }

    //--------//
    // severe //
    //--------//
    /**
     * Log the provided message and exception (and stop the application?).
     *
     * @param msg    the (severe) message
     * @param thrown the exception
     */
    public void severe (String msg,
                        Throwable thrown)
    {
        super.log(Level.SEVERE, "{0} [{1}]", new Object[]{msg, thrown});
        thrown.printStackTrace();
    }

    //---------//
    // warning //
    //---------//
    /**
     * Log a warning with a related exception, then continue.
     *
     * @param msg    the (warning) message
     * @param thrown the related exception, whose stack trace will be printed
     *               only if the constant flag 'printStackTraces' is set.
     */
    public void warning (String msg,
                         Throwable thrown)
    {
        super.log(Level.WARNING, "{0} [{1}]", new Object[]{msg, thrown});
        thrown.printStackTrace();
    }

    //---------//
    // warning //
    //---------//
    public void warning (String msg,
                         Object... params)
    {
        super.log(Level.WARNING, msg, params);
    }

    //--------------//
    // storeLogging //
    //--------------//
    /**
     * Store logging data into a file, either the one defined by
     * "stdouterr" system property, or the default file otherwise.
     */
    private static void storeLogging ()
    {
        File file = null;

        // First consider a potential setting of system property stdouterr
        final String redirection = WellKnowns.STD_OUT_ERR;
        if (redirection != null) {
            file = new File(redirection);
            File dir = file.getParentFile();
            dir.mkdirs();
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("Could not get to directory " + dir);
                file = null;
            }
        }

        // Otherwise, use standard logging file
        if (file == null) {
            file = new File(WellKnowns.TEMP_FOLDER, LOG_FILE_NAME);
            WellKnowns.TEMP_FOLDER.mkdirs();
        }

        // initialize logging to go to rolling log file
        try {
            String path = file.getCanonicalPath();

            // Log file max size 10K, 1 rolling file, append-on-open
            Handler fileHandler = new FileHandler(
                    path,
                    10000,
                    1,
                    false);
            fileHandler.setFormatter(new LogBasicFormatter());
            //fileHandler.setFormatter(new SimpleFormatter());
            topLogger.addHandler(fileHandler);

            // Tell user we are redirecting log
            GregorianCalendar calendar = new GregorianCalendar();            
            String msg = String.format("Logging to %s on %2$tF %2$tT", path, calendar);
            ///topLogger.log(Level.INFO, msg);
            System.out.println(msg);
        } catch (IOException | SecurityException ex) {
            ex.printStackTrace();
        }
    }

    //-------------------//
    // addConsoleHandler //
    //-------------------//
    /**
     * If needed, create a ConsoleHandler and add it to the topLogger.
     */
    private static void addConsoleHandler ()
    {
        // Handler for console (reuse it if it already exists)
        Handler consoleHandler = null;

        for (Handler handler : topLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                consoleHandler = handler;

                break;
            }
        }

        if (consoleHandler == null) {
            consoleHandler = new ConsoleHandler();
            topLogger.addHandler(consoleHandler);
        }

        consoleHandler.setFormatter(new LogBasicFormatter());
        consoleHandler.setLevel(java.util.logging.Level.FINEST);
        consoleHandler.setFilter(new LogEmptyMessageFilter());

        try {
            consoleHandler.setEncoding(WellKnowns.FILE_ENCODING);
        } catch (SecurityException | UnsupportedEncodingException ex) {
            System.err.println(
                    "Cannot setEncoding to " + WellKnowns.FILE_ENCODING
                    + " exception: " + ex);
        }
    }

    //--------------//
    // rebindStream //
    //--------------//
    /**
     * Rebind the provided standard stream to a specific logger.
     *
     * @param level either StdOutErrLevel.STDOUT or StdOutErrLevel.STDERR
     */
    private static void rebindStream (Level level)
    {
        String name = level.getName();

        try {

            java.util.logging.Logger logger = LogManager.getLogManager().getLogger(name);

            if (logger == null) {
                logger = new Logger(name);
                LogManager.getLogManager().addLogger(logger);
                logger = (Logger) LogManager.getLogManager().getLogger(name);
            }

            LoggingStream los = new LoggingStream(logger, level);
            if (level == StdOutErrLevel.STDOUT) {
                System.setOut(new PrintStream(los, true, WellKnowns.FILE_ENCODING));
            } else if (level == StdOutErrLevel.STDERR) {
                System.setErr(new PrintStream(los, true, WellKnowns.FILE_ENCODING));
            }
        } catch (UnsupportedEncodingException ex) {
            System.out.println("Cannot setEncoding to " + WellKnowns.FILE_ENCODING);
        }
    }

    //---------------------//
    // setGlobalParameters //
    //---------------------//
    /**
     * Set configuration parameters in a programmatic way, so that only
     * specific loggers if any need to be set in a configuration file.
     * This file would simply contain lines like:
     * # omr.step.StepMonitor.level=FINEST
     */
    private static void setGlobalParameters ()
    {
        // Global reset
        LogManager.getLogManager().reset();

        // Connect a handler for console
        addConsoleHandler();

        // Connect a handler for GUI log pane
        topLogger.addHandler(new LogGuiHandler());

        // Connect a handler for animation in StepMonitor
        topLogger.addHandler(new LogStepMonitorHandler());

        // Set default level
        topLogger.setLevel(java.util.logging.Level.INFO);

        // Rebind standard output streams to dedicated loggers
        rebindStream(StdOutErrLevel.STDOUT);
        rebindStream(StdOutErrLevel.STDERR);

        // Store logging to file
        storeLogging();
    }
}
