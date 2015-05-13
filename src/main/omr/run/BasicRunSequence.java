//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B a s i c R u n S e q u e n c e                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicRunSequence} is a basic implementation of RunSequence.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "run-sequence")
public class BasicRunSequence
        implements RunSequence
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Run-length encoding.
     * <p>
     * The very first run is always considered to be foreground.
     * If a sequence starts with background, the very first (foreground) length must be zero.
     * So, the RLE array always has an odd number of cells, beginning and ending with Foreground.
     * <p>
     * No zero value should be found in the sequence (except in first position).
     * We can have:
     * <pre>
     * F     (perhaps 0)
     * FBF   (perhaps 0BF)
     * FBFBF (perhaps 0BFBF)
     * etc...
     * </pre>
     */
    @XmlList // Annotation to get all lengths, space-separated, in one single element

    @XmlValue // Annotation to avoid any wrapper

    private short[] rle;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicRunSequence object.
     *
     * @param list list of foreground runs
     */
    public BasicRunSequence (List<Run> list)
    {
        this(encode(list));
    }

    /**
     * Creates a new BasicRunSequence object.
     *
     * @param rle the run-length encoding array
     */
    public BasicRunSequence (short[] rle)
    {
        this.rle = rle;
    }

    /**
     * Creates a new BasicRunSequence object.
     *
     * @param seq the RunSequence to copy
     */
    public BasicRunSequence (BasicRunSequence seq)
    {
        rle = new short[seq.rle.length];
        System.arraycopy(seq.rle, 0, rle, 0, seq.rle.length);
    }

    /**
     * Creates a new BasicRunSequence object.
     */
    public BasicRunSequence ()
    {
        rle = new short[]{0};
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // add //
    //-----//
    @Override
    public boolean add (Run run)
    {
        return addRun(run.getStart(), run.getLength());
    }

    //--------//
    // addRun //
    //--------//
    @Override
    public boolean addRun (int start,
                           int length)
    {
        // Check run validity
        if (start < 0) {
            throw new RuntimeException("Illegal run start " + start);
        }

        if (length <= 0) {
            throw new RuntimeException("Illegal run length " + length);
        }

        // Look for background where foreground run is to take place
        // ...F(B)F... -> ...F(B1FB2)F...
        // .......^
        Itr it = new Itr();

        while (it.hasNext()) {
            Run r = it.next();

            if (r.getStart() > start) {
                int c = it.cursor - 2;
                int back = rle[c - 1];

                if (back < length) {
                    return false;
                }

                int b1 = back - (r.getStart() - start);
                int f = length;
                int b2 = r.getStart() - start - length;

                if ((b1 == 0) && (b2 == 0)) {
                    // ...F(B)F... -> ...F(0F0)F... -> ...F++...
                    // .......^
                    short[] newRle = new short[rle.length - 2];
                    System.arraycopy(rle, 0, newRle, 0, c - 2);
                    newRle[c - 2] = (short) (rle[c - 2] + f + rle[c]);
                    System.arraycopy(rle, c + 1, newRle, c - 1, rle.length - c - 1);
                    rle = newRle;
                } else if (b1 == 0) {
                    // ...F(B)F... -> ...F(0FB2)F... -> ...F+(B2)F...
                    // .......^
                    rle[c - 2] += (short) f;
                    rle[c - 1] = (short) b2;
                } else if (b2 == 0) {
                    // ...F(B)F... -> ...F(B1F0)F... -> ...F(B1)F+...
                    // .......^
                    rle[c - 1] += (short) b1;
                    rle[c] += (short) f;
                } else {
                    short[] newRle = new short[rle.length + 2];
                    System.arraycopy(rle, 0, newRle, 0, c - 1);
                    newRle[c - 1] = (short) b1;
                    newRle[c] = (short) f;
                    newRle[c + 1] = (short) b2;
                    System.arraycopy(rle, c, newRle, c + 2, rle.length - c);
                    rle = newRle;
                }

                return true;
            }
        }

        // Append the run at end of sequence
        int b = start - it.loc;

        if (b < 0) {
            return false;
        } else if (b == 0) {
            // ...F -> ...F+
            rle[rle.length - 1] += (short) length;
        } else {
            // ...F -> ...F(BF')
            short[] newRle = new short[rle.length + 2];
            System.arraycopy(rle, 0, newRle, 0, rle.length);
            newRle[rle.length] = (short) b;
            newRle[rle.length + 1] = (short) length;
            rle = newRle;
        }

        return true;
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof BasicRunSequence) {
            BasicRunSequence that = (BasicRunSequence) obj;

            return Arrays.equals(this.rle, that.rle);
        } else {
            return false;
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (73 * hash) + Arrays.hashCode(this.rle);

        return hash;
    }

    //---------//
    // isEmpty //
    //---------//
    @Override
    public boolean isEmpty ()
    {
        return size() == 0;
    }

    //----------//
    // iterator //
    //----------//
    @Override
    public Iterator<Run> iterator ()
    {
        return new Itr();
    }

    //--------//
    // remove //
    //--------//
    @Override
    public boolean remove (Run run)
    {
        // Find where this run lies in rle
        Iterator<Run> iter = new Itr();

        while (iter.hasNext()) {
            Run r = iter.next();

            if (r.isIdentical(run)) {
                // We are located on the right run
                iter.remove();

                return true;
            }
        }

        return false;
    }

    //------//
    // size //
    //------//
    @Override
    public int size ()
    {
        if (rle[0] == 0) {
            // Case of an initial background run
            return (rle.length - 1) / 2;
        } else {
            return (rle.length + 1) / 2;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return Arrays.toString(rle);
    }

    //--------//
    // encode //
    //--------//
    private static short[] encode (List<Run> list)
    {
        if (list == null) {
            throw new NullPointerException();
        }

        if (list.isEmpty()) {
            return new short[]{0};
        }

        short[] table;
        int size = (2 * list.size()) - 1;
        int start = list.get(0).getStart();
        int cursor = 0;
        int length = 0;
        boolean injectBack = false;

        if (start != 0) {
            // Insert an empty foreground length
            size += 2;
            table = new short[size];
            table[0] = 0;
            cursor = 1;
            injectBack = true;
        } else {
            table = new short[size];
        }

        for (Run run : list) {
            if (injectBack) {
                // Inject background
                table[cursor++] = (short) (run.getStart() - length);
                length = run.getStart();
            }

            // Inject foreground
            table[cursor++] = (short) run.getLength();
            length += run.getLength();

            injectBack = true;
        }

        return table;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of RunSequence interface.
     */
    public static class Adapter
            extends XmlAdapter<BasicRunSequence, RunSequence>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicRunSequence marshal (RunSequence s)
        {
            return (BasicRunSequence) s;
        }

        @Override
        public RunSequence unmarshal (BasicRunSequence s)
        {
            return s;
        }
    }

    //-----//
    // Itr //
    //-----//
    /**
     * Iterator implementation optimized for RLE.
     * <p>
     * The iterator returns only foreground runs.
     */
    private class Itr
            implements Iterator<Run>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Current index in sequence array.
         * Always on an even value, pointing to the length of Foreground to be returned by next() */
        private int cursor = 0;

        /** Start location of foreground run to be returned by next(). */
        private int loc = 0;

        /** <b>Reusable</b> Run structure. This is meant to optimize browsing.
         * Beware, don't keep a pointer to this Run object, make a copy.
         */
        private final Run run = new Run(-1, -1);

        //~ Constructors ---------------------------------------------------------------------------
        public Itr ()
        {
            // Check the case of an initial background run
            if (rle[cursor] == 0) {
                if (rle.length > 1) {
                    loc = rle[1];
                }

                cursor += 2;
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Returns true only if there is still a foreground run to return.
         *
         * @return true if there is still a foreground run available
         */
        @Override
        public final boolean hasNext ()
        {
            return cursor < rle.length;
        }

        /**
         * We return only foreground runs.
         *
         * @return the next foreground run
         * @throws NoSuchElementException if there is no next (foreground) run
         */
        @Override
        public Run next ()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            // ...v.. cursor before next()
            // ...FBF
            // .....^ cursor after next()
            int foreLoc = loc;
            int foreLg = rle[cursor++] & 0xFFFF;

            // Update the (modifiable) run structure
            run.setStart(foreLoc);
            run.setLength(foreLg);

            loc += foreLg;

            if (cursor < rle.length) {
                int backLg = rle[cursor] & 0xFFFF;
                loc += backLg;
            }

            cursor++;

            return run;
        }

        @Override
        public void remove ()
        {
            int c = cursor - 2;

            if (c == 0) {
                if (c == (rle.length - 1)) {
                    // F -> 0
                } else {
                    // (FB)F... -> 0(B')F...
                    rle[1] = (short) (rle[0] + rle[1]);
                }

                rle[0] = 0;
            } else {
                short[] newRle = new short[rle.length - 2];

                if (c == (rle.length - 1)) {
                    // ...F(BF) -> ...F
                    System.arraycopy(rle, 0, newRle, 0, newRle.length);
                } else {
                    // ...F(BFB)F... -> ...F(B')F...
                    System.arraycopy(rle, 0, newRle, 0, c - 1);
                    newRle[c - 1] = (short) (rle[c - 1] + rle[c] + rle[c + 1]);
                    System.arraycopy(rle, c + 2, newRle, c, rle.length - c - 2);
                }

                rle = newRle;
                cursor = c;
            }
        }
    }
}
