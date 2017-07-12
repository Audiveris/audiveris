//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L o g G u i A p p e n d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import org.audiveris.omr.OMR;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class {@code LogGuiAppender} is a log appender that appends the logging messages to
 * the GUI.
 * <p>
 * It uses an intermediate queue to cope with initial conditions when the GUI is not yet ready to
 * accept messages.
 *
 * @author Hervé Bitteur
 */
public class LogGuiAppender
        extends AppenderBase<ILoggingEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /**
     * Size of the mail box.
     * (This cannot be an application Constant, for elaboration dependencies)
     */
    private static final int LOG_MBX_SIZE = 10000;

    /** Temporary mail box for logged messages. */
    private static final ArrayBlockingQueue<ILoggingEvent> logMbx = new ArrayBlockingQueue<ILoggingEvent>(
            LOG_MBX_SIZE);

    //~ Methods ------------------------------------------------------------------------------------
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

        if (OMR.gui != null) {
            OMR.gui.notifyLog();
        }
    }
}
