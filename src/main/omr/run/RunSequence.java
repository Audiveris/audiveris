//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R u n S e q u e n c e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code RunSequence} presents a sequence of runs which can be iterated.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicRunSequence.Adapter.class)
public interface RunSequence
        extends Iterable<Run>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Insert a run.
     *
     * @param run the run to insert (at its provided location)
     * @return true if addition was performed, false otherwise
     */
    boolean add (Run run);

    /**
     * Report whether the sequence contains no foreground run
     *
     * @return true if empty
     */
    boolean isEmpty ();

    /**
     * Remove a run.
     *
     * @param run the run to remove
     * @return true if removal was performed, false otherwise
     */
    boolean remove (Run run);

    /**
     * Report the number of foreground runs in the sequence.
     *
     * @return the number of foreground runs
     */
    int size ();
}
