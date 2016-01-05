//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R u n B o a r d                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.lag.Lag;

import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.UserEvent;

import omr.ui.Board;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunBoard.class);

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]{RunEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
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

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a Run Board on the RunTable of a provided ag
     *
     * @param lag      the lag that encapsulates the runs table
     * @param expanded true for expanded, false for collapsed
     */
    public RunBoard (Lag lag,
                     boolean expanded)
    {
        this(lag.getRunTable(), expanded);
    }

    /**
     * Create a Run Board on the provided RunTable
     *
     * @param runTable the table of runs
     * @param expanded true for expanded, false for collapsed
     */
    public RunBoard (RunTable runTable,
                     boolean expanded)
    {
        super(
                Board.RUN.name
                + ((runTable.getOrientation() == Orientation.VERTICAL) ? " Vert" : " Hori"),
                Board.RUN.position + ((runTable.getOrientation() == Orientation.VERTICAL) ? 100 : 0),
                runTable.getRunService(),
                eventClasses,
                false,
                false,
                false,
                expanded);
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
