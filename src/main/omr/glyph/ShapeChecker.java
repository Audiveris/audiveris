//----------------------------------------------------------------------------//
//                                                                            //
//                          S h a p e C h e c k e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.glyph.Shape.*;
import static omr.glyph.ShapeSet.*;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.log.Logger;

import omr.run.Orientation;

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
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import java.util.*;

/**
 * Class {@code ShapeChecker} gathers additional specific shape checks,
 * still working on symbols in isolation from other symbols, meant to
 * complement the work done by a shape evaluator.
 *
 * <p>Typically, physical shapes (the *_set shape names) must be mapped to
 * the right logical shapes using proper additional tests.</p>
 *
 * <p>Checks are made on the glyph only, the only knowledge about current glyph
 * environment being its staff-based pitch position and the attached stems and
 * ledgers.</p>
 *
 * <p>Checks made in relation with other symbols are not handled here (because
 * the other symbols may not have been recognized yet). Such more elaborated
 * checks are the purpose of {@link omr.glyph.pattern.PatternsChecker}.</p>
 *
 * @author Hervé Bitteur
 */
public class ShapeChecker
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ShapeChecker.class);

    /** Singleton */
    private static ShapeChecker INSTANCE;

    /** Small dynamics with no 'P' or 'F' */
    private static final EnumSet<Shape> SmallDynamics = EnumSet.copyOf(
            shapesOf(
            DYNAMICS_CHAR_M,
            DYNAMICS_CHAR_R,
            DYNAMICS_CHAR_S,
            DYNAMICS_CHAR_Z));

    /** Medium dynamics with a 'P' (but no 'F') */
    private static final EnumSet<Shape> MediumDynamics = EnumSet.copyOf(
            shapesOf(DYNAMICS_MP, DYNAMICS_P, DYNAMICS_PP, DYNAMICS_PPP));

    /** Tall dynamics with an 'F' */
    private static final EnumSet<Shape> TallDynamics = EnumSet.copyOf(
            shapesOf(
            DYNAMICS_F,
            DYNAMICS_FF,
            DYNAMICS_FFF,
            DYNAMICS_FP,
            DYNAMICS_FZ,
            DYNAMICS_MF,
            DYNAMICS_RF,
            DYNAMICS_RFZ,
            DYNAMICS_SF,
            DYNAMICS_SFFZ,
            DYNAMICS_SFP,
            DYNAMICS_SFPP,
            DYNAMICS_SFZ));

    //~ Instance fields --------------------------------------------------------
    /** Map of Shape => Sequence of checkers */
    private final EnumMap<Shape, Collection<Checker>> checkerMap;

    /** Checker that can be used on its own. */
    private Checker stemChecker;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // ShapeChecker //
    //--------------//
    private ShapeChecker ()
    {
        checkerMap = new EnumMap<>(Shape.class);
        registerChecks();
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    public static ShapeChecker getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new ShapeChecker();
        }

        return INSTANCE;
    }

    //-----------//
    // checkStem //
    //-----------//
    /**
     * Basic check for a stem candidate, using gap to closest staff.
     *
     * @param system containing system
     * @param glyph  stem candidate
     * @return true if OK
     */
    public boolean checkStem (SystemInfo system,
                              Glyph glyph)
    {
        return stemChecker.check(system, null, glyph, null);
    }

    //----------//
    // annotate //
    //----------//
    /**
     * Run a series of checks on the provided glyph, based on the
     * candidate shape, and annotate the evaluation accordingly.
     * This annotation can even change the shape itself, thus allowing a move
     * from physical shape (such as WEDGE_set) to proper logical shape
     * (CRESCENDO or DECRESCENDO).
     *
     * @param system   the containing system
     * @param eval     the evaluation to populate
     * @param glyph    the glyph to check for a shape
     * @param features the glyph features
     */
    public void annotate (SystemInfo system,
                          Evaluation eval,
                          Glyph glyph,
                          double[] features)
    {
        if (!constants.applySpecificCheck.getValue()) {
            return;
        }

        //        if (glyph.isVip()) {
        //            logger.info("Checking " + glyph);
        //        }
        //
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
     * (certainly manually) assigned the provided shape.
     * So update the tests internals accordingly.
     *
     * @param shape    the assigned shape
     * @param glyph    the glyph at hand
     * @param features the glyph features
     * @param sheet    the containing sheet
     */
    public void relax (Shape shape,
                       Glyph glyph,
                       double[] features,
                       Sheet sheet)
    {
        Collection<Checker> checks = checkerMap.get(shape);

        if (checks == null) {
            return;
        }

        for (Checker checker : checks) {
            checker.relax(shape, glyph, features, sheet);
        }
    }

    //------------//
    // addChecker //
    //------------//
    /**
     * Add a checker to a series of shapes.
     *
     * @param checker the checker to add
     * @param shapes  the shape(s) for which the check applies
     */
    private void addChecker (Checker checker,
                             Shape... shapes)
    {
        for (Shape shape : shapes) {
            Collection<Checker> checks = checkerMap.get(shape);

            if (checks == null) {
                checks = new ArrayList<>();
                checkerMap.put(shape, checks);
            }

            checks.add(checker);
        }
    }

    //------------//
    // addChecker //
    //------------//
    /**
     * Add a checker to a series of shape ranges.
     *
     * @param checker     the checker to add
     * @param shapeRanges the shape range(s) to which the check applies
     */
    private void addChecker (Checker checker,
                             ShapeSet... shapeRanges)
    {
        for (ShapeSet range : shapeRanges) {
            addChecker(checker, range.getShapes().toArray(new Shape[0]));
        }
    }

    //--------------//
    // correctShape //
    //--------------//
    private boolean correctShape (SystemInfo system,
                                  Glyph glyph,
                                  Evaluation eval,
                                  Shape newShape)
    {
        if (eval.shape != newShape) {
            //            logger.info(
            //                system.getLogPrefix() + "G#" + glyph.getId() + " " +
            //                eval.shape + " -> " + newShape);
            eval.shape = newShape;
        }

        return true;
    }

    //------------//
    // logLogical //
    //------------//
    /**
     * Meant for debugging the mapping from physical to logical shape.
     *
     * @param system   related system
     * @param glyph    the glyph at hand
     * @param eval     the physical evaluation
     * @param newShape the chosen logical shape
     */
    private void logLogical (SystemInfo system,
                             Glyph glyph,
                             Evaluation eval,
                             Shape newShape)
    {
        // For debugging only
        if (eval.grade >= 0.1) {
            logger.info("{0}{1} {2} weight:{3} {4} corrected as {5}",
                    system.getLogPrefix(), glyph, eval, glyph.getWeight(),
                    glyph.getBounds(), newShape);
        }
    }

    //----------------//
    // registerChecks //
    //----------------//
    /**
     * Populate the checkers map.
     */
    private void registerChecks ()
    {
        //        // General constraint check on weight, width, height
        //        new Checker("Constraint", allPhysicalShapes) {
        //                @Override
        //                public boolean check (SystemInfo system,
        //                                      Evaluation eval,
        //                                      Glyph      glyph,
        //                                      double[]   features)
        //                {
        //                    if (!constants.applyConstraintsCheck.getValue()) {
        //                        return true;
        //                    }
        //
        //                    // Apply registered parameters constraints
        //                    return GlyphRegression.getInstance()
        //                                          .constraintsMatched(features, eval);
        //                }
        //
        //                @Override
        //                public void relax (Shape    shape,
        //                                   Glyph    glyph,
        //                                   double[] features,
        //                                   Sheet    sheet)
        //                {
        //                    // Here relax the constraints if so needed
        //                    boolean extended = GlyphRegression.getInstance()
        //                                                      .includeSample(
        //                        features,
        //                        shape);
        //                    logger.info(
        //                        "Constraints " + (extended ? "extended" : "included") +
        //                        " for glyph#" + glyph.getId() + " as " + shape);
        //
        //                    // Record the glyph description to disk
        //                    GlyphRepository.getInstance()
        //                                   .recordOneGlyph(glyph, sheet);
        //                }
        //            };
        new Checker("NotWithinWidth", allPhysicalShapes)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // They must be within the abscissa bounds of the system
                // Except a few shapes
                Shape shape = eval.shape;

                if ((shape == BRACKET)
                    || (shape == BRACE)
                    || (shape == TEXT)
                    || (shape == CHARACTER)) {
                    return true;
                }

                PixelRectangle glyphBox = glyph.getBounds();

                if (((glyphBox.x + glyphBox.width) < system.getLeft())
                    || (glyphBox.x > system.getRight())) {
                    return false;
                }

                return true;
            }
        };

        new Checker("MeasureRest", HW_REST_set)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
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

        new Checker("NotWithinStaffHeight", Clefs)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be within staff height
                return Math.abs(glyph.getPitchPosition()) < 4;
            }
        };

        new Checker("WithinStaffHeight", Dynamics)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be outside staff height
                return Math.abs(glyph.getPitchPosition()) > 4;
            }
        };

        new Checker("TooFarFromLeftBar", Keys)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // They must be rather close to the left side of the measure
                ScoreSystem scoreSystem = system.getScoreSystem();
                Scale scale = scoreSystem.getScale();
                double maxKeyXOffset = scale.toPixels(
                        constants.maxKeyXOffset);
                PixelRectangle box = glyph.getBounds();
                PixelPoint point = box.getLocation();
                SystemPart part = scoreSystem.getPartAt(point);
                Measure measure = part.getMeasureAt(point);

                if (measure == null) {
                    return true;
                }

                Barline insideBar = measure.getInsideBarline();
                Staff staff = part.getStaffAt(point);
                if (staff == null) {
                    return false;
                }
                Clef clef = measure.getFirstMeasureClef(staff.getId());
                int start = (clef != null)
                        ? (clef.getBox().x + clef.getBox().width)
                        : ((insideBar != null)
                        ? insideBar.getLeftX() : measure.getLeftX());

                return (point.x - start) <= maxKeyXOffset;
            }
        };

        new Checker("CommonCutTime", COMMON_TIME)
        {
            private Predicate<Glyph> stemPredicate = new Predicate<Glyph>()
            {
                @Override
                public boolean check (Glyph entity)
                {
                    return entity.getShape() == Shape.STEM;
                }
            };

            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // COMMON_TIME shape is easily confused with CUT_TIME
                // Check presence of a "pseudo-stem"
                PixelRectangle box = glyph.getBounds();
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

        new Checker("Hook", BEAM_HOOK)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
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
        };

        new Checker("Beams", shapesOf(BEAM, BEAM_2, BEAM_3))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                Integer singleThickness = system.getScoreSystem().getScale().
                        getMainBeam();

                if (singleThickness != null) {
                    // Check we have thickness consistent with the number of 
                    // beams (since we know single beam thickness)
                    double meanThickness = glyph.getMeanThickness(
                            Orientation.HORIZONTAL);

                    int nb = (int) Math.rint(
                            meanThickness / singleThickness);

                    switch (nb) {
                    case 1:
                        return correctShape(system, glyph, eval, BEAM);

                    case 2:
                        return correctShape(system, glyph, eval, BEAM_2);

                    case 3:
                        return correctShape(system, glyph, eval, BEAM_3);

                    default:
                        ///logger.warning("Bad beam #" + glyph.getId() + " nb:" + nb);
                        eval.failure = new Evaluation.Failure("beamThickness");

                        return false;
                    }
                } else {
                    return true;
                }
            }
        };

        // Shapes that require a stem on the left side
        new Checker(
                "noLeftStem",
                shapesOf(FlagSets, shapesOf(Flags.getShapes())))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                return glyph.getStem(HorizontalSide.LEFT) != null;
            }
        };

        // Shapes that require a stem nearby
        new Checker("noStem", StemSymbols)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                return glyph.getStemNumber() >= 1;
            }
        };

        new Checker("Text", TEXT, CHARACTER)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check reasonable height (Cannot be too tall when close to staff)
                double maxHeight = (Math.abs(glyph.getPitchPosition()) >= constants.minTitlePitchPosition.
                        getValue())
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
            //                        PixelRectangle box = glyph.getBounds();
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

        new Checker("FullTimeSig", FullTimes)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
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

        new Checker("PartialTimeSig", TIME_69_set, PartialTimes)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
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

        new Checker("StaffGap", Notes.getShapes(), NoteHeads.getShapes(),
                Rests.getShapes(), Dynamics.getShapes(),
                Articulations.getShapes())
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // A note / rest / dynamic cannot be too far from a staff
                PixelPoint center = glyph.getAreaCenter();
                StaffInfo staff = system.getStaffAt(center);

                // Staff may be null when we are modifying system boundaries
                if (staff == null) {
                    return false;
                }

                int gap = staff.getGapTo(glyph);
                int maxGap = system.getScoreSystem().getScale().toPixels(
                        constants.maxGapToStaff);
                return gap <= maxGap;
            }
        };

        stemChecker = new Checker("StaffStemGap",
                shapesOf(shapesOf(STEM),
                shapesOf(Beams.getShapes(),
                Flags.getShapes(),
                FlagSets)))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // A beam / flag / stem  cannot be too far from a staff
                PixelPoint center = glyph.getAreaCenter();
                StaffInfo staff = system.getStaffAt(center);
                int gap = staff.getGapTo(glyph);
                int maxGap = system.getScoreSystem().getScale().toPixels(
                        constants.maxStemGapToStaff);
                return gap <= maxGap;
            }
        };

        new Checker("SmallDynamics", SmallDynamics)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check height
                return glyph.getNormalizedHeight() <= constants.maxSmallDynamicsHeight.
                        getValue();
            }
        };

        new Checker("MediumDynamics", MediumDynamics)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check height
                return glyph.getNormalizedHeight() <= constants.maxMediumDynamicsHeight.
                        getValue();
            }
        };

        new Checker("TallDynamics", TallDynamics)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check height
                return glyph.getNormalizedHeight() <= constants.maxTallDynamicsHeight.
                        getValue();
            }
        };

        new Checker("BelowStaff", Pedals)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Pedal marks must be below the staff
                return glyph.getPitchPosition() > 4;
            }
        };

        new Checker("Tuplet", Tuplets)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Tuplets cannot be too far from a staff
                if (Math.abs(glyph.getPitchPosition()) > constants.maxTupletPitchPosition.
                        getValue()) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                //                    // Simply check the tuplet character via OCR, if available
                //                    // Nota: We must avoid multiple OCR calls on the same glyph
                //                    if (Language.getOcr()
                //                                .isAvailable()) {
                //                        if (glyph.isTransient()) {
                //                            glyph = system.registerGlyph(glyph);
                //                        }
                //
                //                        BasicContent textInfo = glyph.getTextInfo();
                //                        OcrLine  line;
                //
                //                        if (textInfo.getOcrContent() == null) {
                //                            String        language = system.getScoreSystem()
                //                                                           .getScore()
                //                                                           .getLanguage();
                //                            List<OcrLine> lines = textInfo.recognizeGlyph(
                //                                language);
                //
                //                            if ((lines != null) && !lines.isEmpty()) {
                //                                line = lines.get(0);
                //                                textInfo.setOcrInfo(language, line);
                //                            }
                //                        }
                //
                //                        line = textInfo.getOcrLine();
                //
                //                        if (line != null) {
                //                            String str = line.value;
                //                            Shape  shape = eval.shape;
                //
                //                            if (shape == TUPLET_THREE) {
                //                                if (str.equals("3")) {
                //                                    return true;
                //                                }
                //
                //                                //eval.shape = CHARACTER;
                //                            }
                //
                //                            if (shape == TUPLET_SIX) {
                //                                if (str.equals("6")) {
                //                                    return true;
                //                                }
                //
                //                                //eval.shape = CHARACTER;
                //                            }
                //
                //                            eval.failure = new Evaluation.Failure("ocr");
                //
                //                            return false;
                //                        }
                //                    }
                return true;
            }
        };

        new Checker("LongRest", LONG_REST)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be centered on pitch position 0
                if (Math.abs(glyph.getPitchPosition()) > 0.5) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                return true;
            }
        };

        new Checker("Breve", BREVE_REST)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be centered on pitch position -1
                if (Math.abs(glyph.getPitchPosition() + 1) > 0.5) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                return true;
            }
        };

        new Checker("Braces", BRACE, BRACKET)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be centered on left of part barline
                PixelRectangle box = glyph.getBounds();
                PixelPoint left = new PixelPoint(
                        box.x,
                        box.y + (box.height / 2));

                if (left.x > system.getLeft()) {
                    eval.failure = new Evaluation.Failure("notOnLeft");

                    return false;
                }

                // Make sure at least a staff interval is embraced
                boolean embraced = false;
                int intervalTop = Integer.MIN_VALUE;

                for (StaffInfo staff : system.getStaves()) {
                    if (intervalTop != Integer.MIN_VALUE) {
                        int intervalBottom = staff.getFirstLine().yAt(box.x);

                        if ((intervalTop >= box.y)
                            && (intervalBottom <= (box.y + box.height))) {
                            embraced = true; // Ok for this one

                            break;
                        }
                    }

                    intervalTop = staff.getLastLine().yAt(box.x);
                }

                if (!embraced) {
                    eval.failure = new Evaluation.Failure(
                            "noStaffEmbraced");

                    return false;
                }

                return true;
            }
        };

        new Checker("WholeSansLedgers", WHOLE_NOTE)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check that whole notes are not too far from staves
                // without ledgers
                PixelPoint point = glyph.getAreaCenter();
                StaffInfo staff = system.getStaffAt(point);
                double pitch = staff.pitchPositionOf(point);

                if (Math.abs(pitch) <= 6) {
                    return true;
                }

                return staff.getClosestLedger(point) != null;
            }
        };

        new Checker("SystemTop", DAL_SEGNO, DA_CAPO, SEGNO, CODA, BREATH_MARK)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check that these markers are just above first staff
                PixelPoint point = glyph.getAreaCenter();
                StaffInfo staff = system.getStaffAt(point);

                if (staff != system.getFirstStaff()) {
                    return false;
                }

                double pitch = staff.pitchPositionOf(point);

                return pitch <= -5;
            }
        };

        new Checker("Fermata_set", FERMATA_set)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Use moment n21 to differentiate between V & ^
                // TBD: We could use pitch position as well?
                double n21 = glyph.getGeometricMoments().getN21();
                Shape newShape = (n21 > 0) ? FERMATA : FERMATA_BELOW;

                ///logLogical(system, glyph, eval, newShape);
                eval.shape = newShape;

                return true;
            }
        };

        new Checker("FLAG_*_set", FlagSets)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                Shape newShape = null;
                boolean covar = glyph.getGeometricMoments().getN11() > 0;

                switch (eval.shape) {
                case FLAG_1_set:
                    newShape = covar ? FLAG_1 : FLAG_1_UP;

                    break;

                case FLAG_2_set:
                    newShape = covar ? FLAG_2 : FLAG_2_UP;

                    break;

                case FLAG_3_set:
                    newShape = covar ? FLAG_3 : FLAG_3_UP;

                    break;

                case FLAG_4_set:
                    newShape = covar ? FLAG_4 : FLAG_4_UP;

                    break;

                case FLAG_5_set:
                    newShape = covar ? FLAG_5 : FLAG_5_UP;

                    break;
                }

                ///logLogical(system, glyph, eval, newShape);
                eval.shape = newShape;

                return true;
            }
        };

        new Checker("TIME_69_set", TIME_69_set)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Use moment n12 to differentiate between <(6) & >(9)
                double n12 = glyph.getGeometricMoments().getN12();
                Shape newShape = (n12 > 0) ? TIME_NINE : TIME_SIX;
                ///logLogical(system, glyph, eval, newShape);
                eval.shape = newShape;

                return true;
            }
        };

        new Checker("TURN_set", TURN_set)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                Shape newShape;

                // Use aspect to detect turn_up
                double aspect = glyph.getAspect(Orientation.VERTICAL);

                if (aspect > 1) {
                    newShape = TURN_UP;
                } else {
                    // Use xy covariance
                    boolean covar = glyph.getGeometricMoments().getN11() > 0;

                    newShape = covar ? TURN : INVERTED_TURN;
                }

                ///logLogical(system, glyph, eval, newShape);
                eval.shape = newShape;

                return true;
            }
        };

        new Checker("Wedge_set", WEDGE_set)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Use moment n12 to differentiate between < & >
                double n12 = glyph.getGeometricMoments().getN12();
                Shape newShape = (n12 > 0) ? CRESCENDO : DECRESCENDO;

                ///logLogical(system, glyph, eval, newShape);
                eval.shape = newShape;

                return true;
            }
        };
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Checker //
    //---------//
    /**
     * A checker runs a specific check for a given glyph with respect to
     * a collection of candidate shapes.
     */
    private abstract class Checker
    {
        //~ Instance fields ----------------------------------------------------

        /** Unique name for this check */
        public final String name;

        //~ Constructors -------------------------------------------------------
        public Checker (String name,
                        Shape... shapes)
        {
            this.name = name;
            addChecker(this, shapes);
        }

        public Checker (String name,
                        Collection<Shape> shapes)
        {
            this.name = name;
            addChecker(this, shapes.toArray(new Shape[0]));
        }

        public Checker (String name,
                        Collection<Shape>... shapes)
        {
            this.name = name;
            Collection<Shape> allShapes = new ArrayList<>();
            for (Collection<Shape> col : shapes) {
                allShapes.addAll(col);
            }
            addChecker(this, allShapes.toArray(new Shape[allShapes.size()]));
        }

        public Checker (String name,
                        ShapeSet... shapeSets)
        {
            this.name = name;
            addChecker(this, shapeSets);
        }

        public Checker (String name,
                        Shape shape,
                        Collection<Shape> collection)
        {
            this.name = name;

            List<Shape> all = new ArrayList<>();
            all.add(shape);

            all.addAll(collection);

            addChecker(this, all.toArray(new Shape[0]));
        }

        public Checker (String name,
                        Shape shape)
        {
            this.name = name;

            List<Shape> all = new ArrayList<>();
            all.add(shape);

            addChecker(this, all.toArray(new Shape[0]));
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Run the specific test.
         *
         * @param system   the containing system
         * @param eval     the partially-filled evaluation (eval.shape is an
         *                 input/output, eval.grade and eval.failure are outputs)
         * @param glyph    the glyph at hand
         * @param features the glyph features
         * @return true if OK, false otherwise
         */
        public abstract boolean check (SystemInfo system,
                                       Evaluation eval,
                                       Glyph glyph,
                                       double[] features);

        /**
         * Take into account the fact that the provided glyph has been
         * (certainly manually) assigned the provided shape.
         * So update the test internals accordingly.
         *
         * @param shape    the assigned shape
         * @param glyph    the glyph at hand
         * @param features the glyph features
         * @param sheet    the containing sheet
         */
        public void relax (Shape shape,
                           Glyph glyph,
                           double[] features,
                           Sheet sheet)
        {
            // Void by default
        }

        @Override
        public String toString ()
        {
            return name;
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

        Scale.Fraction maxTitleHeight = new Scale.Fraction(
                4d,
                "Maximum normalized height for a title text");

        Scale.Fraction maxLyricsHeight = new Scale.Fraction(
                2.5d,
                "Maximum normalized height for a lyrics text");

        Constant.Double minTitlePitchPosition = new Constant.Double(
                "PitchPosition",
                15d,
                "Minimum absolute pitch position for a title");

        Constant.Double maxTupletPitchPosition = new Constant.Double(
                "PitchPosition",
                15d,
                "Minimum absolute pitch position for a tuplet");

        Constant.Double maxTimePitchPositionMargin = new Constant.Double(
                "PitchPosition",
                1d,
                "Maximum absolute pitch position margin for a time signature");

        Scale.Fraction maxTextGap = new Scale.Fraction(
                5.0,
                "Maximum value for a horizontal gap between glyphs of the same text");

        Scale.Fraction maxKeyXOffset = new Scale.Fraction(
                2,
                "Maximum horizontal offset for a key since clef or measure start");

        Scale.Fraction maxSmallDynamicsHeight = new Scale.Fraction(
                1.5,
                "Maximum height for small dynamics (no p, no f)");

        Scale.Fraction maxMediumDynamicsHeight = new Scale.Fraction(
                2,
                "Maximum height for small dynamics (no p, no f)");

        Scale.Fraction maxTallDynamicsHeight = new Scale.Fraction(
                2.5,
                "Maximum height for small dynamics (no p, no f)");

        Scale.Fraction maxGapToStaff = new Scale.Fraction(
                8,
                "Maximum vertical gap between a note-like glyph and closest staff");

        Scale.Fraction maxStemGapToStaff = new Scale.Fraction(
                12,
                "Maximum vertical gap between a stem and closest staff");

    }
}
