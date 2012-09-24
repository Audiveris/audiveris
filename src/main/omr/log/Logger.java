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

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.constant.UnitManager;

import omr.step.LogStepMonitorHandler;

import omr.util.Param;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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

    /** Specific application parameters */
    private static final Constants constants = new Constants();
    
    /** Parameter that governs call-stack printing. */
    public static final Param<Boolean> defaultStack = new DefaultStack();

    /** Cache this log manager */
    private static LogManager manager;

    /** Temporary mail box for logged messages */
    private static ArrayBlockingQueue<FormattedRecord> logMbx;

    /** Size of the mail box (cannot use a Constant) */
    private static final int LOG_MBX_SIZE = 10000;

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
        if (manager == null) {
            manager = LogManager.getLogManager();
            setGlobalParameters();
        }

        Logger result = (Logger) manager.getLogger(name);

        if (result == null) {
            result = new Logger(name);
            manager.addLogger(result);
            result = (Logger) manager.getLogger(name);

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

    //-----------------------//
    // isPrintStackOnWarning //
    //-----------------------//
    public static boolean isPrintStackOnWarning ()
    {
        return constants.printStackOnWarning.getValue();
    }

    //------------------------//
    // setPrintStackOnWarning //
    //------------------------//
    public static void setPrintStackOnWarning (boolean val)
    {
        constants.printStackOnWarning.setValue(val);
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
     * Log the provided message and stop.
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
     * Log the provided message and exception and stop the application.
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

        if (constants.printStackOnWarning.isSet()) {
            thrown.printStackTrace();
        }
    }

    //---------//
    // warning //
    //---------//
    public void warning (String msg,
                         Object... params)
    {
        super.log(Level.WARNING, msg, params);
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
        // Retrieve the logger at the top of the hierarchy
        java.util.logging.Logger topLogger;
        topLogger = java.util.logging.Logger.getLogger("");

        /** Redirecting stdout and stderr to logging */
        String redirection = WellKnowns.STD_OUT_ERROR;

        if (redirection != null) {
            // initialize logging to go to rolling log file
            manager.reset();

            try {
                // log file max size 10K, 1 rolling file, append-on-open
                Handler fileHandler = new FileHandler(
                        redirection,
                        10_000,
                        1,
                        false);
                fileHandler.setFormatter(new LogBasicFormatter()); //(new SimpleFormatter());
                topLogger.addHandler(fileHandler);

                // Tell user we are redirecting log
                try {
                    String path = new File(redirection).getCanonicalPath();
                    topLogger.log(Level.INFO, "Log is redirected to {0}", path);
                    System.out.println("Log is redirected to " + path);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // now rebind stdout/stderr to logger
                Logger logger;
                LoggingOutputStream los;

                logger = Logger.getLogger("stdout");
                los = new LoggingOutputStream(logger, StdOutErrLevel.STDOUT);
                System.setOut(new PrintStream(los, true, WellKnowns.ENCODING));

                logger = Logger.getLogger("stderr");
                los = new LoggingOutputStream(logger, StdOutErrLevel.STDERR);
                System.setErr(new PrintStream(los, true, WellKnowns.ENCODING));
            } catch (IOException | SecurityException ex) {
                ex.printStackTrace();
            }
        } else {
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

            consoleHandler.setFormatter(new LogBasicFormatter()); // Comment out?
            consoleHandler.setLevel(java.util.logging.Level.FINEST);
            consoleHandler.setFilter(new LogEmptyMessageFilter());

            try {
                consoleHandler.setEncoding(WellKnowns.ENCODING);
            } catch (SecurityException | UnsupportedEncodingException ex) {
                System.err.
                        println(
                        "Cannot setEncoding to " + WellKnowns.ENCODING + " exception: " + ex);
            }
        }

        // Handler for GUI log pane
        topLogger.addHandler(new LogGuiHandler());

        // Handler for animation of progress in StepMonitor
        topLogger.addHandler(new LogStepMonitorHandler());

        // Default level
        topLogger.setLevel(java.util.logging.Level.INFO);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printStackOnWarning = new Constant.Boolean(
                true,
                "Should we print out the stack of any warning logged with exception?");

        //
        final Constant.Boolean printThreadName = new Constant.Boolean(
                false,
                "Should we print out the name of the originating thread?");

    }

    //--------------//
    // DefaultStack //
    //--------------//
    private static class DefaultStack
            extends Param<Boolean>
    {

        @Override
        public Boolean getSpecific ()
        {
            return constants.printStackOnWarning.getValue();
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getSpecific().equals(specific)) {
                constants.printStackOnWarning.setValue(specific);
                getAnonymousLogger().log(Level.INFO,
                        "A call stack will {0} be printed on exception",
                        specific ? "now" : "no longer");
                return true;
            } else {
                return false;
            }
        }
    }
}
