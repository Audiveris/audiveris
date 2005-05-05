//-----------------------------------------------------------------------//
//                                                                       //
//                   L o g B a s i c F o r m a t t e r                   //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Class <code>LogBasicFormatter</code> formats a log record.
 */
public class LogBasicFormatter
        extends Formatter
{
    //~ Instance variables ---------------------------------------------------

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the BasicFormatter was created.
//     private String lineSeparator = (String) java.security.AccessController
//                                        .doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
    private String lineSeparator = "\n";

    //~ Methods --------------------------------------------------------------

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     *
     * @return a formatted log record
     */
    public synchronized String format (LogRecord record)
    {
        String message = formatMessage(record);
        StringBuffer sb = new StringBuffer();

        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(message);
        sb.append(lineSeparator);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }

        notify();

        return sb.toString();
    }
}
