//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                V e r t i c a l s B u i l d e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.FilamentBoard;
import org.audiveris.omr.glyph.dynamic.StickFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.*;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.selection.EntityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code VerticalsBuilder} is in charge of retrieving major vertical sticks of a
 * system.
 * <p>
 * The purpose is to use these major vertical sticks as seeds for stems.
 *
 * @author Hervé Bitteur
 */
public class VerticalsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(VerticalsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The system to process. */
    private final SystemInfo system;

    /** Related sheet. */
    private final Sheet sheet;

    /** Global sheet scale. */
    private final Scale scale;

    /** StemChecker companion. */
    private final StemChecker stemChecker;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new VerticalsBuilder object, using system profile.
     *
     * @param system the underlying system
     */
    public VerticalsBuilder (SystemInfo system)
    {
        this.system = system;

        sheet = system.getSheet();
        scale = sheet.getScale();
        stemChecker = new StemChecker(sheet);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // buildVerticals //
    //----------------//
    /**
     * Build the verticals seeds out of the dedicated system.
     *
     * @throws StepException if processing failed at this step
     */
    public void buildVerticals ()
            throws StepException
    {
        // Display
        if (OMR.gui != null) {
            sheet.getSymbolsEditor().refresh();

            final SheetAssembly assembly = sheet.getStub().getAssembly();

            // Filament board
            EntityService<Filament> fService = sheet.getFilamentIndex().getEntityService();
            assembly.addBoard(SheetTab.DATA_TAB,
                              new FilamentBoard(fService, false));
        }

        // Retrieve candidates
        List<StraightFilament> candidates = retrieveCandidates();

        // Apply seed checks
        checkVerticals(candidates);
    }

    //-----------------//
    // getMinSideRatio //
    //-----------------//
    public static Constant.Ratio getMinSideRatio ()
    {
        return constants.minSideRatio;
    }

    //----------------//
    // checkVerticals //
    //----------------//
    /**
     * This method checks for stem seed glyphs within a collection of vertical filaments
     * in a system.
     *
     * @param sticks the provided collection of vertical filaments
     */
    private void checkVerticals (Collection<StraightFilament> sticks)
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final double minThreshold = stemChecker.getMinThreshold();
        int seedNb = 0;
        logger.debug("S#{} searching verticals on {} sticks", system.getId(), sticks.size());

        for (StraightFilament stick : sticks) {
            if (stick.isVip()) {
                logger.info("VIP checkVerticals for {} in system#{}", stick, system.getId());
            }

            // Check seed is not in a tablature or a header
            Point2D center = stick.getCenter2D();
            Staff staff = system.getClosestStaff(center);

            if ((staff == null) || staff.isTablature() || center.getX() < staff.getHeaderStop()) {
                continue;
            }

            // Run the stem checks
            GradeImpacts impacts = stemChecker.checkStem(stick);
            double res = impacts.getGrade();

            if (res >= minThreshold) {
                final Glyph glyph = glyphIndex.registerOriginal(stick.toGlyph(null));
                glyph.addGroup(GlyphGroup.VERTICAL_SEED); // Needed
                system.addFreeGlyph(glyph);
                seedNb++;
            }
        }

        logger.debug("{}verticals: {}", system.getLogPrefix(), seedNb);
    }

    //--------------------//
    // retrieveCandidates //
    //--------------------//
    /**
     * Retrieve all system sticks that could be seed candidates.
     *
     * @return the collection of suitable sticks found in the system
     */
    private List<StraightFilament> retrieveCandidates ()
    {
        // Select suitable (vertical) sections
        // Since we are looking for major seeds, we'll start only with vertical sections
        List<Section> vSections = new ArrayList<>();

        for (Section section : system.getVerticalSections()) {
            // Check section is within system left and right boundaries
            Point center = section.getAreaCenter();

            if ((center.x > system.getLeft()) && (center.x < system.getRight())) {
                vSections.add(section);
            }
        }

        // Horizontal sections (to contribute to stickers)
        List<Section> hSections = new ArrayList<>();

        for (Section section : system.getHorizontalSections()) {
            // Limit width to 1 pixel
            if (section.getLength(HORIZONTAL) == 1) {
                // Check section is within system left and right boundaries
                Point center = section.getAreaCenter();

                if ((center.x > system.getLeft()) && (center.x < system.getRight())) {
                    hSections.add(section);
                }
            }
        }

        final StickFactory factory = new StickFactory(
                Orientation.VERTICAL,
                system,
                sheet.getFilamentIndex(),
                null,
                scale.getMaxStem(),
                scale.toPixels((Scale.Fraction) constants
                        .getConstant(constants.minCoreSectionLength, system.getProfile())),
                constants.minSideRatio.getValue());

        return factory.retrieveSticks(vSections, hSections);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio minSideRatio = new Constant.Ratio(
                0.4,
                "Minimum ratio of filament length to be actually enlarged");

        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1.5,
                "Minimum length for core sections");
    }
}
