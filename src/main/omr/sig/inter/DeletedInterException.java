//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            D e l e t e d I n t e r E x c e p t i o n                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

/**
 * Class {@code DeletedInterException} defines an exception thrown when an Inter
 * instance no longer exists in its SIG.
 *
 * @author Hervé Bitteur
 */
public class DeletedInterException
        extends Exception
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final Inter inter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DeletedInterException} object.
     *
     * @param inter the deleted inter
     */
    public DeletedInterException (Inter inter)
    {
        this.inter = inter;
    }
}
