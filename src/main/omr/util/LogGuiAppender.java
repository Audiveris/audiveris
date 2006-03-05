//-----------------------------------------------------------------------//
//                                                                       //
//                      L o g G u i A p p e n d e r                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

import omr.Main;
import omr.ui.Jui;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Class <code>LogGuiAppender</code> is a specific Log Appender, to be used
 * with log4j utility, and which logs directly into the GUI log pane.
 *
 * <p/> To be activated, this class must be explicitly listed as an
 * appender in the "log4j.properties" configuration file. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LogGuiAppender
        extends org.apache.log4j.AppenderSkeleton
{
    //~ Instance variables ------------------------------------------------

    // Temporary string buffer, to elaborate logged message layout
    private StringBuffer sbuf = new StringBuffer(128);

    //~ Methods -----------------------------------------------------------

    //-------//
    // close //
    //-------//
    /**
     * Called when the appender must be closed. It's a void routine for the
     * time being.
     */
    public void close ()
    {
    }

    //----------------//
    // requiresLayout //
    //----------------//
     /**
     * This appender requires no external layout, since we do it
     * internally.
     *
     * @return false
     */
    public boolean requiresLayout ()
    {
        return false;
    }

    //--------//
    // append //
    //--------//
    /**
     * Appends one message to be logged
     *
     * @param event the event to be logged
     */
    protected void append (LoggingEvent event)
    {
        Jui jui = Main.getJui();

        if (jui != null) {
            sbuf.setLength(0);
            sbuf.append(event.getLevel().toString());
            sbuf.append(" - ");
            sbuf.append(event.getRenderedMessage());
            sbuf.append(Layout.LINE_SEP);
            jui.logPane.log(sbuf.toString());
        }
    }
}
