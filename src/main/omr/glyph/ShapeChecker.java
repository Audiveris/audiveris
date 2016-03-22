//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h a p e C h e c k e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.classifier.Evaluation;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.glyph.Shape.*;
import static omr.glyph.ShapeSet.*;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.text.TextBuilder;
import omr.text.TextLine;

import omr.util.LiveParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

/**
 * Class {@code ShapeChecker} gathers additional specific shape checks, meant to
 * complement the work done by a shape classifier.
 * <p>
 * Typically, physical shapes (the *_set shape names) must be mapped to the right logical shapes
 * using proper additional tests.
 * <p>
 * The available context varies according to the symbol candidate shape.
 *
 * @author Hervé Bitteur
 */
public class ShapeChecker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            ShapeChecker.class);

    /** Singleton */
    private static ShapeChecker INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    //    /** Small dynamics with no 'P' or 'F' */
    //    private static final EnumSet<Shape> SmallDynamics = EnumSet.copyOf(
    //            shapesOf(DYNAMICS_CHAR_M, DYNAMICS_CHAR_R, DYNAMICS_CHAR_S, DYNAMICS_CHAR_Z));
    //
    //    /** Medium dynamics with a 'P' (but no 'F') */
    //    private static final EnumSet<Shape> MediumDynamics = EnumSet.copyOf(
    //            shapesOf(DYNAMICS_MP, DYNAMICS_P, DYNAMICS_PP, DYNAMICS_PPP));
    //
    //    /** Tall dynamics with an 'F' */
    //    private static final EnumSet<Shape> TallDynamics = EnumSet.copyOf(
    //            shapesOf(
    //                    DYNAMICS_F,
    //                    DYNAMICS_FF,
    //                    DYNAMICS_FFF,
    //                    DYNAMICS_FP,
    //                    DYNAMICS_FZ,
    //                    DYNAMICS_MF,
    //                    DYNAMICS_RF,
    //                    DYNAMICS_RFZ,
    //                    DYNAMICS_SF,
    //                    DYNAMICS_SFFZ,
    //                    DYNAMICS_SFP,
    //                    DYNAMICS_SFPP,
    //                    DYNAMICS_SFZ));
    //
    /** Map of Shape => Sequence of checkers */
    private final EnumMap<Shape, Collection<Checker>> checkerMap;

    //~ Constructors -------------------------------------------------------------------------------
    private ShapeChecker ()
    {
        checkerMap = new EnumMap<Shape, Collection<Checker>>(Shape.class);
        registerChecks();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // annotate //
    //----------//
    /**
     * Run a series of checks on the provided glyph, based on the
     * candidate shape, and annotate the evaluation accordingly.
     * This annotation can even change the shape itself, thus allowing a move
     * from physical shape (such as HW_REST_set) to proper logical shape
     * (HALF_REST or WHOLE_REST).
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
        if (!constants.applySpecificCheck.isSet()) {
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
                    eval.failure = new Evaluation.Failure(checker.name + ":" + eval.failure);
                } else {
                    eval.failure = new Evaluation.Failure(checker.name);
                }

                return;
            }
        }
    }

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
            logger.info(
                    "{}{} {} weight:{} {} corrected as {}",
                    system.getLogPrefix(),
                    glyph,
                    eval,
                    glyph.getWeight(),
                    glyph.getBounds(),
                    newShape);
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

                Rectangle glyphBox = glyph.getBounds();

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
                /**
                 * Problem.
                 * These half / whole rest signs are generally on standard pitch values:
                 * Standard pitch for whole: -1.5
                 * Standard pitch for half: -0.5
                 *
                 * But because of other notes in the same staff-measure, they may appear
                 * on different pitch values, as they see fit. See Telemann example.
                 * Whole is always stuck to an upper line, half is always stuck to a lower line.
                 * This can be translated as 2*p = 4*k+1 for wholes and 4*k-1 for halves
                 */
                final double pp = system.estimatedPitch(glyph.getCenter());
                final int p2 = (int) Math.rint(2 * pp); // Pitch * 2

                switch (p2) {
                case -9:
                case -5:
                case -1:
                case 3:
                case 7:
                case 11:
                    eval.shape = Shape.HALF_REST; // Standard pitch: -0.5

                    return true;

                case -11:
                case -7:
                case -3:
                case 1:
                case 5:
                case 9:
                    eval.shape = Shape.WHOLE_REST; // Standard pitch: -1.5

                    return true;

                default:
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
                final double pp = system.estimatedPitch(glyph.getCenter());
                final double pitchAbs = Math.abs(pp);

                // Very strict for percussion
                if (eval.shape == Shape.PERCUSSION_CLEF) {
                    return pitchAbs < 2;
                }

                // Must be within staff height for the other clefs
                return pitchAbs < 4;
            }
        };

        new Checker("WithinStaffHeight", Dynamics.getShapes(), Fermatas)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be outside staff height
                final double pp = system.estimatedPitch(glyph.getCenter());

                return Math.abs(pp) > 4;
            }
        };

        new Checker("WithinHeader", SmallClefs)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Must be on right side of system header
                return Math.abs(glyph.getCenter().x) > system.getFirstStaff().getHeaderStop();
            }
        };

        //
        //        new Checker("CommonCutTime", COMMON_TIME)
        //        {
        //            // TODO: this no longer works, since shape is not recorded in glyph!
        //            private Predicate<Glyph> stemPredicate = new Predicate<Glyph>()
        //            {
        //                @Override
        //                public boolean check (Glyph entity)
        //                {
        //                    return entity.getShape() == Shape.STEM;
        //                }
        //            };
        //
        //            @Override
        //            public boolean check (SystemInfo system,
        //                                  Evaluation eval,
        //                                  Glyph glyph,
        //                                  double[] features)
        //            {
        //                // COMMON_TIME shape is easily confused with CUT_TIME
        //                // Check presence of a "pseudo-stem"
        //                Rectangle box = glyph.getBounds();
        //                box.grow(-box.width / 4, 0);
        //
        //                List<Glyph> neighbors = system.lookupIntersectedGlyphs(box, glyph);
        //
        //                if (Glyphs.contains(neighbors, stemPredicate)) {
        //                    eval.shape = Shape.CUT_TIME;
        //                }
        //
        //                return true;
        //            }
        //        };
        //
        new Checker("Text", TEXT)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Check reasonable height (Cannot be too tall when close to staff)
                final double pp = system.estimatedPitch(glyph.getCenter());
                double maxHeight = (Math.abs(pp) >= constants.minTitlePitchPosition.getValue())
                        ? constants.maxTitleHeight.getValue()
                        : constants.maxLyricsHeight.getValue();

                int interline = system.getSheet().getInterline();
                double normedHeight = (double) glyph.getHeight() / interline;

                if (normedHeight >= maxHeight) {
                    eval.failure = new Evaluation.Failure("tooHigh");

                    return false;
                }

                return true;
            }
        };

        new Checker("WholeTimeSig", WholeTimes)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                final double pp = system.estimatedPitch(glyph.getCenter());
                double absPos = Math.abs(pp);
                double maxDy = constants.maxTimePitchPositionMargin.getValue();

                // A whole time shape must be on 0 position
                if (absPos > maxDy) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                // Total height for a complete time sig is staff height
                int interline = system.getSheet().getInterline();
                double normedHeight = (double) glyph.getHeight() / interline;

                if (normedHeight > 4.5) {
                    eval.failure = new Evaluation.Failure("tooHigh");

                    return false;
                }

                return true;
            }
        };

        new Checker("PartialTimeSig", PartialTimes)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                final double pp = system.estimatedPitch(glyph.getCenter());
                double absPos = Math.abs(pp);
                double maxDy = constants.maxTimePitchPositionMargin.getValue();

                // A partial time shape must be on -2 or +2 positions
                if (Math.abs(absPos - 2) > maxDy) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                return true;
            }
        };

        new Checker(
                "StaffGap",
                Notes.getShapes(),
                Rests.getShapes(),
                Dynamics.getShapes(),
                Articulations.getShapes())
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // A note / rest / dynamic cannot be too far from a staff
                Point center = glyph.getCenter();
                Staff staff = system.getClosestStaff(center);

                // Staff may be null when we are modifying system boundaries
                if (staff == null) {
                    return false;
                }

                int gap = staff.gapTo(glyph.getBounds());
                int maxGap = system.getSheet().getScale().toPixels(constants.maxGapToStaff);

                return gap <= maxGap;
            }
        };

        //        new Checker("SmallDynamics", SmallDynamics)
        //        {
        //            @Override
        //            public boolean check (SystemInfo system,
        //                                  Evaluation eval,
        //                                  Glyph glyph,
        //                                  double[] features)
        //            {
        //                // Check height
        //                return glyph.getNormalizedHeight() <= constants.maxSmallDynamicsHeight.getValue();
        //            }
        //        };
        //
        //        new Checker("MediumDynamics", MediumDynamics)
        //        {
        //            @Override
        //            public boolean check (SystemInfo system,
        //                                  Evaluation eval,
        //                                  Glyph glyph,
        //                                  double[] features)
        //            {
        //                // Check height
        //                return glyph.getNormalizedHeight() <= constants.maxMediumDynamicsHeight.getValue();
        //            }
        //        };
        //
        //        new Checker("TallDynamics", TallDynamics)
        //        {
        //            @Override
        //            public boolean check (SystemInfo system,
        //                                  Evaluation eval,
        //                                  Glyph glyph,
        //                                  double[] features)
        //            {
        //                // Check height
        //                return glyph.getNormalizedHeight() <= constants.maxTallDynamicsHeight.getValue();
        //            }
        //        };
        new Checker("BelowStaff", Pedals)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph,
                                  double[] features)
            {
                // Pedal marks must be below the staff
                final double pp = system.estimatedPitch(glyph.getCenter());

                if (pp <= 4) {
                    return false;
                }

                // Pedal marks cannot intersect the staff
                Staff staff = system.getClosestStaff(glyph.getCenter());

                return staff.gapTo(glyph.getBounds()) > 0;
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
                final double pp = system.estimatedPitch(glyph.getCenter());

                if (Math.abs(pp) > constants.maxTupletPitchPosition.getValue()) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                // Simply check the tuplet character via OCR, if available
                // Nota: We should avoid multiple OCR calls on the same glyph
                if (TextBuilder.getOcr().isAvailable()) {
                    final Sheet sheet = system.getSheet();

                    if (glyph.isTransient()) {
                        glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                        system.addFreeGlyph(glyph);
                    }

                    final LiveParam<String> textParam = sheet.getStub().getLanguageParam();
                    final String language = textParam.getTarget();
                    final List<TextLine> lines = TextBuilder.scanGlyph(
                            glyph,
                            language,
                            sheet);

                    if ((lines != null) && !lines.isEmpty()) {
                        TextLine line = lines.get(0);
                        String str = line.getValue();
                        Shape shape = eval.shape;

                        if (shape == TUPLET_THREE) {
                            if (str.equals("3")) {
                                return true;
                            }
                        }

                        if (shape == TUPLET_SIX) {
                            if (str.equals("6")) {
                                return true;
                            }
                        }

                        eval.failure = new Evaluation.Failure("ocr");
                    }

                    return false;
                }

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
                final double pp = system.estimatedPitch(glyph.getCenter());

                if (Math.abs(pp) > 0.5) {
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
                final double pp = system.estimatedPitch(glyph.getCenter());

                if (Math.abs(pp + 1) > 0.5) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                return true;
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
                // Check that these markers are just above first system staff
                Rectangle bounds = glyph.getBounds();
                Point bottom = new Point(
                        bounds.x + (bounds.width / 2),
                        bounds.y + bounds.height);
                Staff staff = system.getClosestStaff(bottom);

                if (staff != system.getFirstStaff()) {
                    return false;
                }

                double pitch = staff.pitchPositionOf(bottom);

                return (constants.minDirectionPitchPosition.getValue() <= pitch)
                       && (pitch <= -5);
            }
        };
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Checker //
    //---------//
    /**
     * A checker runs a specific check for a given glyph with respect to a set of
     * candidate shapes.
     */
    private abstract class Checker
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Unique name for this check */
        public final String name;

        //~ Constructors ---------------------------------------------------------------------------
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

            Collection<Shape> allShapes = new ArrayList<Shape>();

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

            List<Shape> all = new ArrayList<Shape>();
            all.add(shape);

            all.addAll(collection);

            addChecker(this, all.toArray(new Shape[0]));
        }

        public Checker (String name,
                        Shape shape)
        {
            this.name = name;

            List<Shape> all = new ArrayList<Shape>();
            all.add(shape);

            addChecker(this, all.toArray(new Shape[0]));
        }

        //~ Methods --------------------------------------------------------------------------------
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
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean applySpecificCheck = new Constant.Boolean(
                true,
                "Should we apply specific checks on shape candidates?");

        private final Scale.Fraction maxTitleHeight = new Scale.Fraction(
                4.0,
                "Maximum normalized height for a title text");

        private final Scale.Fraction maxLyricsHeight = new Scale.Fraction(
                2.5,
                "Maximum normalized height for a lyrics text");

        private final Constant.Double minDirectionPitchPosition = new Constant.Double(
                "PitchPosition",
                -13.0,
                "Minimum pitch value for a  segno / coda direction");

        private final Constant.Double minTitlePitchPosition = new Constant.Double(
                "PitchPosition",
                15.0,
                "Minimum absolute pitch position for a title");

        private final Constant.Double maxTupletPitchPosition = new Constant.Double(
                "PitchPosition",
                15.0,
                "Maximum absolute pitch position for a tuplet");

        private final Constant.Double maxTimePitchPositionMargin = new Constant.Double(
                "PitchPosition",
                1.0,
                "Maximum absolute pitch position margin for a time signature");

        private final Scale.Fraction maxSmallDynamicsHeight = new Scale.Fraction(
                1.5,
                "Maximum height for small dynamics (no p, no f)");

        private final Scale.Fraction maxMediumDynamicsHeight = new Scale.Fraction(
                2.0,
                "Maximum height for small dynamics (with p, no f)");

        private final Scale.Fraction maxTallDynamicsHeight = new Scale.Fraction(
                2.5,
                "Maximum height for tall dynamics (with f)");

        private final Scale.Fraction maxGapToStaff = new Scale.Fraction(
                8.0,
                "Maximum vertical gap between a note-like glyph and closest staff");
    }
}
