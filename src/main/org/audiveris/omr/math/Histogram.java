//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       H i s t o g r a m                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.math;

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
 * Class <code>Histogram</code> is an histogram implementation which handles integer counts
 * in buckets, the buckets identities being values of type K.
 *
 * @param <K> the precise number type for histogram buckets
 * @author Hervé Bitteur
 */
public class Histogram<K extends Number>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** To sort peaks by decreasing value. */
    public final Comparator<PeakEntry<K>> reversePeakComparator = (e1,
                                                                   e2) -> Double.compare(
                                                                           e2.getValue(),
                                                                           e1.getValue());

    /** To sort double peaks by decreasing value. */
    public final Comparator<PeakEntry<Double>> reverseDoublePeakComparator = (e1,
                                                                              e2) -> Double.compare(
                                                                                      e2.getValue(),
                                                                                      e1.getValue());

    /** To sort double peaks by decreasing value. */
    public final Comparator<MaxEntry<K>> reverseMaxComparator = (e1,
                                                                 e2) -> Double.compare(
                                                                         e2.getValue(),
                                                                         e1.getValue());

    /**
     * Underlying map. :
     * - K for the type of entity to be accumulated
     * - Integer for the cumulated number in each bucket
     */
    protected final SortedMap<K, Integer> map = new TreeMap<>();

    /** Total count. */
    protected int totalCount = 0;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new Histogram object, with no pre-defined range of buckets.
     */
    public Histogram ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // bucketSet //
    //-----------//
    /**
     * Report the set of buckets.
     *
     * @return histogram buckets
     */
    public Set<K> bucketSet ()
    {
        return map.keySet();
    }

    //-------//
    // clear //
    //-------//
    /**
     * Empty the histogram.
     */
    public void clear ()
    {
        map.clear();
        totalCount = 0;
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

    //------------//
    // dataString //
    //------------//
    /**
     * Report histogram content as a string.
     *
     * @return content as string
     */
    public String dataString ()
    {
        StringBuilder sb = new StringBuilder("[");

        boolean first = true;

        for (Map.Entry<K, Integer> entry : entrySet()) {
            sb.append(
                    String.format(
                            "%s%s=%d",
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
    /**
     * Report histogram content as an entry set.
     *
     * @return entry set
     */
    public Set<Map.Entry<K, Integer>> entrySet ()
    {
        return map.entrySet();
    }

    //-------------//
    // firstBucket //
    //-------------//
    /**
     * Report the first key in histogram range.
     *
     * @return first key
     */
    public K firstBucket ()
    {
        if (map.isEmpty()) {
            return null;
        }

        return map.firstKey();
    }

    //----------//
    // getCount //
    //----------//
    /**
     * Report the cumulated count at specified bucket.
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

    //----------------//
    // getDoublePeaks //
    //----------------//
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
            } else if (isAbove) { // Above -> Below
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
                        maxima.add(new MaxEntry<>(prevKey, prevValue / (double) totalCount));
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
     * @return the maximum entry (key and value)
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

    //---------//
    // getPeak //
    //---------//
    /**
     * Retrieve details on a specific peak
     *
     * @param quorumRatio quorum ratio to select peaks
     * @param spreadRatio spread ratio, if any, to refine values
     * @param index       desired peak index (counted from 0)
     * @return the desired peak
     */
    public PeakEntry<Double> getPeak (double quorumRatio,
                                      Double spreadRatio,
                                      int index)
    {
        PeakEntry<Double> peak = null;

        // Find peak(s) using quorum threshold
        List<PeakEntry<Double>> peaks = getDoublePeaks(getQuorumValue(quorumRatio));

        if (index < peaks.size()) {
            peak = peaks.get(index);

            // Refine peak using spread threshold?
            if (spreadRatio != null) {
                peaks = getDoublePeaks(getQuorumValue(peak.getValue() * spreadRatio));

                if (index < peaks.size()) {
                    peak = peaks.get(index);
                }
            }
        }

        return peak;
    }

    //----------------//
    // getQuorumValue //
    //----------------//
    /**
     * Based on the current population, report the quorum value
     * corresponding to the provided quorum ratio
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
    /**
     * Increase bucket with provided value.
     *
     * @param bucket bucket to increase
     * @param delta  added value
     */
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
    /**
     * Report the last key in histogram range.
     *
     * @return last key
     */
    public K lastBucket ()
    {
        if (map.isEmpty()) {
            return null;
        }

        return map.lastKey();
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

        return ((prev.doubleValue() * (nextCount - count)) + (next.doubleValue() * (count
                - prevCount))) / (nextCount - prevCount);
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

    //-------//
    // print //
    //-------//
    /**
     * Print content to provided stream.
     *
     * @param stream output
     */
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
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(
                String.format(
                        "%s-%s",
                        (firstBucket() != null) ? firstBucket().toString() : "",
                        (lastBucket() != null) ? lastBucket().toString() : ""));
        sb.append(" size:").append(size());

        sb.append(" ").append(dataString());

        sb.append("}");

        return sb.toString();
    }

    //--------//
    // values //
    //--------//
    /**
     * Report the collection of buckets counts.
     *
     * @return buckets counts
     */
    public Collection<Integer> values ()
    {
        return map.values();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // DoublePeak //
    //------------//
    /**
     * A peak of double values.
     */
    public static class DoublePeak
            extends Peak<Double>
    {

        private DoublePeak (double first,
                            double best,
                            double second)
        {
            super(first, best, second);
        }
    }

    //------------//
    // HistoEntry //
    //------------//
    /**
     * @param <K> specific type for x
     */
    public static interface HistoEntry<K extends Number>
    {

        /**
         * Report the x value for highest count in bucket
         *
         * @return x value for best count
         */
        K getBest ();

        /**
         * Report the highest count
         *
         * @return the highest count in the bucket
         */
        double getValue ();
    }

    //----------//
    // MaxEntry //
    //----------//
    /**
     * A counted maximum value.
     *
     * @param <K> precise type
     */
    public static class MaxEntry<K extends Number>
            implements HistoEntry<K>
    {

        /** Key at local maximum. */
        private K key;

        /** Related count. (normalized by total histogram count) */
        private final double value;

        /**
         * Create a MaxEntry object
         *
         * @param key   x value
         * @param value y value
         */
        public MaxEntry (K key,
                         double value)
        {
            this.key = key;
            this.value = value;
        }

        /**
         * Report x value
         *
         * @return x
         */
        @Override
        public K getBest ()
        {
            return getKey();
        }

        /**
         * Report x value
         *
         * @return the x key
         */
        public K getKey ()
        {
            return key;
        }

        /**
         * Report y value
         *
         * @return the y value
         */
        @Override
        public double getValue ()
        {
            return value;
        }

        /**
         * Set x value
         *
         * @param key x value
         */
        public void setKey (K key)
        {
            this.key = key;
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
    /**
     * We are interested in the triplet: first, best, second.
     *
     * @param <K> precise x type
     */
    public static class Peak<K extends Number>
    {

        /** Key at beginning of range. */
        public final K first;

        /** Key at highest count in range. */
        public final K best;

        /** Key at end of range. */
        public final K second;

        /**
         * Create a Peak object.
         *
         * @param first  x at start
         * @param best   x at best y
         * @param second x at stop
         */
        public Peak (K first,
                     K best,
                     K second)
        {
            this.first = first;
            this.best = best;
            this.second = second;
        }

        @Override
        public String toString ()
        {
            return String.format(
                    "(%.1f,%.1f,%.1f)",
                    first.floatValue(),
                    best.floatValue(),
                    second.floatValue());
        }
    }

    //-----------//
    // PeakEntry //
    //-----------//
    /**
     * A counted peak.
     *
     * @param <K> number type
     */
    public static class PeakEntry<K extends Number>
            implements HistoEntry<K>
    {

        /** The peak data */
        private final Peak<K> key;

        /** Count at best value (normalized by total histogram count) */
        private final double value;

        /**
         * Create a PeakEntry.
         *
         * @param key   x value
         * @param value y value
         */
        public PeakEntry (Peak<K> key,
                          double value)
        {
            this.key = key;
            this.value = value;
        }

        /**
         * Report best bucket in peak range.
         *
         * @return best bucket
         */
        @Override
        public K getBest ()
        {
            return key.best;
        }

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
        @Override
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
