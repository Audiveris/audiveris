//----------------------------------------------------------------------------//
//                                                                            //
//                           G l y p h C h e c k s                            //
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
import static omr.glyph.Shape.*;
import static omr.glyph.ShapeRange.*;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;

import omr.stick.Stick;

import java.util.*;

/**
 * Class <code>GlyphChecks</code> gathers additional specific glyph checks,
 * meant to complement the work done by the neural network evaluator
 *
 * @author Hervé Bitteur
 */
public class GlyphChecks
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphChecks.class);

    /** Map of Shape => Sequence of checkers */
    private static final Map<Shape, Collection<Checker>> checkerMap = new TreeMap<Shape, Collection<Checker>>();

    static {
        registerChecks();
    }

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // GlyphChecks // Not meant to be instantiated
    //-------------//
    private GlyphChecks ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // specificCheck //
    //---------------//
    /**
     * Run a check on the provided glyph, according to the candidate shape
     * @param shape the shape found by the evaluator
     * @param glyph the glyph to check for a shape
     * @return the inferred shape if the shape is confirmed, null otherwise
     */
    public static Shape specificCheck (Shape shape,
                                       Glyph glyph)
    {
        Collection<Checker> checks = checkerMap.get(shape);

        if (checks == null) {
            return shape;
        }

        Shape res = null;

        for (Checker checker : checks) {
            res = checker.check(shape, glyph);

            if (res == null) {
                return null;
            }
        }

        return res;
    }

    //------------//
    // addChecker //
    //------------//
    /**
     * Add a checker to a series of shapes
     * @param checker the checker to add
     * @param shapes the shape(s) to which the check applies
     */
    private static void addChecker (Checker  checker,
                                    Shape... shapes)
    {
        for (Shape shape : shapes) {
            Collection<Checker> checks = checkerMap.get(shape);

            if (checks == null) {
                checks = new ArrayList<Checker>();
                checkerMap.put(shape, checks);
            }

            checks.add(checker);
        }
    }

    //------------//
    // addChecker //
    //------------//
    /**
     * Add a checker to a series os shape ranges
     * @param checker the checker to add
     * @param shapeRanges the shape range(s) to which the check applies
     */
    private static void addChecker (Checker       checker,
                                    ShapeRange... shapeRanges)
    {
        for (ShapeRange range : shapeRanges) {
            addChecker(checker, range.getShapes().toArray(new Shape[0]));
        }
    }

    //----------------//
    // registerChecks //
    //----------------//
    /**
     * Populate the checkers map
     */
    private static void registerChecks ()
    {
        new Checker(WHOLE_OR_HALF_REST) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    int pp = (int) Math.rint(2 * glyph.getPitchPosition());

                    if (pp == -1) {
                        return Shape.HALF_REST;
                    } else if (pp == -3) {
                        return Shape.WHOLE_REST;
                    } else {
                        return null;
                    }
                }
            };

        new Checker(Clefs) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    // Check reasonable height
                    if (glyph.getNormalizedHeight() > constants.maxClefHeight.getValue()) {
                        return null;
                    }

                    // Check distance from closest staff
                    if (Math.abs(glyph.getPitchPosition()) >= 15) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(BEAM_HOOK) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                { // Check we have exactly 1 stem

                    if (glyph.getStemNumber() != 1) {
                        return null;
                    }

                    if (!validBeamHookSlope(glyph)) {
                        return null;
                    }

                    return shape;
                }

                /**
                 * Check if the candidate glyph can be a beam hook with a
                 * reasonable slope
                 * @param glyph the candidate
                 * @return true if glyph slope is reasonable
                 */
                private boolean validBeamHookSlope (Glyph glyph)
                {
                    try {
                        Stick          stick = (Stick) glyph;
                        double         slope = stick.getLine()
                                                    .getInvertedSlope(); // vertical lag!

                        PixelRectangle box = glyph.getContourBox();
                        double         maxSlope = (double) box.height / (double) box.width;

                        return Math.abs(slope) <= maxSlope;
                    } catch (Exception ignored) {
                        return false;
                    }
                }
            };

        // Shapes that require a stem on the left side
        new Checker(HeadAndFlags) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    if (glyph.getLeftStem() == null) {
                        return null;
                    } else {
                        return shape;
                    }
                }
            };

        // Shapes that require a stem nearby
        new Checker(StemSymbols) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    if (glyph.getStemNumber() < 1) {
                        return null;
                    } else {
                        return shape;
                    }
                }
            };

        new Checker(TEXT, CHARACTER) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    // Check reasonable height (Cannot be too tall when close to staff)
                    double maxHeight = (Math.abs(glyph.getPitchPosition()) >= constants.minTitlePitchPosition.getValue())
                                       ? constants.maxTitleHeight.getValue()
                                       : constants.maxLyricsHeight.getValue();

                    if (glyph.getNormalizedHeight() >= maxHeight) {
                        return null;
                    }

                    // Check there is no huge horizontal gap between parts
                    if (hugeGapBetweenParts(glyph)) {
                        return null;
                    }

                    return shape;
                }

                /**
                 * Browse the collection of provided glyphs to make sure there
                 * is no huge horizontal gap included
                 * @param glyphs the collection of glyphs that compose the text
                 * candidate
                 * @param sheet needed for scale of the context
                 * @return true if gap found
                 */
                private boolean hugeGapBetweenParts (Glyph compound)
                {
                    if (compound.getParts()
                                .isEmpty()) {
                        return false;
                    }

                    // Sort glyphs by abscissa
                    List<Glyph> glyphs = new ArrayList<Glyph>(
                        compound.getParts());
                    Collections.sort(glyphs, Glyphs.globalComparator);

                    final Scale scale = new Scale(glyphs.get(0).getInterline());
                    final int   maxGap = scale.toPixels(constants.maxTextGap);
                    int         gapStart = 0;
                    Glyph       prev = null;

                    for (Glyph glyph : glyphs) {
                        PixelRectangle box = glyph.getContourBox();

                        if (prev != null) {
                            if ((box.x - gapStart) > maxGap) {
                                if (logger.isFineEnabled()) {
                                    logger.fine(
                                        "huge gap detected between glyphs #" +
                                        prev.getId() + " & " + glyph.getId());
                                }

                                return true;
                            }
                        }

                        prev = glyph;
                        gapStart = (box.x + box.width) - 1;
                    }

                    return false;
                }
            };

        new Checker(FullTimes) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    double absPos = Math.abs(glyph.getPitchPosition());
                    double maxDy = constants.maxTimePitchPositionMargin.getValue();

                    // A full time shape must be on 0 position
                    if (absPos > maxDy) {
                        return null;
                    }

                    // Total height for a complete time sig is staff height
                    if (glyph.getNormalizedHeight() > 4.5) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(PartialTimes) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    double absPos = Math.abs(glyph.getPitchPosition());
                    double maxDy = constants.maxTimePitchPositionMargin.getValue();

                    // A partial time shape must be on -2 or +2 positions
                    if (Math.abs(absPos - 2) > maxDy) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(Dynamics) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    // Check distance from closest staff
                    if (Math.abs(glyph.getPitchPosition()) >= 15) {
                        return null;
                    }

                    // Limit width
                    if (glyph.getNormalizedWidth() >= 12) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(NOTEHEAD_BLACK) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    if (glyph.getNormalizedWeight() > constants.maxHeadBlackWeight.getValue()) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(Notes, NoteHeads, Rests, HeadAndFlags) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    // A note / rest cannot be too far from a staff
                    if (Math.abs(glyph.getPitchPosition()) >= 15) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(Pedals) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    // Pedal marks must be below the staff
                    if (glyph.getPitchPosition() <= 4) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(Tuplets) {
                public Shape check (Shape shape,
                                    Glyph glyph)
                {
                    // Tuplets cannot be too far from a staff
                    if (Math.abs(glyph.getPitchPosition()) > constants.maxTupletPitchPosition.getValue()) {
                        return null;
                    }

                    return shape;
                }
            };
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Checker //
    //---------//
    /**
     * A checker runs a specific check for a given glyph wrt a collection of
     * potential shapes
     */
    private abstract static class Checker
    {
        //~ Constructors -------------------------------------------------------

        public Checker (Shape... shapes)
        {
            addChecker(this, shapes);
        }

        public Checker (EnumSet<Shape> shapes)
        {
            addChecker(this, shapes.toArray(new Shape[0]));
        }

        public Checker (ShapeRange... shapeRanges)
        {
            addChecker(this, shapeRanges);
        }

        //~ Methods ------------------------------------------------------------

        /**
         * Run the specific test
         * @param shape the potential shape
         * @param glyph the glyph at hand
         * @return the inferred shape, which is null for negative test
         */
        public abstract Shape check (Shape shape,
                                     Glyph glyph);
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.AreaFraction maxHeadBlackWeight = new Scale.AreaFraction(
            1.2,
            "Maximum normalized weight for a NOTEHEAD_BLACK");
        Scale.Fraction     maxClefHeight = new Scale.Fraction(
            9d,
            "Maximum normalized height for a clef");
        Scale.Fraction     maxTitleHeight = new Scale.Fraction(
            4d,
            "Maximum normalized height for a title text");
        Scale.Fraction     maxLyricsHeight = new Scale.Fraction(
            2.5d,
            "Maximum normalized height for a lyrics text");
        Constant.Double    minTitlePitchPosition = new Constant.Double(
            "PitchPosition",
            15d,
            "Minimum absolute pitch position for a title");
        Constant.Double    maxTupletPitchPosition = new Constant.Double(
            "PitchPosition",
            15d,
            "Minimum absolute pitch position for a tuplet");
        Constant.Double    maxTimePitchPositionMargin = new Constant.Double(
            "PitchPosition",
            1d,
            "Maximum absolute pitch position margin for a time signature");
        Scale.Fraction     maxTextGap = new Scale.Fraction(
            5.0,
            "Maximum value for a horizontal gap between glyphs of the same text");
    }
}
