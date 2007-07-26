//----------------------------------------------------------------------------//
//                                                                            //
//                         L o g G u i H a n d l e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.Main;

import omr.step.LogStepMonitorHandler;
import omr.ui.MainGui;

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
        MainGui gui = Main.getGui();

        if (gui != null) {
        	String message = record.getMessage();
            if (!message.equals("") &&
            	!message.equals(LogStepMonitorHandler.FORCE)) {
                gui.logPane.log(record);
            }
        }
    }
}
