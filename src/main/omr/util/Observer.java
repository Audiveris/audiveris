//-----------------------------------------------------------------------//
//                                                                       //
//                            O b s e r v e r                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

/**
 * Interface <code>Observer</code> defines the entries for a class that can
 * receive information about an E entity. The Observer is passed the entity
 * value, using a Push model.
 *
 * @param <E> precise type for the entity to be handed out
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface Observer<E>
{
    //--------//
    // update //
    //--------//
    /**
     * Inform about the entity, whose information is to be taken into
     * account
     *
     * @param entity the entity to be used
     */
    void update (E entity);
}
