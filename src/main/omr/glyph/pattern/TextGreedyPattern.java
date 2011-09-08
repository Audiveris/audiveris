//----------------------------------------------------------------------------//
//                                                                            //
//                     T e x t G r e e d y P a t t e r n                      //
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
import omr.glyph.text.TextBlob;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code TextGreedyPattern} tries to build long series of unassigned
 * glyphs and checks them for TEXT shape via the neural network.
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
        List<Region> regions = new ArrayList<Region>();
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

        Scale.AreaFraction minWeight = new Scale.AreaFraction(
            0.5,
            "Minimum normalized weight to be part of a greedy text");
        Evaluation.Doubt   maxDoubt = new Evaluation.Doubt(
            100d,
            "Maximum doubt for greedy text glyphs");
    }

    //----------//
    // MyRegion //
    //----------//
    private class MyRegion
        extends Region
    {
        //~ Instance fields ----------------------------------------------------

        final double         minWeight = constants.minWeight.getValue();
        final GlyphEvaluator evaluator = GlyphNetwork.getInstance();
        final double         maxDoubt = constants.maxDoubt.getValue();

        //~ Constructors -------------------------------------------------------

        public MyRegion (String  name,
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
                                     Glyph    compound)
        {
            // Check whether this series could be a text
            Collection<Glyph> neighbors = blob.getGlyphs();

            Evaluation        vote = evaluator.vote(compound, maxDoubt, system);
            logger.fine(
                "Checking" + Glyphs.toString(" ", neighbors) + ":" + vote);

            if ((vote != null) && (vote.shape == Shape.TEXT)) {
                compound = system.addGlyph(compound);
                compound.setShape(vote.shape, vote.doubt);

                if (logger.isFineEnabled()) {
                    logger.info(
                        "Greedy TEXT glyph#" + compound.getId() +
                        Glyphs.toString(" ", neighbors));
                }

                ///logger.warning("Greedy text#" + compound.getId());
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
            if (glyph.isKnown()) {
                return false;
            }

            // Respect user choice!
            if (glyph.isManualShape()) {
                return false;
            }

            return true;
        }
    }
}
