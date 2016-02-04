//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t a f f L i n e C l e a n e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.Lag;
import omr.lag.Lags;

import omr.sheet.Sheet;
import omr.sheet.Staff;

import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code StaffLineCleaner} handles the "removal" of staff line pixels.
 * <ol>
 * <li>It removes from global {@link Lags#HLAG} lag the (horizontal) sections used by staff lines.
 * <li>It dispatches vertical & remaining horizontal sections into their containing system(s).
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class StaffLineCleaner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffLineCleaner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Horizontal lag. */
    private final Lag hLag;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffLineCleaner} object.
     *
     * @param sheet the related sheet, which holds the v and h lags
     */
    public StaffLineCleaner (Sheet sheet)
    {
        this.sheet = sheet;

        hLag = sheet.getLagManager().getLag(Lags.HLAG);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process ()
    {
        StopWatch watch = new StopWatch("StaffLineCleaner");

        // Replace staff line filaments by lighter data
        watch.start("simplify staff lines");

        for (Staff staff : sheet.getStaffManager().getStaves()) {
            List<LineInfo> originals = staff.simplifyLines(sheet);

            // Remove staff line sections from hLag
            for (LineInfo line : originals) {
                hLag.removeSections(((StaffFilament) line).getMembers());
            }
        }

        // Regenerate hLag from noStaff buffer
        sheet.getLagManager().rebuildHLag();

        // Dispatch sections to relevant systems
        watch.start("populate systems");
        sheet.getSystemManager().populateSystems();

        if (constants.printWatch.isSet()) {
            watch.print();
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

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
