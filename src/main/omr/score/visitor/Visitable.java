//----------------------------------------------------------------------------//
//                                                                            //
//                             V i s i t a b l e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;


/**
 * Interface <code>Visitable</code> must be implemented by any node to be
 * visited in the Score hierarchy
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface Visitable
{
    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    /**
     * General entry for any visiting
     *
     * @param visitor concrete visitor object to define the actual processing
     * @return true if children must be visited also, false otherwise
     */
    boolean accept (Visitor visitor);
}
