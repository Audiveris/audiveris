//----------------------------------------------------------------------------//
//                                                                            //
//                      H o r i z o n t a l s F i l t e r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.ConstantSet;

import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.BasicLag;
import omr.lag.JunctionShiftPolicy;
import omr.lag.Lag;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import static omr.run.Orientation.HORIZONTAL;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.ui.RunsViewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code HorizontalsFilter} filters the full sheet image for
 * runs suitable for ledgers (and endings).
 *
 * @author Hervé Bitteur
 */
public class HorizontalsFilter
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            HorizontalsBuilder.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------
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

    //~ Methods ----------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Start from the original binarized image and build the runs and
     * sections that could compose ledgers or endings.
     */
    public void process ()
    {
        final Scale scale = sheet.getScale();
        final int minDistance = scale.toPixels(
                constants.minDistanceFromStaff);
        final int minLength = scale.toPixels(
                HorizontalsBuilder.getMinFullLedgerLength());
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
                sheet.getWholeVerticalTable().getBuffer(),
                minLength).createTable("huge-hori", filter);

        if (Main.getGui() != null) {
            RunsViewer runsViewer = sheet.getRunsViewer();
            runsViewer.display(hugeHoriTable);
        }

        final Lag lag = new BasicLag(
                "hHugeLag",
                Orientation.HORIZONTAL);

        final int maxShift = scale.toPixels(
                HorizontalsBuilder.getMaxShift());

        final SectionsBuilder sectionsBuilder = new SectionsBuilder(
                lag,
                new JunctionShiftPolicy(maxShift));

        sectionsBuilder.createSections(hugeHoriTable, true);

        sheet.setHorizontalFullLag(lag);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction minDistanceFromStaff = new Scale.Fraction(
                0.5,
                "Minimum vertical distance from nearest staff");

    }
}
