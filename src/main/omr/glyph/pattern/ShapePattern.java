/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeRange;
import static omr.glyph.ShapeRange.*;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.Clef;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sheet.Scale;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import java.util.*;

/**
 * Class {@code ShapePattern} defines the general glyph pattern based on glyph
 * shape
 *
 * @author Hervé Bitteur
 */
public class ShapePattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ShapePattern.class);

    //~ Instance fields --------------------------------------------------------

    /** Map of Shape => Sequence of checkers */
    private final EnumMap<Shape, Collection<ShapeChecker>> shapeCheckerMap;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ShapePattern //
    //--------------//
    /**
     * Creates a new ShapePattern object.
     */
    public ShapePattern (SystemInfo system)
    {
        super("Shape", system);

        shapeCheckerMap = new EnumMap<Shape, Collection<ShapeChecker>>(
            Shape.class);
        registerShapeChecks();
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    /**
     * A general pattern to check some glyph shapes within their environment
     * @return the number of glyphs deassigned
     */
    @Override
    public int run ()
    {
        int modifNb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            Shape shape = glyph.getShape();

            if ((shape == null) || glyph.isManualShape()) {
                continue;
            }

            Collection<ShapeChecker> checkers = shapeCheckerMap.get(shape);

            if (checkers == null) {
                continue;
            }

            for (ShapeChecker checker : checkers) {
                if (!checker.check(shape, glyph)) {
                    glyph.setShape(null, Evaluation.ALGORITHM);
                    modifNb++;

                    break;
                }
            }
        }

        return modifNb;
    }

    //---------------------//
    // registerShapeChecks //
    //---------------------//
    private void registerShapeChecks ()
    {
        new ShapeChecker(BRACKET, BRACE) {
                public boolean check (Shape shape,
                                      Glyph glyph)
                {
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
                                "Deassigned " + shape + " glyph #" +
                                glyph.getId());
                        }

                        return false;
                    } else {
                        return true;
                    }
                }
            };

        new ShapeChecker(Tuplets) {
                public boolean check (Shape shape,
                                      Glyph glyph)
                {
                    // They must be within the abscissa bounds of the system
                    PixelRectangle glyphBox = glyph.getContourBox();

                    if (((glyphBox.x + glyphBox.width) < system.getLeft()) ||
                        (glyphBox.x > system.getRight())) {
                        return false;
                    }

                    return true;
                }
            };

        new ShapeChecker(Keys) {
                public boolean check (Shape shape,
                                      Glyph glyph)
                {
                    // They must be rather close to the left side of the measure
                    ScoreSystem     scoreSystem = system.getScoreSystem();
                    Scale           scale = scoreSystem.getScale();
                    double          maxKeyXOffset = scale.toPixels(
                        constants.maxKeyXOffset);
                    SystemRectangle box = scoreSystem.toSystemRectangle(
                        glyph.getContourBox());
                    SystemPoint     point = box.getLocation();
                    SystemPart      part = scoreSystem.getPartAt(point);
                    Measure         measure = part.getMeasureAt(point);
                    Staff           staff = part.getStaffAt(point);
                    Clef            clef = measure.getFirstMeasureClef(
                        staff.getId());
                    int             start = (clef != null)
                                            ? (clef.getBox().x +
                                            clef.getBox().width)
                                            : measure.getLeftX();

                    if ((point.x - start) > maxKeyXOffset) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Glyph#" + glyph.getId() +
                                " Key too far on right");
                        }

                        return false;
                    }

                    return true;
                }
            };
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // ShapeChecker //
    //--------------//
    /**
     * A checker runs a series of checks wrt the system glyphs of a given shape
     */
    private abstract class ShapeChecker
    {
        //~ Constructors -------------------------------------------------------

        public ShapeChecker (Shape... shapes)
        {
            addChecker(shapes);
        }

        public ShapeChecker (EnumSet<Shape> shapes)
        {
            addChecker(shapes.toArray(new Shape[0]));
        }

        public ShapeChecker (ShapeRange... shapeRanges)
        {
            addChecker(shapeRanges);
        }

        //~ Methods ------------------------------------------------------------

        /**
         * Run the specific test
         * @param system the containing system
         * @param shape the potential shape
         * @param glyph the glyph at hand
         * @return true for positive test, false otherwise
         */
        public abstract boolean check (Shape shape,
                                       Glyph glyph);

        private void addChecker (Shape[] shapes)
        {
            for (Shape shape : shapes) {
                Collection<ShapeChecker> checks = shapeCheckerMap.get(shape);

                if (checks == null) {
                    checks = new ArrayList<ShapeChecker>();
                    shapeCheckerMap.put(shape, checks);
                }

                checks.add(this);
            }
        }

        private void addChecker (ShapeRange[] shapeRanges)
        {
            for (ShapeRange range : shapeRanges) {
                addChecker(range.getShapes().toArray(new Shape[0]));
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxKeyXOffset = new Scale.Fraction(
            2,
            "Maximum horizontal offset for a key since clef or measure start");
    }
}
