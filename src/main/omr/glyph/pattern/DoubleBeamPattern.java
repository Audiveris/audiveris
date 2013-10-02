//----------------------------------------------------------------------------//
//                                                                            //
//                     D o u b l e B e a m P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.*;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Set;

/**
 * Class {@code DoubleBeamPattern} looks for BEAM_2 shape as compound
 * for beams with just one stem.
 *
 * @author Hervé Bitteur
 */
public class DoubleBeamPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            DoubleBeamPattern.class);

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // DoubleBeamPattern //
    //-------------------//
    /**
     * Creates a new DoubleBeamPattern object.
     *
     * @param system the system to process
     */
    public DoubleBeamPattern (SystemInfo system)
    {
        super("DoubleBeam", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (final Glyph beam : system.getGlyphs()) {
            if ((beam.getShape() != Shape.BEAM)
                || beam.isManualShape()
                || (beam.getStemNumber() != 1)) {
                continue;
            }

            if (beam.isVip() || logger.isDebugEnabled()) {
                logger.info("Checking single-stem beam #{}", beam.getId());
            }

            final Glyph stem = beam.getFirstStem();

            // Look for a beam glyph next to it
            final Rectangle beamBox = beam.getBounds();
            beamBox.grow(1, 1);

            Set<Glyph> candidates = Glyphs.lookupGlyphs(
                    system.getGlyphs(),
                    new Predicate<Glyph>()
            {
                @Override
                public boolean check (Glyph glyph)
                {
                    return (glyph != stem) && (glyph != beam)
                           && (glyph.getShape() == Shape.BEAM)
                           && glyph.getBounds()
                            .intersects(beamBox);
                }
            });

            for (Glyph candidate : candidates) {
                if (beam.isVip()
                    || candidate.isVip()
                    || logger.isDebugEnabled()) {
                    logger.info("Beam candidate #{}", candidate);
                }

                Glyph compound = system.buildTransientCompound(
                        Arrays.asList(beam, candidate));
                Evaluation eval = GlyphNetwork.getInstance()
                        .vote(
                        compound,
                        system,
                        Grades.noMinGrade);

                if (eval != null) {
                    // Assign and insert into system & lag environments
                    compound = system.addGlyph(compound);
                    compound.setEvaluation(eval);

                    if (compound.isVip() || logger.isDebugEnabled()) {
                        logger.info(
                                "Compound #{} built as {}",
                                compound.getId(),
                                compound.getEvaluation());
                    }

                    nb++;

                    break;
                }
            }
        }

        return nb;
    }
}
