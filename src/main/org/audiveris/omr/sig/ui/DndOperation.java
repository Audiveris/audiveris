//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     D n d O p e r a t i o n                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.BasicStroke;
import org.audiveris.omr.OMR;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.curve.Curves;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.OmrGlassPane;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.List;

/**
 * Class {@code DndOperation} handles one DnD operation with a moving inter.
 *
 * @author Hervé Bitteur
 */
public class DndOperation
{

    private static final Logger logger = LoggerFactory.getLogger(DndOperation.class);

    /** Related sheet. */
    private final Sheet sheet;

    /** GlassPane. */
    private final OmrGlassPane glass = OMR.gui.getGlassPane();

    /** Zoom ratio on sheet view, to be "replicated" on glass pane. */
    private final double zoomRatio;

    /** Pay-load: the Inter instance being "moved". */
    private final Inter ghost;

    /** Ghost tracker. */
    private final InterTracker ghostTracker;

    /** Non-decorated symbol to determine inter geometry. */
    private final ShapeSymbol symbol;

    /** Stroke for symbol curves (slurs, wedges, endings). */
    private final Stroke curveStroke;

    /** Current staff. */
    private Staff staff;

    /** Current system. */
    private SystemInfo system;

    /**
     * Creates a new {@code DndOperation} object.
     *
     * @param sheet  the related sheet
     * @param zoom   zoom applied on display
     * @param ghost  the inter being dragged
     * @param symbol the originating symbol
     */
    public DndOperation (Sheet sheet,
                         Zoom zoom,
                         Inter ghost,
                         ShapeSymbol symbol)
    {
        this.sheet = sheet;
        this.zoomRatio = zoom.getRatio();
        this.ghost = ghost;
        this.symbol = symbol;

        curveStroke = buildCurveStroke();

        ghostTracker = ghost.getTracker(sheet);
    }

    //------//
    // drop //
    //------//
    /**
     * Drop the ghost inter at provided location.
     * <p>
     * Finalize ghost info (staff and bounds), insert into proper SIG and link to partners if any.
     *
     * @param dropPoint provided drop location
     */
    public void drop (Point dropPoint)
    {
        if (staff == null) {
            final List<Staff> staves = sheet.getStaffManager().getStavesOf(dropPoint);

            if (staves.isEmpty()) {
                logger.info("Drop point lies beyond sheet limits");
                return;
            }

            if (staves.size() == 1) {
                staff = staves.get(0);
            } else {
                // Prompt user...
                final int option = StaffSelection.getInstance().prompt();

                if (option >= 0) {
                    staff = staves.get(option);
                } else {
                    return;
                }
            }
        }

        // Staff
        ghost.setStaff(staff);

        updateGhost(dropPoint); // dropPoint can be modified

        sheet.getInterController().addInters(Arrays.asList(ghost));
        ///sheet.getSymbolsEditor().openEditMode(ghost);

        logger.debug("Dropped {} at {}", this, dropPoint);
    }

    //----------------//
    // enteringTarget //
    //----------------//
    /**
     * Call-back for mouse entering target component (the sheet view).
     */
    public void enteringTarget ()
    {
        symbol.updateModel(sheet);
        updateImage(sheet.getScale().getInterline());
    }

    //----------//
    // getGhost //
    //----------//
    public Inter getGhost ()
    {
        return ghost;
    }

    //-----------------//
    // getGhostTracker //
    //-----------------//
    public InterTracker getGhostTracker ()
    {
        return ghostTracker;
    }

    //-------------------//
    // getStaffReference //
    //-------------------//
    /**
     * Report the staff reference point for the moving inter location.
     * <p>
     * We use a "sticky staff" approach to visually indicate the current related staff.
     *
     * @param location (input/output) current inter location, which can be modified to locate the
     *                 inter differently (typically for a snap to grid)
     * @return the location of reference entity
     */
    public Point getStaffReference (Point location)
    {
        Staff closestStaff = sheet.getStaffManager().getClosestStaff(location);

        if (closestStaff == null) {
            staff = null;
            system = null;
        } else {
            double pp = closestStaff.pitchPositionOf(location);

            if (Math.abs(pp) <= 4) {
                // We are within staff height, so let's pick up this staff
                if (staff != closestStaff) {
                    if (system != closestStaff.getSystem()) {
                        system = closestStaff.getSystem();
                    }

                    // Adjust image size WRT new interline
                    if ((staff == null) || (staff.getSpecificInterline() != closestStaff
                            .getSpecificInterline())) {
                        updateImage(closestStaff.getSpecificInterline());
                    }

                    staff = closestStaff;
                    system = staff.getSystem();
                }
            }
        }

        ghost.setStaff(staff);
        ghostTracker.setSystem(system);

        if (staff == null) {
            return null;
        }

        updateGhost(location); // This may modify location slightly

        LineInfo line = staff.getLines().get(2); // Middle staff line

        if (location.x < line.getEndPoint(HorizontalSide.LEFT).getX()) {
            return PointUtil.rounded(line.getEndPoint(HorizontalSide.LEFT));
        }

        if (location.x > line.getEndPoint(HorizontalSide.RIGHT).getX()) {
            return PointUtil.rounded(line.getEndPoint(HorizontalSide.RIGHT));
        }

        return new Point(location.x, line.yAt(location.x));
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

    //------------------//
    // buildCurveStroke //
    //------------------//
    /**
     * Build stroke for curves, based on sheet scale and view zoom.
     *
     * @return curve stroke
     */
    private Stroke buildCurveStroke ()
    {
        Scale scale = sheet.getScale();
        Integer fore = (scale != null) ? scale.getFore() : null;
        double thickness = (fore != null) ? fore : Curves.DEFAULT_THICKNESS;
        float curveThickness = (float) (zoomRatio * thickness);

        return new BasicStroke(curveThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    //-------------//
    // updateGhost //
    //-------------//
    /**
     * Update ghost location and geometry according to the provided new location.
     *
     * @param location (input/output) ghost new location
     */
    private void updateGhost (Point location)
    {
        // We use the non-decorated symbol
        final int staffInterline = staff.getSpecificInterline();
        final MusicFont font = (ShapeSet.Heads.contains(ghost.getShape()))
                ? MusicFont.getHeadFont(sheet.getScale(), staffInterline)
                : MusicFont.getBaseFont(staffInterline);
        ghost.deriveFrom(symbol, font, location, Alignment.AREA_CENTER);
    }

    //-------------//
    // updateImage //
    //-------------//
    /**
     * Update ghost image based on provided interline value.
     * <ul>
     * <li>Called when the mouse location enters the sheet view.
     * <li>Called also when moving from one staff to another if these staves exhibit different
     * interline values.
     * </ul>
     *
     * @param interline provided interline value
     */
    private void updateImage (int interline)
    {
        // Adapt image to current interline
        int zoomedInterline = (int) Math.rint(zoomRatio * interline);
        MusicFont font = MusicFont.getBaseFont(zoomedInterline);

        glass.setImage(symbol.getDecoratedSymbol().buildImage(font, curveStroke));
    }
}
