//----------------------------------------------------------------------------//
//                                                                            //
//                         F a i l u r e R e s u l t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;


/**
 * Class {@code FailureResult} is the root of all results that store a
 * failure.
 *
 * @author Hervé Bitteur
 */
public class FailureResult
    extends Result
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // FailureResult //
    //---------------//
    /**
     * Create a new FailureResult object.
     *
     * @param comment A comment that describe the failure reason
     */
    public FailureResult (String comment)
    {
        super(comment);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable string about this failure instance
     *
     * @return a descriptive string
     */
    @Override
    public String toString ()
    {
        return "Failure:" + super.toString();
    }
}
