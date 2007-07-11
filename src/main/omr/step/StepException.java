//----------------------------------------------------------------------------//
//                                                                            //
//                         S t e p E x c e p t i o n                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;


/**
 * Class <code>StepException</code> describes an exception occurring while
 * doing OMR processing, and which should immediately stop the current Step.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StepException
    extends Exception
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Construct an <code>StepException</code> with no detail message.
     */
    public StepException ()
    {
        super();
    }

    /**
     * Construct an <code>StepException</code> with detail message.
     * @param message the related message
     */
    public StepException (String message)
    {
        super(message);
    }

    /**
     * Construct an <code>StepException</code> from an existing exception.
     * @param ex the related exception
     */
    public StepException (Throwable ex)
    {
        super(ex);
    }
}
