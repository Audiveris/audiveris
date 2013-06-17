//----------------------------------------------------------------------------//
//                                                                            //
//                         S t e p E x c e p t i o n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

/**
 * Class {@code StepException} describes an exception occurring while
 * doing OMR processing, and which should immediately stop the current
 * Step.
 *
 * @author Hervé Bitteur
 */
public class StepException
        extends Exception
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Construct an {@code StepException} with no detail message.
     */
    public StepException ()
    {
        super();
    }

    /**
     * Construct an {@code StepException} with detail message.
     *
     * @param message the related message
     */
    public StepException (String message)
    {
        super(message);
    }

    /**
     * Construct an {@code StepException} from an existing exception.
     *
     * @param ex the related exception
     */
    public StepException (Throwable ex)
    {
        super(ex);
    }
}
