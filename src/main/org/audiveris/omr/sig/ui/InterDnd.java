//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         I n t e r D n d                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

/**
 * Class {@code InterDnd} handles one DnD operation with a moving inter (ghost).
 * <p>
 * The dragged inter originates from the ShapeBoard, it can move between systems
 * (as opposed to an {@link InterEditor}) until the inter is finally dropped into a system
 * or abandoned outside the sheet view.
 * <p>
 * It cannot shift view limits.
 *
 * @see InterEditor
 *
 * @author Hervé Bitteur
 */
public class InterDnd
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterDnd.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** GlassPane. */
    private final OmrGlassPane glass = OMR.gui.getGlassPane();

    /** Zoom ratio on sheet view, to be "replicated" on glass pane. */
    private final double zoomRatio;

    /** Pay-load: the Inter instance being "moved". */
    private final Inter ghost;

    /** Dedicated ghost tracker. */
    private final InterTracker tracker;

    /** Non-decorated symbol to determine inter geometry. */
    private final ShapeSymbol symbol;

    /** Stroke for symbol curves (slurs, wedges, endings). */
    private final Stroke curveStroke;

    /** Current staff. */
    private Staff staff;

    /** Current system. */
    private SystemInfo system;

    /** Current staff reference point, if any. */
    private Point staffReference;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code InterDnd} object.
     *
     * @param ghost  the inter being dragged
     * @param sheet  the containing sheet
     * @param symbol the originating symbol
     */
    public InterDnd (Inter ghost,
                     Sheet sheet,
                     ShapeSymbol symbol)
    {
        this.sheet = sheet;
        this.ghost = ghost;
        this.symbol = symbol;

        final ScrollView scrollView = sheet.getStub().getAssembly().getSelectedScrollView();
        zoomRatio = scrollView.getView().getZoom().getRatio();

        curveStroke = buildCurveStroke();

        tracker = ghost.getTracker(sheet);
    }

    //~ Methods ------------------------------------------------------------------------------------
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
            // Some ghosts require being located in a staff, not even a user prompt is relevant!
            if (ghost.imposeWithinStaffHeight()) {
                return;
            }

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

        if (updateGhost(dropPoint)) {
            // dropPoint may have been modified, as well as ghost staff
            staff = ghost.getStaff();

            if (staff != null) {
                sheet.getInterController().addInter(ghost);
                ///sheet.getSymbolsEditor().openEditMode(ghost);
                logger.debug("Dropped {} at {}", this, dropPoint);
            } else {
                logger.debug("Ghost {} could not be dropped on staff", ghost);
            }
        }
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

    //------------//
    // getTracker //
    //------------//
    public InterTracker getTracker ()
    {
        return tracker;
    }

    //--------------//
    // hasReference //
    //--------------//
    public boolean hasReference ()
    {
        return staffReference != null;
    }

    //------//
    // move //
    //------//
    /**
     * Move inter, compute the staff reference point if any and perhaps resize the inter.
     * <p>
     * We use a "sticky staff" approach to visually indicate the current related staff.
     *
     * @param location (input/output) current inter location, which can be modified to locate the
     *                 inter differently (typically for a snap to grid)
     */
    public void move (Point location)
    {
        staffReference = null;

        final Staff closestStaff = sheet.getStaffManager().getClosestStaff(location);

        if (closestStaff == null) {
            staff = null;
            system = null;
        } else {
            if (closestStaff.contains(location)) {
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
            } else if (ghost.imposeWithinStaffHeight()) {
                staff = null;
                system = null;
            }
        }

        ghost.setStaff(staff);
        tracker.setSystem(system);

        if (staff != null) {
            if (updateGhost(location)) {
                // Location may have been modified slightly, as well as ghost staff
                staff = ghost.getStaff();

                if (staff != null) {
                    // Retrieve staff reference
                    final LineInfo line = staff.getMidLine();

                    if (location.x < line.getEndPoint(HorizontalSide.LEFT).getX()) {
                        staffReference = PointUtil.rounded(line.getEndPoint(HorizontalSide.LEFT));
                    } else if (location.x > line.getEndPoint(HorizontalSide.RIGHT).getX()) {
                        staffReference = PointUtil.rounded(line.getEndPoint(HorizontalSide.RIGHT));
                    } else {
                        staffReference = new Point(location.x, line.yAt(location.x));
                    }
                }
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');

        if (staff != null) {
            sb.append(" staff:").append(staff.getId());
        }

        if (ghost != null) {
            sb.append(" ghost:").append(ghost);
        }

        return sb.append('}').toString();
    }

    //----------------//
    // getSceneBounds //
    //----------------//
    /**
     * The scene is composed of inter image plus its decorations if any
     * (staff reference, support links, ledgers).
     *
     * @return bounding box of inter + decorations if any + reference point
     */
    public Rectangle getSceneBounds ()
    {
        if (staffReference == null) {
            return null;
        }

        final Rectangle box = tracker.getSceneBounds();
        box.add(staffReference);

        return box;
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics2D g)
    {
        if (staffReference != null) {
            // Draw line to staff reference if any
            g.setColor(Color.RED);

            final Point center = ghost.getCenter();
            g.drawLine(center.x, center.y, staffReference.x, staffReference.y);
        }

        tracker.render(g);
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
        final Scale scale = sheet.getScale();
        final Integer fore = (scale != null) ? scale.getFore() : null;
        final double thickness = (fore != null) ? fore : Curves.DEFAULT_THICKNESS;
        final float curveThickness = (float) (zoomRatio * thickness);

        return new BasicStroke(curveThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    //-------------//
    // updateGhost //
    //-------------//
    /**
     * Update ghost location and geometry according to the provided new location.
     *
     * @param location (input/output) ghost new location
     * @return true if OK
     */
    private boolean updateGhost (Point location)
    {
        // We use the non-decorated symbol
        final int staffInterline = staff.getSpecificInterline();
        final MusicFont font = (ShapeSet.Heads.contains(ghost.getShape()))
                ? MusicFont.getHeadFont(sheet.getScale(), staffInterline)
                : MusicFont.getBaseFont(staffInterline);

        return ghost.deriveFrom(symbol, sheet, font, location, Alignment.AREA_CENTER);
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
        final int zoomedInterline = (int) Math.rint(zoomRatio * interline);
        final MusicFont font = MusicFont.getBaseFont(zoomedInterline);
        glass.setImage(symbol.getDecoratedSymbol().buildImage(font, curveStroke));
    }
}
