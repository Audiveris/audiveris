//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              M o d e l e s s O p t i o n P a n e                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Exchanger;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * Class {@code ModelessOptionPane} is a basis for providing dialogs similar to
 * the ones provided by JOptionPane, but in a mode-less way.
 *
 * @author Hervé Bitteur
 */
public class ModelessOptionPane
        extends JOptionPane
{

    private static final Logger logger = LoggerFactory.getLogger(ModelessOptionPane.class);

    //-------------------//
    // showConfirmDialog //
    //-------------------//
    /**
     * Spawn a mode-less dialog where the number of choices is determined by the
     * {@code optionType} parameter.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if
     *                        {@code null}, or if the {@code parentComponent} has no {@code Frame},
     *                        a default {@code Frame} is used
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param optionType      an int designating the options available on the dialog:
     *                        {@code YES_NO_OPTION}, {@code YES_NO_CANCEL_OPTION}, or
     *                        {@code OK_CANCEL_OPTION}
     * @return an int indicating the option selected by the user
     * @exception HeadlessException if {@code GraphicsEnvironment}/code} returns {@code true}
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public static int showModelessConfirmDialog (Component parentComponent,
                                                 Object message,
                                                 String title,
                                                 int optionType)
            throws HeadlessException
    {
        final Exchanger<Integer> exchanger = new Exchanger<>();

        final JOptionPane pane = new JOptionPane(message, QUESTION_MESSAGE, optionType);
        Window window = getWindowForComponent(parentComponent);

        final JDialog dialog = (window instanceof Frame) ? new JDialog((Frame) window, title)
                : new JDialog((Dialog) window, title);

        WindowAdapter adapter = new WindowAdapter()
        {
            private boolean gotFocus = false;

            @Override
            public void windowGainedFocus (WindowEvent we)
            {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    pane.selectInitialValue();
                    gotFocus = true;
                }
            }

            @Override
            public void windowClosing (WindowEvent e)
            {
                e.getWindow().dispose();

                try {
                    exchanger.exchange(optionOf(pane));
                } catch (InterruptedException ex) {
                    logger.warn("Exchange got interrupted", ex);
                }
            }
        };

        dialog.addWindowListener(adapter);
        dialog.addWindowFocusListener(adapter);
        dialog.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentShown (ComponentEvent ce)
            {
                // reset value to ensure closing works properly
                pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            }
        });
        pane.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange (PropertyChangeEvent event)
            {
                // Let the defaultCloseOperation handle the closing
                // if the user closed the window without selecting a button
                // (newValue = null in that case).  Otherwise, close the dialog.
                if (dialog.isVisible() && (event.getSource() == pane)
                            && (event.getPropertyName().equals(VALUE_PROPERTY))
                            && (event.getNewValue() != null)
                            && (event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE)) {
                    JOptionPane pane = (JOptionPane) event.getSource();

                    dialog.setVisible(false);

                    try {
                        exchanger.exchange(optionOf(pane));
                    } catch (InterruptedException ex) {
                        logger.warn("Exchange got interrupted", ex);
                    }
                }
            }
        });
        dialog.add(pane, BorderLayout.CENTER);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);

        // Put the calling thread on wait, until the user has made a choice
        try {
            Integer val = exchanger.exchange(null);

            return val;
        } catch (InterruptedException ex) {
            logger.warn("Exchange got interrupted", ex);

            return JOptionPane.CANCEL_OPTION;
        }
    }

    //-----------------------//
    // getWindowForComponent //
    //-----------------------//
    private static Window getWindowForComponent (Component parentComponent)
            throws HeadlessException
    {
        if (parentComponent == null) {
            return getRootFrame();
        }

        if (parentComponent instanceof Frame || parentComponent instanceof Dialog) {
            return (Window) parentComponent;
        }

        return getWindowForComponent(parentComponent.getParent());
    }

    //----------//
    // optionOf //
    //----------//
    private static int optionOf (JOptionPane pane)
    {
        Object selectedValue = pane.getValue();

        if (selectedValue == null) {
            return JOptionPane.CLOSED_OPTION;
        } else if (selectedValue instanceof Integer) {
            return ((Integer) selectedValue);
        } else {
            return JOptionPane.CLOSED_OPTION;
        }
    }
}
