//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 I n s t a n c e s W a t c h e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A debug utility to keep track of active instances of a given class.
 *
 * @author Hervé Bitteur
 * @param <E> precise instances class
 */
public class InstancesWatcher<E>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InstancesWatcher.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Weak references to instances. */
    private final Set<WeakReference<E>> actives = new LinkedHashSet<WeakReference<E>>();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Register an instance.
     * (to be called in class constructor)
     *
     * @param ref the instance to reference
     */
    public synchronized void addRef (E ref)
    {
        actives.add(new WeakReference<E>(ref));
    }

    /**
     * Dump the collection of instances still active.
     * (to be called at any time)
     */
    public synchronized void listRefs ()
    {
        logger.info("Actives left:");

        int count = 0;

        for (Iterator<WeakReference<E>> it = actives.iterator(); it.hasNext();) {
            WeakReference<E> weak = it.next();
            E ref = weak.get();

            if (ref != null) {
                logger.info("  @{} {}", Integer.toHexString(ref.hashCode()), ref);
                count++;
            } else {
                it.remove();
            }
        }

        logger.info("Actives count: {}", count);
    }
}
