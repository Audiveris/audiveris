//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R u n T a b l e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.image.PixelSource;
import static omr.image.PixelSource.BACKGROUND;

import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;

import omr.util.Predicate;

import ij.process.ByteProcessor;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code RunTable} handles a rectangular assembly of oriented runs.
 * <p>
 * A RunTable is implemented as an array of run sequences, each run sequence being encoded as RLE
 * (Run Length Encoding) as follows:
 * <p>
 * The very first run is always considered to be foreground.
 * If a sequence starts with background, the very first (foreground) length must be zero.
 * So, the RLE array always has an odd number of cells, beginning and ending with Foreground.
 * An empty sequence is encoded as null (rather than an array containing a single 0 value).
 * <p>
 * No zero value should be found in the sequence (except in position 0, followed by a positive
 * background length and a positive foreground length).
 * <p>
 * We can have these various kinds of sequence, where 'F' stands for the length of a foreground run
 * and 'B' for the length of a background run:
 * <pre>
 * null    (for an empty sequence)
 * [F]     (>0)
 * [FBF]   (perhaps 0BF)
 * [FBFBF] (perhaps 0BFBF)
 * etc...
 * </pre>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "run-table")
public class RunTable
        implements Cloneable, PixelSource, Oriented, EventSubscriber<LocationEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            RunTable.class);

    /** Events that can be published on the table run service */
    public static final Class<?>[] eventsWritten = new Class<?>[]{RunEvent.class};

    /** Events observed on location service */
    public static final Class<?>[] eventsRead = new Class<?>[]{LocationEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** (Debugging) name of this runs table. */
    @XmlAttribute
    private final String name;

    /** Orientation, the same for this table and all contained runs. */
    @XmlAttribute
    private final Orientation orientation;

    /** Width of the table. */
    @XmlAttribute
    private final int width;

    /** Height of the table. */
    @XmlAttribute
    private final int height;

    /** Sequences of runs per row. */
    @XmlJavaTypeAdapter(SequencesAdapter.class)
    private final short[][] sequences;

    /** Hosted event service for UI events related to this table (Runs), if any. */
    @XmlTransient
    private SelectionService runService;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RunTable object.
     *
     * @param name        table name (meant for debugging)
     * @param orientation orientation of each run
     * @param width       table width
     * @param height      table height
     */
    public RunTable (String name,
                     Orientation orientation,
                     int width,
                     int height)
    {
        this.name = name;
        this.orientation = orientation;
        this.width = width;
        this.height = height;

        // Allocate the sequences, according to orientation
        Rectangle rect = orientation.oriented(new Rectangle(0, 0, width, height));
        sequences = new short[rect.height][];
    }

    /**
     * No-arg constructor, needed for JAXB.
     */
    private RunTable ()
    {
        this.name = null;
        this.orientation = null;
        this.width = 0;
        this.height = 0;
        this.sequences = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // addRun //
    //--------//
    /**
     * Insert a run in the sequence found at provided index.
     *
     * @param index index of the sequence in table
     * @param run   the run to insert (at its provided location)
     * @return true if addition was performed, false otherwise
     */
    public boolean addRun (int index,
                           Run run)
    {
        return addRun(index, run.getStart(), run.getLength());
    }

    //--------//
    // addRun //
    //--------//
    /**
     * Insert a run in the sequence found at provided index.
     *
     * @param index  index of the sequence in table
     * @param start  start of run
     * @param length length of run
     * @return true if addition was performed, false otherwise
     */
    public boolean addRun (int index,
                           int start,
                           int length)
    {
        short[] rle = sequences[index];

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
        Itr it = new Itr(index);

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
                    sequences[index] = newRle;
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
                    sequences[index] = newRle;
                }

                return true;
            }
        }

        // Append the run at end of sequence
        int b = start - it.loc;

        if (b < 0) {
            return false;
        } else if (b == 0) {
            if (rle != null) {
                // ...F -> ...F+
                rle[rle.length - 1] += (short) length;
            } else {
                // null -> F+
                final short[] newRle = new short[1];
                newRle[0] = (short) length;
                sequences[index] = newRle;
            }
        } else {
            final short[] newRle;

            if (rle != null) {
                // ...F -> ...F(BF')
                newRle = new short[rle.length + 2];
                System.arraycopy(rle, 0, newRle, 0, rle.length);
                newRle[rle.length] = (short) b;
                newRle[rle.length + 1] = (short) length;
            } else {
                // null -> 0(BF')
                newRle = new short[3];
                newRle[0] = 0;
                newRle[1] = (short) b;
                newRle[2] = (short) length;
            }

            sequences[index] = newRle;
        }

        return true;
    }

    //------//
    // copy //
    //------//
    /**
     * Make a copy of the table.
     *
     * @return another table with runs content identical to this one
     */
    public RunTable copy ()
    {
        return copy(name + "(copy)");
    }

    //------//
    // copy //
    //------//
    /**
     * Make a copy of the table.
     *
     * @param name a new name for the copy
     * @return another table with runs content identical to this one
     */
    public RunTable copy (String name)
    {
        RunTable clone = new RunTable(name, orientation, width, height);

        for (int i = 0; i < sequences.length; i++) {
            short[] seq = getSequence(i);

            if (seq != null) {
                short[] rle = new short[seq.length];
                System.arraycopy(seq, 0, rle, 0, seq.length);
                clone.sequences[i] = rle;
            }
        }

        return clone;
    }

    //--------------------//
    // cutLocationService //
    //--------------------//
    public void cutLocationService (SelectionService locationService)
    {
        for (Class<?> eventClass : eventsRead) {
            locationService.unsubscribe(eventClass, this);
        }
    }

    //--------//
    // dumpOf //
    //--------//
    /**
     * Report an image of the table.
     *
     * @return a drawing of the table
     */
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s%n", this));

        // Prepare output buffer
        ByteProcessor buffer = getBuffer();

        // Print the buffer
        sb.append('+');

        for (int c = 0; c < width; c++) {
            sb.append('=');
        }

        sb.append(String.format("+%n"));

        for (int row = 0; row < height; row++) {
            sb.append('|');

            for (int col = 0; col < buffer.getWidth(); col++) {
                sb.append((buffer.get(col, row) == BACKGROUND) ? '-' : 'X');
            }

            sb.append(String.format("|%n"));
        }

        sb.append('+');

        for (int c = 0; c < width; c++) {
            sb.append('=');
        }

        sb.append(String.format("+"));

        return sb.toString();
    }

    //---------------//
    // dumpSequences //
    //---------------//
    /**
     * Dump the internals of the table.
     */
    public void dumpSequences ()
    {
        System.out.println(toString());

        for (int i = 0; i < sequences.length; i++) {
            short[] rle = sequences[i];
            System.out.printf("%4d:%s%n", i, Arrays.toString(rle));
        }
    }

    //-----//
    // get //
    //-----//
    /**
     * {@inheritDoc}
     * <p>
     * <b>Beware</b>, this implementation is not efficient enough for bulk operations.
     * For such needs, a much more efficient way is to first retrieve a full buffer, via {@link
     * #getBuffer()} method, then use this temporary buffer as the {@link PixelSource} instead of
     * this table.
     *
     * @param x absolute abscissa
     * @param y absolute ordinate
     * @return the pixel value (FOREGROUND or BACKGROUND)
     */
    @Override
    public final int get (int x,
                          int y)
    {
        Run run = getRunAt(x, y);

        return (run != null) ? 0 : BACKGROUND;
    }

    //-----------//
    // getBuffer //
    //-----------//
    /**
     * Fill a rectangular buffer with the table runs
     *
     * @return the filled buffer
     */
    public ByteProcessor getBuffer ()
    {
        // Prepare output buffer
        ByteProcessor buffer = new ByteProcessor(width, height);
        buffer.invert();

        switch (orientation) {
        case HORIZONTAL:

            for (int row = 0, size = getSize(); row < size; row++) {
                for (Itr it = new Itr(row); it.hasNext();) {
                    Run run = it.next();

                    for (int c = run.getStart(); c <= run.getStop(); c++) {
                        buffer.set(c, row, 0);
                    }
                }
            }

            break;

        case VERTICAL:

            for (int row = 0, size = getSize(); row < size; row++) {
                for (Itr it = new Itr(row); it.hasNext();) {
                    Run run = it.next();

                    for (int col = run.getStart(); col <= run.getStop(); col++) {
                        buffer.set(row, col, 0);
                    }
                }
            }

            break;
        }

        return buffer;
    }

    //------------------//
    // getBufferedImage //
    //------------------//
    /**
     * Report a BufferedImage painted with the content of this table.
     *
     * @return the buffered image
     */
    public BufferedImage getBufferedImage ()
    {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
        render(g);
        g.dispose();

        return img;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the absolute dimension of the table, width along x axis
     * and height along the y axis.
     *
     * @return the absolute dimension
     */
    public Dimension getDimension ()
    {
        return new Dimension(width, height);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the absolute height of the table, regardless of runs orientation.
     *
     * @return the table height
     */
    @Override
    public int getHeight ()
    {
        return height;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name assigned to this table
     *
     * @return the name
     */
    public String getName ()
    {
        return name;
    }

    //----------------//
    // getOrientation //
    //----------------//
    /**
     * Report the orientation of table runs
     *
     * @return the orientation of the runs
     */
    @Override
    public Orientation getOrientation ()
    {
        return orientation;
    }

    //----------//
    // getRunAt //
    //----------//
    /**
     * Report the run found at given coordinates, if any.
     *
     * @param x absolute abscissa
     * @param y absolute ordinate
     * @return the run found, or null otherwise
     */
    public final Run getRunAt (int x,
                               int y)
    {
        Point oPt = orientation.oriented(new Point(x, y));

        // Protection
        if ((oPt.y < 0) || (oPt.y >= sequences.length)) {
            return null;
        }

        for (Itr it = new Itr(oPt.y); it.hasNext();) {
            Run run = it.next();

            if (run.getStart() > oPt.x) {
                return null;
            }

            if (run.getStop() >= oPt.x) {
                return run;
            }
        }

        return null;
    }

    //---------------//
    // getRunService //
    //---------------//
    /**
     * Report the table run selection service
     *
     * @return the run selection service
     */
    public SelectionService getRunService ()
    {
        if (runService == null) {
            runService = new SelectionService(name, eventsWritten);
        }

        return runService;
    }

    //---------//
    // getSize //
    //---------//
    /**
     * Report the number of sequences of runs in the table.
     * This is the width for a table of vertical runs and the height for a table of horizontal runs.
     *
     * @return the table size (in terms of sequences, including the null ones)
     */
    public final int getSize ()
    {
        return sequences.length;
    }

    //------------------//
    // getTotalRunCount //
    //------------------//
    /**
     * Report the total number of foreground runs in table
     *
     * @return the total runs count
     */
    public int getTotalRunCount ()
    {
        int total = 0;

        for (short[] seq : sequences) {
            total += sequenceSize(seq);
        }

        return total;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the table width, regardless of the runs orientation.
     *
     * @return the table width
     */
    @Override
    public int getWidth ()
    {
        return width;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include the content of the provided table into this one.
     * <p>
     * The tables must have the same dimension and orientation.
     *
     * @param that the table of runs to include into this one
     */
    public void include (RunTable that)
    {
        if (that == null) {
            throw new IllegalArgumentException("Cannot include a null RunTable");
        }

        if (that.orientation != orientation) {
            throw new IllegalArgumentException(
                    "Cannot include a RunTable of different orientation");
        }

        if (that.width != width) {
            throw new IllegalArgumentException("Cannot include a RunTable of different width");
        }

        if (that.height != height) {
            throw new IllegalArgumentException("Cannot include a RunTable of different height");
        }

        for (int row = 0, size = getSize(); row < size; row++) {
            for (Itr it = that.new Itr(row); it.hasNext();) {
                Run thatRun = it.next();
                addRun(row, thatRun);
            }
        }
    }

    //-----------------//
    // isSequenceEmpty //
    //-----------------//
    /**
     * Report whether the sequence at provided index contains no (foreground) run.
     *
     * @param index provided index
     * @return true if sequence is empty
     */
    public boolean isSequenceEmpty (int index)
    {
        return sequences[index] == null;
    }

    //----------//
    // iterator //
    //----------//
    /**
     * Returns an iterator over the sequence of runs at provided index.
     *
     * @param index index of sequence in table
     * @return the run iterator
     */
    public Iterator<Run> iterator (int index)
    {
        return new Itr(index);
    }

    //-----------//
    // lookupRun //
    //-----------//
    /**
     * Given an absolute point, retrieve the containing run if any.
     *
     * @param point coordinates of the given point
     * @return the run found, or null otherwise
     */
    public Run lookupRun (Point point)
    {
        Point oPt = orientation.oriented(point);

        if ((oPt.y < 0) || (oPt.y >= getSize())) {
            return null;
        }

        for (Itr it = new Itr(oPt.y); it.hasNext();) {
            Run run = it.next();

            if (run.getStart() > oPt.x) {
                return null;
            }

            if (run.getStop() >= oPt.x) {
                return run;
            }
        }

        return null;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Interest on Location =&gt; Run
     *
     * @param locationEvent the interesting event
     */
    @Override
    public void onEvent (LocationEvent locationEvent)
    {
        try {
            // Ignore RELEASING
            if (locationEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            logger.debug("RunsTable {}: {}", name, locationEvent);

            if (locationEvent instanceof LocationEvent) {
                // Location => Run
                handleEvent(locationEvent);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------//
    // purge //
    //-------//
    /**
     * Purge a runs table of all runs that match the provided predicate.
     *
     * @param predicate the filter to detect runs to remove
     * @return this table, to allow easy chaining
     */
    public RunTable purge (Predicate<Run> predicate)
    {
        return purge(predicate, null);
    }

    //-------//
    // purge //
    //-------//
    /**
     * Purge a runs table of all runs that match the provided predicate, and
     * populate the provided 'removed' table with the removed runs.
     *
     * @param predicate the filter to detect runs to remove
     * @param removed   (output) a table to be filled, if not null, with purged runs
     * @return this table, to allow easy chaining
     */
    public RunTable purge (Predicate<Run> predicate,
                           RunTable removed)
    {
        // Check parameters
        if (removed != null) {
            if (removed.orientation != orientation) {
                throw new IllegalArgumentException("'removed' table is of different orientation");
            }

            if ((removed.width != width) || (removed.height != height)) {
                throw new IllegalArgumentException("'removed' table is of different dimension");
            }
        }

        for (int i = 0, size = getSize(); i < size; i++) {
            for (Itr it = new Itr(i); it.hasNext();) {
                Run run = it.next();

                if (predicate.check(run)) {
                    it.remove();

                    if (removed != null) {
                        removed.addRun(i, run);
                    }
                }
            }
        }

        return this;
    }

    //-----------//
    // removeRun //
    //-----------//
    /**
     * Remove the provided run at indicated position.
     * <p>
     * A runtime exception is thrown if the run is not found in the table.
     *
     * @param index the index of sequence where run is to be found
     * @param run   the run to remove
     */
    public void removeRun (int index,
                           Run run)
    {
        // Find where this run lies in rle
        Iterator<Run> iter = new Itr(index);

        while (iter.hasNext()) {
            Run r = iter.next();

            if (r.isIdentical(run)) {
                // We are located on the right run
                iter.remove();

                return;
            }
        }

        throw new RuntimeException(this + " Cannot find " + run + " at pos " + index);
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the table runs onto the clip area of the provided graphics environment.
     *
     * @param g target environment
     */
    public void render (Graphics2D g)
    {
        // Potential clipping area (perhaps null)
        Rectangle clip = g.getClipBounds();

        switch (getOrientation()) {
        case HORIZONTAL: {
            int minRow = (clip != null) ? Math.max(clip.y, 0) : 0;
            int maxRow = (clip != null) ? (Math.min((clip.y + clip.height), getHeight()) - 1)
                    : (getHeight() - 1);

            for (int row = minRow; row <= maxRow; row++) {
                for (Itr it = new Itr(row); it.hasNext();) {
                    Run run = it.next();
                    g.fillRect(run.getStart(), row, run.getLength(), 1);
                }
            }
        }

        break;

        case VERTICAL: {
            int minRow = (clip != null) ? Math.max(clip.x, 0) : 0;
            int maxRow = (clip != null) ? (Math.min((clip.x + clip.width), getWidth()) - 1)
                    : (getWidth() - 1);

            for (int row = minRow; row <= maxRow; row++) {
                for (Itr it = new Itr(row); it.hasNext();) {
                    Run run = it.next();
                    g.fillRect(row, run.getStart(), 1, run.getLength());
                }
            }
        }

        break;
        }
    }

    //--------------------//
    // setLocationService //
    //--------------------//
    public void setLocationService (SelectionService locationService)
    {
        for (Class<?> eventClass : eventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" ").append(name);
        sb.append(" ").append(orientation);
        sb.append(" ").append(width).append("x").append(height);

        // Debug
        if (true) {
            sb.append(" runs:").append(getTotalRunCount());
        }

        sb.append("}");

        return sb.toString();
    }

    //--------//
    // encode //
    //--------//
    /**
     * (Package-private) method to encode a list of runs into a table sequence.
     *
     * @param list the list of runs to compose the sequence
     * @return the sequence ready to be inserted into table
     */
    static short[] encode (List<Run> list)
    {
        if ((list == null) || list.isEmpty()) {
            return null;
        }

        short[] seq;
        int size = (2 * list.size()) - 1;
        int start = list.get(0).getStart();
        int cursor = 0;
        int length = 0;
        boolean injectBack = false;

        if (start != 0) {
            // Insert an empty foreground length
            size += 2;
            seq = new short[size];
            seq[0] = 0;
            cursor = 1;
            injectBack = true;
        } else {
            seq = new short[size];
        }

        for (Run run : list) {
            if (injectBack) {
                // Inject background
                seq[cursor++] = (short) (run.getStart() - length);
                length = run.getStart();
            }

            // Inject foreground
            seq[cursor++] = (short) run.getLength();
            length += run.getLength();

            injectBack = true;
        }

        return seq;
    }

    //-------------//
    // getSequence //
    //-------------//
    /**
     * (package private) Report the sequence of runs at a given index
     *
     * @param index the desired index
     * @return the MODIFIABLE sequence of rows
     */
    final short[] getSequence (int index)
    {
        return sequences[index];
    }

    //-------------//
    // isIdentical //
    //-------------//
    /**
     * (package private) Field by field comparison (meant for unitary tests)
     *
     * @param that the other RunTable to compare with
     * @return true if identical
     */
    boolean isIdentical (RunTable that)
    {
        // Check null entities
        if (that == null) {
            return false;
        }

        if ((this.orientation == that.orientation)
            && (this.width == that.width)
            && (this.height == that.height)) {
            // Check run sequences, row by row
            for (int row = 0, size = getSize(); row < size; row++) {
                short[] thisSeq = getSequence(row);
                short[] thatSeq = that.getSequence(row);

                if (!Arrays.equals(thisSeq, thatSeq)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    //-------------//
    // setSequence //
    //-------------//
    /**
     * (package private) method meant to optimize the filling of a whole run sequence.
     *
     * @param index position in sequences list
     * @param seq   the run sequence already populated
     */
    void setSequence (int index,
                      short[] seq)
    {
        sequences[index] = seq;
    }

    /**
     * Report the number of foreground runs in the sequence.
     *
     * @param rle
     * @return the number of foreground runs
     */
    private static int sequenceSize (short[] rle)
    {
        if ((rle == null) || (rle.length == 0)) {
            return 0;
        }

        if (rle[0] == 0) {
            // Case of an initial background run
            return (rle.length - 1) / 2;
        } else {
            return (rle.length + 1) / 2;
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in location => Run
     *
     * @param location
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        ///logger.info("RunTable location: {}", locationEvent);
        Rectangle rect = locationEvent.getData();

        if (rect == null) {
            return;
        }

        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        if ((rect.width == 0) && (rect.height == 0)) {
            Point pt = rect.getLocation();

            // Publish Run information
            Run run = getRunAt(pt.x, pt.y);
            getRunService().publish(new RunEvent(this, hint, movement, run));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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

        /** The index of sequence being iterated upon. */
        private final int index;

        /** Current position in sequence array.
         * Always on an even position, pointing to the length of Foreground to be returned by
         * next() */
        private int cursor = 0;

        /** Start location of foreground run to be returned by next(). */
        private int loc = 0;

        /** <b>Reusable</b> Run structure. This is just a buffer meant to optimize browsing.
         * Beware, don't keep a pointer to this Run object, make a copy.
         */
        private final Run run = new Run(-1, -1);

        //~ Constructors ---------------------------------------------------------------------------
        public Itr (int index)
        {
            this.index = index;

            final short[] rle = sequences[index];

            // Check the case of an initial background run
            if (rle != null) {
                if (rle[cursor] == 0) {
                    if (rle.length > 1) {
                        loc = rle[1];
                    }

                    cursor += 2;
                }
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
            final short[] rle = sequences[index];

            if (rle == null) {
                return false;
            }

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

            final short[] rle = sequences[index];

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
            final short[] rle = sequences[index];
            int c = cursor - 2;

            if (c == 0) {
                if (c == (rle.length - 1)) {
                    // F -> null
                    sequences[index] = null;
                } else {
                    // (FB)F... -> 0(B')F...
                    rle[1] = (short) (rle[0] + rle[1]);
                    rle[0] = 0;
                }
            } else {
                final short[] newRle = new short[rle.length - 2];

                if (c == (rle.length - 1)) {
                    // ...F(BF) -> ...F
                    System.arraycopy(rle, 0, newRle, 0, newRle.length);
                } else {
                    // ...F(BFB)F... -> ...F(B')F...
                    System.arraycopy(rle, 0, newRle, 0, c - 1);
                    newRle[c - 1] = (short) (rle[c - 1] + rle[c] + rle[c + 1]);
                    System.arraycopy(rle, c + 2, newRle, c, rle.length - c - 2);
                }

                if ((newRle.length == 1) && (newRle[0] == 0)) {
                    sequences[index] = null;
                } else {
                    sequences[index] = newRle;
                }

                cursor = c;
            }
        }
    }

    //------------------//
    // SequencesAdapter //
    //------------------//
    /**
     * Meant for customized JAXB support of sequences.
     */
    private static class SequencesAdapter
            extends XmlAdapter<ShortVector[], short[][]>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public ShortVector[] marshal (short[][] data)
                throws Exception
        {
            final ShortVector[] seqArray = new ShortVector[data.length];

            for (int i = 0; i < data.length; i++) {
                seqArray[i] = new ShortVector(data[i]);
            }

            return seqArray;
        }

        @Override
        public short[][] unmarshal (ShortVector[] seqArray)
        {
            final short[][] matrix = new short[seqArray.length][];

            for (int i = 0; i < seqArray.length; i++) {
                ShortVector seq = seqArray[i];

                if ((seq != null) && (seq.vector.length != 0)) {
                    matrix[i] = seq.vector;
                }
            }

            return matrix;
        }
    }

    //-------------//
    // ShortVector //
    //-------------//
    /**
     * Temporary structure for (un)marshaling purpose.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class ShortVector
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlValue // Annotation to avoid any wrapper

        private short[] vector;

        //~ Constructors ---------------------------------------------------------------------------
        public ShortVector ()
        {
        }

        public ShortVector (short[] vector)
        {
            this.vector = vector;
        }
    }
}
