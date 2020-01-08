//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 C u r s o r C o n t r o l l e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(CursorController.class);

    /** Cursor when waiting. */
    public static final Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);

    /** Cursor in standard status. */
    public static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    /** Delay before busy cursor is displayed. */
    public static final long DELAY = constants.delay.getValue(); // in milliseconds

    private CursorController ()
    {
    }

    //----------------//
    // createListener //
    //----------------//
    /**
     * Wraps an action listener with a busy cursor if not performed within DELAY.
     *
     * @param component          owner of cursor
     * @param mainActionListener real listener to be wrapped
     * @return the wrapped listener
     */
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
                    timer.schedule(timerTask, DELAY);
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
     * complete within DELAY.
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
            timer.schedule(timerTask, DELAY);
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
     * @param message  the message to display if processing does not complete within DELAY
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
        timer.schedule(timerTask, DELAY);

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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer delay = new Constant.Integer(
                "ms",
                500,
                "Default delay value in milli-seconds");
    }
}
