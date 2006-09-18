//----------------------------------------------------------------------------//
//                                                                            //
//                         L o g G u i H a n d l e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.Main;

import omr.ui.Jui;

import java.util.logging.*;

/**
 * Class <code>LogGuiAppender</code> is a specific Log Appender, to be used with
 * logging utility, and which logs directly into the GUI log pane.
 *
 * <p/> To be activated, this class must be explicitly listed as a handler in
 * the logging configuration file.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LogGuiHandler
    extends java.util.logging.Handler
{
    //~ Methods ----------------------------------------------------------------

    //-------//
    // close //
    //-------//
    /**
     * Called when the handler must be closed. It's a void routine for the time
     * being.
     */
    public void close ()
    {
    }

    //-------//
    // close //
    //-------//
    /**
     * Flush any buffered output.. It's a void routine for the time being.
     */
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
    public void publish (LogRecord record)
    {
        Jui jui = Main.getJui();

        if (jui != null) {
            StringBuffer sbuf = new StringBuffer(128);
            sbuf.append(record.getLevel().toString());
            sbuf.append(" - ");
            sbuf.append(record.getMessage());
            sbuf.append("\n");
            jui.logPane.log(sbuf.toString());
        }
    }
}
