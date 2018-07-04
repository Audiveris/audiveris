//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l y p h s C o n t r o l l e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.SelectionService;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code GlyphsController} is a common basis for interactive glyph handling,
 * used by any user interface which needs to act on the actual glyph data.
 * <p>
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
}
