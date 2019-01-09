//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R u n B o a r d                                         //
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
package org.audiveris.omr.run;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.RunEvent;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code RunBoard} is dedicated to display of Run information.
 *
 * @author Hervé Bitteur
 */
public class RunBoard
        extends Board
{

    private static final Logger logger = LoggerFactory.getLogger(RunBoard.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]{RunEvent.class};

    /** Field for run length */
    private final LIntegerField rLength = new LIntegerField(
            false,
            "Length",
            "Length of run in pixels");

    /** Field for run start */
    private final LIntegerField rStart = new LIntegerField(
            false,
            "Start",
            "Pixel coordinate at start of run");

    /**
     * Create a Run Board on the RunTable of a provided ag
     *
     * @param lag      the lag that encapsulates the runs table
     * @param selected true for pre-selected, false for collapsed
     */
    public RunBoard (Lag lag,
                     boolean selected)
    {
        this(lag.getRunTable(), selected);
    }

    /**
     * Create a Run Board on the provided RunTable
     *
     * @param runTable the table of runs
     * @param selected true for pre-selected, false for collapsed
     */
    public RunBoard (RunTable runTable,
                     boolean selected)
    {
        super(
                Board.RUN.name + ((runTable.getOrientation() == Orientation.VERTICAL) ? " Vert"
                : " Hori"),
                Board.RUN.position + ((runTable.getOrientation() == Orientation.VERTICAL) ? 100
                : 0),
                runTable.getRunService(),
                eventClasses,
                selected,
                false,
                false,
                false);
        defineLayout();
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Run Selection has been modified
     *
     * @param event the notified event
     */
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("RunBoard: {}", event);

            if (event instanceof RunEvent) {
                final RunEvent runEvent = (RunEvent) event;
                final Run run = runEvent.getData();

                if (run != null) {
                    rStart.setValue(run.getStart());
                    rLength.setValue(run.getLength());
                } else {
                    emptyFields(getBody());
                }
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final FormLayout layout = Panel.makeFormLayout(1, 3);
        final PanelBuilder builder = new PanelBuilder(layout, getBody());
        final CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------

        builder.add(rStart.getLabel(), cst.xy(1, r));
        builder.add(rStart.getField(), cst.xy(3, r));

        builder.add(rLength.getLabel(), cst.xy(5, r));
        builder.add(rLength.getField(), cst.xy(7, r));
    }
}
