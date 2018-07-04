//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F i l a m e n t V i e w                                    //
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.lag.Section;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.EntityView;
import org.audiveris.omr.ui.selection.EntityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Class {@code FilamentView} handles a view on sheet filaments.
 *
 *
 * @author Hervé Bitteur
 */
public class FilamentView
        extends EntityView<Filament>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FilamentView.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FilamentView} object.
     *
     * @param entityService UI service on filaments
     */
    public FilamentView (EntityService<Filament> entityService)
    {
        super(entityService);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected void render (Graphics2D g)
    {
        // Render all filaments
        for (Filament fil : entityService.getIndex().getEntities()) {
            for (Section section : fil.getMembers()) {
                section.render(g, false, null);
            }
        }
    }

    @Override
    protected void renderItems (Graphics2D g)
    {
        // Render selected filament(s) is any
        List<Filament> list = entityService.getSelectedEntityList();

        if ((list != null) && !list.isEmpty()) {
            ///logger.info("{} selected filaments", list.size());
            for (Filament fil : list) {
                for (Section section : fil.getMembers()) {
                    section.render(g, false, Color.BLUE);
                }
            }
        }

        // Grid lines, if any
        Sheet sheet = ((FilamentIndex) entityService.getIndex()).getSheet();

        if (sheet != null) {
            sheet.renderItems(g);
        }
    }
}
