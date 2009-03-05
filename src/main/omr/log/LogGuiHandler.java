//----------------------------------------------------------------------------//
//                                                                            //
//                         L o g G u i H a n d l e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.log;

import omr.Main;

import java.util.concurrent.BlockingQueue;
import java.util.logging.LogRecord;

/**
 * <p>Class <code>LogGuiAppender</code> is a specific Log Appender, to be used with
 * logging utility, and which logs directly into the GUI log pane.</p>
 *
 * <p>As a <code>Handler</code>, it should be added to the highest-level
 * logger instance, either programmatically or in a properties file.</p>
 *
 * @author Herv&eacute; Bitteur
 * @author Brenton Partridge
 * @version $Id$
 */
public class LogGuiHandler
    extends java.util.logging.Handler
{
    //~ Instance fields --------------------------------------------------------

    /** The mailbox where log records are stored for display */
    private final BlockingQueue<LogRecord> logMbx = Logger.getMailbox();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // LogGuiHandler //
    //---------------//
    /**
     * Creates a new LogGuiHandler object.
     */
    public LogGuiHandler ()
    {
        setFilter(new LogEmptyMessageFilter());
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // close //
    //-------//
    /**
     * Called when the handler must be closed. It's a void routine for the time
     * being.
     */
    @Override
    public void close ()
    {
    }

    //-------//
    // close //
    //-------//
    /**
     * Flush any buffered output. It's a void routine for the time being.
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
            if (!logMbx.offer(record)) {
                System.err.println("Logger Mailbox is full");
            }

            if (Main.getGui() != null) {
                Main.getGui()
                    .notifylog();
            }
        }
    }
}
