//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    F i l a m e n t I n d e x                                   //
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.IdEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.util.BasicIndex;
import org.audiveris.omr.util.IntUtil;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

/**
 * Class {@code FilamentIndex} is a global index for handled filaments in a sheet.
 *
 * @author Hervé Bitteur
 */
public class FilamentIndex
        extends BasicIndex<Filament>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FilamentIndex.class);

    /** Events that can be published on filament service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        EntityListEvent.class, IdEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FilamentIndex} object.
     *
     * @param sheet the related sheet
     */
    public FilamentIndex (Sheet sheet)
    {
        super(new AtomicInteger(0));

        this.sheet = sheet;

        // Declared VIP IDs?
        List<Integer> vipIds = IntUtil.parseInts(constants.vipFilaments.getValue());

        if (!vipIds.isEmpty()) {
            logger.info("VIP filaments:{} in {}", vipIds, sheet);
            setVipIds(vipIds);
        }

        // User filament service?
        if (OMR.gui != null) {
            FilamentService filamentService = new FilamentService();
            setEntityService(filamentService);

            //
            //            // Subscriptions
            //            for (Class<?> eventClass : eventsRead) {
            //                entityService.subscribeStrongly(eventClass, filamentService);
            //            }
            //
            //            // Subscription on location
            //            sheet.getLocationService().subscribeStrongly(LocationEvent.class, filamentService);
            //
            //
            //            // Specific view on filaments
            //            FilamentView view = new FilamentView(filamentService);
            //            view.setLocationService(sheet.getLocationService());
            //
            //            ScrollView slv = new ScrollView(view);
            //            BoardsPane boardsPane = new BoardsPane(
            //                    new PixelBoard(sheet),
            //                    new FilamentBoard(filamentService, true));
            //
            //            sheet.getAssembly().addViewTab(SheetTab.FILAMENT_TAB, slv, boardsPane);
        } else {
            entityService = null;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "filamentIndex";
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * @return the sheet
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //---------//
    // publish //
    //---------//
    /**
     * Convenient debug UI method to publish and focus on a filament.
     *
     * @param filament the provided filament
     */
    public void publish (final Filament filament)
    {
        if (entityService != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    entityService.publish(
                            new EntityListEvent<Filament>(
                                    this,
                                    SelectionHint.ENTITY_INIT,
                                    MouseMovement.PRESSING,
                                    filament));
                }
            });
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String vipFilaments = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP filaments IDs");
    }

    //-----------------//
    // FilamentService //
    //-----------------//
    private class FilamentService
            extends EntityService<Filament>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public FilamentService ()
        {
            super(FilamentIndex.this, sheet.getLocationService(), eventsAllowed);
        }
    }
}
