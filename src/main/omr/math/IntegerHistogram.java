//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                I n t e g e r H i s t o g r a m                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class {@code IntegerHistogram} is an histogram where buckets are integers.
 *
 * @author Hervé Bitteur
 */
public class IntegerHistogram
        extends Histogram<Integer>
{
    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // getDerivative //
    //---------------//
    public int getDerivative (int key)
    {
        return getCount(key) - getCount(key - 1);
    }

    //------------------//
    // getPreciseMaxima //
    //------------------//
    /**
     * Report the local maximum points, refined by the lowest derivative points, and
     * sorted by decreasing count
     *
     * @return the (count-based) sorted sequence of local maxima
     */
    public List<MaxEntry<Integer>> getPreciseMaxima ()
    {
        // Get all local maxima
        final List<MaxEntry<Integer>> maxima = getLocalMaxima();

        // Refine their key, using derivative information
        for (MaxEntry<Integer> entry : maxima) {
            int bestDer = 0;
            int bestK = -1;
            int key = entry.getKey();

            for (int k = key, kMax = lastBucket(); k <= kMax; k++) {
                int der = getDerivative(k);

                if (bestDer > der) {
                    bestDer = der;
                    bestK = k;
                }
            }

            if (bestK != -1) {
                entry.setKey(bestK - 1);
            }
        }

        return maxima;
    }

    //-------//
    // print //
    //-------//
    @Override
    public void print (PrintStream stream)
    {
        stream.print("[\n");

        for (Map.Entry<Integer, Integer> entry : entrySet()) {
            Integer key = entry.getKey();
            int der = getDerivative(key);
            stream.format(" %s: v:%d d:%d\n", key.toString(), entry.getValue(), der);
        }

        stream.println("]");
    }
}
