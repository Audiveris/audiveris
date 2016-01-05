//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   W r a p p e d I n t e g e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Class {@code WrappedInteger} is a wrapper around an Integer, with the ability to set
 * an initial value.
 *
 * @author Hervé Bitteur
 */
public class WrappedInteger
        extends Wrapper<Integer>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new WrappedInteger object.
     *
     * @param value the initial value
     */
    public WrappedInteger (Integer value)
    {
        this.value = value;
    }
}
