//-----------------------------------------------------------------------//
//                                                                       //
//                           D i r e c t o r y                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

/**
 * Interface <code>Directory</code> defines the ability to retrieve an
 * entity knowing some key (this is somewhat different and much lighter
 * than a Map)
 *
 * @param <K> type for the key
 * @param <E> type for the entity
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface Directory <K, E>
{
    /**
     * Retrieve an entity from the directory, knowing its key
     *
     * @param key the way to designate the entity
     * @return the entity found, or null if not
     */
    E getEntity (K key);
}
