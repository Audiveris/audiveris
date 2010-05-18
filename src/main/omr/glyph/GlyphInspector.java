//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h I n s p e c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder.CompoundAdapter;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Scale.InterlineFraction;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.*;

/**
 * Class <code>GlyphInspector</code> is at a System level, dedicated to the
 * inspection of retrieved glyphs, their recognition being usually based on
 * features used by a neural network evaluator.
 *
 * @author Hervé Bitteur
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

    /** Related compound builder */
    private final CompoundBuilder compoundBuilder;

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
    private final double maxBassDotPitchDy;
    private final double maxBassDotDx;

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
        compoundBuilder = new CompoundBuilder(system);

        maxCloseStemDx = scale.toPixels(constants.maxCloseStemDx);
        minCloseStemOverlap = scale.toPixels(constants.minCloseStemOverlap);
        maxCloseStemLength = scale.toPixels(constants.maxCloseStemLength);
        maxNaturalOverlap = scale.toPixels(constants.maxNaturalOverlap);
        maxSharpNonOverlap = scale.toPixels(constants.maxSharpNonOverlap);
        alterMaxDoubt = constants.alterMaxDoubt.getValue();

        clefHalfWidth = scale.toPixels(constants.clefHalfWidth);
        clefMaxDoubt = constants.clefMaxDoubt.getValue();
        maxBassDotPitchDy = constants.maxBassDotPitchDy.getValue();
        maxBassDotDx = scale.toPixels(constants.maxBassDotDx);
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
        GlyphEvaluator evaluator = GlyphNetwork.getInstance();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == null) {
                // Get vote
                Evaluation vote = evaluator.vote(glyph, maxDoubt);

                if ((vote != null) && !glyph.isShapeForbidden(vote.shape)) {
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
        // For Symbols & Leaves
        system.retrieveGlyphs();
        system.removeInactiveGlyphs();
        evaluateGlyphs(maxDoubt);
        system.removeInactiveGlyphs();

        // For Compounds
        retrieveCompounds(maxDoubt);
        system.removeInactiveGlyphs();
        evaluateGlyphs(maxDoubt);
        system.removeInactiveGlyphs();
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
        final SortedSet<Glyph> stems = Glyphs.sortedSet();

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
                final int            rX = rBox.x + (rBox.width / 2);
                final int            dx = rX - lX;

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
                    logger.fine(
                        "close stems: " + Glyphs.toString(glyph, other));
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
    // runBassPattern //
    //----------------//
    /**
     * Check for segmented bass clefs, in the neighborhood of typical vertical
     * two-dot patterns
     * @return the number of bass clefs fixed
     */
    public int runBassPattern ()
    {
        int             successNb = 0;

        // Specific adapter definition for bass clefs
        CompoundAdapter bassAdapter = new BassAdapter(system, clefMaxDoubt);

        for (Glyph top : system.getGlyphs()) {
            // Look for top dot
            if ((top.getShape() != Shape.DOT) ||
                (Math.abs(top.getPitchPosition() - -3) > maxBassDotPitchDy)) {
                continue;
            }

            int       topX = top.getCentroid().x;
            StaffInfo topStaff = system.getStaffAtY(top.getCentroid().y);

            // Look for bottom dot right underneath, and in the same staff
            for (Glyph bot : system.getGlyphs()) {
                if ((bot.getShape() != Shape.DOT) ||
                    (Math.abs(bot.getPitchPosition() - -1) > maxBassDotPitchDy)) {
                    continue;
                }

                if (Math.abs(bot.getCentroid().x - topX) > maxBassDotDx) {
                    continue;
                }

                if (system.getStaffAtY(bot.getCentroid().y) != topStaff) {
                    continue;
                }

                // Here we have a couple
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Got bass dots #" + top.getId() + " & #" + bot.getId());
                }

                Glyph compound = compoundBuilder.buildCompound(
                    top,
                    system.getGlyphs(),
                    bassAdapter);

                if (compound != null) {
                    successNb++;
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

    //-----------------//
    // runShapePattern //
    //-----------------//
    /**
     * A general pattern to check some glyph shapes within their environment
     * @return the number of glyphs deassigned
     */
    public int runShapePattern ()
    {
        int modifNb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            Shape shape = glyph.getShape();

            if (glyph.isManualShape()) {
                continue;
            }

            if ((Shape.BRACKET == shape) || (Shape.BRACE == shape)) {
                // Make sure at least a staff interval is embraced
                PixelRectangle box = glyph.getContourBox();
                boolean        embraced = false;
                int            intervalTop = Integer.MIN_VALUE;

                for (StaffInfo staff : system.getStaves()) {
                    if (intervalTop != Integer.MIN_VALUE) {
                        int intervalBottom = staff.getFirstLine()
                                                  .yAt(box.x);

                        if ((intervalTop >= box.y) &&
                            (intervalBottom <= (box.y + box.height))) {
                            embraced = true; // Ok for this one

                            break;
                        }
                    }

                    intervalTop = staff.getLastLine()
                                       .yAt(box.x);
                }

                if (!embraced) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Deassigned " + shape + " glyph #" + glyph.getId());
                    }

                    glyph.setShape(null, Evaluation.ALGORITHM);
                    modifNb++;
                }
            }

            // Glyph which must be between left and right sides of system
            if (ShapeRange.Tuplets.contains(shape)) {
                PixelRectangle glyphBox = glyph.getContourBox();

                if (((glyphBox.x + glyphBox.width) < system.getLeft()) ||
                    (glyphBox.x > system.getRight())) {
                    glyph.setShape(null, Evaluation.ALGORITHM);
                    modifNb++;
                }
            }
        }

        return modifNb;
    }

    //-----------//
    // checkClef //
    //-----------//
    /**
     * Try to recognize a clef in the compound of the provided glyphs
     * @param glyphs the parts of a clef candidate
     * @return true if successful
     */
    private boolean checkClef (Collection<Glyph> glyphs)
    {
        Glyphs.purgeManualShapes(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildTransientCompound(glyphs);
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

            Glyph compound = system.buildTransientCompound(glyphs);
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
        // Sort suitable glyphs by decreasing weight
        List<Glyph> glyphs = new ArrayList<Glyph>(system.getGlyphs());
        Collections.sort(glyphs, Glyph.reverseWeightComparator);

        // Now process each seed in turn, by looking at smaller ones
        BasicAdapter adapter = new BasicAdapter(system, maxDoubt);

        for (int index = 0; index < glyphs.size(); index++) {
            Glyph seed = glyphs.get(index);

            if (adapter.isCandidateSuitable(seed)) {
                compoundBuilder.buildCompound(
                    seed,
                    glyphs.subList(index + 1, glyphs.size()),
                    adapter);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // BasicAdapter //
    //--------------//
    /**
     * Class <code>BasicAdapter</code> is a CompoundAdapter meant to retrieve
     * all compounds (in a system).
     */
    private class BasicAdapter
        extends CompoundBuilder.AbstractAdapter
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Construct a BasicAdapter around a given seed
         * @param system the containing system
         * @param maxDoubt maximum acceptable doubt
         */
        public BasicAdapter (SystemInfo system,
                             double     maxDoubt)
        {
            super(system, maxDoubt);
        }

        //~ Methods ------------------------------------------------------------

        @Implement(CompoundAdapter.class)
        public boolean isCandidateSuitable (Glyph glyph)
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
                   (glyph.getDoubt() >= GlyphInspector.getMinCompoundPartDoubt()))));
        }

        @Implement(CompoundAdapter.class)
        public boolean isCompoundValid (Glyph compound)
        {
            Evaluation eval = GlyphNetwork.getInstance()
                                          .vote(compound, maxDoubt);

            if ((eval != null) &&
                eval.shape.isWellKnown() &&
                (eval.shape != Shape.CLUTTER) &&
                (!seed.isKnown() || (eval.doubt < seed.getDoubt()))) {
                chosenEvaluation = eval;

                return true;
            } else {

                return false;
            }
        }

        @Implement(CompoundAdapter.class)
        public PixelRectangle getIntersectionBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                    "Compound seed has not been set");
            }

            PixelRectangle box = new PixelRectangle(seed.getContourBox());
            Scale          scale = system.getScoreSystem()
                                         .getScale();
            int            boxWiden = scale.toPixels(
                GlyphInspector.constants.boxWiden);
            box.grow(boxWiden, boxWiden);

            return box;
        }
    }

    //-------------//
    // BassAdapter //
    //-------------//
    private class BassAdapter
        extends CompoundBuilder.TopShapeAdapter
    {
        //~ Constructors -------------------------------------------------------

        public BassAdapter (SystemInfo system,
                            double     maxDoubt)
        {
            super(system, maxDoubt, ShapeRange.BassClefs);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape() ||
                   ShapeRange.BassClefs.contains(glyph.getShape());
        }

        @Override
        public PixelRectangle getIntersectionBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                    "Compound seed has not been set");
            }

            Scale          scale = system.getScoreSystem()
                                         .getScale();
            PixelRectangle pixRect = new PixelRectangle(seed.getCentroid());
            pixRect.add(
                new PixelPoint(
                    pixRect.x - scale.toPixels(new InterlineFraction(2)),
                    pixRect.y + scale.toPixels(new InterlineFraction(3))));

            return pixRect;
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
            0.3,
            "Box widening to check intersection with compound");
        Evaluation.Doubt alterMaxDoubt = new Evaluation.Doubt(
            6,
            "Maximum doubt for alteration sign verification");
        Evaluation.Doubt patternsMaxDoubt = new Evaluation.Doubt(
            1.5,
            "Maximum doubt for cleanup phase");
        Evaluation.Doubt leafMaxDoubt = new Evaluation.Doubt(
            1.3,
            "Maximum acceptance doubt for a leaf");
        Evaluation.Doubt symbolMaxDoubt = new Evaluation.Doubt(
            1.2,
            "Maximum doubt for a symbol");
        Evaluation.Doubt textMaxDoubt = new Evaluation.Doubt(
            3.0,
            "Maximum doubt for a text symbol");
        Evaluation.Doubt minCompoundPartDoubt = new Evaluation.Doubt(
            1.5,
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
            "Half width of a clef");
        Scale.Fraction   maxBassDotDx = new Scale.Fraction(
            0.25,
            "Tolerance on Bass dot abscissae");
        Constant.Double  maxBassDotPitchDy = new Constant.Double(
            "pitch",
            0.5,
            "Ordinate tolerance on a Bass dot pitch position");
        Evaluation.Doubt clefMaxDoubt = new Evaluation.Doubt(
            3d,
            "Maximum doubt for clef verification");
        Evaluation.Doubt hookMaxDoubt = new Evaluation.Doubt(
            5d,
            "Maximum doubt for beam hook verification");
    }
}
