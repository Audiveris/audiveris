//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R e d u c t i o n S t e p                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.step;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.StaffLine;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.SigReducer;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>ReductionStep</code> implements <b>REDUCTION</b> step, which tries to reduce
 * the SIG incrementally after structures (notes + stems + beams) have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class ReductionStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ReductionStep.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ReductionStep object.
     */
    public ReductionStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Void context)
        throws StepException
    {
        // Detect false BeamGroup's
        for (SystemInfo system : sheet.getSystems()) {
            BeamGroupInter.checkBeamGroups(system);
        }

        // Measure typical length of stem free portion
        final List<Integer> lengths = new ArrayList<>();

        for (SystemInfo system : sheet.getSystems()) {
            for (Inter s : system.getSig().inters(StemInter.class)) {
                final StemInter stem = (StemInter) s;
                final Integer lg = stem.getFreeLength();

                if (lg != null) {
                    lengths.add(lg);
                }
            }
        }

        if (!lengths.isEmpty()) {
            Collections.sort(lengths);

            final int medianValue = lengths.get(lengths.size() / 2);
            final double medianFraction = sheet.getScale().pixelsToFrac(medianValue);

            logger.info(
                    "Stems free length median value: {} pixels, {} interlines",
                    medianValue,
                    String.format("%.1f", medianFraction));
        }

        // Clean up glyphs
        final Set<Glyph> toKeep = new LinkedHashSet<>();
        sheet.getSystems().forEach(system -> {
            // Staves lines glyphs
            system.getStaves().forEach(staff -> staff.getLines().forEach(line -> {
                final Glyph glyph = ((StaffLine) line).getGlyph();
                if (glyph != null) {
                    toKeep.add(glyph);
                }
            }));

            // SIG inters
            final SIGraph sig = system.getSig();
            sig.vertexSet().forEach(inter -> {
                final Glyph glyph = inter.getGlyph();
                if (glyph != null) {
                    toKeep.add(glyph);
                }
            });
        });

        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        glyphIndex.getEntities().forEach(glyph -> {
            if (!toKeep.contains(glyph)) {
                glyphIndex.remove(glyph);
            }
        });
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
        throws StepException
    {
        new SigReducer(system, true).reduceFoundations();

        if (constants.refineStemHeadEnd.isSet()) {
            // Refine precise stem head end, based on leading head
            for (Inter s : system.getSig().inters(StemInter.class)) {
                final StemInter stem = (StemInter) s;
                stem.refineHeadEnd();
            }
        }

        if (constants.refineStemTailEnd.isSet()) {
            // Refine precise stem tail end, based on last beam if any
            for (Inter s : system.getSig().inters(StemInter.class)) {
                final StemInter stem = (StemInter) s;
                stem.refineTailEnd();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean refineStemHeadEnd = new Constant.Boolean(
                true,
                "Should we refine every stem head end at terminating head anchor?");

        private final Constant.Boolean refineStemTailEnd = new Constant.Boolean(
                false,
                "Should we refine every stem tail end at last beam?");
    }
}
