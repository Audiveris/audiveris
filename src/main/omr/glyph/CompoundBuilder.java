//----------------------------------------------------------------------------//
//                                                                            //
//                       C o m p o u n d B u i l d e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.util.*;

/**
 * Class {@code CompoundBuilder} defines a generic way to smartly build glyph
 * compounds, and provides derived variants.
 *
 * @author Hervé Bitteur
 */
public class CompoundBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    protected final SystemInfo system;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // CompoundBuilder //
    //-----------------//
    /**
     * Creates a new CompoundBuilder object.
     *
     * @param system the containing system
     */
    public CompoundBuilder (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // buildCompound //
    //---------------//
    /**
     * Try to build a compound, starting from given seed and looking into the
     * collection of suitable glyphs.
     *
     * <p>If successful, this method assign the proper shape to the compound,
     * and insert it in the system/lag environment.
     *
     * @param seed the initial glyph around which the compound is built
     * @param suitables collection of potential glyphs
     * @param adapter the specific behavior of the compound tests
     * @return the compound built if successful, null otherwise
     */
    public Glyph buildCompound (Glyph             seed,
                                Collection<Glyph> suitables,
                                CompoundAdapter   adapter)
    {
        adapter.setSeed(seed);

        // Build a (perhaps specific) box around the seed
        PixelRectangle box = adapter.getIntersectionBox();

        // Retrieve good neighbors among the suitable glyphs
        Set<Glyph>     neighbors = new HashSet<Glyph>();

        // Include the seed in the compound glyphs
        neighbors.add(seed);

        for (Glyph g : suitables) {
            if (adapter.isCandidateSuitable(g) && adapter.isGlyphClose(box, g)) {
                neighbors.add(g);
            }
        }

        if (neighbors.size() > 1) {
            if (logger.isFineEnabled()) {
                logger.finest(
                    "neighbors=" + Glyphs.toString(neighbors) + " seed=" +
                    seed);
            }

            Glyph compound = system.buildTransientCompound(neighbors);

            if (adapter.isCompoundValid(compound)) {
                // If this compound duplicates an original glyph,
                // make sure the shape was not forbidden in the original
                Glyph original = system.getSheet()
                                       .getVerticalLag()
                                       .getOriginal(compound);

                if ((original == null) ||
                    !original.isShapeForbidden(
                    adapter.getChosenEvaluation().shape)) {
                    // Assign and insert into system & lag environments
                    compound = system.addGlyph(compound);
                    system.computeGlyphFeatures(compound);
                    compound.setEvaluation(adapter.getChosenEvaluation());

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Compound #" + compound.getId() + " built as " +
                            compound.getShape());
                    }

                    return compound;
                }
            }
        }

        return null;
    }

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * Interface {@code CompoundAdapter} provides the needed features for
     * a generic compound building.
     */
    public static interface CompoundAdapter
    {
        //~ Methods ------------------------------------------------------------

        //---------------------//
        // isCandidateSuitable //
        //---------------------//
        /**
         * Predicate for a glyph to be a potential part of the building (the
         * location criteria is handled separately)
         * @param glyph the glyph to check
         * @return true if the glyph is suitable for inclusion
         */
        boolean isCandidateSuitable (Glyph glyph);

        //---------------------//
        // getChosenEvaluation //
        //---------------------//
        /**
         * Report the evaluation chosen for the compound, not always the 1st one
         * @return the evaluation (shape + doubt) chosen
         */
        Evaluation getChosenEvaluation ();

        //-----------------//
        // isCompoundValid //
        //-----------------//
        /**
         * Predicate to check the validity of the newly built compound.
         * If valid, the chosenEvaluation is assigned accordingly.
         * @param compound the resulting compound glyph to check
         * @return true if the compound is found OK. The compound shape is not
         * assigned by this method, but can be later retrieved through
         * getChosenEvaluation() method.
         */
        boolean isCompoundValid (Glyph compound);

        /**
         * Predicate to check whether a given glyph is close enough to the
         * provided compound box
         * @param box the provided compound box
         * @param glyph the glyph to check for proximity
         * @return true if glyph is close enough
         */
        boolean isGlyphClose (PixelRectangle box,
                              Glyph          glyph);

        //--------------------//
        // getIntersectionBox //
        //--------------------//
        /**
         * The box to use when looking for intersecting candidates (this
         * typically requires that a seed has been defined beforehand via the
         * setSeed() method, otherwise a NullPointerException is thrown)
         * @return the common box, in pixels
         */
        PixelRectangle getIntersectionBox ();

        //---------//
        // setSeed //
        //---------//
        /**
         * Define the seed glyph around which the compound will be built
         * @param seed the seed glyph
         */
        void setSeed (Glyph seed);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------------//
    // AbstractAdapter //
    //-----------------//
    public abstract static class AbstractAdapter
        implements CompoundAdapter
    {
        //~ Instance fields ----------------------------------------------------

        /** Dedicated system */
        protected final SystemInfo system;

        /** Maximum doubt for a compound */
        protected final double maxDoubt;

        /** Originating seed */
        protected Glyph seed;

        /** The result of compound evaluation */
        protected Evaluation chosenEvaluation;

        //~ Constructors -------------------------------------------------------

        /**
         * Construct a AbstractCompoundAdapter
         * @param system the containing system
         * @param maxDoubt maximum acceptable doubt for the compound shape
         */
        public AbstractAdapter (SystemInfo system,
                                double     maxDoubt)
        {
            this.system = system;
            this.maxDoubt = maxDoubt;
        }

        //~ Methods ------------------------------------------------------------

        public Evaluation getChosenEvaluation ()
        {
            return chosenEvaluation;
        }

        public boolean isGlyphClose (PixelRectangle box,
                                     Glyph          glyph)
        {
            // By default, we use box intersection
            return box.intersects(glyph.getContourBox());
        }

        public void setSeed (Glyph seed)
        {
            this.seed = seed;
        }
    }

    //-----------------//
    // TopShapeAdapter //
    //-----------------//
    /**
     * This compound adapter tries to find some specific shapes among the top
     * evaluations found for the compound
     */
    public abstract static class TopShapeAdapter
        extends AbstractAdapter
    {
        //~ Instance fields ----------------------------------------------------

        /** Collection of desired shapes for a valid compound */
        protected final EnumSet<Shape> desiredShapes;

        /** Specific predicate for desired shapes*/
        protected final Predicate<Shape> predicate = new Predicate<Shape>() {
            public boolean check (Shape shape)
            {
                return desiredShapes.contains(shape);
            }
        };


        //~ Constructors -------------------------------------------------------

        /**
         * Create a TopShapeAdapter instance
         * @param system the containing system
         * @param maxDoubt maximum acceptable doubt on compound shape
         * @param desiredShapes the valid shapes for the compound
         */
        public TopShapeAdapter (SystemInfo     system,
                                double         maxDoubt,
                                EnumSet<Shape> desiredShapes)
        {
            super(system, maxDoubt);
            this.desiredShapes = desiredShapes;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isCompoundValid (Glyph compound)
        {
            // Check if a desired shape appears in the top evaluations
            final Evaluation vote = GlyphNetwork.getInstance()
                                                .topVote(
                compound,
                maxDoubt,
                system,
                predicate);

            if (vote != null) {
                chosenEvaluation = vote;

                return true;
            } else {
                return false;
            }
        }
    }
}
