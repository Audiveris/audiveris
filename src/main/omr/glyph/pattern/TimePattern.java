//----------------------------------------------------------------------------//
//                                                                            //
//                           T i m e P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.EnumSet;

/**
 * Class {@code TimePattern} verifies the time signature glyphs.
 *
 * @author Hervé Bitteur
 */
public class TimePattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TimePattern.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TimePattern object.
     * @param system the containing system
     */
    public TimePattern (SystemInfo system)
    {
        super("Time", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * Check that each staff begins with a time
     * @return the number of times rebuilt
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int successNb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (!ShapeRange.Times.contains(glyph.getShape())) {
                continue;
            }

            // We must find a time out of these glyphs
            Glyph compound = system.buildCompound(
                glyph,
                true,
                system.getGlyphs(),
                new TimeSigAdapter(
                    system,
                    Grades.timeMinGrade,
                    ShapeRange.FullTimes));

            if (compound != null) {
                successNb++;
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

        Scale.Fraction xOffset = new Scale.Fraction(
            0.25d,
            "Core Time horizontal offset");
        Scale.Fraction yOffset = new Scale.Fraction(
            0.25d,
            "Core Time vertical offset");
    }

    //----------------//
    // TimeSigAdapter //
    //----------------//
    /**
     * Compound adapter to search for a time sig shape
     */
    private class TimeSigAdapter
        extends CompoundBuilder.TopRawAdapter
    {
        //~ Constructors -------------------------------------------------------

        public TimeSigAdapter (SystemInfo     system,
                               double         minGrade,
                               EnumSet<Shape> desiredShapes)
        {
            super(system, minGrade, desiredShapes);
        }

        //~ Methods ------------------------------------------------------------

        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape();
        }

        @Override
        public Evaluation getChosenEvaluation ()
        {
            return new Evaluation(chosenEvaluation.shape, Evaluation.ALGORITHM);
        }

        public PixelRectangle computeReferenceBox ()
        {
            // Retrieve environment (staff)
            final int      xOffset = scale.toPixels(constants.xOffset);
            final int      yOffset = scale.toPixels(constants.yOffset);

            // Define the core box to intersect time glyph(s)
            PixelPoint     center = seed.getAreaCenter();
            StaffInfo      staff = system.getStaffAt(center);

            PixelRectangle rect = seed.getContourBox();
            rect.grow(-xOffset, 0);
            rect.y = staff.getFirstLine()
                          .yAt(center.x) + yOffset;
            rect.height = staff.getLastLine()
                               .yAt(center.x) - yOffset - rect.y;

            // Draw the time core box, for visual debug
            seed.addAttachment("t", rect);

            return rect;
        }
    }
}
