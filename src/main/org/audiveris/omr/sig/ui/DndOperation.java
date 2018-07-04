//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     D n d O p e r a t i o n                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.OmrGlassPane;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Class {@code DndOperation} handles one DnD operation with a moving inter.
 *
 * @author Hervé Bitteur
 */
public class DndOperation
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DndOperation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** GlassPane. */
    private final OmrGlassPane glass = OMR.gui.getGlassPane();

    private final Zoom zoom;

    /** Pay-load: the Inter instance being "moved". */
    private final Inter ghost;

    /** Staff currently related. */
    private Staff staff;

    /** System currently related. */
    private SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DndOperation} object.
     *
     * @param sheet the related sheet
     * @param zoom  zoom applied on display
     * @param ghost the inter being "moved"
     */
    public DndOperation (Sheet sheet,
                         Zoom zoom,
                         Inter ghost)
    {
        this.sheet = sheet;
        this.zoom = zoom;
        this.ghost = ghost;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // drop //
    //------//
    /**
     * Drop the ghost inter at provided location.
     * <p>
     * Finalize ghost info (staff and bounds), insert into proper SIG
     * and link to partners if any.
     *
     * @param center provided location
     */
    public void drop (Point center)
    {
        if (staff == null) {
            logger.warn("No staff selected for drop");

            return;
        }

        // Staff
        ghost.setStaff(staff);

        // Bounds
        final int staffInterline = staff.getSpecificInterline();
        final MusicFont font = (ShapeSet.Heads.contains(ghost.getShape()))
                ? MusicFont.getHeadFont(sheet.getScale(), staffInterline)
                : MusicFont.getBaseFont(staffInterline);
        final ShapeSymbol symbol = Symbols.getSymbol(ghost.getShape());
        final Dimension dim = symbol.getDimension(font);
        final Rectangle bounds = new Rectangle(
                center.x - (dim.width / 2),
                center.y - (dim.height / 2),
                dim.width,
                dim.height);
        ghost.setBounds(bounds);

        sheet.getInterController().addInters(Arrays.asList(ghost));

        logger.debug("Dropped {} at {}", this, center);
    }

    //----------------//
    // enteringTarget //
    //----------------//
    /**
     * Call-back when mouse is entering the target component.
     */
    public void enteringTarget ()
    {
        updateImage(sheet.getScale().getInterline());
    }

    //----------//
    // getGhost //
    //----------//
    public Inter getGhost ()
    {
        return ghost;
    }

    //--------------//
    // getReference //
    //--------------//
    /**
     * Report the reference point for the moving inter located on 'center' point.
     * <p>
     * Staff above
     * Staff above or below
     * Staff below: coda, ... all markers?
     * Note head on right: alteration
     * HeadChord above or below: articulation
     * etc
     *
     * @param center current inter center
     * @return the location of reference entity
     */
    public Point getReference (Point center)
    {
        // By default, use a "sticky staff" approach...
        Staff closestStaff = sheet.getStaffManager().getClosestStaff(center);

        if (closestStaff == null) {
            staff = null;
            system = null;
        } else {
            double pp = closestStaff.pitchPositionOf(center);

            if (Math.abs(pp) <= 4) {
                // We are within staff height, so let's pick up this staff
                if (staff != closestStaff) {
                    if (system != closestStaff.getSystem()) {
                        system = closestStaff.getSystem();

                        // Retrieve system heads????????????????????????
                    }

                    // Adjust image size WRT new interline
                    if ((staff == null)
                        || (staff.getSpecificInterline() != closestStaff.getSpecificInterline())) {
                        updateImage(closestStaff.getSpecificInterline());
                    }

                    staff = closestStaff;
                    system = staff.getSystem();
                }
            }
        }

        if (staff == null) {
            return null;
        }

        LineInfo line = staff.getLines().get(2);

        if (center.x < line.getEndPoint(HorizontalSide.LEFT).getX()) {
            return PointUtil.rounded(line.getEndPoint(HorizontalSide.LEFT));
        }

        if (center.x > line.getEndPoint(HorizontalSide.RIGHT).getX()) {
            return PointUtil.rounded(line.getEndPoint(HorizontalSide.RIGHT));
        }

        return new Point(center.x, line.yAt(center.x));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("DnDOperation{");

        if (staff != null) {
            sb.append(" staff:").append(staff.getId());
        }

        if (ghost != null) {
            sb.append(" ghost:").append(ghost);
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // updateImage //
    //-------------//
    /**
     * Build ghost image based on provided interline value.
     *
     * @param interline provided interline
     */
    private void updateImage (int interline)
    {
        // Adapt image to current zoom and interline
        int zoomedInterline = (int) Math.rint(zoom.getRatio() * interline);
        Shape shape = ghost.getShape();
        BufferedImage image = MusicFont.buildImage(shape, zoomedInterline, true); // Decorated

        if (image != null) {
            glass.setImage(image);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
    }
}
