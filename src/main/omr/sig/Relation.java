//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          R e l a t i o n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Interface {@code Relation} describes a relation between two Interpretation instances.
 *
 * @author Hervé Bitteur
 */
public interface Relation
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Details for tip.
     *
     * @return relation details
     */
    String getDetails ();

    /**
     * Short name.
     *
     * @return the relation short name
     */
    String getName ();

    /**
     * Relation description when seen from one of its involved inters
     *
     * @param inter the interpretation point of view
     * @return the inter-based description
     */
    String seenFrom (Inter inter);

    /**
     * Report a long description of the relation
     *
     * @param sig the containing sig
     * @return long description
     */
    String toLongString (SIGraph sig);
}
