//----------------------------------------------------------------------------//
//                                                                            //
//                        W r a p p e d B o o l e a n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Class {@code WrappedBoolean} is a specific wrapper around a boolean,
 * meant to carry an output boolean as method parameter.
 *
 * @author Hervé Bitteur
 */
public class WrappedBoolean
        extends Wrapper<Boolean>
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // WrappedBoolean //
    //----------------//
    /**
     * Creates a new WrappedBoolean object.
     *
     * @param value the initial boolean value
     */
    public WrappedBoolean (boolean value)
    {
        set(value);
    }

    //~ Methods ----------------------------------------------------------------
    //-------//
    // isSet //
    //-------//
    /**
     * Report the current boolean value
     *
     * @return the current value
     */
    public boolean isSet ()
    {
        return value;
    }

    //-----//
    // set //
    //-----//
    /**
     * Assign the boolean value
     *
     * @param value the assigned value
     */
    public final void set (boolean value)
    {
        this.value = value;
    }
}
