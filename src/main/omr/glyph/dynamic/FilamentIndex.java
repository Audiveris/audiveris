//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    F i l a m e n t I n d e x                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.selection.EntityListEvent;
import omr.selection.EntityService;
import omr.selection.IdEvent;

import omr.sheet.Sheet;

import omr.util.BasicIndex;
import omr.util.IntUtil;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    /** Events that can be published on inter service. */
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
        super("F");

        this.sheet = sheet;

        // Declared VIP IDs?
        List<Integer> vipIds = IntUtil.parseInts(constants.vipFilaments.getValue());

        if (!vipIds.isEmpty()) {
            logger.info("VIP filaments: {}", vipIds);
            setVipIds(vipIds);
        }

        // User filament service?
        if (OMR.getGui() != null) {
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
        return "filaments";
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
