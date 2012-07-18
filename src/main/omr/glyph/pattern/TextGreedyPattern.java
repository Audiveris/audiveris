//----------------------------------------------------------------------------//
//                                                                            //
//                     T e x t G r e e d y P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;
import omr.text.TextBlob;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code TextGreedyPattern} tries to build long series of unassigned
 * glyphs and checks them for TEXT shape via the evaluator.
 *
 * TODO: This pattern performs no geometric check between glyphs of a blob, so
 * very distant glyphs can be OCR'ed as a single text glyph!
 *
 * @author Hervé Bitteur
 */
public class TextGreedyPattern
        extends AbstractBlobPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            TextGreedyPattern.class);

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // TextGreedyPattern //
    //-------------------//
    /**
     * Creates a new TextGreedyPattern object.
     * @param system the containing system
     */
    public TextGreedyPattern (SystemInfo system)
    {
        super("TextGreedy", system);
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // buildRegions //
    //--------------//
    @Override
    protected List<Region> buildRegions ()
    {
        // Here we take the whole system space
        // TODO: perhaps we could restrain to system center region?
        List<Region> regions = new ArrayList<>();
        regions.add(new MyRegion("System", null));

        return regions;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Evaluation.Grade minGrade = new Evaluation.Grade(
                1d,
                "Minimum grade for greedy text glyphs");
    }

    //----------//
    // MyRegion //
    //----------//
    private class MyRegion
            extends Region
    {
        //~ Instance fields ----------------------------------------------------

        final ShapeEvaluator evaluator = GlyphNetwork.getInstance();

        final double minGrade = constants.minGrade.getValue();

        //~ Constructors -------------------------------------------------------
        public MyRegion (String name,
                         Polygon polygon)
        {
            super(name, polygon);
        }

        //~ Methods ------------------------------------------------------------
        //-----------//
        // checkBlob //
        //-----------//
        @Override
        protected boolean checkBlob (TextBlob blob,
                                     Glyph compound)
        {
            // Check whether this series could be a text
            Collection<Glyph> neighbors = blob.getGlyphs();

            Evaluation vote = evaluator.vote(compound, system, minGrade);
            logger.fine("Checking{0}:{1}", new Object[]{
                        Glyphs.toString(" ", neighbors), vote});

            if ((vote != null) && (vote.shape == Shape.TEXT)) {
                compound = system.addGlyph(compound);
                compound.setEvaluation(vote);

                logger.fine("Greedy TEXT {0}{1}",
                            new Object[]{compound.idString(), Glyphs.toString(
                            " ", neighbors)});
                return true;
            } else {
                return false;
            }
        }

        //----------------//
        // checkCandidate //
        //----------------//
        @Override
        protected boolean checkCandidate (Glyph glyph)
        {
            // Common checks
            if (!super.checkCandidate(glyph)) {
                return false;
            }

            if (glyph.isKnown()) {
                return false;
            }

            return true;
        }
    }
}
