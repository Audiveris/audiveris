//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    F i l a m e n t B o a r d                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.ui.selection.EntityService;

import omr.ui.Board;
import omr.ui.EntityBoard;

/**
 * Class {@code FilamentBoard} is an EntityBoard for filaments.
 *
 * @author Hervé Bitteur
 */
public class FilamentBoard
        extends EntityBoard<Filament>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code FilamentBoard} object.
     *
     * @param service  filament service
     * @param selected true for pre-selected
     */
    public FilamentBoard (EntityService service,
                          boolean selected)
    {
        super(Board.FILAMENT, service, selected);
    }
}
