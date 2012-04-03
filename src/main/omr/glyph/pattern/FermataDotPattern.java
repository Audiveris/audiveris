//----------------------------------------------------------------------------//
//                                                                            //
//                     F e r m a t a D o t P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.*;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Class {@code FermataDotPattern} looks for FERMATA & FERMATA_BELOW
 * shaped-glyphs to make sure that the "associated" dot is not
 * assigned anything else (such as staccato).
 *
 * @author Hervé Bitteur
 */
public class FermataDotPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        FermataDotPattern.class);

    /** Dot avatars */
    private static final List<Shape> dots = Arrays.asList(
        Shape.DOT_set,
        Shape.AUGMENTATION_DOT,
        Shape.STACCATO);

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // FermataDotPattern //
    //-------------------//
    /**
     * Creates a new FermataDotPattern object.
     * @param system the system to process
     */
    public FermataDotPattern (SystemInfo system)
    {
        super("FermataDot", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int nb = 0;

        for (final Glyph fermata : system.getGlyphs()) {
            if ((fermata.getShape() != Shape.FERMATA) &&
                (fermata.getShape() != Shape.FERMATA_BELOW)) {
                continue;
            }

            if (fermata.isVip() || logger.isFineEnabled()) {
                logger.info(
                    "Checking fermata #" + fermata.getId() + " " +
                    fermata.getEvaluation());
            }

            // Find related dot within glyph box
            final PixelRectangle box = fermata.getContourBox();

            Set<Glyph>           candidates = Glyphs.lookupGlyphs(
                system.getGlyphs(),
                new Predicate<Glyph>() {
                        public boolean check (Glyph glyph)
                        {
                            return (glyph != fermata) &&
                                   glyph.getContourBox()
                                        .intersects(box) &&
                                   dots.contains(glyph.getShape());
                        }
                    });

            for (Glyph candidate : candidates) {
                if (fermata.isVip() ||
                    candidate.isVip() ||
                    logger.isFineEnabled()) {
                    logger.info("Dot candidate #" + candidate);
                }

                Glyph      compound = system.buildTransientCompound(
                    Arrays.asList(fermata, candidate));
                Evaluation eval = GlyphNetwork.getInstance()
                                              .vote(
                    compound,
                    system,
                    Grades.noMinGrade);

                if (eval != null) {
                    // Assign and insert into system & nest environments
                    compound = system.addGlyph(compound);
                    compound.setEvaluation(eval);

                    if (compound.isVip() || logger.isFineEnabled()) {
                        logger.info(
                            "Compound #" + compound.getId() + " built as " +
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
