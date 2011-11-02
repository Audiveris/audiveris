//----------------------------------------------------------------------------//
//                                                                            //
//                   A r t i c u l a t i o n P a t t e r n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.NotePosition;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
import omr.util.VerticalSide;

import java.util.List;

/**
 * Class {@code ArticulationPattern} verifies that any articulation
 * glyph has corresponding note(s) in the staff underneath.
 *
 * @author Hervé Bitteur
 */
public class ArticulationPattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ArticulationPattern.class);

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // ArticulationPattern //
    //---------------------//
    /**
     * Creates a new ArticulationPattern object.
     * @param system the system to process
     */
    public ArticulationPattern (SystemInfo system)
    {
        super("Articulation", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int xMargin = system.getSheet()
                            .getScale()
                            .toPixels(constants.xMargin);
        int nb = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (!ShapeRange.Articulations.contains(glyph.getShape())) {
                continue;
            }

            PixelPoint     center = glyph.getAreaCenter();
            NotePosition   pos = system.getNoteStaffAt(center);
            StaffInfo      staff = pos.getStaff();
            PixelRectangle box = glyph.getContourBox();

            // Extend height till end of staff area
            double limit = staff.getLimitAtX(VerticalSide.BOTTOM, center.x);
            box.height = (int) Math.rint(limit) - box.y;
            box.grow(xMargin, 0);

            List<Glyph> glyphs = system.lookupIntersectedGlyphs(box, glyph);
            boolean     hasNote = Glyphs.contains(
                glyphs,
                new Predicate<Glyph>() {
                        public boolean check (Glyph entity)
                        {
                            Shape shape = entity.getShape();

                            return ShapeRange.NoteHeads.contains(shape) ||
                                   ShapeRange.Notes.contains(shape) ||
                                   ShapeRange.Rests.contains(shape);
                        }
                    });

            if (!hasNote) {
                if (logger.isFineEnabled()) {
                    logger.info("Deassign articulation glyph#" + glyph.getId());
                }

                glyph.setShape(null, Evaluation.ALGORITHM);
                nb++;
            }
        }

        return nb;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction xMargin = new Scale.Fraction(
            0.5,
            "Abscissa margin around articulation");
    }
}
