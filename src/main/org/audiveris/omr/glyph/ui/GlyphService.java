//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h S e r v i c e                                    //
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
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.ViewParameters.SelectionMode;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.IdEvent;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code GlyphService} is an EntityService for glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphService
        extends EntityService<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphService.class);

    /** Events that can be published on a glyph service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        IdEvent.class, EntityListEvent.class
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code GlyphService} object.
     *
     * @param index           underlying glyph index
     * @param locationService related service for location info
     */
    public GlyphService (EntityIndex<Glyph> index,
                         SelectionService locationService)
    {
        super(index, locationService, eventsAllowed);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getMostRelevant //
    //-----------------//
    @Override
    protected Glyph getMostRelevant (List<Glyph> list)
    {
        switch (list.size()) {
        case 0:
            return null;

        case 1:
            return list.get(0);

        default:

            List<Glyph> copy = new ArrayList<Glyph>(list);
            Collections.sort(copy, Glyphs.byWeight);

            return copy.get(0);
        }
    }

    //---------------------//
    // handleLocationEvent //
    //---------------------//
    /**
     * Interest in location &rArr; list (basket?)
     *
     * @param locationEvent the location event
     */
    @Override
    protected void handleLocationEvent (LocationEvent locationEvent)
    {
        // Search only when in MODE_GLYPH or MODE_INTER
        if (ViewParameters.getInstance().getSelectionMode() != SelectionMode.MODE_SECTION) {
            super.handleLocationEvent(locationEvent);

            if (basket.size() > 1) {
                // Build compound on-the-fly and publish it (no impact on basket)
                Glyph compound = GlyphFactory.buildGlyph(basket);
                publish(
                        new EntityListEvent<Glyph>(
                                this,
                                SelectionHint.ENTITY_TRANSIENT,
                                locationEvent.movement,
                                compound));
            }
        }
    }
}
