//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         V i s i t a b l e I n t e r p r e t a t i o n                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

/**
 * Interface {@code VisitableInterpretation} must be implemented by any class to be
 * visited by an InterVisitor.
 *
 * @author Hervé Bitteur
 */
public interface VisitableInter
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * General entry for any visiting
     *
     * @param visitor the concrete visitor object which defines the actual
     *                processing
     */
    void accept (InterVisitor visitor);
}
