//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                H o r i z o n t a l s F i l t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.ConstantSet;

import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.BasicLag;
import omr.lag.JunctionShiftPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import static omr.run.Orientation.HORIZONTAL;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code HorizontalsFilter} filters the full sheet image for runs suitable for
 * ledgers (and endings).
 *
 * @author Hervé Bitteur
 */
public class HorizontalsFilter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HorizontalsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    //-------------------//
    // HorizontalsFilter //
    //-------------------//
    /**
     * Creates a new HorizontalsFilter object.
     *
     * @param sheet the related sheet
     */
    public HorizontalsFilter (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Start from the binarized image and build the runs and sections
     * that could compose ledgers or endings.
     */
    public void process ()
    {
        final Scale scale = sheet.getScale();
        final int minDistance = scale.toPixels(constants.minDistanceFromStaff);
        final int minRunLength = scale.toPixels(constants.minRunLength);
        final StaffManager staffManager = sheet.getStaffManager();
        final RunsTableFactory.Filter filter = new RunsTableFactory.Filter()
        {
            @Override
            public boolean check (int x,
                                  int y,
                                  int length)
            {
                // Check that the run stands outside of staves.
                Point center = new Point(x + (length / 2), y);
                StaffInfo staff = staffManager.getStaffAt(center);

                return staff.distanceTo(center) >= minDistance;
            }
        };

        final RunsTable hugeHoriTable = new RunsTableFactory(
                HORIZONTAL,
                sheet.getPicture().getSource(Picture.SourceKey.BINARY),
                minRunLength).createTable("huge-hori", filter);

        final Lag lag = new BasicLag("hHugeLag", Orientation.HORIZONTAL);
        final int maxShift = scale.toPixels(constants.maxRunShift);
        final SectionsBuilder sectionsBuilder = new SectionsBuilder(
                lag,
                new JunctionShiftPolicy(maxShift));

        sectionsBuilder.createSections(hugeHoriTable, true);

        sheet.setLag(Lags.FULL_HLAG, lag);
        sheet.getSystemManager().dispatchHorizontalHugeSections();

        if (Main.getGui() != null) {
            // Display a view on this lag
            HoriController horiController = new HoriController(sheet, lag);
            horiController.refresh();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Scale.Fraction minDistanceFromStaff = new Scale.Fraction(
                0.5,
                "Minimum vertical distance from nearest staff");

        Scale.Fraction maxRunShift = new Scale.Fraction(
                0.05,
                "Max shift between two runs of ledger sections");

        Scale.Fraction minRunLength = new Scale.Fraction(1.0, "Minimum length for a ledger run");
    }
}
