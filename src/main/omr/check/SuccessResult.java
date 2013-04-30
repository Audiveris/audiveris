//----------------------------------------------------------------------------//
//                                                                            //
//                         S u c c e s s R e s u l t                          //
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
 * Class {@code SuccessResult} is the root of all results that store a
 * success.
 *
 * @author Hervé Bitteur
 */
public class SuccessResult
    extends Result
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SuccessResult //
    //---------------//
    /**
     * Creates a new SuccessResult object.
     *
     * @param comment A description of the success
     */
    public SuccessResult (String comment)
    {
        super(comment);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    /**
     * Report a description of this success
     *
     * @return A descriptive string
     */
    @Override
    public String toString ()
    {
        return "Success:" + super.toString();
    }
}
