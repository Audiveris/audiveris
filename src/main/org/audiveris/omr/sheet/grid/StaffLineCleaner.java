//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t a f f L i n e C l e a n e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.dynamic.SectionCompound;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code StaffLineCleaner} handles the "removal" of staff line pixels.
 * <ol>
 * <li>It removes from global {@link Lags#HLAG} lag the (horizontal) sections used by staff lines.
 * <li>It dispatches vertical and remaining horizontal sections into their containing system(s).
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
    /**
     * Clean the staff lines by "removing" the line glyphs.
     */
    public void process ()
    {
        StopWatch watch = new StopWatch("StaffLineCleaner");

        // Replace staff line filaments by lighter data
        watch.start("simplify staff lines");

        for (Staff staff : sheet.getStaffManager().getStaves()) {
            List<LineInfo> originals = staff.simplifyLines(sheet);

            // Remove staff line sections from hLag
            for (LineInfo line : originals) {
                hLag.removeSections(((SectionCompound) line).getMembers());
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
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
