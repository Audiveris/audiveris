//----------------------------------------------------------------------------//
//                                                                            //
//                   A r t i c u l a t i o n P a t t e r n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.sheet.NotePosition;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code ArticulationPattern} verifies that any articulation
 * glyph has corresponding note(s) below or above in the staff.
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
    private static final Logger logger = LoggerFactory.getLogger(
            ArticulationPattern.class);

    //~ Constructors -----------------------------------------------------------
    //---------------------//
    // ArticulationPattern //
    //---------------------//
    /**
     * Creates a new ArticulationPattern object.
     *
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
            if (!ShapeSet.Articulations.contains(glyph.getShape())
                || glyph.isManualShape()) {
                continue;
            }

            Point center = glyph.getAreaCenter();
            NotePosition pos = system.getNoteStaffAt(center);
            StaffInfo staff = pos.getStaff();
            Rectangle box = glyph.getBounds();

            // Extend height till end of staff area
            double topLimit = staff.getLimitAtX(VerticalSide.TOP, center.x);
            double botLimit = staff.getLimitAtX(VerticalSide.BOTTOM, center.x);
            box.y = (int) Math.rint(topLimit);
            box.height = (int) Math.rint(botLimit) - box.y;
            box.grow(xMargin, 0);

            List<Glyph> glyphs = system.lookupIntersectedGlyphs(box, glyph);
            boolean hasNote = Glyphs.contains(
                    glyphs,
                    new Predicate<Glyph>()
            {
                @Override
                public boolean check (Glyph entity)
                {
                    Shape shape = entity.getShape();

                    return ShapeSet.NoteHeads.contains(shape)
                           || ShapeSet.Notes.contains(shape)
                           || ShapeSet.Rests.contains(shape);
                }
            });

            if (!hasNote) {
                logger.debug("Deassign articulation {}", glyph.idString());

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
