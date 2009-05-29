//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>GlyphInspector</code> is at a System level, dedicated to the
 * inspection of retrieved glyphs, their recognition being usually based on
 * features used by a neural network evaluator.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    /** Related scale */
    private final Scale scale;

    /** Related lag */
    private final GlyphLag lag;

    // Constants for alter verification
    private final int    maxCloseStemDx;
    private final int    minCloseStemOverlap;
    private final int    maxCloseStemLength;
    private final int    maxNaturalOverlap;
    private final int    maxSharpNonOverlap;
    private final double alterMaxDoubt;

    // Constants for clef verification
    private final int    clefHalfWidth;
    private final double clefMaxDoubt;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // GlyphInspector //
    //----------------//
    /**
     * Create an GlyphInspector instance.
     *
     * @param system the dedicated system
     */
    public GlyphInspector (SystemInfo system)
    {
        this.system = system;
        scale = system.getSheet()
                      .getScale();
        lag = system.getSheet()
                    .getVerticalLag();

        maxCloseStemDx = scale.toPixels(constants.maxCloseStemDx);
        minCloseStemOverlap = scale.toPixels(constants.minCloseStemOverlap);
        maxCloseStemLength = scale.toPixels(constants.maxCloseStemLength);
        maxNaturalOverlap = scale.toPixels(constants.maxNaturalOverlap);
        maxSharpNonOverlap = scale.toPixels(constants.maxSharpNonOverlap);
        alterMaxDoubt = constants.alterMaxDoubt.getValue();

        clefHalfWidth = scale.toPixels(constants.clefHalfWidth);
        clefMaxDoubt = constants.clefMaxDoubt.getValue();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getHookMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a beam hook
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getHookMaxDoubt ()
    {
        return constants.hookMaxDoubt.getValue();
    }

    //-----------------//
    // getLeafMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a leaf
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getLeafMaxDoubt ()
    {
        return constants.leafMaxDoubt.getValue();
    }

    //-------------------------//
    // getMinCompoundPartDoubt //
    //-------------------------//
    /**
     * Report the minimum doubt value to be considered as part of a compound
     * @return the doubt threshold for a compound part
     */
    public static double getMinCompoundPartDoubt ()
    {
        return constants.minCompoundPartDoubt.getValue();
    }

    //--------------------//
    // getPatternsMaxDoubt //
    //--------------------//
    /**
     * Report the maximum doubt for a cleanup
     *
     *
     * @return maximum acceptable doubt value
     */
    public static double getPatternsMaxDoubt ()
    {
        return constants.patternsMaxDoubt.getValue();
    }

    //-------------------//
    // getSymbolMaxDoubt //
    //-------------------//
    /**
     * Report the maximum doubt for a symbol
     *
     * @return maximum acceptable doubt value
     */
    public static double getSymbolMaxDoubt ()
    {
        return constants.symbolMaxDoubt.getValue();
    }

    //-----------------//
    // getTextMaxDoubt //
    //-----------------//
    /**
     * Report the maximum doubt for a text symbol
     *
     * @return maximum acceptable doubt value
     */
    public static double getTextMaxDoubt ()
    {
        return constants.textMaxDoubt.getValue();
    }

    //----------------//
    // evaluateGlyphs //
    //----------------//
    /**
     * All unassigned symbol glyphs of a given system, for which we can get
     * a positive vote from the evaluator, are assigned the voted shape.
     * @param maxDoubt the upper limit on doubt to accept an evaluation
     */
    public void evaluateGlyphs (double maxDoubt)
    {
        Evaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                // Get vote
                Evaluation vote = evaluator.vote(glyph, maxDoubt);

                if (vote != null) {
                    glyph.setShape(vote.shape, vote.doubt);
                }
            }
        }
    }

    //---------------//
    // inspectGlyphs //
    //---------------//
    /**
     * Process the given system, by retrieving unassigned glyphs, evaluating
     * and assigning them if OK, or trying compounds otherwise.
     *
     * @param maxDoubt the maximum acceptable doubt for this processing
     */
    public void inspectGlyphs (double maxDoubt)
    {
        system.retrieveGlyphs();
        evaluateGlyphs(maxDoubt);
        system.removeInactiveGlyphs();
        retrieveCompounds(maxDoubt);
        evaluateGlyphs(maxDoubt);
    }

    //-----------------//
    // runAlterPattern //
    //-----------------//
    /**
     * Verify the case of stems very close to each other since they may result
     * from wrong segmentation of sharp or natural signs
     * @return the number of cases fixed
     */
    public int runAlterPattern ()
    {
        // First retrieve the collection of all stems in the system
        // Ordered naturally by their abscissa
        final SortedSet<Glyph> stems = new TreeSet<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isStem() && glyph.isActive()) {
                PixelRectangle box = glyph.getContourBox();

                // Check stem length
                if (box.height <= maxCloseStemLength) {
                    stems.add(glyph);
                }
            }
        }

        int successNb = 0; // Success counter

        // Then, look for close stems
        for (Glyph glyph : stems) {
            if (!glyph.isStem()) {
                continue;
            }

            final PixelRectangle lBox = glyph.getContourBox();
            final int            lX = lBox.x + (lBox.width / 2);

            //logger.info("Checking stems close to glyph #" + glyph.getId());
            for (Glyph other : stems.tailSet(glyph)) {
                if ((other == glyph) || !other.isStem()) {
                    continue;
                }

                // Check horizontal distance
                final PixelRectangle rBox = other.getContourBox();
                final int            dx = lX - lX;

                if (dx > maxCloseStemDx) {
                    break; // Since the set is ordered, no candidate is left
                }

                // Check vertical overlap
                final int commonTop = Math.max(lBox.y, rBox.y);
                final int commonBot = Math.min(
                    lBox.y + lBox.height,
                    rBox.y + rBox.height);
                final int overlap = commonBot - commonTop;

                if (overlap < minCloseStemOverlap) {
                    continue;
                }

                if (logger.isFineEnabled()) {
                    logger.fine("close stems: " + Glyphs.toString(glyph, other));
                }

                // "hide" the stems to not perturb evaluation
                glyph.setShape(null);
                other.setShape(null);

                boolean success = false;

                if (overlap <= maxNaturalOverlap) {
                    // TODO: implement this case !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    //                    logger.info(
                    //                        "Natural glyph rebuilt as #" + compound.getId());
                    //                    success = true;
                } else {
                    success = checkSharp(lBox, rBox);
                }

                if (success) {
                    successNb++;
                } else {
                    // Restore stem shapes
                    glyph.setShape(Shape.COMBINING_STEM);
                    other.setShape(Shape.COMBINING_STEM);
                }
            }
        }

        return successNb;
    }

    //----------------//
    // runClefPattern //
    //----------------//
    /**
     * Verify the initial clefs of a system
     * @return the number of clefs fixed
     */
    public int runClefPattern ()
    {
        int successNb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (!glyph.isClef()) {
                continue;
            }

            if (logger.isFineEnabled()) {
                logger.fine("Glyph#" + glyph.getId() + " " + glyph.getShape());
            }

            PixelPoint center = glyph.getAreaCenter();
            StaffInfo  staff = system.getStaffAtY(center.y);

            // Look in the other staves
            for (StaffInfo oStaff : system.getStaves()) {
                if (oStaff == staff) {
                    continue;
                }

                // Is there a clef in this staff, with similar abscissa?
                PixelRectangle oBox = new PixelRectangle(
                    center.x - clefHalfWidth,
                    oStaff.getFirstLine().yAt(center.x),
                    2 * clefHalfWidth,
                    oStaff.getHeight());

                if (logger.isFineEnabled()) {
                    logger.fine("oBox: " + oBox);
                }

                Collection<Glyph> glyphs = system.lookupIntersectedGlyphs(oBox);

                if (logger.isFineEnabled()) {
                    logger.fine(Glyphs.toString(glyphs));
                }

                if (!foundClef(glyphs)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "No clef found at x:" + center.x + " in staff " +
                            oStaff);
                    }

                    if (checkClef(glyphs)) {
                        successNb++;
                    }
                }
            }
        }

        return successNb;
    }

    //-------------//
    // tryCompound //
    //-------------//
    /**
     * Try to build a compound, starting from given seed and looking into the
     * collection of suitable glyphs.
     *
     * <p>Note that this method has no impact on the system/lag environment.
     * It is the caller's responsability, for a successful (i.e. non-null)
     * compound, to assign its shape and to add the glyph to the system/lag.
     *
     * @param seed the initial glyph around which the compound is built
     * @param suitables collection of potential glyphs
     * @param adapter the specific behavior of the compound tests
     * @return the compound built if successful, null otherwise
     */
    public Glyph tryCompound (Glyph           seed,
                              List<Glyph>     suitables,
                              CompoundAdapter adapter)
    {
        // Build box extended around the seed
        Rectangle   rect = seed.getContourBox();
        Rectangle   box = new Rectangle(
            rect.x - adapter.getBoxDx(),
            rect.y - adapter.getBoxDy(),
            rect.width + (2 * adapter.getBoxDx()),
            rect.height + (2 * adapter.getBoxDy()));

        // Retrieve good neighbors among the suitable glyphs
        List<Glyph> neighbors = new ArrayList<Glyph>();

        // Include the seed in the compound glyphs
        neighbors.add(seed);

        for (Glyph g : suitables) {
            if (!adapter.isSuitable(g)) {
                continue;
            }

            if (box.intersects(g.getContourBox())) {
                neighbors.add(g);
            }
        }

        if (neighbors.size() > 1) {
            if (logger.isFineEnabled()) {
                logger.finest(
                    "neighbors=" + Glyphs.toString(neighbors) + " seed=" + seed);
            }

            Glyph compound = system.buildCompound(neighbors);

            if (adapter.isValid(compound)) {
                // If this compound duplicates an original glyph, 
                // make sure the shape was not forbidden in the original
                Glyph original = system.getSheet()
                                       .getVerticalLag()
                                       .getOriginal(compound);

                if ((original == null) ||
                    !original.isShapeForbidden(compound.getShape())) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Inserted compound " + compound);
                    }

                    return compound;
                }
            }
        }

        return null;
    }

    //-----------//
    // checkClef //
    //-----------//
    /**
     * Try to recognize a glef in the compound of the provided glyphs
     * @param glyphs the parts of a clef candidate
     * @return true if successful
     */
    private boolean checkClef (Collection<Glyph> glyphs)
    {
        Glyphs.purgeManualShapes(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildCompound(glyphs);
        system.computeGlyphFeatures(compound);

        final Evaluation[] votes = GlyphNetwork.getInstance()
                                               .getEvaluations(compound);

        // Check if a clef appears in the top evaluations
        for (Evaluation vote : votes) {
            if (vote.doubt > clefMaxDoubt) {
                break;
            }

            if (ShapeRange.Clefs.contains(vote.shape)) {
                compound = system.addGlyph(compound);
                compound.setShape(vote.shape, Evaluation.ALGORITHM);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        vote.shape + " rebuilt as glyph#" + compound.getId());
                }

                return true;
            }
        }

        return false;
    }

    //------------//
    // checkSharp //
    //------------//
    /**
     * Check if, around the two (stem) boxes, there is actually a sharp sign
     * @param lbox contour box of left stem
     * @param rBox contour box of right stem
     * @return true if successful
     */
    private boolean checkSharp (PixelRectangle lBox,
                                PixelRectangle rBox)
    {
        final int lX = lBox.x + (lBox.width / 2);
        final int rX = rBox.x + (rBox.width / 2);
        final int dyTop = Math.abs(lBox.y - rBox.y);
        final int dyBot = Math.abs(
            (lBox.y + lBox.height) - rBox.y - rBox.height);

        if ((dyTop <= maxSharpNonOverlap) && (dyBot <= maxSharpNonOverlap)) {
            if (logger.isFineEnabled()) {
                logger.fine("SHARP sign?");
            }

            final int            halfWidth = (3 * maxCloseStemDx) / 2;
            final int            hMargin = minCloseStemOverlap / 2;
            final PixelRectangle box = new PixelRectangle(
                ((lX + rX) / 2) - halfWidth,
                Math.min(lBox.y, rBox.y) - hMargin,
                2 * halfWidth,
                Math.max(lBox.y + lBox.height, rBox.y + rBox.height) -
                Math.min(lBox.y, rBox.y) + (2 * hMargin));

            if (logger.isFineEnabled()) {
                logger.fine("outerBox: " + box);
            }

            // Look for glyphs in this outer box
            final Set<Glyph> glyphs = lag.lookupGlyphs(system.getGlyphs(), box);
            Glyphs.purgeManualShapes(glyphs);

            if (glyphs.isEmpty()) {
                return false;
            }

            Glyph compound = system.buildCompound(glyphs);
            system.computeGlyphFeatures(compound);

            final Evaluation[] votes = GlyphNetwork.getInstance()
                                                   .getEvaluations(compound);

            // Check if a sharp appears in the top evaluations
            for (Evaluation vote : votes) {
                if (vote.doubt > alterMaxDoubt) {
                    break;
                }

                if (vote.shape == Shape.SHARP) {
                    compound = system.addGlyph(compound);
                    compound.setShape(vote.shape, Evaluation.ALGORITHM);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "SHARP rebuilt as glyph#" + compound.getId());
                    }

                    return true;
                }
            }
        }

        return false;
    }

    //-----------//
    // foundClef //
    //-----------//
    /**
     * Check whether the provided collection of glyphs contains a clef
     * @param glyphs the provided glyphs
     * @return trur if a clef shape if found
     */
    private boolean foundClef (Collection<Glyph> glyphs)
    {
        for (Glyph gl : glyphs) {
            if (gl.isClef()) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Found glyph#" + gl.getId() + " as " + gl.getShape());
                }

                return true;
            }
        }

        return false;
    }

    //-------------------//
    // retrieveCompounds //
    //-------------------//
    /**
     * In the specified system, look for glyphs portions that should be
     * considered as parts of compound glyphs
     */
    private void retrieveCompounds (double maxDoubt)
    {
        BasicAdapter adapter = new BasicAdapter(maxDoubt);

        // Collect glyphs suitable for participating in compound building
        List<Glyph>  suitables = new ArrayList<Glyph>(
            system.getGlyphs().size());

        for (Glyph glyph : system.getGlyphs()) {
            if (adapter.isSuitable(glyph)) {
                suitables.add(glyph);
            }
        }

        // Sort suitable glyphs by decreasing weight
        Collections.sort(
            suitables,
            new Comparator<Glyph>() {
                    public int compare (Glyph o1,
                                        Glyph o2)
                    {
                        return o2.getWeight() - o1.getWeight();
                    }
                });

        // Now process each seed in turn, by looking at smaller ones
        for (int index = 0; index < suitables.size(); index++) {
            Glyph seed = suitables.get(index);
            adapter.setSeed(seed);

            Glyph compound = tryCompound(
                seed,
                suitables.subList(index + 1, suitables.size()),
                adapter);

            if (compound != null) {
                compound = system.addGlyph(compound);
                compound.setShape(
                    adapter.getVote().shape,
                    adapter.getVote().doubt);
            }
        }
    }

    //~ Inner Interfaces -------------------------------------------------------

    //-----------------//
    // CompoundAdapter //
    //-----------------//
    /**
     * Interface <code>CompoundAdapter</code> provides the needed features for
     * a generic compound building.
     */
    public static interface CompoundAdapter
    {
        //~ Methods ------------------------------------------------------------

        /** Extension in abscissa to look for neighbors
         * @return the extension on left and right
         */
        int getBoxDx ();

        /** Extension in ordinate to look for neighbors
         * @return the extension on top and bottom
         */
        int getBoxDy ();

        /**
         * Predicate for a glyph to be a potential part of the building (the
         * location criteria is handled separately)
         * @param glyph the glyph to check
         * @return true if the glyph is suitable for inclusion
         */
        boolean isSuitable (Glyph glyph);

        /** Predicate to check the success of the newly built compound
         * @param compound the resulting compound glyph to check
         * @return true if the compound is found OK
         */
        boolean isValid (Glyph compound);
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // BasicAdapter //
    //--------------//
    /**
     * Class <code>BasicAdapter</code> is a CompoundAdapter meant to retrieve
     * all compounds (in a system). It is reusable from one candidate to the
     * other, by using the setSeed() method.
     */
    private class BasicAdapter
        implements CompoundAdapter
    {
        //~ Instance fields ----------------------------------------------------

        /** Maximum doubt for a compound */
        private final double maxDoubt;

        /** The seed being considered */
        private Glyph seed;

        /** The result of compound evaluation */
        private Evaluation vote;

        //~ Constructors -------------------------------------------------------

        public BasicAdapter (double maxDoubt)
        {
            this.maxDoubt = maxDoubt;
        }

        //~ Methods ------------------------------------------------------------

        @Implement(CompoundAdapter.class)
        public int getBoxDx ()
        {
            return scale.toPixels(constants.boxWiden);
        }

        @Implement(CompoundAdapter.class)
        public int getBoxDy ()
        {
            return scale.toPixels(constants.boxWiden);
        }

        public void setSeed (Glyph seed)
        {
            this.seed = seed;
        }

        @Implement(CompoundAdapter.class)
        public boolean isSuitable (Glyph glyph)
        {
            return glyph.isActive() &&
                   (!glyph.isKnown() ||
                   (!glyph.isManualShape() &&
                   ((glyph.getShape() == Shape.DOT) ||
                   (glyph.getShape() == Shape.SLUR) ||
                   (glyph.getShape() == Shape.CLUTTER) ||
                   (glyph.getShape() == Shape.VOID_NOTEHEAD) ||
                   (glyph.getShape() == Shape.VOID_NOTEHEAD_2) ||
                   (glyph.getShape() == Shape.VOID_NOTEHEAD_3) ||
                   (glyph.getDoubt() >= getMinCompoundPartDoubt()))));
        }

        @Implement(CompoundAdapter.class)
        public boolean isValid (Glyph compound)
        {
            vote = GlyphNetwork.getInstance()
                               .vote(compound, maxDoubt);

            if (vote != null) {
                compound.setShape(vote.shape, vote.doubt);
            }

            return (vote != null) && vote.shape.isWellKnown() &&
                   (vote.shape != Shape.CLUTTER) &&
                   (!seed.isKnown() || (vote.doubt < seed.getDoubt()));
        }

        public Evaluation getVote ()
        {
            return vote;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction   boxWiden = new Scale.Fraction(
            0.15,
            "Box widening to check intersection with compound");
        Evaluation.Doubt alterMaxDoubt = new Evaluation.Doubt(
            6,
            "Maximum doubt for alteration sign verification");
        Evaluation.Doubt patternsMaxDoubt = new Evaluation.Doubt(
            1.2,
            "Maximum doubt for cleanup phase");
        Evaluation.Doubt leafMaxDoubt = new Evaluation.Doubt(
            1.2,
            "Maximum acceptance doubt for a leaf");
        Evaluation.Doubt symbolMaxDoubt = new Evaluation.Doubt(
            1.2,
            "Maximum doubt for a symbol");
        Evaluation.Doubt textMaxDoubt = new Evaluation.Doubt(
            3.0,
            "Maximum doubt for a text symbol");
        Evaluation.Doubt minCompoundPartDoubt = new Evaluation.Doubt(
            1.020,
            "Minimum doubt for a suitable compound part");
        Scale.Fraction   maxCloseStemDx = new Scale.Fraction(
            0.7d,
            "Maximum horizontal distance for close stems");
        Scale.Fraction   maxSharpNonOverlap = new Scale.Fraction(
            1d,
            "Maximum vertical non overlap for sharp stems");
        Scale.Fraction   maxNaturalOverlap = new Scale.Fraction(
            1.5d,
            "Maximum vertical overlap for natural stems");
        Scale.Fraction   minCloseStemOverlap = new Scale.Fraction(
            0.5d,
            "Minimum vertical overlap for close stems");
        Scale.Fraction   maxCloseStemLength = new Scale.Fraction(
            3d,
            "Maximum length for close stems");
        Scale.Fraction   clefHalfWidth = new Scale.Fraction(
            2d,
            "half width of a clef");
        Evaluation.Doubt clefMaxDoubt = new Evaluation.Doubt(
            3d,
            "Maximum doubt for clef verification");
        Evaluation.Doubt hookMaxDoubt = new Evaluation.Doubt(
            5d,
            "Maximum doubt for beam hook verification");
    }
}
