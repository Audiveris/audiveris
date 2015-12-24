//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M y E n t i t y                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.itf;

/**
 * Interface {@code MyEntity}
 *
 * @author Hervé Bitteur
 */
public interface MyEntity
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the ID of this entity
     *
     * @return the entity ID
     */
    int getId ();

    /**
     * Assign an ID to this entity
     *
     * @param id the ID to be assigned to the entity
     */
    void setId (int id);
}
