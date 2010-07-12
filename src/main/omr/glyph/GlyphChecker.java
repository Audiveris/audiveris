//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h C h e c k e r                           //
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
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;

import java.util.*;

/**
 * Class <code>GlyphChecker</code> gathers additional specific glyph checks,
 * still working on symbols in isolation from other symbols, meant to complement
 * the work done by an evaluator (neural network evaluator or regression
 * evaluator).
 *
 * <p>Checks are made on the glyph only, the only knowledge about current glyph
 * environment being its staff-based pitch position and the attached stems and
 * ledgers.
 *
 * <p>Check made in relation with other symbols are not handled here (because
 * the other symbols may not have been recognized yet). Such more elaborated
 * checks are the purpose of the patterns step with 
 * {@link omr.glyph.pattern.PatternsChecker}.
 *
 * @author Hervé Bitteur
 */
public class GlyphChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphChecker.class);

    /** Singleton */
    private static GlyphChecker INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Map of Shape => Sequence of checkers */
    private final EnumMap<Shape, Collection<Checker>> checkerMap;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // GlyphChecker //
    //-------------//
    private GlyphChecker ()
    {
        checkerMap = new EnumMap<Shape, Collection<Checker>>(Shape.class);
        registerChecks();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static GlyphChecker getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphChecker();
        }

        return INSTANCE;
    }

    //---------------//
    // specificCheck //
    //---------------//
    /**
     * Run a check on the provided glyph, according to the candidate shape
     * @param shape the shape found by the evaluator
     * @param glyph the glyph to check for a shape
     * @param features the glyph features
     * @return the inferred shape if the shape is confirmed, null otherwise
     */
    public Shape specificCheck (Shape    shape,
                                Glyph    glyph,
                                double[] features)
    {
        if (!constants.applySpecificCheck.getValue()) {
            return shape;
        }

        Collection<Checker> checks = checkerMap.get(shape);

        if (checks == null) {
            return shape;
        }

        Shape res = null;

        for (Checker checker : checks) {
            res = checker.check(shape, glyph, features);

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
    private void addChecker (Checker  checker,
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
    private void addChecker (Checker       checker,
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
    private void registerChecks ()
    {
        // General constraint check on weight, width, height
        new Checker(allSymbols) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // Apply registered parameters constraints
                    if (!constants.applyConstraintsCheck.getValue() ||
                        GlyphRegression.getInstance()
                                       .constraintsMatched(glyph, shape)) {
                        return shape;
                    } else {
                        return null;
                    }
                }
            };

        new Checker(WHOLE_OR_HALF_REST) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // Must be within staff height
                    if (Math.abs(glyph.getPitchPosition()) >= 4) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(BEAM_HOOK) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    if (glyph.getStemNumber() < 1) {
                        return null;
                    } else {
                        return shape;
                    }
                }
            };

        new Checker(TEXT, CHARACTER) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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
                    Collections.sort(glyphs, Glyph.globalComparator);

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
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
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

        new Checker(Notes, NoteHeads, Rests, HeadAndFlags) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // A note / rest cannot be too far from a staff
                    if (Math.abs(glyph.getPitchPosition()) >= 15) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(Pedals) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // Pedal marks must be below the staff
                    if (glyph.getPitchPosition() <= 4) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(Tuplets) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // Tuplets cannot be too far from a staff
                    if (Math.abs(glyph.getPitchPosition()) > constants.maxTupletPitchPosition.getValue()) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(LONG_REST) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // Must be centered on pitch position 0
                    if (Math.abs(glyph.getPitchPosition()) > 0.5) {
                        return null;
                    }

                    return shape;
                }
            };

        new Checker(BREVE_REST) {
                public Shape check (Shape    shape,
                                    Glyph    glyph,
                                    double[] features)
                {
                    // Must be centered on pitch position -1
                    if (Math.abs(glyph.getPitchPosition() + 1) > 0.5) {
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
    private abstract class Checker
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
         * @param features the glyph features
         * @return the inferred shape, which is null for negative test
         */
        public abstract Shape check (Shape    shape,
                                     Glyph    glyph,
                                     double[] features);
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean applySpecificCheck = new Constant.Boolean(
            true,
            "Should we apply specific checks on shape candidates?");
        Constant.Boolean applyConstraintsCheck = new Constant.Boolean(
            true,
            "Should we apply constraints checks on shape candidates?");
        Scale.Fraction   maxTitleHeight = new Scale.Fraction(
            4d,
            "Maximum normalized height for a title text");
        Scale.Fraction   maxLyricsHeight = new Scale.Fraction(
            2.5d,
            "Maximum normalized height for a lyrics text");
        Constant.Double  minTitlePitchPosition = new Constant.Double(
            "PitchPosition",
            15d,
            "Minimum absolute pitch position for a title");
        Constant.Double  maxTupletPitchPosition = new Constant.Double(
            "PitchPosition",
            15d,
            "Minimum absolute pitch position for a tuplet");
        Constant.Double  maxTimePitchPositionMargin = new Constant.Double(
            "PitchPosition",
            1d,
            "Maximum absolute pitch position margin for a time signature");
        Scale.Fraction   maxTextGap = new Scale.Fraction(
            5.0,
            "Maximum value for a horizontal gap between glyphs of the same text");
    }
}
