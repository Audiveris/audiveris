//----------------------------------------------------------------------------//
//                                                                            //
//                   P r o c e s s i n g E x c e p t i o n                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr;


/**
 * Class <code>ProcessingException</code> describes an exception occurring while
 * doing OMR processing, and which should immediately stop the current Step, as
 * well as potential subsequent steps.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ProcessingException
    extends Exception
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Construct an <code>ProcessingException</code> with no detail message.
     */
    public ProcessingException ()
    {
        super();
    }
}
