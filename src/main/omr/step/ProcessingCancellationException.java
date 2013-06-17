//----------------------------------------------------------------------------//
//                                                                            //
//       P r o c e s s i n g C a n c e l l a t i o n E x c e p t i o n        //
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
 * Class {@code ProcessingCancellationException} describes the
 * exception raised then the processing of a sheet has been cancelled
 * (generally for time out).
 *
 * @author Hervé Bitteur
 */
public class ProcessingCancellationException
        extends RuntimeException
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Construct an {@code ProcessingCancellationException} with no detail
     * message.
     */
    public ProcessingCancellationException ()
    {
        super();
    }

    /**
     * Construct an {@code ProcessingCancellationException} with detail
     * message.
     *
     * @param message the related message
     */
    public ProcessingCancellationException (String message)
    {
        super(message);
    }

    /**
     * Construct an {@code ProcessingCancellationException} from an
     * existing exception.
     *
     * @param ex the related exception
     */
    public ProcessingCancellationException (Throwable ex)
    {
        super(ex);
    }
}
