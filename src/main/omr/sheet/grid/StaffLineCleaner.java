//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t a f f L i n e C l e a n e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.dynamic.SectionCompound;

import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;

import omr.run.Orientation;

import omr.sheet.Sheet;
import omr.sheet.Staff;

import omr.util.Navigable;
import omr.util.Predicate;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import omr.glyph.Symbol.Group;
import omr.run.RunTable;
import omr.sheet.Picture;

/**
 * Class {@code StaffLineCleaner} handles the "removal" of staff line pixels.
 *
 * @author Hervé Bitteur
 */
class StaffLineCleaner
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
            staff.simplifyLines(sheet);
        }

        // Remove staff line stuff from hLag
        watch.start("purge hLag");

        List<Section> staffLinesSections = removeStaffLineSections(hLag);
        logger.debug(
                "{}StaffLine sections removed: {}",
                sheet.getLogPrefix(),
                staffLinesSections.size());

        // Make the NO_STAFF buffer & table available
        RunTable noStaffTable = sheet.getPicture().buildNoStaffTable();
        sheet.getPicture().setTable(Picture.TableKey.NO_STAFF, noStaffTable);

        // Regenerate hLag from noStaff buffer
        sheet.getLagManager().rebuildHLag();

        // Dispatch sections to relevant systems
        watch.start("populate systems");
        sheet.getSystemManager().populateSystems();

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //-------------------------//
    // removeStaffLineSections //
    //-------------------------//
    private List<Section> removeStaffLineSections (Lag hLag)
    {
        List<Section> removedSections = hLag.purgeSections(
                new Predicate<Section>()
                {
                    @Override
                    public boolean check (Section section)
                    {
                        SectionCompound compound = section.getCompound();

                        if ((compound != null) && (compound.hasGroup(Group.STAFF_LINE))) {
                            /**
                             * Narrow horizontal section can be kept to avoid
                             * over-segmentation between vertical sections.
                             * TODO: keep this?
                             * TODO: use constant (instead of 1-pixel width)?
                             */
                            if ((section.getLength(Orientation.HORIZONTAL) == 1)
                                && (section.getLength(Orientation.VERTICAL) > 1)) {
                                if (section.isVip() || logger.isDebugEnabled()) {
                                    logger.info("Keeping staffline section {}", section);
                                }

                                section.setCompound(null);

                                return false;
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    }
                });

        return removedSections;
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
