//-----------------------------------------------------------------------//
//                                                                       //
//                              L o g g e r                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

import omr.constant.UnitManager;
import org.apache.log4j.spi.LoggerFactory;

/**
 * Class <code>Logger</code> is a specific subclass of log4j Logger,
 * augmented to fit the needs of Audiveris project
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Logger
        extends org.apache.log4j.Logger
{
    //~ Static variables/initializers -------------------------------------

    private static LoggerFactory factory = new OmrLoggerFactory();

    //~ Constructors ------------------------------------------------------

    //--------//
    // Logger //
    //--------//
    /**
     * Creates a new Logger object.
     *
     * @param name the name of the class
     */
    protected Logger (String name)
    {
        super(name);
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // getLogger //
    //-----------//
    /**
     * Report (and possibly create) the logger related to the provided
     * class
     *
     * @param cl the related class
     * @return the logger
     */
    public static Logger getLogger (Class cl)
    {
        //System.out.println("getLogger for " + cl.getName());
        Logger logger = (Logger) org.apache.log4j.Logger.getLogger
            (cl.getName(),
             factory);

        // Insert in the hierarchy of units with logger
        UnitManager.getInstance().addLogger(logger);

        return logger;
    }

    //-----------//
    // logAssert //
    //-----------//
    /**
     * Assert the provided condition, and stop the application if the
     * condition is false, since this is supposed to detect a programming
     * error
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

    //--------//
    // severe //
    //--------//
    /**
     * Log the provided message and stop
     *
     * @param msg the (severe) message
     */
    public void severe (Object msg)
    {
        fatal(msg);
        new Throwable().printStackTrace();
        System.exit(-1);
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
    public void severe (Object msg,
                        Throwable thrown)
    {
        fatal(msg, thrown);
        System.exit(-1);
    }

    //---------//
    // warning //
    //---------//
    /**
     * Log a warning message and continue
     *
     * @param msg the (warning) message
     */
    public void warning (Object msg)
    {
        error(msg);
    }

    //---------//
    // warning //
    //---------//
    /**
     * Log a warning with a related exception, then continue
     *
     * @param msg the (warning) message
     * @param thrown the related exception
     */
    public void warning (Object msg,
                         Throwable thrown)
    {
        error(msg, thrown);
    }

    //~ Classes -----------------------------------------------------------

    //-------//
    // Level //
    //-------//
    /**
     * Class <code>Level</code> is defined here as a convenience for omr
     * user, to avoid the need for directly referencing
     * org.apache.log4j.Level
     */
    public static class Level
            extends org.apache.log4j.Level
    {
        //~ Constructors --------------------------------------------------

        /**
         * A relay to log4j Level constructor
         *
         * @param level
         * @param levelStr
         * @param syslogEquivalent
         */
        protected Level (int level,
                         String levelStr,
                         int syslogEquivalent)
        {
            super(level, levelStr, syslogEquivalent);
        }
    }
}
