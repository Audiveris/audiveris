//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F i l a m e n t V i e w                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.lag.Section;

import omr.selection.EntityService;

import omr.sheet.Sheet;

import omr.ui.EntityView;

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
