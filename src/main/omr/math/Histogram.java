//----------------------------------------------------------------------------//
//                                                                            //
//                             H i s t o g r a m                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code Histogram} is an histogram implementation which handles integer
 * counts in buckets, the buckets identities being values of type K.
 *
 * @param <K> the precise type for histogram buckets
 *
 * @author Hervé Bitteur
 */
public class Histogram<K>
{
    //~ Instance fields --------------------------------------------------------

    /** To sort entries by decreasing value */
    public final Comparator<Entry<K, Integer>> reverseValueComparator = new Comparator<Entry<K, Integer>>() {
        public int compare (Entry<K, Integer> e1,
                            Entry<K, Integer> e2)
        {
            // Put largest value first!
            return Integer.signum(e2.getValue() - e1.getValue());
        }
    };

    /**
     * Underlying map:
     * - K for the type of entity to be accumulated
     * - Integer for the cumulated number in each bucket
     */
    protected final SortedMap<K, Integer> map = new TreeMap<K, Integer>();

    /** Total count */
    protected int totalCount = 0;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // Histogram //
    //-----------//
    /**
     * Creates a new Histogram object, with no pre-defined range of buckets
     */
    public Histogram ()
    {
    }

    //-----------//
    // Histogram //
    //-----------//
    /**
     * Creates a new Histogram object, with pre-definition of the bucket range
     * @param first the first bucket of the foreseen range
     * @param last the last bucket of the foreseen range
     */
    public Histogram (K first,
                      K last)
    {
        map.put(first, 0);
        map.put(last, 0);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getCount //
    //----------//
    /**
     * Report the count of specified bucket
     * @param bucket the bucket of interest
     * @return the bucket count (zero for any empty bucket)
     */
    public int getCount (K bucket)
    {
        Integer count = map.get(bucket);

        if (count == null) {
            return 0;
        } else {
            return count;
        }
    }

    //--------------//
    // getMaxBucket //
    //--------------//
    /**
     * Report the bucket with highest count
     * @return the most popular bucket
     */
    public K getMaxBucket ()
    {
        int max = Integer.MIN_VALUE;
        K   bucket = null;

        for (Map.Entry<K, Integer> entry : map.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                bucket = entry.getKey();
            }
        }

        return bucket;
    }

    //-------------//
    // getMaxCount //
    //-------------//
    /**
     * Report the highest count among all buckets
     * @return the largest count value
     */
    public int getMaxCount ()
    {
        int max = Integer.MIN_VALUE;

        for (Map.Entry<K, Integer> entry : map.entrySet()) {
            max = Math.max(max, entry.getValue());
        }

        return max;
    }

    //-----------//
    // getMaxima //
    //-----------//
    /**
     * Report the list of detected maxima in this histogram
     * @param quorumRatio minimum value for significant maxima and minima,
     * defined as ratio of total sum of values
     * @return the sequence of maxima (key & value), sorted by decreasing value
     */
    public List<Entry<K, Integer>> getMaxima (double quorumRatio)
    {
        final List<Entry<K, Integer>> maxima = new ArrayList<Entry<K, Integer>>();

        // Compute min count
        final int         minCount = getQuorumValue(quorumRatio);

        ///System.out.println("minCount: " + minCount);

        // Current status WRT min count threshold
        boolean           isAbove = false;

        // Current maximum
        Entry<K, Integer> best = null;

        for (Entry<K, Integer> entry : map.entrySet()) {
            int value = entry.getValue();

            if (value >= minCount) {
                if (isAbove) {
                    // Above -> Above
                    if ((best == null) || (value > best.getValue())) {
                        best = entry;
                    }
                } else {
                    // Below -> Above
                    best = entry;
                    isAbove = true;
                }
            } else {
                if (isAbove) {
                    // Above -> Below
                    maxima.add(best);
                    best = null;
                    isAbove = false;
                } else {
                    // Below -> Below
                }
            }
        }

        if (isAbove) {
            maxima.add(best);
        }

        // Sort by decreasing count value
        Collections.sort(maxima, reverseValueComparator);

        return maxima;
    }

    //------------//
    // getMaximum //
    //------------//
    /**
     * Report the maximum entry in this histogram
     * @return the maximum entry (key & value)
     */
    public Entry<K, Integer> getMaximum ()
    {
        Entry<K, Integer> maximum = null;

        for (Entry<K, Integer> entry : map.entrySet()) {
            int value = entry.getValue();

            if ((maximum == null) || (value > maximum.getValue())) {
                maximum = entry;
            }
        }

        return maximum;
    }

    //----------//
    // getPeaks //
    //----------//
    /**
     * Report the sequence of bucket ranges whose count is equal to or greater
     * than the specified minCount value
     * @param minCount the desired minimum count value
     * @return the (perhaps empty but not null) sequence of pairs of buckets
     */
    public List<Pair<K>> getPeaks (int minCount)
    {
        final List<Pair<K>> peaks = new ArrayList<Pair<K>>();
        boolean             isAbove = false;
        K                   start = null;
        K                   stop = null;

        for (Map.Entry<K, Integer> entry : map.entrySet()) {
            if (entry.getValue() >= minCount) {
                if (isAbove) { // Above -> Above
                    stop = entry.getKey();
                } else { // Below -> Above
                    stop = start = entry.getKey();
                    isAbove = true;
                }
            } else {
                if (isAbove) { // Above -> Below
                    peaks.add(new Pair<K>(start, stop));
                    stop = start = null;
                    isAbove = false;
                } else { // Below -> Below
                }
            }
        }

        if (isAbove) {
            peaks.add(new Pair<K>(start, stop));
        }

        return peaks;
    }

    //----------------//
    // getQuorumValue //
    //----------------//
    /**
     * Based on the current population, report the quorum value coresponding
     * to the provided quorum ratio
     * @param quorumRatio quorum specified as a percentage of total count
     * @return the quorum value
     */
    public int getQuorumValue (double quorumRatio)
    {
        return (int) Math.rint(quorumRatio * getTotalCount());
    }

    //---------------//
    // getTotalCount //
    //---------------//
    /**
     * Report the total counts of all buckets
     * @return the sum of all counts
     */
    public int getTotalCount ()
    {
        return totalCount;
    }

    //-----------//
    // bucketSet //
    //-----------//
    public Set<K> bucketSet ()
    {
        return map.keySet();
    }

    //-------//
    // clear //
    //-------//
    public void clear ()
    {
        map.clear();
        totalCount = 0;
    }

    //------------//
    // dataString //
    //------------//
    public String dataString ()
    {
        StringBuilder sb = new StringBuilder("[");

        boolean       first = true;

        for (Map.Entry<K, Integer> entry : entrySet()) {
            sb.append(
                String.format(
                    "%s%s:%d",
                    first ? "" : " ",
                    entry.getKey().toString(),
                    entry.getValue()));
            first = false;
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // entrySet //
    //----------//
    public Set<Map.Entry<K, Integer>> entrySet ()
    {
        return map.entrySet();
    }

    //-------------//
    // firstBucket //
    //-------------//
    public K firstBucket ()
    {
        return map.firstKey();
    }

    //---------------//
    // increaseCount //
    //---------------//
    public void increaseCount (K   bucket,
                               int delta)
    {
        Integer count = map.get(bucket);

        if (count == null) {
            map.put(bucket, delta);
        } else {
            map.put(bucket, count + delta);
        }

        totalCount += delta;
    }

    //------------//
    // lastBucket //
    //------------//
    public K lastBucket ()
    {
        return map.lastKey();
    }

    //-------//
    // print //
    //-------//
    public void print (PrintStream stream)
    {
        stream.println(dataString());
    }

    //------//
    // size //
    //------//
    /**
     * Report the number of non empty buckets
     * @return the number of non empty buckets
     */
    public int size ()
    {
        return map.size();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(
            String.format(
                " %s-%s",
                (firstBucket() != null) ? firstBucket().toString() : "",
                (lastBucket() != null) ? lastBucket().toString() : ""));
        sb.append(" size:")
          .append(size());

        sb.append(" ")
          .append(dataString());

        sb.append("}");

        return sb.toString();
    }

    //--------//
    // values //
    //--------//
    public Collection<Integer> values ()
    {
        return map.values();
    }

    //~ Inner Classes ----------------------------------------------------------

    //------//
    // Pair //
    //------//
    public static class Pair<K>
    {
        //~ Instance fields ----------------------------------------------------

        public final K first;
        public final K second;

        //~ Constructors -------------------------------------------------------

        public Pair (K first,
                     K second)
        {
            this.first = first;
            this.second = second;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return "(" + first + "," + second + ")";
        }
    }
}
