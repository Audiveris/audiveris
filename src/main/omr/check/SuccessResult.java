//-----------------------------------------------------------------------//
//                                                                       //
//                       S u c c e s s R e s u l t                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.check;

/**
 * Class <code>SuccessResult</code> is the root of all results that store a
 * success.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SuccessResult
        extends Result
{
    //~ Constructors ------------------------------------------------------

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

    //~ Methods -----------------------------------------------------------

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
