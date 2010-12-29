//----------------------------------------------------------------------------//
//                                                                            //
//                          S h a p e P a t t e r n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.util.Implement;

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
     * @param system the containing system
     */
    public ShapePattern (SystemInfo system)
    {
        super("Shape", system);

        shapeCheckerMap = new EnumMap<Shape, Collection<ShapeChecker>>(
            Shape.class);
        registerShapeChecks();
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * A general pattern to check some glyph shapes within their environment
     * @return the number of glyphs deassigned
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
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
        //        new ShapeChecker(Shape.WHOLE_NOTE) {
        //                @Override
        //                public boolean check (Shape shape,
        //                                      Glyph glyph)
        //                {
        //                    // Check that whole notes are not too far from staves 
        //                    // without ledgers
        //                    PixelPoint point = glyph.getAreaCenter();
        //                    StaffInfo  staff = system.getStaffAtY(point.y);
        //                    double     pitch = staff.pitchPositionOf(point);
        //
        //                    if (Math.abs(pitch) <= 6) {
        //                        return true;
        //                    }
        //
        //                    Set<Ledger> ledgers = staff.getLedgersToStaff(
        //                        point,
        //                        system);
        //
        //                    return !ledgers.isEmpty();
        //                }
        //            };
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

        public ShapeChecker (Collection<Shape>... shapeCollections)
        {
            Collection<Shape> shapes = new HashSet<Shape>();

            for (Collection<Shape> col : shapeCollections) {
                shapes.addAll(col);
            }

            addChecker(shapes.toArray(new Shape[0]));
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
    }
}
