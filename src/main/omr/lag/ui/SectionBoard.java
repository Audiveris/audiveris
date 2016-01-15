//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e c t i o n B o a r d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Orientation;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.UserEvent;

import omr.ui.Board;
import omr.ui.EntityBoard;
import omr.ui.field.LIntegerField;

import com.jgoodies.forms.layout.CellConstraints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code SectionBoard} defines a board dedicated to the display of {@link
 * Section} information, it can also be used as an input means by directly entering the
 * section id in the proper Id spinner.
 *
 * @author Hervé Bitteur
 */
public class SectionBoard
        extends EntityBoard<Section>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SectionBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying lag */
    protected final Lag lag;

    /** Field for left abscissa. */
    private final LIntegerField x = new LIntegerField(false, "X", "Left abscissa in pixels");

    /** Field for top ordinate. */
    private final LIntegerField y = new LIntegerField(false, "Y", "Top ordinate in pixels");

    /** Field for width. */
    private final LIntegerField width = new LIntegerField(
            false,
            "Width",
            "Horizontal width in pixels");

    /** Field for height. */
    private final LIntegerField height = new LIntegerField(
            false,
            "Height",
            "Vertical height in pixels");

    /** Field for weight. */
    private final LIntegerField weight = new LIntegerField(
            false,
            "Weight",
            "Number of pixels in this section");

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a Section Board
     *
     * @param lag      the related lag
     * @param expanded true for expanded, false for collapsed
     */
    public SectionBoard (Lag lag,
                         boolean expanded)
    {
        super(
                new Desc(
                        Board.SECTION.name
                        + ((lag.getOrientation() == Orientation.VERTICAL) ? " Vert" : " Hori"),
                        Board.SECTION.position
                        + ((lag.getOrientation() == Orientation.VERTICAL) ? 100 : 0)),
                lag.getEntityService(),
                expanded);

        this.lag = lag;

        // Initial status
        x.setEnabled(false);
        y.setEnabled(false);
        weight.setEnabled(false);
        width.setEnabled(false);
        height.setEnabled(false);

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Section Selection has been modified
     *
     * @param event the section event
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("SectionBoard: {}", event);

            // Standard behavior
            super.onEvent(event); // ->  (count, vip, dump, id)

            if (event instanceof EntityListEvent) {
                handleEvent((EntityListEvent<Section>) event);
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
        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.add(x.getLabel(), cst.xy(5, r));
        builder.add(x.getField(), cst.xy(7, r));

        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(weight.getLabel(), cst.xy(1, r));
        builder.add(weight.getField(), cst.xy(3, r));

        builder.add(y.getLabel(), cst.xy(5, r));
        builder.add(y.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in EntityListEvent
     *
     * @param listEvent
     */
    private void handleEvent (EntityListEvent<Section> listEvent)
    {
        // Info on last section in list
        final Section section = listEvent.getEntity();

        // Update section fields in this board
        emptyFields(getBody());

        if (section != null) {
            // We have a valid section, let's display its fields
            Rectangle box = section.getBounds();
            x.setValue(box.x);
            y.setValue(box.y);
            width.setValue(box.width);
            height.setValue(box.height);
            weight.setValue(section.getWeight());
        }

        x.setEnabled(section != null);
        y.setEnabled(section != null);
        weight.setEnabled(section != null);
        width.setEnabled(section != null);
        height.setEnabled(section != null);
    }
}
