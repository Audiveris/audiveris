//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h a p e C h e c k e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import static org.audiveris.omr.glyph.Shape.BRACE;
import static org.audiveris.omr.glyph.Shape.BRACKET;
import static org.audiveris.omr.glyph.Shape.CHARACTER;
import static org.audiveris.omr.glyph.Shape.CODA;
import static org.audiveris.omr.glyph.Shape.DAL_SEGNO;
import static org.audiveris.omr.glyph.Shape.DA_CAPO;
import static org.audiveris.omr.glyph.Shape.HW_REST_set;
import static org.audiveris.omr.glyph.Shape.LONG_REST;
import static org.audiveris.omr.glyph.Shape.PERCUSSION_CLEF;
import static org.audiveris.omr.glyph.Shape.SEGNO;
import static org.audiveris.omr.glyph.Shape.TEXT;
import static org.audiveris.omr.glyph.Shape.TIME_CUSTOM;
import static org.audiveris.omr.glyph.ShapeSet.Articulations;
import static org.audiveris.omr.glyph.ShapeSet.Clefs;
import static org.audiveris.omr.glyph.ShapeSet.Dynamics;
import static org.audiveris.omr.glyph.ShapeSet.FermataArcs;
import static org.audiveris.omr.glyph.ShapeSet.Markers;
import static org.audiveris.omr.glyph.ShapeSet.Pedals;
import static org.audiveris.omr.glyph.ShapeSet.Rests;
import static org.audiveris.omr.glyph.ShapeSet.SmallClefs;
import static org.audiveris.omr.glyph.ShapeSet.Tuplets;
import static org.audiveris.omr.glyph.ShapeSet.WholeTimes;
import static org.audiveris.omr.glyph.ShapeSet.allPhysicalShapes;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

/**
 * Class <code>ShapeChecker</code> gathers additional specific shape checks, meant to
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

    private static final Logger logger = LoggerFactory.getLogger(ShapeChecker.class);

    //~ Instance fields ----------------------------------------------------------------------------

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
        checkerMap = new EnumMap<>(Shape.class);
        registerChecks();
    }

    //~ Methods ------------------------------------------------------------------------------------

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
                             Collection<Shape> shapes)
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
            addChecker(checker, range.getShapes());
        }
    }

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
     * @param system the containing system
     * @param eval   the evaluation to populate
     * @param glyph  the glyph to check for a shape
     */
    public void annotate (SystemInfo system,
                          Evaluation eval,
                          Glyph glyph)
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
            if (!(checker.check(system, eval, glyph))) {
                if (eval.failure != null) {
                    eval.failure = new Evaluation.Failure(checker.name + ":" + eval.failure);
                } else {
                    eval.failure = new Evaluation.Failure(checker.name);
                }

                return;
            }
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
                                  Glyph glyph)
            {
                // They must be within the abscissa bounds of the system
                // Except a few shapes
                Shape shape = eval.shape;

                if ((shape == BRACKET) || (shape == BRACE) || (shape == TEXT)
                        || (shape == CHARACTER)) {
                    return true;
                }

                Rectangle glyphBox = glyph.getBounds();

                return !(((glyphBox.x + glyphBox.width) < system.getLeft()) || (glyphBox.x > system
                        .getRight()));
            }
        };

        new Checker("MeasureRest", HW_REST_set)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                /**
                 * Problem.
                 * These half / whole rest signs are generally on standard pitch values:
                 * Standard pitch for whole: -1.5
                 * Standard pitch for half: -0.5
                 * <p>
                 * But because of other notes in the same staff-measure, they may appear
                 * on different pitch values, as they see fit. See Telemann example.
                 * Whole is always stuck to an upper line, half is always stuck to a lower line.
                 * This can be translated as 2*p = 4*k +1 for wholes and 4*k -1 for halves
                 * <p>
                 * We also check that such measure rest candidate is not stuck to a stem,
                 * which can appear when conflicting with a beam hook.
                 */
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    eval.failure = new Evaluation.Failure("tablature");

                    return false;
                }

                final int p2 = (int) Math.rint(2 * pp); // Pitch * 2

                if (!checkNoStem(system, glyph)) {
                    eval.failure = new Evaluation.Failure("stem");

                    return false;
                }

                switch (p2) {
                    case -13, -9, -5, -1, 3, 7, 11, 15 -> {
                        eval.shape = Shape.HALF_REST; // Standard pitch: -0.5

                        return true;
                    }
                    case -15, -11, -7, -3, 1, 5, 9, 13 -> {
                        eval.shape = Shape.WHOLE_REST; // Standard pitch: -1.5

                        return true;
                    }
                    default -> {
                        eval.failure = new Evaluation.Failure("pitch");

                        return false;
                    }
                }
            }

            /**
             * Check that the measure-rest candidate stays away from any stem.
             */
            private boolean checkNoStem (SystemInfo system,
                                         Glyph glyph)
            {
                final int minDx = system.getSheet().getScale().toPixels(constants.measureRestDx);
                final Rectangle box = glyph.getBounds();
                box.grow(minDx, 0);
                final Point center = glyph.getCenter();

                final List<Inter> stems = system.getSig().inters(Shape.STEM);
                for (Inter inter : stems) {
                    final StemInter stem = (StemInter) inter;
                    if (stem.getBounds().intersects(box)) {
                        final Point2D cross = LineUtil.intersectionAtY(stem.getMedian(), center.y);
                        if (box.contains(cross)) {
                            return false;
                        }
                    }
                }

                return true;
            }
        };

        new Checker("NotWithinStaffHeight", shapesOf(Clefs, WholeTimes, Arrays.asList(TIME_CUSTOM)))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    return false;
                }

                final double pitchAbs = Math.abs(pp);

                // Very strict for percussion
                if (eval.shape == Shape.PERCUSSION_CLEF) {
                    return pitchAbs < 2;
                }

                // Must be within staff height for the other clefs
                return pitchAbs < 4;
            }
        };

        new Checker("WithinStaffHeight", shapesOf(Dynamics.getShapes(), FermataArcs))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Must be outside staff height
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    return false;
                }

                return Math.abs(pp) > 4;
            }
        };

        new Checker("WithinHeader", SmallClefs)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Must be on right side of system header
                return Math.abs(glyph.getCenter2D().getX()) > system.getHeaderStop();
            }
        };

        new Checker("NotWithinHeader", PERCUSSION_CLEF)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Percussion clef must be within system header
                return Math.abs(glyph.getCenter2D().getX()) < system.getHeaderStop();
            }
        };

        new Checker("Text", TEXT)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Check reasonable height (Cannot be too tall when close to staff)
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    eval.failure = new Evaluation.Failure("tablature");
                    return false;
                }

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
                                  Glyph glyph)
            {
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    eval.failure = new Evaluation.Failure("tablature");
                    return false;
                }

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
        //
        //        new Checker("PartialTimeSig", PartialTimes)
        //        {
        //            // It can be a num or den of a larger time sig
        //            // It can also be the measure number above a multiple rest
        //            @Override
        //            public boolean check (SystemInfo system,
        //                                  Evaluation eval,
        //                                  Glyph glyph)
        //            {
        //                final double pp = system.estimatedPitch(glyph.getCenter2D());
        //                double absPos = Math.abs(pp);
        //                double maxDy = constants.maxTimePitchPositionMargin.getValue();
        //
        //                // A partial time shape must be on -2 or +2 positions
        //                if (Math.abs(absPos - 2) > maxDy) {
        //                    eval.failure = new Evaluation.Failure("pitch");
        //
        //                    return false;
        //                }
        //
        //                return true;
        //            }
        //        };
        //
        new Checker(
                "StaffGap",
                shapesOf(Rests.getShapes(), Dynamics.getShapes(), Articulations.getShapes()))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // A note / rest / dynamic cannot be too far from a staff
                Staff staff = system.getClosestStaff(glyph.getCenter2D());

                if (staff == null) {
                    return false;
                }

                double gap = staff.gapTo(glyph.getBounds());
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
                                  Glyph glyph)
            {
                final Point2D glyphCenter = glyph.getCenter2D();

                // Pedal marks must be below the staff
                final Double pp = system.estimatedPitch(glyphCenter);
                if (pp == null) {
                    return false;
                }

                if (pp <= 4) {
                    return false;
                }

                // Pedal marks cannot intersect the staff
                Staff staff = system.getClosestStaff(glyphCenter);

                return staff.gapTo(glyph.getBounds()) > 0;
            }
        };

        new Checker("AboveStaff", Markers)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                final Point2D glyphCenter = glyph.getCenter2D();

                // Markers must be above the staff
                final Double pp = system.estimatedPitch(glyphCenter);
                if (pp == null) {
                    return false;
                }

                if (pp >= -4) {
                    return false;
                }

                // Markers cannot intersect the staff
                Staff staff = system.getClosestStaff(glyphCenter);

                return staff.gapTo(glyph.getBounds()) > 0;
            }
        };

        new Checker("Tuplet", Tuplets)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Tuplets cannot be too far from a staff
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    eval.failure = new Evaluation.Failure("tablature");
                    return false;
                }

                if (Math.abs(pp) > constants.maxTupletPitchPosition.getValue()) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                //                // Simply check the tuplet character via OCR, if available
                //                // Nota: We should avoid multiple OCR calls on the same glyph
                //                if (TextBuilder.getOcr().isAvailable()) {
                //                    logger.info("ocr on glyph#{} eval:{}", glyph, eval);
                //                    final Sheet sheet = system.getSheet();
                //
                //                    if (glyph.isTransient()) {
                //                        glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                //                        system.addFreeGlyph(glyph);
                //                    }
                //
                //                    final LiveParam<String> textParam = sheet.getStub().getLanguageParam();
                //                    final String language = textParam.getTarget();
                //                    final List<TextLine> lines = TextBuilder.scanGlyph(
                //                            glyph,
                //                            language,
                //                            sheet);
                //
                //                    if ((lines != null) && !lines.isEmpty()) {
                //                        TextLine line = lines.get(0);
                //                        String str = line.getValue();
                //                        Shape shape = eval.shape;
                //
                //                        if (shape == TUPLET_THREE) {
                //                            if (str.equals("3")) {
                //                                return true;
                //                            }
                //                        }
                //
                //                        if (shape == TUPLET_SIX) {
                //                            if (str.equals("6")) {
                //                                return true;
                //                            }
                //                        }
                //
                //                        eval.failure = new Evaluation.Failure("ocr");
                //                    }
                //
                //                    return false;
                //                }
                //
                return true;
            }
        };

        new Checker("LongRest", LONG_REST)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Must be centered on pitch position 0
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    eval.failure = new Evaluation.Failure("tablature");
                    return false;
                }

                if (Math.abs(pp) > 0.5) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                return true;
            }
        };

        //        new Checker("BreveRest", BREVE_REST)
        //        {
        //            @Override
        //            public boolean check (SystemInfo system,
        //                                  Evaluation eval,
        //                                  Glyph glyph)
        //            {
        //                // Must be centered on pitch position -1
        //                final double pp = system.estimatedPitch(glyph.getCenter2D());
        //
        //                if (Math.abs(pp + 1) > 0.5) {
        //                    eval.failure = new Evaluation.Failure("pitch");
        //
        //                    return false;
        //                }
        //
        //                return true;
        //            }
        //        };
        //
        new Checker("SystemTop", Arrays.asList(DAL_SEGNO, DA_CAPO, SEGNO, CODA))
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Check that these markers are just above first system staff
                Rectangle bounds = glyph.getBounds();
                Point bottom = new Point(bounds.x + (bounds.width / 2), bounds.y + bounds.height);
                Staff staff = system.getClosestStaff(bottom);

                if ((staff != system.getFirstStaff()) || staff.isTablature()) {
                    return false;
                }

                final Point center = glyph.getCenter();
                double pitch = staff.pitchPositionOf(center);

                final LineInfo firstConcreteLine = staff.getLines().get(0);
                final int yMax = firstConcreteLine.yAt(center.x);

                return (constants.minMarkerPitchPosition.getValue() <= pitch) && (center.y <= yMax);
            }
        };

        new Checker("MeasureRepeats", ShapeSet.RepeatBars)
        {
            @Override
            public boolean check (SystemInfo system,
                                  Evaluation eval,
                                  Glyph glyph)
            {
                // Check these signs are located on staff mid line
                final Double pp = system.estimatedPitch(glyph.getCenter2D());
                if (pp == null) {
                    eval.failure = new Evaluation.Failure("tablature");
                    return false;
                }

                if (Math.abs(pp) > constants.maxMeasureRepeatPitchPosition.getValue()) {
                    eval.failure = new Evaluation.Failure("pitch");

                    return false;
                }

                return true;
            }
        };
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of ShapeChecker in the application.
     *
     * @return the instance
     */
    public static ShapeChecker getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //----------//
    // shapesOf //
    //----------//
    private static List<Shape> shapesOf (Collection<Shape> col1,
                                         Collection<Shape> col2)
    {
        final List<Shape> list = new ArrayList<>();
        list.addAll(col1);
        list.addAll(col2);

        return list;
    }

    //----------//
    // shapesOf //
    //----------//
    private static List<Shape> shapesOf (Collection<Shape> col1,
                                         Collection<Shape> col2,
                                         Collection<Shape> col3)
    {
        final List<Shape> list = new ArrayList<>();
        list.addAll(col1);
        list.addAll(col2);
        list.addAll(col3);

        return list;
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
        /** Unique name for this check. */
        public final String name;

        Checker (String name,
                 Collection<Shape> shapes)
        {
            this.name = name;
            addChecker(this, shapes);
        }

        Checker (String name,
                 Shape shape)
        {
            this.name = name;

            List<Shape> all = new ArrayList<>();
            all.add(shape);

            addChecker(this, all);
        }

        Checker (String name,
                 Shape shape,
                 Collection<Shape> collection)
        {
            this.name = name;

            List<Shape> all = new ArrayList<>();
            all.add(shape);

            all.addAll(collection);

            addChecker(this, all);
        }

        Checker (String name,
                 ShapeSet... shapeSets)
        {
            this.name = name;
            addChecker(this, shapeSets);
        }

        /**
         * Run the specific test.
         *
         * @param system the containing system
         * @param eval   the partially-filled evaluation (eval.shape is an
         *               input/output, eval.grade and eval.failure are outputs)
         * @param glyph  the glyph at hand
         * @return true if OK, false otherwise
         */
        public abstract boolean check (SystemInfo system,
                                       Evaluation eval,
                                       Glyph glyph);

        @Override
        public String toString ()
        {
            return name;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean applySpecificCheck = new Constant.Boolean(
                true,
                "Should we apply specific checks on shape candidates?");

        private final Scale.Fraction maxTitleHeight = new Scale.Fraction(
                4.0,
                "Maximum normalized height for a title text");

        private final Scale.Fraction maxLyricsHeight = new Scale.Fraction(
                2.5,
                "Maximum normalized height for a lyrics text");

        private final Constant.Double minMarkerPitchPosition = new Constant.Double(
                "PitchPosition",
                -13.0,
                "Minimum pitch value for a  segno / coda marker");

        private final Constant.Double maxMeasureRepeatPitchPosition = new Constant.Double(
                "PitchPosition",
                1.0,
                "Maximum absolute pitch position for a measure repeat sign");

        private final Constant.Double minTitlePitchPosition = new Constant.Double(
                "PitchPosition",
                15.0,
                "Minimum absolute pitch position for a title");

        private final Constant.Double maxTupletPitchPosition = new Constant.Double(
                "PitchPosition",
                17.0,
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

        private final Scale.Fraction measureRestDx = new Scale.Fraction(
                0.2,
                "Minimum horizontal margin around a measure-rest candidate");
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {
        static final ShapeChecker INSTANCE = new ShapeChecker();
    }
}
