//----------------------------------------------------------------------------//
//                                                                            //
//                         L o g G u i H a n d l e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import omr.Main;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Class {@code LogGuiAppender} is a specific Log Appender, to be used
 * with logging utility, and which logs directly into the GUI log pane.
 *
 * <p>As a {@code Handler}, it should be added to the highest-level
 * logger instance, either programmatically or in a properties file.</p>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class LogGuiHandler
        extends java.util.logging.Handler
{
    //~ Instance fields --------------------------------------------------------

    /** The mailbox where log records are stored for display. */
    private final BlockingQueue<FormattedRecord> logMbx = Logger.getMailbox();

    //~ Constructors -----------------------------------------------------------
    //
    //---------------//
    // LogGuiHandler //
    //---------------//
    /**
     * Creates a new LogGuiHandler object.
     */
    public LogGuiHandler ()
    {
        setFormatter(new MiniFormatter());
        setFilter(new LogEmptyMessageFilter());
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------//
    // close //
    //-------//
    /**
     * Called when the handler must be closed.
     * It's a void routine for the time being.
     */
    @Override
    public void close ()
    {
    }

    //-------//
    // flush //
    //-------//
    /**
     * Flush any buffered output.
     * It's a void routine for the time being.
     */
    @Override
    public void flush ()
    {
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish one log record
     *
     * @param record the record to be logged
     */
    @Override
    public void publish (LogRecord record)
    {
        if (isLoggable(record)) {
            try {
                String msg = getFormatter().formatMessage(record);
                FormattedRecord formatted = new FormattedRecord(
                        record.getLevel(), msg);
                if (!logMbx.offer(formatted)) {
                    System.err.println("Logger Mailbox is full");
                }

                if (Main.getGui() != null) {
                    Main.getGui().notifyLog();
                }
            } catch (Throwable ex) {
                System.err.println("Error formatting log record");
                ex.printStackTrace();
            }
        }
    }

    //---------------//
    // MiniFormatter //
    //---------------//
    /**
     * A minimal formatter, since we just need the formatMessage()
     * feature provided by the abstract Formatter class.
     */
    private class MiniFormatter
            extends Formatter
    {

        @Override
        public String format (LogRecord record)
        {
            // We don't use this feature
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
