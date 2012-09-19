//----------------------------------------------------------------------------//
//                                                                            //
//                       I n s t a n c e s W a t c h e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.log.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A debug utility to keep track of active instances of a given class.
 *
 * @author Hervé Bitteur
 */
public class InstancesWatcher<E>
{

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(InstancesWatcher.class);

    /** Weak references to instances. */
    private Set<WeakReference<E>> actives = new LinkedHashSet<>();

    /**
     * Register an instance.
     * (to be called in class constructor)
     *
     * @param ref the instance to reference
     */
    public synchronized void addRef (E ref)
    {
        actives.add(new WeakReference(ref));
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
                logger.info("  @{0} {1}", Integer.toHexString(ref.hashCode()), ref);
                count++;
            } else {
                it.remove();
            }
        }

        logger.info("Actives count: {0}", count);
    }
}
