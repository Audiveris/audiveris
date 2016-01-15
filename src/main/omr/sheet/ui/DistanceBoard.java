//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D i s t a n c e B o a r d                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.image.ChamferDistance;
import omr.image.DistanceTable;

import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.UserEvent;

import omr.sheet.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code DistanceBoard} is a PixelBoard where the 'level' field is in fact used
 * to display distance value.
 *
 * @author Hervé Bitteur
 */
public class DistanceBoard
        extends PixelBoard
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DistanceBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The distance table to browse. */
    private final DistanceTable table;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code DistanceBoard} object.
     *
     * @param sheet related sheet
     * @param table distance table to browse
     */
    public DistanceBoard (Sheet sheet,
                          DistanceTable table)
    {
        super(sheet);
        this.table = table;

        // Customize the 'level' field
        level.getLabel().setText("Dist");
        level.getLabel().setToolTipText("Distance to foreground");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof LocationEvent) {
                super.onEvent(event); // To display location coordinates

                LocationEvent sheetLocation = (LocationEvent) event;
                Rectangle rect = sheetLocation.getData();

                if (rect != null) {
                    Point point = rect.getLocation();

                    if ((point.x < table.getWidth()) && (point.y < table.getHeight())) {
                        // Display distance value
                        int raw = table.getValue(point.x, point.y);

                        if (raw == ChamferDistance.VALUE_UNKNOWN) {
                            level.setText("none");
                        } else {
                            double distance = raw / (double) table.getNormalizer();
                            level.setText(String.format("%.1f", distance));
                        }
                    } else {
                        level.setText("");
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }
}
