//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    W r a p p e d D o u b l e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;


/**
 * Class {@code WrappedDouble} is a wrapper around a Double, with the ability to set
 * an initial value.
 *
 * @author Hervé Bitteur
 */
public class WrappedDouble
    extends Wrapper<Double>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new WrappedDouble object.
     *
     * @param value the initial value
     */
    public WrappedDouble (Double value)
    {
        this.value = value;
    }
}
