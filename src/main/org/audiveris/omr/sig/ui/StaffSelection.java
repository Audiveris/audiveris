//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t a f f S e l e c t i o n                                  //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.OMR;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.CLOSED_OPTION;

/**
 * Class {@code StaffSelection} aims at selecting proper staff, based on user location.
 *
 * @author Hervé Bitteur
 */
public class StaffSelection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StaffSelection.class);

    /** Singleton. */
    private static volatile StaffSelection INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Resource injection. */
    private final ResourceMap resources = Application.getInstance().getContext().getResourceMap(
            StaffSelection.class);

    private final String className = getClass().getSimpleName();

    /** Options objects for dialog. */
    private final Object[] options = new Object[]{
        resources.getImageIcon(className + ".UP.icon"),
        resources.getImageIcon(className + ".DOWN.icon"),
        resources.getString(className + ".cancel")
    };

    /** Option pane. */
    private final JOptionPane pane = new JOptionPane(
            resources.getString(className + ".message"),
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            null,
            options);

    /** Reusable dialog. */
    private final JDialog dialog = pane.createDialog(
            OMR.gui.getFrame(),
            resources.getString(className + ".title"));

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the singleton
     *
     * @return the unique instance of this class
     */
    public static StaffSelection getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (StaffSelection.class) {
                if (INSTANCE == null) {
                    INSTANCE = new StaffSelection();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * Prompt the user for staff selection.
     *
     * @return the chosen option: 0 for UP, 1 for down, else -1
     */
    public int prompt ()
    {
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();

        if (selectedValue == null) {
            return CLOSED_OPTION;
        }

        for (int counter = 0; counter < 2; counter++) {
            if (options[counter].equals(selectedValue)) {
                return counter;
            }
        }

        return CLOSED_OPTION; // Either closed or cancelled
    }
}
