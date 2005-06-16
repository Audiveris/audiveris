//-----------------------------------------------------------------------//
//                                                                       //
//                         C h e c k R e s u l t                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.check;

/**
 * Class <code>CheckResult</code> encapsulates the <b>result</b> of a
 * check, composed of a value (double) and a flag which can be RED, YELLOW
 * or GREEN.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class CheckResult
{
    //~ Instance variables ------------------------------------------------

    /** Flag the result (RED, YELLOW, GREEN) */
    public int flag;

    /** Numerical result value */
    public double value;
}
