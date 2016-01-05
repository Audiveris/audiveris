//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r S e r v i c e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.selection.EntityListEvent;
import omr.selection.EntityService;
import omr.selection.IdEvent;
import omr.selection.SelectionService;

import omr.sig.inter.Inter;

import omr.util.EntityIndex;

/**
 * Class {@code InterService} is an EntityService for inters.
 *
 * @author Hervé Bitteur
 */
public class InterService
        extends EntityService<Inter>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Events that can be published on inter service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        EntityListEvent.class, IdEvent.class
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code InterService} object.
     *
     * @param index           underlying inter index (InterManager)
     * @param locationService related service for location info
     */
    public InterService (EntityIndex<Inter> index,
                         SelectionService locationService)
    {
        super(index, locationService, eventsAllowed);
    }
}
