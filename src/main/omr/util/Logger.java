//----------------------------------------------------------------------------//
//                                                                            //
//                                L o g g e r                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.constant.UnitManager;

import java.util.logging.Level;
import java.util.logging.LogManager;

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
        LogManager manager = LogManager.getLogManager();
        Logger     result = (Logger) manager.getLogger(name);

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
    public void severe (String msg)
    {
        super.severe(msg);
        new Throwable().printStackTrace();

        ///System.exit(-1);
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

        ///System.exit(-1);
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
    public void warning (String    msg,
                         Throwable thrown)
    {
        super.warning(msg);
        thrown.printStackTrace();
    }
}
