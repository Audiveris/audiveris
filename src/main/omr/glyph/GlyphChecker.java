//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h C h e c k e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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
import omr.glyph.text.Language;
import omr.glyph.text.OcrLine;
import omr.glyph.text.TextInfo;
import omr.glyph.text.TextLine;

import omr.grid.StaffInfo;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Clef;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sheet.Ledger;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

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

    //--------------//
    // GlyphChecker //
    //--------------//
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

    //----------//
    // annotate //
    //----------//
    /**
     * Run a series of checks on the provided glyph, according to the candidate
     * shape, and annotate the evaluation accordingly
     * @param system the containing system
     * @param eval the evaluation to populate
     * @param glyph the glyph to check for a shape
     * @param features the glyph features
     */
    public void annotate (SystemInfo system,
                          Evaluation eval,
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
            if (!(checker.check(system, eval, glyph, features))) {
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

    //-------//
    // relax //
    //-------//
    /**
     * Take into account the fact that the provided glyph has been
     * (certainly manually) assigned the provided shape. So update the tests
     * internals accordingly.
     * @param shape the assigned shape
     * @param glyph the glyph at hand
     * @param features the glyph features
     */
    public void relax (Shape    shape,
                       Glyph    glyph,
                       double[] features)
    {
        Collection<Checker> checks = checkerMap.get(shape);

        if (checks == null) {
            return;
        }

        for (Checker checker : checks) {
            checker.relax(shape, glyph, features);
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
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    if (!constants.applyConstraintsCheck.getValue()) {
                        return true;
                    }

                    // Apply registered parameters constraints
                    return GlyphRegression.getInstance()
                                          .constraintsMatched(features, eval);
                }

                @Override
                public void relax (Shape    shape,
                                   Glyph    glyph,
                                   double[] features)
                {
                    // Here relax the constraints if so needed
                    boolean widened = GlyphRegression.getInstance()
                                                     .includeSample(
                        features,
                        shape);
                    logger.info(
                        "Constraints " + (widened ? "widened" : "included") +
                        " for glyph#" + glyph.getId() + " as " + shape);
                }
            };

        new Checker("NotWithinWidth", allSymbols) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // They must be within the abscissa bounds of the system
                    // Except a few shapes
                    Shape shape = eval.shape;

                    if ((shape == BRACKET) ||
                        (shape == BRACE) ||
                        (shape == TEXT) ||
                        (shape == CHARACTER)) {
                        return true;
                    }

                    PixelRectangle glyphBox = glyph.getContourBox();

                    if (((glyphBox.x + glyphBox.width) < system.getLeft()) ||
                        (glyphBox.x > system.getRight())) {
                        return false;
                    }

                    return true;
                }
            };

        new Checker("MeasureRest", WHOLE_OR_HALF_REST) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
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
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }
                }
            };

        new Checker("NotWithinStaffHeight", Clefs) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Must be within staff height
                    return Math.abs(glyph.getPitchPosition()) < 4;
                }
            };

        new Checker("WithinStaffHeight", Dynamics) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Must be outside staff height
                    return Math.abs(glyph.getPitchPosition()) > 4;
                }
            };

        new Checker("TooFarFromLeftBar", Keys) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // They must be rather close to the left side of the measure
                    ScoreSystem    scoreSystem = system.getScoreSystem();
                    Scale          scale = scoreSystem.getScale();
                    double         maxKeyXOffset = scale.toPixels(
                        constants.maxKeyXOffset);
                    PixelRectangle box = glyph.getContourBox();
                    PixelPoint     point = box.getLocation();
                    SystemPart     part = scoreSystem.getPartAt(point);
                    Measure        measure = part.getMeasureAt(point);
                    Barline        insideBar = measure.getInsideBarline();
                    Staff          staff = part.getStaffAt(point);
                    Clef           clef = measure.getFirstMeasureClef(
                        staff.getId());
                    int            start = (clef != null)
                                           ? (clef.getBox().x +
                                           clef.getBox().width)
                                           : ((insideBar != null)
                                              ? insideBar.getLeftX()
                                              : measure.getLeftX());

                    return (point.x - start) <= maxKeyXOffset;
                }
            };

        new Checker("CommonCutTime", COMMON_TIME) {
                private Predicate<Glyph> stemPredicate = new Predicate<Glyph>() {
                    public boolean check (Glyph entity)
                    {
                        return entity.getShape() == Shape.COMBINING_STEM;
                    }
                };

                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // COMMON_TIME shape is easily confused with CUT_TIME
                    // Check presence of a "pseudo-stem"
                    PixelRectangle box = glyph.getContourBox();
                    box.grow(-box.width / 4, 0);

                    List<Glyph> neighbors = system.lookupIntersectedGlyphs(
                        box,
                        glyph);

                    if (Glyphs.contains(neighbors, stemPredicate)) {
                        eval.shape = Shape.CUT_TIME;
                    }

                    return true;
                }
            };

        new Checker("Hook", BEAM_HOOK) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Check we have exactly 1 stem
                    if (glyph.getStemNumber() != 1) {
                        eval.failure = new Evaluation.Failure("stem!=1");

                        return false;
                    }

                    // Hook slope is not reliable, so this test is disabled
                    //                    if (!validBeamHookSlope(glyph)) {
                    //                        eval.failure = new Evaluation.Failure("slope");
                    //
                    //                        return false;
                    //                    }
                    return true;
                }

                //                /**
                //                 * Check if the candidate glyph can be a beam hook with a
                //                 * reasonable slope
                //                 * @param glyph the candidate
                //                 * @return true if glyph slope is reasonable
                //                 */
                //                private boolean validBeamHookSlope (Glyph glyph)
                //                {
                //                    try {
                //                        Stick          stick = (Stick) glyph;
                //                        double         slope = stick.getAbsoluteLine()
                //                                                    .getSlope();
                //
                //                        PixelRectangle box = glyph.getContourBox();
                //                        double         maxSlope = (double) box.height / (double) box.width;
                //
                //                        return Math.abs(slope) <= maxSlope;
                //                    } catch (Exception ignored) {
                //                        return false;
                //                    }
                //                }
            };

        // Shapes that require a stem on the left side
        new Checker("noLeftStem", Flags) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    return glyph.getLeftStem() != null;
                }
            };

        // Shapes that require a stem nearby
        new Checker("noStem", StemSymbols) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    return glyph.getStemNumber() >= 1;
                }
            };

        new Checker("Text", TEXT, CHARACTER) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
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

//                    // Check there is no huge horizontal gap between parts
//                    if (hugeGapBetweenParts(glyph)) {
//                        eval.failure = new Evaluation.Failure("gaps");
//
//                        return false;
//                    }

                    return true;
                }

//                /**
//                 * Browse the collection of provided glyphs to make sure there
//                 * is no huge horizontal gap included
//                 * @param glyphs the collection of glyphs that compose the text
//                 * candidate
//                 * @param sheet needed for scale of the context
//                 * @return true if gap found
//                 */
//                private boolean hugeGapBetweenParts (Glyph compound)
//                {
//                    if (compound.getParts()
//                                .isEmpty()) {
//                        return false;
//                    }
//
//                    // Sort glyphs by abscissa
//                    List<Glyph> glyphs = new ArrayList<Glyph>(
//                        compound.getParts());
//                    Collections.sort(glyphs, Glyph.abscissaComparator);
//
//                    final Scale scale = new Scale(glyphs.get(0).getInterline());
//                    final int   maxGap = scale.toPixels(constants.maxTextGap);
//                    int         gapStart = 0;
//                    Glyph       prev = null;
//
//                    for (Glyph glyph : glyphs) {
//                        PixelRectangle box = glyph.getContourBox();
//
//                        if (prev != null) {
//                            if ((box.x - gapStart) > maxGap) {
//                                if (logger.isFineEnabled()) {
//                                    logger.fine(
//                                        "huge gap detected between glyphs #" +
//                                        prev.getId() + " & " + glyph.getId());
//                                }
//
//                                return true;
//                            }
//                        }
//
//                        prev = glyph;
//                        gapStart = (box.x + box.width) - 1;
//                    }
//
//                    return false;
//                }
            };

        new Checker("FullTimeSig", FullTimes) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
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

        new Checker("PartialTimeSig", PartialTimes) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
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

        new Checker("StaffDist", Notes, NoteHeads, Rests, Dynamics) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // A note / rest / dynamic cannot be too far from a staff
                    return Math.abs(glyph.getPitchPosition()) < 15;
                }
            };

        new Checker("BelowStaff", Pedals) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Pedal marks must be below the staff
                    return glyph.getPitchPosition() > 4;
                }
            };

        new Checker("Tuplet", Tuplets) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Tuplets cannot be too far from a staff
                    if (Math.abs(glyph.getPitchPosition()) > constants.maxTupletPitchPosition.getValue()) {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }

                    // Simply check the tuplet character via OCR, if available
                    // Nota: We must avoid multiple OCR calls on the same glyph
                    if (TextLine.useOCR()) {
                        if (glyph.isTransient()) {
                            glyph = system.registerGlyph(glyph);
                        }

                        TextInfo textInfo = glyph.getTextInfo();
                        OcrLine  line = null;

                        if (textInfo.getOcrContent() == null) {
                            List<OcrLine> lines = Language.getOcr()
                                                          .recognize(
                                glyph.getImage(),
                                null,
                                "g" + glyph.getId() + ".");

                            ///OCR logger.warning("GlyphChecker OCR " + glyph + " " + lines);
                            if ((lines != null) && !lines.isEmpty()) {
                                line = lines.get(0);
                                textInfo.setOcrInfo(
                                    Language.getDefaultLanguage(),
                                    line);
                            }
                        }

                        line = textInfo.getOcrLine();

                        String str = line.value;
                        Shape  shape = eval.shape;

                        if ((shape == TUPLET_THREE) && str.equals("3")) {
                            return true;
                        }

                        if ((shape == TUPLET_SIX) && str.equals("6")) {
                            return true;
                        }

                        eval.failure = new Evaluation.Failure("ocr");

                        return false;
                    }

                    return true;
                }
            };

        new Checker("LongRest", LONG_REST) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
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
                public boolean check (SystemInfo system,
                                      Evaluation eval,
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

        new Checker("Braces", BRACE, BRACKET) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Must be centered on left of part barline
                    PixelRectangle box = glyph.getContourBox();
                    PixelPoint     left = new PixelPoint(
                        box.x,
                        box.y + (box.height / 2));

                    if (left.x > system.getLeft()) {
                        eval.failure = new Evaluation.Failure("notOnLeft");

                        return false;
                    }

                    // Make sure at least a staff interval is embraced
                    boolean embraced = false;
                    int     intervalTop = Integer.MIN_VALUE;

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
                        eval.failure = new Evaluation.Failure(
                            "noStaffEmbraced");

                        return false;
                    }

                    return true;
                }
            };

        new Checker("WholeSansLedgers", WHOLE_NOTE) {
                public boolean check (SystemInfo system,
                                      Evaluation eval,
                                      Glyph      glyph,
                                      double[]   features)
                {
                    // Check that whole notes are not too far from staves
                    // without ledgers
                    PixelPoint point = glyph.getAreaCenter();
                    StaffInfo  staff = system.getStaffAt(point);
                    double     pitch = staff.pitchPositionOf(point);

                    if (Math.abs(pitch) <= 6) {
                        return true;
                    }

                    Ledger ledger = staff.getClosestLedger(point);

                    return ledger != null;
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
         * @param eval the partially-filled evaluation (eval.shape is an input,
         * eval.grade and eval.failure are outputs)
         * @param glyph the glyph at hand
         * @param features the glyph features
         * @return true if OK, false otherwise
         */
        public abstract boolean check (SystemInfo system,
                                       Evaluation eval,
                                       Glyph      glyph,
                                       double[]   features);

        /**
         * Take into account the fact that the provided glyph has been
         * (certainly manually) assigned the provided shape. So update the test
         * internals accordingly.
         * @param shape the assigned shape
         * @param glyph the glyph at hand
         * @param features the glyph features
         */
        public void relax (Shape    shape,
                           Glyph    glyph,
                           double[] features)
        {
            // Void by default
        }
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
        Scale.Fraction   maxKeyXOffset = new Scale.Fraction(
            2,
            "Maximum horizontal offset for a key since clef or measure start");
    }
}
