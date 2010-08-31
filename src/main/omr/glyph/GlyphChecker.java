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
import omr.glyph.text.Language;
import omr.glyph.text.OcrLine;

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
     * @param eval the evaluation to populate
     * @param glyph the glyph to check for a shape
     * @param features the glyph features
     */
    public void specificCheck (Evaluation eval,
                               Glyph      glyph,
                               double[]   features)
    {
        if (!constants.applySpecificCheck.getValue()) {
            return;
        }

        Collection<Checker> checks = checkerMap.get(eval.shape);

        if (checks == null) {
            return;
        }

        for (Checker checker : checks) {
            if (!(checker.check(eval, glyph, features))) {
                if (eval.failure != null) {
                    eval.failure = new Evaluation.Failure(
                        checker.name + ":" + eval.failure);
                } else {
                    eval.failure = new Evaluation.Failure(checker.name);
                }

                return;
            }
        }
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
     * Add a checker to a series of shape ranges
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
        new Checker("Constraint", allSymbols) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Apply registered parameters constraints
                    return !constants.applyConstraintsCheck.getValue() ||
                           GlyphRegression.getInstance()
                                          .constraintsMatched(features, eval);
                }
            };

        new Checker("MeasureRest", WHOLE_OR_HALF_REST) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    int pp = (int) Math.rint(2 * glyph.getPitchPosition());

                    if (pp == -1) {
                        eval.shape = Shape.HALF_REST;

                        return true;
                    } else if (pp == -3) {
                        eval.shape = Shape.WHOLE_REST;

                        return true;
                    } else {
                        return false;
                    }
                }
            };

        new Checker("Clefs", Clefs) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Must be within staff height
                    return Math.abs(glyph.getPitchPosition()) < 4;
                }
            };

        new Checker("Hook", BEAM_HOOK) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Check we have exactly 1 stem
                    if (glyph.getStemNumber() != 1) {
                        eval.failure = new Evaluation.Failure("stem!=1");

                        return false;
                    }

                    if (!validBeamHookSlope(glyph)) {
                        eval.failure = new Evaluation.Failure("slope");

                        return false;
                    }

                    return true;
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
        new Checker("noLeftStem", HeadAndFlags) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    return glyph.getLeftStem() != null;
                }
            };

        // Shapes that require a stem nearby
        new Checker("noStem", StemSymbols) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    return glyph.getStemNumber() >= 1;
                }
            };

        new Checker("Text", TEXT, CHARACTER) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Check reasonable height (Cannot be too tall when close to staff)
                    double maxHeight = (Math.abs(glyph.getPitchPosition()) >= constants.minTitlePitchPosition.getValue())
                                       ? constants.maxTitleHeight.getValue()
                                       : constants.maxLyricsHeight.getValue();

                    if (glyph.getNormalizedHeight() >= maxHeight) {
                        eval.failure = new Evaluation.Failure("tooHigh");

                        return false;
                    }

                    // Check there is no huge horizontal gap between parts
                    if (hugeGapBetweenParts(glyph)) {
                        eval.failure = new Evaluation.Failure("gaps");

                        return false;
                    }

                    return true;
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

        new Checker("FullTS", FullTimes) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    double absPos = Math.abs(glyph.getPitchPosition());
                    double maxDy = constants.maxTimePitchPositionMargin.getValue();

                    // A full time shape must be on 0 position
                    if (absPos > maxDy) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    // Total height for a complete time sig is staff height
                    if (glyph.getNormalizedHeight() > 4.5) {
                        eval.failure = new Evaluation.Failure("tooHigh");

                        return false;
                    }

                    return true;
                }
            };

        new Checker("PartialTS", PartialTimes) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    double absPos = Math.abs(glyph.getPitchPosition());
                    double maxDy = constants.maxTimePitchPositionMargin.getValue();

                    // A partial time shape must be on -2 or +2 positions
                    if (Math.abs(absPos - 2) > maxDy) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    return true;
                }
            };

        new Checker("Dynamics", Dynamics) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Check distance from closest staff
                    if (Math.abs(glyph.getPitchPosition()) >= 15) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    // Limit width
                    if (glyph.getNormalizedWidth() >= 12) {
                        eval.failure = new Evaluation.Failure("tooWide");

                        return false;
                    }

                    return true;
                }
            };

        new Checker("StaffDist", Notes, NoteHeads, Rests, HeadAndFlags) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // A note / rest cannot be too far from a staff
                    return Math.abs(glyph.getPitchPosition()) < 15;
                }
            };

        new Checker("belowStaff", Pedals) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Pedal marks must be below the staff
                    return glyph.getPitchPosition() > 4;
                }
            };

        new Checker("Tuplet", Tuplets) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Tuplets cannot be too far from a staff
                    if (Math.abs(glyph.getPitchPosition()) > constants.maxTupletPitchPosition.getValue()) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    // Simply check the tuplet character via OCR
                    List<OcrLine> lines = Language.getOcr()
                                                  .recognize(
                        glyph.getImage(),
                        null,
                        "g" + glyph.getId() + ".");

                    if ((lines != null) && !lines.isEmpty()) {
                        String str = lines.get(0).value;
                        Shape  shape = eval.shape;

                        if ((shape == TUPLET_THREE) && str.equals("3")) {
                            return true;
                        }

                        if ((shape == TUPLET_SIX) && str.equals("6")) {
                            return true;
                        }
                    }

                    eval.failure = new Evaluation.Failure("ocr");

                    return false;
                }
            };

        new Checker("Long", LONG_REST) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Must be centered on pitch position 0
                    if (Math.abs(glyph.getPitchPosition()) > 0.5) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    return true;
                }
            };

        new Checker("Breve", BREVE_REST) {
                public boolean check (Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Must be centered on pitch position -1
                    if (Math.abs(glyph.getPitchPosition() + 1) > 0.5) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    return true;
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
        //~ Instance fields ----------------------------------------------------

        /** Unique name for this check */
        public final String name;

        //~ Constructors -------------------------------------------------------

        public Checker (String   name,
                        Shape... shapes)
        {
            this.name = name;
            addChecker(this, shapes);
        }

        public Checker (String         name,
                        EnumSet<Shape> shapes)
        {
            this.name = name;
            addChecker(this, shapes.toArray(new Shape[0]));
        }

        public Checker (String        name,
                        ShapeRange... shapeRanges)
        {
            this.name = name;
            addChecker(this, shapeRanges);
        }

        //~ Methods ------------------------------------------------------------

        /**
         * Run the specific test
         * @param eval the partially-filled evaluation
         * @param glyph the glyph at hand
         * @param features the glyph features
         * @return true if OK, false otherwise
         */
        public abstract boolean check (Evaluation eval,
                                       Glyph      glyph,
                                       double[]   features);
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
