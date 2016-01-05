//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S e c t i o n S e r v i c e                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.selection.EntityListEvent;
import omr.selection.EntityService;
import omr.selection.IdEvent;
import omr.selection.SelectionService;

import omr.util.EntityIndex;

/**
 * Class {@code SectionService} is an EntityService for sections.
 *
 * @author Hervé Bitteur
 */
public class SectionService
        extends EntityService<Section>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Events that can be published on section service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        IdEvent.class, EntityListEvent.class
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SectionService} object.
     *
     * @param index           underlying section index (typically a lag)
     * @param locationService related service for location info
     */
    public SectionService (EntityIndex<Section> index,
                           SelectionService locationService)
    {
        super(index, locationService, eventsAllowed);
    }
}
