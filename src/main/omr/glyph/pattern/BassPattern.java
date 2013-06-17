//----------------------------------------------------------------------------//
//                                                                            //
//                           B a s s P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder;
import omr.glyph.CompoundBuilder.CompoundAdapter;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code BassPattern} checks for segmented bass clefs, in the
 * neighborhood of typical vertical two-dot patterns
 *
 * @author Hervé Bitteur
 */
public class BassPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            BassPattern.class);

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // BassPattern //
    //-------------//
    /**
     * Creates a new BassPattern object.
     *
     * @param system the containing system
     */
    public BassPattern (SystemInfo system)
    {
        super("Bass", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int successNb = 0;

        // Constants for clef verification
        final double maxBassDotPitchDy = constants.maxBassDotPitchDy.getValue();
        final double maxBassDotDx = scale.toPixels(constants.maxBassDotDx);

        // Specific adapter definition for bass clefs
        CompoundAdapter bassAdapter = new BassAdapter(
                system,
                Grades.clefMinGrade);

        for (Glyph top : system.getGlyphs()) {
            // Look for top dot
            if ((top.getShape() != Shape.DOT_set)
                || (Math.abs(top.getPitchPosition() - -3) > maxBassDotPitchDy)) {
                continue;
            }

            int topX = top.getCentroid().x;
            StaffInfo topStaff = system.getStaffAt(top.getCentroid());

            // Look for bottom dot right underneath, and in the same staff
            for (Glyph bot : system.getGlyphs()) {
                if ((bot.getShape() != Shape.DOT_set)
                    || (Math.abs(bot.getPitchPosition() - -1) > maxBassDotPitchDy)) {
                    continue;
                }

                if (Math.abs(bot.getCentroid().x - topX) > maxBassDotDx) {
                    continue;
                }

                if (system.getStaffAt(bot.getCentroid()) != topStaff) {
                    continue;
                }

                // Here we have a couple
                logger.debug(
                        "Got bass dots #{} & #{}",
                        top.getId(),
                        bot.getId());

                Glyph compound = system.buildCompound(
                        top,
                        true,
                        system.getGlyphs(),
                        bassAdapter);

                if (compound != null) {
                    successNb++;
                }
            }
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxBassDotDx = new Scale.Fraction(
                0.25,
                "Tolerance on Bass dot abscissae");

        Constant.Double maxBassDotPitchDy = new Constant.Double(
                "pitch",
                0.5,
                "Ordinate tolerance on a Bass dot pitch position");

    }

    //-------------//
    // BassAdapter //
    //-------------//
    /**
     * This is the compound adapter meant to build bass clefs
     */
    private class BassAdapter
            extends CompoundBuilder.TopShapeAdapter
    {
        //~ Constructors -------------------------------------------------------

        public BassAdapter (SystemInfo system,
                            double minGrade)
        {
            super(system, minGrade, ShapeSet.BassClefs);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Rectangle computeReferenceBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                        "Compound seed has not been set");
            }

            Rectangle pixRect = new Rectangle(seed.getCentroid());
            pixRect.add(
                    new Point(
                    pixRect.x - (2 * scale.getInterline()),
                    pixRect.y + (3 * scale.getInterline())));

            return pixRect;
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape()
                   || ShapeSet.BassClefs.contains(glyph.getShape());
        }
    }
}
