//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l y p h s C o n t r o l l e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphsModel;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.symbol.SymbolFactory;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.VoidTask;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code GlyphsController} is a common basis for interactive glyph handling,
 * used by any user interface which needs to act on the actual glyph data.
 * <p>
 * There are two main methods in this class ({@link #asyncAssignGlyphs} and
 * {@link #asyncDeassignGlyphs}). They share common characteristics:
 * <ul>
 * <li>They are processed asynchronously</li>
 * <li>Their action is recorded in the sheet script</li>
 * <li>They update the following steps, if any</li>
 * </ul>
 * Since the bus of user selections is used, the methods of this class are meant to be used from
 * within a user action, otherwise you must use a direct access to similar synchronous actions in
 * the underlying {@link GlyphsModel}.
 *
 * @author Hervé Bitteur
 */
public class GlyphsController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphsController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related model. */
    protected final GlyphsModel model;

    /** Cached sheet, if any. */
    protected final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an instance of GlyphsController, with its underlying GlyphsModel instance.
     *
     * @param model the related glyphs model
     */
    public GlyphsController (GlyphsModel model)
    {
        this.model = model;

        sheet = model.getSheet();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // asyncAssignGlyphs //
    //-------------------//
    /**
     * Asynchronously assign a shape to the selected glyph.
     *
     * @param glyph the glyph to interpret
     * @param shape the shape to be assigned
     * @return the task that carries out the processing
     */
    public Task<Void, Void> asyncAssignGlyph (Glyph glyph,
                                              Shape shape)
    {
        try {
            StaffManager staffManager = sheet.getStaffManager();
            glyph = sheet.getGlyphIndex().registerOriginal(glyph);

            if (ShapeSet.Barlines.contains(shape)) {
                //            // Special case for barlines assignment or deassignment
                //            return new BarlineTask(sheet, shape, compound, glyphs).launch(sheet);
                if (shape == Shape.BRACE) {
                    Inter brace = new BraceInter(glyph, 1);
                    Rectangle box = glyph.getBounds();
                    Point top = new Point(box.x + (box.width / 2), box.y);
                    Staff topStaff = staffManager.getClosestStaff(top);
                    Point bot = new Point(box.x + (box.width / 2), box.y + box.height);
                    Staff botStaff = staffManager.getClosestStaff(bot);

                    if (topStaff.getSystem() == botStaff.getSystem()) {
                        SystemInfo system = topStaff.getSystem();
                        system.getSig().addVertex(brace);
                        sheet.getStub().setModified(true);
                        sheet.getGlyphIndex().publish(null);
                        sheet.getInterIndex().publish(brace);
                        logger.info("Added specific {}", brace);
                    }
                } else {
                    return null;
                }
            } else {
                // TODO: while interacting with user, make sure we have the related staff & system
                SystemInfo system = null;
                Staff staff = null;
                Point center = glyph.getCenter();
                List<Staff> staves = staffManager.getStavesOf(center);

                if (staves.isEmpty()) {
                    throw new IllegalStateException("No staff for " + center);
                }

                Inter ghost = SymbolFactory.createGhost(shape, 1);
                ghost.setGlyph(glyph);
                ghost.setBounds(glyph.getBounds());

                if (staves.size() == 1) {
                    // We are within one staff height
                    staff = staves.get(0);
                    system = staff.getSystem();
                } else {
                    // We are between two staves
                    SystemInfo prevSystem = null;
                    StaffLoop:
                    for (int i = 0; i < 2; i++) {
                        system = staves.get(i).getSystem();

                        if (system != prevSystem) {
                            Collection<Partnership> partnerships = ghost.searchPartnerships(
                                    system,
                                    false);

                            for (Partnership p : partnerships) {
                                if (p.partner.getStaff() != null) {
                                    staff = p.partner.getStaff();

                                    break StaffLoop;
                                }
                            }
                        }

                        prevSystem = system;
                    }

                    if (staff == null) {
                        // TODO: Ask user!
                    }
                }

                if (staff != null) {
                    new AssignTask(system, staff, shape, glyph).execute();
                } else {
                    logger.warn("No staff known at {}", center);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error assigning " + shape + " {}", ex);
        }

        return null;
    }

    //-----------------//
    // getGlyphService //
    //-----------------//
    /**
     * Report the underlying glyph service.
     *
     * @return the related glyph service
     */
    public EntityService<? extends Glyph> getGlyphService ()
    {
        return model.getGlyphService();
    }

    //----------------//
    // getLatestShape //
    //----------------//
    /**
     * Report the latest non-null shape that was assigned, or null if none.
     *
     * @return latest shape assigned, or null if none
     */
    public Shape getLatestShapeAssigned ()
    {
        return model.getLatestShape();
    }

    //--------------------//
    // getLocationService //
    //--------------------//
    /**
     * Report the event service to use for LocationEvent.
     * When no sheet is available, override this method to point to another service
     *
     * @return the event service to use for LocationEvent
     */
    public SelectionService getLocationService ()
    {
        return model.getSheet().getLocationService();
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Report the underlying model.
     *
     * @return the underlying glyphs model
     */
    public GlyphsModel getModel ()
    {
        return model;
    }

    //----------------//
    // setLatestShape //
    //----------------//
    /**
     * Assign the latest shape.
     *
     * @param shape the latest shape
     */
    public void setLatestShapeAssigned (Shape shape)
    {
        model.setLatestShape(shape);
    }

    //---------//
    // publish //
    //---------//
    protected void publish (Glyph glyph)
    {
        // Update immediately the glyph info as displayed
        if (model.getGlyphService() != null) {
            model.getGlyphService().publish(
                    new EntityListEvent<Glyph>(
                            this,
                            SelectionHint.GLYPH_MODIFIED,
                            null,
                            Arrays.asList(glyph)));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // AssignTask //
    //------------//
    private class AssignTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        private final Staff staff;

        private final Shape shape;

        private final Glyph glyph;

        //~ Constructors ---------------------------------------------------------------------------
        public AssignTask (SystemInfo system,
                           Staff staff,
                           Shape shape,
                           Glyph glyph)
        {
            this.system = system;
            this.staff = staff;
            this.shape = shape;
            this.glyph = glyph;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected Void doInBackground ()
                throws Exception
        {
            final int interline = (staff != null) ? staff.getSpecificInterline()
                    : sheet.getScale().getInterline();

            model.assignGlyph(glyph, staff, interline, shape, 1.0);

            // Plus other impacted items...
            //TODO
            //
            return null;
        }
    }
}
