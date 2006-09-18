//----------------------------------------------------------------------------//
//                                                                            //
//                     S e l e c t i o n O b s e r v e r                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.selection;


/**
 * Interface <code>SelectionObserver</code> is a specific Observer interface,
 * meant for Selection objects
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface SelectionObserver
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Used to access a readable name for this observer
     *
     * @return a readable name (useful for debugging)
     */
    String getName ();

    /**
     * This method is called whenever one of the observed selections has
     * changed.
     *
     * @param selection the updated selection, source of this notification
     * @param hint      a potential notification hint
     */
    void update (Selection     selection,
                 SelectionHint hint);
}
