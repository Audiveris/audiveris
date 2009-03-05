//----------------------------------------------------------------------------//
//                                                                            //
//                                L o g g e r                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.log;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.constant.UnitManager;

import omr.step.LogStepMonitorHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Class <code>Logger</code> is a specific subclass of standard java Logger,
 * augmented to fit the needs of Audiveris project.
 *
 * <p>The levels in descending order are: <ol>
 * <li>SEVERE (highest value)</li>
 * <li>WARNING</li>
 * <li>INFO</li>
 * <li>CONFIG</li>
 * <li>FINE</li>
 * <li>FINER</li>
 * <li>FINEST (lowest value)</li>
 *</ol>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Logger
    extends java.util.logging.Logger
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Cache this log manager */
    private static LogManager manager;

    /** Temporary mail box for logged messages */
    private static ArrayBlockingQueue<LogRecord> logMbx;

    /** Size of the mail box (cannot use a Constant) */
    private static final int LOG_MBX_SIZE = 1000;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Logger // Not meant to be instantiated from outside
    //--------//
    private Logger (String name)
    {
        super(name, null);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Report (and possibly create) the logger related to the provided class
     *
     * @param cl the related class
     * @return the logger
     */
    public static Logger getLogger (Class cl)
    {
        return getLogger(cl.getName());
    }

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Report (and possibly create) the logger related to the provided name
     * (usually the full class name)
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
            UnitManager.getInstance()
                       .addLogger(result);
        }

        return result;
    }

    //------------//
    // getMailbox //
    //------------//
    /**
     * Report the mailbox used as a buffer for log messages before they get
     * displayed by a GUI
     * @return the GUI mailbox
     */
    public static synchronized BlockingQueue<LogRecord> getMailbox ()
    {
        if (logMbx == null) {
            logMbx = new ArrayBlockingQueue<LogRecord>(LOG_MBX_SIZE);
        }

        return logMbx;
    }

    //-------------------//
    // getEffectiveLevel //
    //-------------------//
    /**
     * Report the resulting level for the logger, which may be inherited from
     * parents higher in the hierarchy
     *
     * @return The effective logging level for this logger
     */
    public Level getEffectiveLevel ()
    {
        java.util.logging.Logger logger = this;
        Level                    level = getLevel();

        while (level == null) {
            logger = logger.getParent();

            if (logger == null) {
                return null;
            }

            level = logger.getLevel();
        }

        return level;
    }

    //---------------//
    // isFineEnabled //
    //---------------//
    /**
     * Check if a Debug (Fine) would actually be logged
     *
     * @return true if to be logged
     */
    public boolean isFineEnabled ()
    {
        return isLoggable(Level.FINE);
    }

    //----------//
    // setLevel //
    //----------//
    /**
     * Set the logger level, using a level name
     *
     * @param levelStr the name of the level (case is irrelevant), such as Fine
     *                 or INFO
     */
    public void setLevel (String levelStr)
    {
        setLevel(Level.parse(levelStr.toUpperCase()));
    }

    //-----//
    // log //
    //-----//
    /**
     * Overridden version, just to insert the name of the calling thread
     * @param level A message level identifier
     * @param msg The string message
     */
    @Override
    public void log (Level  level,
                     String msg)
    {
        StringBuilder sb = new StringBuilder();

        //        if (constants.printThreadName.getValue()) {
        //            sb.append(Thread.currentThread().getName())
        //              .append(": ");
        //        }
        sb.append(msg);

        super.log(level, sb.toString());
    }

    //-----------//
    // logAssert //
    //-----------//
    /**
     * Assert the provided condition, and stop the application if the condition
     * is false, since this is supposed to detect a programming error
     *
     * @param exp the expression to check
     * @param msg the related error message
     */
    public void logAssert (boolean exp,
                           String  msg)
    {
        if (!exp) {
            severe(msg);
        }
    }

    //--------//
    // severe //
    //--------//
    /**
     * Log the provided message and stop
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
    /**
     * Log the provided message and exception, then stop the application
     *
     * @param msg the (severe) message
     * @param thrown the exception
     */
    public void severe (String    msg,
                        Throwable thrown)
    {
        super.severe(msg);
        thrown.printStackTrace();
    }

    //---------//
    // warning //
    //---------//
    /**
     * Log a warning with a related exception, then continue
     *
     * @param msg the (warning) message
     * @param thrown the related exception, whose stack trace will be printed
     *               only if the constant flag 'printStackTraces' is set.
     */
    public void warning (String    msg,
                         Throwable thrown)
    {
        super.warning(msg + " [" + thrown + "]");

        if (constants.printStackOnWarning.getValue()) {
            thrown.printStackTrace();
        }
    }

    //---------------------//
    // setGlobalParameters //
    //---------------------//
    /**
     * This method defines configuration parameters in a programmatic way, so
     * that only specific loggers if any need to be set in a configuration file
     * This file would simply contain lines like:
     *  # omr.step.StepMonitor.level=FINEST
     */
    private static void setGlobalParameters ()
    {
        // Retrieve the logger at the top of the hierarchy
        java.util.logging.Logger topLogger;
        topLogger = java.util.logging.Logger.getLogger("");

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
        consoleHandler.setLevel(java.util.logging.Level.FINE);
        consoleHandler.setFilter(new LogEmptyMessageFilter());

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
            false,
            "Should we print out the stack of any warning logged with exception?");
        final Constant.Boolean printThreadName = new Constant.Boolean(
            false,
            "Should we print out the name of the originating thread?");
        final Constant.Integer msgQueueSize = new Constant.Integer(
            "Messages",
            10000,
            "Size of message queue");
    }
}
