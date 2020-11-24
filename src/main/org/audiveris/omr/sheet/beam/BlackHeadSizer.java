//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B l a c k H e a d S i z e r                                  //
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
package org.audiveris.omr.sheet.beam;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.image.MorphoProcessor;
import org.audiveris.omr.image.StructureElement;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.BlackHeadScale;
import org.audiveris.omr.sheet.Scale.MusicFontScale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.util.Dumping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code BlackHeadSizer}
 *
 * @author Hervé Bitteur
 */
public class BlackHeadSizer
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BlackHeadSizer.class);

    private final Sheet sheet;

    /** Scale-dependent global constants. */
    private final Parameters params;

    /**
     * Creates a new {@code BlackHeadSizer} object.
     *
     * @param sheet related sheet
     */
    public BlackHeadSizer (Sheet sheet)
    {
        this.sheet = sheet;
        params = new Parameters(sheet.getScale());
    }

    //---------//
    // process //
    //---------//
    /**
     * Infer typical black head size (width and height) from relevant spots.
     * <p>
     * TODO: This is based on beam-targeted spots, perhaps a specific closing for heads would be
     * more appropriate to avoid any ledger portions.
     *
     * @param spots spots retrieved for beams
     */
    public void process (List<Glyph> spots)
    {
        final float radius = (float) (params.diameter - 1) / 2;
        logger.info(
                "Spots black-head retrieval diameter: {}",
                String.format("%.1f", params.diameter));

        final int[] seOffset = {0, 0};
        final StructureElement se = new StructureElement(0, 1, radius, seOffset);
        final MorphoProcessor mp = new MorphoProcessor(se);

        // Filter the spots based on typical weight, width and height.
        // Then derive main width and main height.
        // For visual check, use group BLACK_HEAD_SPOT and BLACK_STACK_SPOT
        final List<Glyph> singles = new ArrayList<>();
        final List<Glyph> stacks = new ArrayList<>();

        for (Glyph glyph : spots) {
            // First check on glyph
            if (!checkSpot(glyph)) {
                continue;
            }

            // Perform blackHead-oriented closing
            glyph = closeBlackHead(mp, glyph);

            // Re-check glyph which may have dramatically changed
            if ((glyph == null) || !checkSpot(glyph)) {
                continue;
            }

            final int height = glyph.getHeight();
            final int weight = glyph.getWeight();

            if ((height <= params.maxHeight) && (weight <= params.maxWeight)) {
                glyph.addGroup(GlyphGroup.BLACK_HEAD_SPOT);
                singles.add(glyph);
            } else if ((height >= params.minStackHeight) && (weight >= params.minStackWeight)) {
                glyph.addGroup(GlyphGroup.BLACK_STACK_SPOT);
                stacks.add(glyph);
            }
        }

        logger.debug("singles: {}", Glyphs.ids(singles));
        logger.debug("stacks: {}", Glyphs.ids(stacks));

        measureSingles(singles);
    }

    //-----------//
    // checkSpot //
    //-----------//
    /**
     * Check weight, width and height of head candidate spot.
     *
     * @param glyph the glyph to check
     * @return true if OK
     */
    private boolean checkSpot (Glyph glyph)
    {
        final int width = glyph.getWidth();

        if ((width < params.minWidth) || (width > params.maxWidth)) {
            return false;
        }

        final int weight = glyph.getWeight();

        if ((weight < params.minWeight) || (weight > params.maxStackWeight)) {
            return false;
        }

        final int height = glyph.getHeight();

        return !((height < params.minHeight) || (height > params.maxStackHeight));
    }

    //----------------//
    // closeBlackHead //
    //----------------//
    /**
     * On the provided (beam-oriented) spot, perform a blackHead-oriented closing.
     *
     * @param spot raw beam-oriented spot
     * @return head-oriented glyph
     */
    private Glyph closeBlackHead (MorphoProcessor mp,
                                  Glyph spot)
    {
        ByteProcessor buffer = spot.getBuffer();
        mp.close(buffer);

        buffer.threshold(params.binarizationThreshold);

        RunTableFactory runFactory = new RunTableFactory(SpotsBuilder.SPOT_ORIENTATION);
        RunTable spotTable = runFactory.createTable(buffer);
        List<Glyph> glyphs = GlyphFactory.buildGlyphs(spotTable, spot.getTopLeft());

        if (glyphs.size() != 1) {
            return null;
        }

        Glyph glyph = glyphs.get(0);
        final Point center = glyph.getCentroid();
        final List<SystemInfo> relevants = sheet.getSystemManager().getSystemsOf(center, null);

        for (SystemInfo system : relevants) {
            // Check glyph is within system abscissa boundaries
            if ((center.x >= system.getLeft()) && (center.x <= system.getRight())) {
                glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                system.addFreeGlyph(glyph);
            }
        }

        return glyph;
    }

    //----------------//
    // measureSingles //
    //----------------//
    /**
     * When there are enough instances of single black heads, we can measure their
     * typical dimensions and derive proper point size for related MusicFont.
     *
     * @param singles the collection of single black heads found
     */
    private void measureSingles (List<Glyph> singles)
    {
        if (singles.size() < params.singlesQuorum) {
            return;
        }

        // Discard extreme widths values and work on core values [1/4 .. 3/4]
        Collections.sort(singles, Glyphs.byWidth);

        List<Glyph> core = singles.subList(singles.size() / 4, (3 * singles.size()) / 4);

        final Population widths = new Population();
        final Population heights = new Population();

        for (Glyph single : core) {
            widths.includeValue(single.getWidth());
            heights.includeValue(single.getHeight());
        }

        final BlackHeadScale blackHeadScale = new BlackHeadScale(
                widths.getMeanValue(),
                widths.getStandardDeviation(),
                heights.getMeanValue(),
                heights.getStandardDeviation());
        sheet.getScale().setBlackHeadScale(blackHeadScale);
        logger.info("Core black head count: {} {}", core.size(), blackHeadScale);

        final double w = blackHeadScale.getWidthMean();
        final MusicFontScale musicFontScale = MusicFont.buildMusicFontScale(w);
        sheet.getScale().setMusicFontScale(musicFontScale);
        logger.info("{}", musicFontScale);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction closingDiameter = new Scale.Fraction(
                0.9,
                "Closing diameter for black head spot");

        private final Constant.Integer binarizationThreshold = new Constant.Integer(
                "pixel",
                110,
                "Global binarization threshold for heads");

        private final Scale.Fraction minWidth = new Scale.Fraction(
                1.0,
                "Minimum width for black head spot");

        private final Scale.Fraction maxWidth = new Scale.Fraction(
                2.0,
                "Maximum width for black head spot");

        private final Scale.Fraction minHeight = new Scale.Fraction(
                0.9,
                "Minimum height for black head spot");

        private final Scale.Fraction maxHeight = new Scale.Fraction(
                1.5,
                "Maximum height for black head spot");

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.8,
                "Minimum weight for black head spot");

        private final Scale.AreaFraction maxWeight = new Scale.AreaFraction(
                1.5,
                "Maximum weight for black head spot");

        private final Scale.Fraction minStackHeight = new Scale.Fraction(
                2.0,
                "Minimum height for vertical stack of black head spots");

        private final Scale.Fraction maxStackHeight = new Scale.Fraction(
                4.0,
                "Maximum height for vertical stack of black head spots");

        private final Scale.AreaFraction minStackWeight = new Scale.AreaFraction(
                2.0,
                "Minimum weight for vertical stack of black head spots");

        private final Scale.AreaFraction maxStackWeight = new Scale.AreaFraction(
                4.0,
                "Maximum weight for vertical stack of black head spots");

        private final Constant.Integer singlesQuorum = new Constant.Integer(
                "glyphs",
                20,
                "Quorum for usable singles");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {

        final int minWidth;

        final int maxWidth;

        final int minHeight;

        final int maxHeight;

        final int minWeight;

        final int maxWeight;

        final int minStackHeight;

        final int maxStackHeight;

        final int minStackWeight;

        final int maxStackWeight;

        final double diameter;

        final int binarizationThreshold;

        final int singlesQuorum;

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        Parameters (Scale scale)
        {
            minWidth = scale.toPixels(constants.minWidth);
            maxWidth = scale.toPixels(constants.maxWidth);
            minHeight = scale.toPixels(constants.minHeight);
            maxHeight = scale.toPixels(constants.maxHeight);
            minWeight = scale.toPixels(constants.minWeight);
            maxWeight = scale.toPixels(constants.maxWeight);
            minStackHeight = scale.toPixels(constants.minStackHeight);
            maxStackHeight = scale.toPixels(constants.maxStackHeight);
            minStackWeight = scale.toPixels(constants.minStackWeight);
            maxStackWeight = scale.toPixels(constants.maxStackWeight);
            diameter = scale.toPixelsDouble(constants.closingDiameter);
            binarizationThreshold = constants.binarizationThreshold.getValue();
            singlesQuorum = constants.singlesQuorum.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
