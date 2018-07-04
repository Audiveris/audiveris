//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C h e c k B o a r d                                       //
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
package org.audiveris.omr.check;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code CheckBoard} defines a board dedicated to the display of check result
 * information.
 *
 * @param <C> The {@link Checkable} entity type to be checked
 *
 * @author Hervé Bitteur
 */
public class CheckBoard<C>
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CheckBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** For display of check suite results */
    private final CheckPanel<C> checkPanel;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a Check Board.
     *
     * @param name             the name of the check
     * @param suite            the check suite to be used, if any
     * @param selectionService which selection service to use
     * @param eventList        which event classes to expect
     */
    public CheckBoard (String name,
                       CheckSuite<C> suite,
                       SelectionService selectionService,
                       Class[] eventList)
    {
        super(
                name,
                Board.CHECK.position,
                selectionService,
                eventList,
                false, // Selected
                false, // Count
                false, // Vip
                false); // Dump
        checkPanel = new CheckPanel<C>(suite);

        if (suite != null) {
            defineLayout(suite.getName());
        }

        // define default content
        tellObject(null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // applySuite //
    //------------//
    /**
     * Assign a (new) suite to the check board and apply it to the provided object.
     *
     * @param suite  the (new) check suite to be used
     * @param object the object to apply the checks suite on
     */
    public synchronized void applySuite (CheckSuite<C> suite,
                                         C object)
    {
        final boolean toBuild = checkPanel.getComponent() == null;
        checkPanel.setSuite(suite);

        if (toBuild) {
            defineLayout(suite.getName());
        }

        tellObject(object);
    }

    //
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when C Selection has been modified.
     *
     * @param event the Event to perform check upon its data
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            tellObject((C) event.getData()); // Compiler warning
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //------------//
    // tellObject //
    //------------//
    /**
     * Render the result of checking the given object.
     *
     * @param object the object whose check result is to be displayed
     */
    protected final void tellObject (C object)
    {
        if (object == null) {
            setVisible(false);
        } else {
            setVisible(true);
            checkPanel.passForm(object);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String name)
    {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout, getBody());

        ///builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.add(checkPanel.getComponent(), cst.xy(1, r));
    }
}
