//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      W a i t i n g T a s k                                     //
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

import java.awt.BorderLayout;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.audiveris.omr.ui.Colors;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;

/**
 * Class {@code WaitingTask} is an application background task that can display a
 * temporary waiting dialog until the task is completed.
 * <p>
 * Method {@code doInBackground} must be provided in subclass.
 * <p>
 * Method {@code finished} can be provided in subclass to trigger some other action when the task
 * is completed.
 *
 * @param <T> the result type returned by {@code doInBackground} and {@code get} methods
 * @param <V> the type used for carrying out intermediate results by {@code publish} and
 *            {@code process} methods
 *
 * @author Hervé Bitteur
 */
public abstract class WaitingTask<T, V>
        extends Task<T, V>
{

    /** Timer is used to delay the initial display of the waiting dialog. */
    protected final Timer timer = new Timer();

    /** The waiting dialog to be displayed. */
    protected JDialog dialog;

    /**
     * Create a {@code WaitingTask} with default dialog title and display delay.
     *
     * @param application   The SingleFrameApplication umbrella
     * @param dialogMessage message in the waiting dialog
     */
    public WaitingTask (final SingleFrameApplication application,
                        final String dialogMessage)
    {
        this(application, dialogMessage, null, null);
    }

    /**
     * Create a {@code WaitingTask}.
     *
     * @param application The SingleFrameApplication umbrella
     * @param message     message in the waiting dialog
     * @param title       title for the waiting dialog, null for a default title
     * @param delay       delay (in ms) before the waiting dialog is built and shown.
     *                    If null then CursorController.DELAY value is used
     */
    public WaitingTask (final SingleFrameApplication application,
                        final String message,
                        final String title,
                        Long delay)
    {
        super(application);

        if (delay == null) {
            delay = CursorController.DELAY;
        }

        timer.schedule(new TimerTask()
        {
            @Override
            public void run ()
            {
                final String dialogTitle = (title != null) ? title
                        : application.getContext().getResourceMap(WaitingTask.class)
                                .getString("title");

                dialog = new JDialog(application.getMainFrame(), dialogTitle, false);
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                // Animated progress bar
                JProgressBar bar = new JProgressBar();
                bar.setIndeterminate(true);
                bar.setForeground(Colors.PROGRESS_BAR);
                panel.add(bar, BorderLayout.NORTH);

                // Message
                panel.add(new JLabel(message), BorderLayout.CENTER);

                dialog.setContentPane(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(application.getMainFrame());
                dialog.setVisible(true);
            }
        }, delay);
    }

    @Override
    protected void finished ()
    {
        timer.cancel();

        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }
}
