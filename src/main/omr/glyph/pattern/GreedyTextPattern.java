//----------------------------------------------------------------------------//
//                                                                            //
//                     G r e e d y T e x t P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code GreedyTextPattern} tries to build long series of unassigned
 * glyphs and check them for text shape.
 *
 * @author Hervé Bitteur
 */
public class GreedyTextPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        GreedyTextPattern.class);

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // GreedyTextPattern //
    //-------------------//
    /**
     * Creates a new GreedyTextPattern object.
     * @param system the containing system
     */
    public GreedyTextPattern (SystemInfo system)
    {
        super("GreedyText", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int                  successNb = 0;
        Scale                scale = system.getScoreSystem()
                                           .getScale();
        final double         minWeight = constants.minWeight.getValue();
        final double         maxDoubt = constants.maxDoubt.getValue();
        final GlyphEvaluator evaluator = GlyphNetwork.getInstance();
        final int            maxDx = scale.toPixels(constants.maxDx);

        List<Glyph>          glyphs = new ArrayList<Glyph>(system.getGlyphs());
        int                  index = -1;

        for (Glyph glyph : glyphs) {
            index++;

            if (glyph.isKnown() ||
                glyph.isManualShape() ||
                (glyph.getNormalizedWeight() < minWeight)) {
                continue;
            }

            if (logger.isFineEnabled()) {
                logger.fine("Starting a greedy glyph#" + glyph.getId());
            }

            // Start a box
            PixelRectangle box = glyph.getContourBox();
            box.width += maxDx;

            List<Glyph> neighbors = new ArrayList<Glyph>();
            neighbors.add(glyph);

            otherLoop: 
            for (Glyph other : glyphs.subList(index + 1, glyphs.size())) {
                if (other.isKnown() || other.isManualShape()) {
                    continue;
                }

                PixelRectangle oBox = other.getContourBox();

                if (oBox.x > (box.x + box.width)) {
                    break otherLoop; // Give up
                } else if (!box.intersects(oBox)) {
                    continue;
                }

                neighbors.add(other);
                box = box.union(other.getContourBox());
                box.width += maxDx;

                // Check whether this series could be a text
                Glyph      compound = system.buildTransientCompound(neighbors);
                Evaluation vote = evaluator.vote(compound, maxDoubt, system);
                logger.fine(
                    "Checking " + vote + Glyphs.toString(" ", neighbors));

                if ((vote != null) && (vote.shape == Shape.TEXT)) {
                    compound = system.addGlyph(compound);
                    compound.setShape(vote.shape, vote.doubt);

                    if (logger.isFineEnabled()) {
                        logger.info(
                            "Greedy TEXT glyph#" + compound.getId() +
                            Glyphs.toString(" ", neighbors));
                    }

                    successNb++;

                    break otherLoop;
                }
            }
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.AreaFraction minWeight = new Scale.AreaFraction(
            0.5,
            "Minimum normalized weight to start a greedy text");
        Scale.Fraction     maxDx = new Scale.Fraction(
            0.2,
            "Maximum horizontal gap between two character boxes");
        Evaluation.Doubt   maxDoubt = new Evaluation.Doubt(
            100d,
            "Maximum doubt for greedy text glyphs");
    }
}
