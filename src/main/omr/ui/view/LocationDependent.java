//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                L o c a t i o n D e p e n d e n t                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import java.awt.Rectangle;

/**
 * Interface {@code LocationDependent} indicates an entity whose behavior may depend
 * on location currently defined (such as via user interface).
 *
 * @author Hervé Bitteur
 */
public interface LocationDependent
{
    //~ Methods ------------------------------------------------------------------------------------

    /** Update the entity with user current location.
     *
     * @param rect the user selected location
     */
    void updateUserLocation (Rectangle rect);
}
