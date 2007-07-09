//----------------------------------------------------------------------------//
//                                                                            //
//                     L o g B a s i c F o r m a t t e r                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.logging.*;

/**
 * Class <code>LogBasicFormatter</code> formats a log record.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LogBasicFormatter
    extends Formatter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Line separator string.  This is the value of the line.separator */
    private static String lineSeparator = "\n";

    /** Standard format */
    private static final String format = "{0,time}";

    /** Should we print time in log */
    private static final boolean PRINT_TIME = true;

    /** Should we print Class name in log */
    private static final boolean PRINT_CLASS = true;

    /** Should we print Method name in log */
    private static final boolean PRINT_METHOD = true;

    //~ Instance fields --------------------------------------------------------

    private Date          dat = new Date();
    private MessageFormat formatter;
    private Object[]      args = new Object[1];

    //~ Methods ----------------------------------------------------------------

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     *
     * @return a formatted log record
     */
    public synchronized String format (LogRecord record)
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append(" ");

        // First line (if any)
        if (PRINT_TIME) {
            dat.setTime(record.getMillis());
            args[0] = dat;

            StringBuffer text = new StringBuffer();

            if (formatter == null) {
                formatter = new MessageFormat(format);
            }

            formatter.format(args, text, null);
            sb.append(text);
        }

        if (PRINT_CLASS) {
            sb.append(" ");

            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName());
            } else {
                sb.append(record.getLoggerName());
            }
        }

        if (PRINT_METHOD) {
            if (record.getSourceMethodName() != null) {
                sb.append(" ");
                sb.append(record.getSourceMethodName());
            }
        }

        if (sb.length() > 0) {
            //sb.append(lineSeparator);
            sb.append(" -- ");
        }

        // Second line
        String message = formatMessage(record);

        //sb.append(record.getLevel().getLocalizedName());
        sb.append(record.getLevel().getName());
        sb.append(": ");
        sb.append(message);
        sb.append(lineSeparator);

        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            record.getThrown()
                  .printStackTrace(pw);
            pw.close();
            sb.append(sw.toString());
        }

        return sb.toString();
    }
}
