//----------------------------------------------------------------------------//
//                                                                            //
//                       L o g L e v e l s F i l t e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import java.util.logging.LogRecord;

/**
 * Filters out log messages that are
 * of different types than the ones
 * specified in the constructor.
 */
public class LogLevelsFilter
    implements java.util.logging.Filter
{
    //~ Instance fields --------------------------------------------------------

    private final java.util.logging.Level[] levels;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LogLevelsFilter object.
     *
     */
    public LogLevelsFilter (java.util.logging.Level... levels)
    {
        this.levels = levels;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Checks if the record is loggable.
     * In this implementation, returns true
     * if and only if the record's message
     * is non-null and not an empty string.
     */
    public boolean isLoggable (LogRecord record)
    {
        for (java.util.logging.Level level : levels) {
            if (record.getLevel()
                      .equals(level)) {
                return true;
            }
        }

        return false;
    }
}
