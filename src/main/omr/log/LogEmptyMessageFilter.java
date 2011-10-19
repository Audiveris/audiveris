//----------------------------------------------------------------------------//
//                                                                            //
//                 L o g E m p t y M e s s a g e F i l t e r                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import java.util.logging.LogRecord;

/**
 * Filters out log messages that have null or empty message strings.
 * Should be set as the filter for <code>Handler</code>'s that
 * display messages on screen.
 */
public class LogEmptyMessageFilter
    implements java.util.logging.Filter
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Checks if the record is loggable.
     * In this implementation, returns true if and only if the record's message
     * is non-null and not an empty string.
     */
    public boolean isLoggable (LogRecord record)
    {
        String message = record.getMessage();

        return ((message != null) && (message.length() > 0));
    }
}
