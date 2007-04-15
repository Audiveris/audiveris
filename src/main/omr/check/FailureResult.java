//----------------------------------------------------------------------------//
//                                                                            //
//                         F a i l u r e R e s u l t                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.check;


/**
 * Class <code>FailureResult</code> is the root of all results that store a
 * failure.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
