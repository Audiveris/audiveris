//----------------------------------------------------------------------------//
//                                                                            //
//                             H i s t o g r a m                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
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
 * Class {@code Histogram} is an histogram implementation which handles
 * integer counts in buckets, the buckets identities being values of
 * type K.
 *
 * @param <K> the precise type for histogram buckets
 *
 * @author Hervé Bitteur
 */
public class Histogram<K extends Number>
{
    //~ Instance fields --------------------------------------------------------

    /** To sort peaks by decreasing value */
    public final Comparator<PeakEntry<K>> reversePeakComparator = new Comparator<PeakEntry<K>>()
    {
        @Override
        public int compare (PeakEntry<K> e1,
                            PeakEntry<K> e2)
        {
            // Put largest value first!
            return Double.compare(e2.getValue(), e1.getValue());
        }
    };

    /** To sort double peaks by decreasing value */
    public final Comparator<PeakEntry<Double>> reverseDoublePeakComparator = new Comparator<PeakEntry<Double>>()
    {
        @Override
        public int compare (PeakEntry<Double> e1,
                            PeakEntry<Double> e2)
        {
            // Put largest value first!
            return Double.compare(e2.getValue(), e1.getValue());
        }
    };

    /** To sort double peaks by decreasing value */
    public final Comparator<MaxEntry<K>> reverseMaxComparator = new Comparator<MaxEntry<K>>()
    {
        @Override
        public int compare (MaxEntry<K> e1,
                            MaxEntry<K> e2)
        {
            // Put largest value first!
            return Double.compare(e2.getValue(), e1.getValue());
        }
    };

    /**
     * Underlying map:
     * - K for the type of entity to be accumulated
     * - Integer for the cumulated number in each bucket
     */
    protected final SortedMap<K, Integer> map = new TreeMap<>();

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
     *
     * @param first the first bucket of the foreseen range
     * @param last  the last bucket of the foreseen range
     */
    public Histogram (K first,
                      K last)
    {
        map.put(first, 0);
        map.put(last, 0);
    }

    //~ Methods ----------------------------------------------------------------
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

        boolean first = true;

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

    //----------//
    // getCount //
    //----------//
    /**
     * Report the count of specified bucket
     *
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

    //----------//
    // getPeaks //
    //----------//
    /**
     * Report the sequence of bucket peaks whose count is equal to or
     * greater than the specified minCount value.
     *
     * @param minCount the desired minimum count value
     * @return the (perhaps empty but not null) sequence of peaks of buckets
     */
    public List<PeakEntry<Double>> getDoublePeaks (int minCount)
    {
        final List<PeakEntry<Double>> peaks = new ArrayList<>();
        K start = null;
        K stop = null;
        K best = null;
        Integer bestCount = null;
        boolean isAbove = false;

        for (Entry<K, Integer> entry : map.entrySet()) {
            if (entry.getValue() >= minCount) {
                if ((bestCount == null) || (bestCount < entry.getValue())) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }

                if (isAbove) { // Above -> Above
                    stop = entry.getKey();
                } else { // Below -> Above
                    stop = start = entry.getKey();
                    isAbove = true;
                }
            } else {
                if (isAbove) { // Above -> Below
                    peaks.add(
                            new PeakEntry<>(
                            createDoublePeak(start, best, stop, minCount),
                            (double) bestCount / totalCount));
                    stop = start = best = null;
                    bestCount = null;
                    isAbove = false;
                } else { // Below -> Below
                }
            }
        }

        // Last range
        if (isAbove) {
            peaks.add(
                    new PeakEntry<>(
                    createDoublePeak(start, best, stop, minCount),
                    (double) bestCount / totalCount));
        }

        // Sort by decreasing count values
        Collections.sort(peaks, reverseDoublePeakComparator);

        return peaks;
    }

    //----------------//
    // getLocalMaxima //
    //----------------//
    /**
     * Report the local maximum points, sorted by decreasing count
     *
     * @return the (count-based) sorted sequence of local maxima
     */
    public List<MaxEntry<K>> getLocalMaxima ()
    {
        final List<MaxEntry<K>> maxima = new ArrayList<>();
        K prevKey = null;
        int prevValue = 0;
        boolean growing = false;

        for (Entry<K, Integer> entry : map.entrySet()) {
            K key = entry.getKey();
            int value = entry.getValue();

            if (prevKey != null) {
                if (value >= prevValue) {
                    growing = true;
                } else {
                    if (growing) {
                        // End of a local max
                        maxima.add(
                                new MaxEntry<>(
                                prevKey,
                                prevValue / (double) totalCount));
                    }

                    growing = false;
                }
            }

            prevKey = key;
            prevValue = value;
        }

        // Sort by decreasing count values
        Collections.sort(maxima, reverseMaxComparator);

        return maxima;
    }

    //--------------//
    // getMaxBucket //
    //--------------//
    /**
     * Report the bucket with highest count
     *
     * @return the most popular bucket
     */
    public K getMaxBucket ()
    {
        int max = Integer.MIN_VALUE;
        K bucket = null;

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
     *
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

    //------------//
    // getMaximum //
    //------------//
    /**
     * Report the maximum entry in this histogram
     *
     * @return the maximum entry (key & value)
     */
    public Map.Entry<K, Integer> getMaximum ()
    {
        Map.Entry<K, Integer> maximum = null;

        for (Map.Entry<K, Integer> entry : map.entrySet()) {
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
     * Report the sequence of bucket peaks whose count is equal to or greater
     * than the specified minCount value
     *
     * @param minCount the desired minimum count value
     * @param absolute if true, absolute counts values are reported in peaks,
     *                 otherwise relative counts to total histogram are used
     * @param sorted   if true, the reported sequence is sorted by decreasing
     *                 count value, otherwise it is reported as naturally found along K data.
     * @return the (perhaps empty but not null) sequence of peaks of buckets
     */
    public List<PeakEntry<K>> getPeaks (int minCount,
                                        boolean absolute,
                                        boolean sorted)
    {
        final List<PeakEntry<K>> peaks = new ArrayList<>();
        K start = null;
        K stop = null;
        K best = null;
        Integer bestCount = null;
        boolean isAbove = false;

        for (Entry<K, Integer> entry : map.entrySet()) {
            if (entry.getValue() >= minCount) {
                if ((bestCount == null) || (bestCount < entry.getValue())) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }

                if (isAbove) { // Above -> Above
                    stop = entry.getKey();
                } else { // Below -> Above
                    stop = start = entry.getKey();
                    isAbove = true;
                }
            } else {
                if (isAbove) { // Above -> Below
                    peaks.add(
                            new PeakEntry<>(
                            new Peak<>(start, best, stop),
                            absolute ? bestCount : ((double) bestCount / totalCount)));
                    stop = start = best = null;
                    bestCount = null;
                    isAbove = false;
                } else { // Below -> Below
                }
            }
        }

        // Last range
        if (isAbove) {
            peaks.add(
                    new PeakEntry<>(
                    new Peak<>(start, best, stop),
                    absolute ? bestCount : ((double) bestCount / totalCount)));
        }

        // Sort by decreasing count values?
        if (sorted) {
            Collections.sort(peaks, reversePeakComparator);
        }

        return peaks;
    }

    //----------------//
    // getQuorumValue //
    //----------------//
    /**
     * Based on the current population, report the quorum value coresponding
     * to the provided quorum ratio
     *
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
     *
     * @return the sum of all counts
     */
    public int getTotalCount ()
    {
        return totalCount;
    }

    //---------------//
    // increaseCount //
    //---------------//
    public void increaseCount (K bucket,
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
     *
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

    //------------------//
    // createDoublePeak //
    //------------------//
    private DoublePeak createDoublePeak (K first,
                                         K best,
                                         K second,
                                         int count)
    {
        // Use interpolation for more accurate data on first & second
        double preciseFirst = first.doubleValue();
        K prevKey = prevKey(first);

        if (prevKey != null) {
            preciseFirst = preciseKey(prevKey, first, count);
        }

        double preciseSecond = second.doubleValue();
        K nextKey = nextKey(second);

        if (nextKey != null) {
            preciseSecond = preciseKey(second, nextKey, count);
        }

        return new DoublePeak(preciseFirst, best.doubleValue(), preciseSecond);
    }

    //---------//
    // nextKey //
    //---------//
    private K nextKey (K key)
    {
        boolean found = false;

        for (K k : map.keySet()) {
            if (found) {
                return k;
            } else if (key.equals(k)) {
                found = true;
            }
        }

        return null;
    }

    //------------//
    // preciseKey //
    //------------//
    private double preciseKey (K prev,
                               K next,
                               int count)
    {
        // Use interpolation for accurate data between prev & next keys
        double prevCount = getCount(prev);
        double nextCount = getCount(next);

        return ((prev.doubleValue() * (nextCount - count))
                + (next.doubleValue() * (count - prevCount))) / (nextCount
                                                                 - prevCount);
    }

    //---------//
    // prevKey //
    //---------//
    private K prevKey (K key)
    {
        K prev = null;

        for (K k : map.keySet()) {
            if (key.equals(k)) {
                return prev;
            } else {
                prev = k;
            }
        }

        return null;
    }

    //~ Inner Classes ----------------------------------------------------------
    //------------//
    // DoublePeak //
    //------------//
    public static class DoublePeak
            extends Peak<Double>
    {
        //~ Constructors -------------------------------------------------------

        private DoublePeak (double first,
                            double best,
                            double second)
        {
            super(first, best, second);
        }
    }

    //----------//
    // MaxEntry //
    //----------//
    public static class MaxEntry<K extends Number>
    {
        //~ Instance fields ----------------------------------------------------

        /** Key at local maximum */
        private final K key;

        /** Related count (normalized by total histogram count) */
        private final double value;

        //~ Constructors -------------------------------------------------------
        public MaxEntry (K key,
                         double value)
        {
            this.key = key;
            this.value = value;
        }

        //~ Methods ------------------------------------------------------------
        /**
         * @return the key
         */
        public K getKey ()
        {
            return key;
        }

        /**
         * @return the value
         */
        public double getValue ()
        {
            return value;
        }

        @Override
        public String toString ()
        {
            return getKey() + "=" + (float) getValue();
        }
    }

    //------//
    // Peak //
    //------//
    public static class Peak<K extends Number>
    {
        //~ Instance fields ----------------------------------------------------

        /** Value at beginning of range */
        public final K first;

        /** Value at highest point in range */
        public final K best;

        /** Value at end of range */
        public final K second;

        //~ Constructors -------------------------------------------------------
        public Peak (K first,
                     K best,
                     K second)
        {
            this.first = first;
            this.best = best;
            this.second = second;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "(" + first.floatValue() + "," + best.floatValue() + ","
                   + second.floatValue() + ")";
        }
    }

    //-----------//
    // PeakEntry //
    //-----------//
    public static class PeakEntry<K extends Number>
    {
        //~ Instance fields ----------------------------------------------------

        /** The peak data */
        private final Peak<K> key;

        /** Count at best value (normalized by total histogram count) */
        private final double value;

        //~ Constructors -------------------------------------------------------
        public PeakEntry (Peak<K> key,
                          double value)
        {
            this.key = key;
            this.value = value;
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Returns the key.
         *
         * @return the key
         */
        public Peak<K> getKey ()
        {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        public double getValue ()
        {
            return value;
        }

        @Override
        public String toString ()
        {
            return key + "=" + (float) value;
        }
    }
}
