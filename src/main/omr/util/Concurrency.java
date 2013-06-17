//----------------------------------------------------------------------------//
//                                                                            //
//                           C o n c u r r e n c y                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Interface {@code Concurrency} declares if an entity (class
 * instance) can be used by concurrent threads.
 * This complements the JCIP annotations in a more dynamic way.
 *
 * @author Hervé Bitteur
 */
public interface Concurrency
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report whether the entity can be used concurrently
     *
     * @return true if thread safe, false otherwise
     */
    boolean isThreadSafe ();
}
