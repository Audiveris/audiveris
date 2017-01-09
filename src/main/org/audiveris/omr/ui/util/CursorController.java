//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 C u r s o r C o n t r o l l e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.ui.util;

import org.audiveris.omr.ui.OmrGui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.SwingWorker;

/**
 * Delayed busy cursor.
 *
 * @author from Simon White
 * see http://www.catalysoft.com/articles/busycursor.html
 */
public class CursorController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CursorController.class);

    public static final Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);

    public static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    public static final int delay = 500; // in milliseconds

    //~ Constructors -------------------------------------------------------------------------------
    private CursorController ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    public static ActionListener createListener (final Component component,
                                                 final ActionListener mainActionListener)
    {
        ActionListener actionListener = new ActionListener()
        {
            @Override
            public void actionPerformed (final ActionEvent ae)
            {
                TimerTask timerTask = new TimerTask()
                {
                    @Override
                    public void run ()
                    {
                        component.setCursor(busyCursor);
                    }
                };

                Timer timer = new Timer();

                try {
                    timer.schedule(timerTask, delay);
                    mainActionListener.actionPerformed(ae);
                } finally {
                    timer.cancel();
                    component.setCursor(defaultCursor);
                }
            }
        };

        return actionListener;
    }

    //-------------------------//
    // launchWithDelayedCursor //
    //-------------------------//
    /**
     * Launch the provided runnable with a busy cursor displayed if processing does not
     * complete within delay.
     *
     * @param runnable the processing to perform
     */
    public static void launchWithDelayedCursor (Runnable runnable)
    {
        final JFrame frame = OmrGui.getApplication().getMainFrame();
        final TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run ()
            {
                frame.setCursor(busyCursor);
            }
        };

        Timer timer = new Timer();

        try {
            timer.schedule(timerTask, delay);
            runnable.run(); // Here is the real stuff
        } finally {
            timer.cancel();
            frame.setCursor(defaultCursor);
        }
    }

    //--------------------------//
    // launchWithDelayedMessage //
    //--------------------------//
    /**
     * Launch the provided runnable with a delayed message.
     *
     * @param message  the message to display if processing does not complete within delay
     * @param runnable the processing to perform
     */
    public static void launchWithDelayedMessage (final String message,
                                                 final Runnable runnable)
    {
        final TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run ()
            {
                logger.info(message);
            }
        };

        final Timer timer = new Timer();
        timer.schedule(timerTask, delay);

        new SwingWorker()
        {
            @Override
            public Void doInBackground ()
            {
                runnable.run(); // Here is the real stuff

                return null;
            }

            @Override
            protected void done ()
            {
                timer.cancel();
            }
        }.execute();
    }
}
