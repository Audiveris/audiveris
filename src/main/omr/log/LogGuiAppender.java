//----------------------------------------------------------------------------//
//                                                                            //
//                         L o g G u i A p p e n d e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import omr.Main;

import omr.ui.MainGui;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class {@code LogGuiAppender} is a log appender that appends the
 * logging messages to the GUI.
 * It uses an intermediate queue to cope with initial conditions when the GUI
 * is not yet ready to accept messages.
 *
 * @author Hervé Bitteur
 */
public class LogGuiAppender
        extends AppenderBase<ILoggingEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * Size of the mail box.
     * (This cannot be an application Constant, for elaboration dependencies)
     */
    private static final int LOG_MBX_SIZE = 10000;

    /** Temporary mail box for logged messages. */
    private static ArrayBlockingQueue<ILoggingEvent> logMbx = new ArrayBlockingQueue<ILoggingEvent>(
            LOG_MBX_SIZE);

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // getEventCount //
    //---------------//
    public static int getEventCount ()
    {
        return logMbx.size();
    }

    //-----------//
    // pollEvent //
    //-----------//
    public static ILoggingEvent pollEvent ()
    {
        return logMbx.poll();
    }

    //--------//
    // append //
    //--------//
    @Override
    protected void append (ILoggingEvent event)
    {
        logMbx.offer(event);

        MainGui gui = Main.getGui();

        if (gui != null) {
            gui.notifyLog();
        }
    }
}
