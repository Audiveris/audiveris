//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             N e s t                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.ui.selection.SelectionService;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface {@code Nest} describes a collection manager, with the ability to retrieve
 * a member within the collection based on its ID or its location.
 * It also handles a SelectionService that deals with member selection.
 *
 * @param <T> specific type for collection member
 *
 * @author Hervé Bitteur
 */
public interface Nest<T>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Look up for all members that are contained in the provided rectangle
     *
     * @param rect the provided containing rectangle
     * @return the member found, which may be empty
     */
    Set<T> containedMembers (Rectangle rect);

    /**
     * Remove link and subscription to locationService
     *
     * @param locationService the location service
     */
    void cutServices (SelectionService locationService);

    /**
     * Export the whole unmodifiable collection of members in the nest.
     *
     * @return the collection of members
     */
    Collection<T> getAllMembers ();

    /**
     * Retrieve a member via its Id.
     *
     * @param id the member id to search for
     * @return the member found, or null otherwise
     */
    T getMember (int id);

    /**
     * Report the nest selection service.
     *
     * @return the nest selection service
     */
    SelectionService getMemberService ();

    /**
     * Report a name for this nest instance.
     *
     * @return a (distinguished) name
     */
    String getName ();

    /**
     * Report the currently selected list of members if any
     *
     * @return the current list of members, or null
     */
    List<T> getSelectedMembers ();

    /**
     * Look up for <b>all</b> members that intersect the provided rectangle.
     *
     * @param rect the coordinates rectangle
     * @return the members found, which may be an empty list
     */
    Set<T> intersectedMembers (Rectangle rect);

    /**
     * Check whether the provided member is among the VIP ones
     *
     * @param member the member (ID) to check
     * @return true if this is a VIP member
     */
    boolean isVip (T member);

    /**
     * Assign a unique id (within this Nest instance) to the provided member.
     *
     * @param member the provided member
     * @return the assigned unique id
     */
    int register (T member);

    /**
     * Remove the provided member
     *
     * @param member the member to remove
     */
    void remove (T member);

    /**
     * Inject dependency on location service, and trigger subscriptions
     *
     * @param locationService the location service
     */
    void setServices (SelectionService locationService);
}
