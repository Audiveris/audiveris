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
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RunTable} handles a rectangular assembly of oriented runs.
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
    @XmlElement(name = "runs")
    private final RunSequence[] sequences;

    /** Hosted event service for UI events related to this table (Runs), if any. */
    private SelectionService runService;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RunTable object.
     *
     * @param name        name for debugging
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

        // Prepare the collections of runs, one sequence per pos value
        sequences = new RunSequence[rect.height];

        for (int i = 0; i < rect.height; i++) {
            sequences[i] = new BasicRunSequence();
        }

        ///logger.info("{} created", this);
    }

    /**
     * Meant for JAXB.
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
    //--------------------//
    // cutLocationService //
    //--------------------//
    public void cutLocationService (SelectionService locationService)
    {
        for (Class<?> eventClass : eventsRead) {
            locationService.unsubscribe(eventClass, this);
        }
    }

    //-----//
    // get //
    //-----//
    /**
     * {@inheritDoc}
     *
     * <br><b>Beware</b>, this implementation is not efficient enough for bulk operations.
     * For such needs, a much more efficient way is to first retrieve a full buffer, via {@link
     * #getBuffer()} method, then use this temporary buffer as the {@link PixelSource} instead of
     * this table.
     *
     * @param x absolute abscissa
     * @param y absolute ordinate
     * @return the pixel gray level
     */
    @Override
    public final int get (int x,
                          int y)
    {
        Run run = getRunAt(x, y);

        return (run != null) ? 0 : BACKGROUND;
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

        RunSequence seq = getSequence(oPt.y);

        for (Run run : seq) {
            if (run.getStart() > oPt.x) {
                return null;
            }

            if (run.getStop() >= oPt.x) {
                return run;
            }
        }

        return null;
    }

    //-------------//
    // getSequence //
    //-------------//
    /**
     * Report the sequence of runs at a given index
     *
     * @param index the desired index
     * @return the MODIFIABLE sequence of rows
     */
    public final RunSequence getSequence (int index)
    {
        return sequences[index];
    }

    //---------//
    // getSize //
    //---------//
    /**
     * Report the number of sequences of runs in the table
     *
     * @return the table size (in terms of sequences)
     */
    public final int getSize ()
    {
        return sequences.length;
    }

    //------//
    // copy //
    //------//
    /**
     * Make a copy of the table, but sharing the run instances
     *
     * @return another table on the same run instances
     */
    public RunTable copy ()
    {
        return copy(name + "(copy)");
    }

    //------//
    // copy //
    //------//
    /**
     * Make a copy of the table, but sharing the run instances
     *
     * @param name a new name for the copy
     * @return another table on the same run instances
     */
    public RunTable copy (String name)
    {
        RunTable clone = new RunTable(name, orientation, width, height);

        for (int i = 0; i < sequences.length; i++) {
            BasicRunSequence seq = (BasicRunSequence) getSequence(i);
            clone.sequences[i] = new BasicRunSequence(seq);
        }

        return clone;
    }

    //--------//
    // dumpOf //
    //--------//
    /**
     * Report the image of the runs table.
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
    public void dumpSequences ()
    {
        System.out.println(toString());

        for (int i = 0; i < sequences.length; i++) {
            RunSequence seq = sequences[i];
            System.out.printf("%4d:%s%n", i, seq.toString());
        }
    }

    //-----------//
    // getBuffer //
    //-----------//
    /**
     * Fill a rectangular buffer with the runs
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

            for (int row = 0; row < getSize(); row++) {
                RunSequence seq = getSequence(row);

                for (Run run : seq) {
                    for (int c = run.getStart(); c <= run.getStop(); c++) {
                        buffer.set(c, row, 0);
                    }
                }
            }

            break;

        case VERTICAL:

            for (int row = 0; row < getSize(); row++) {
                RunSequence seq = getSequence(row);

                for (Run run : seq) {
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
     * Report a BufferedImage painted with the content of this RunTable.
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
    @Override
    public int getHeight ()
    {
        return height;
    }

    //---------//
    // getName //
    //---------//
    /**
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
     * @return the orientation of the runs
     */
    @Override
    public Orientation getOrientation ()
    {
        return orientation;
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

    //------------------//
    // getTotalRunCount //
    //------------------//
    /**
     * Report the total number of runs in table
     *
     * @return the total runs count
     */
    public int getTotalRunCount ()
    {
        int total = 0;

        for (RunSequence seq : sequences) {
            total += seq.size();
        }

        return total;
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return width;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include the content of the provided table into this one
     *
     * @param that the table of runs to include into this one
     */
    public void include (RunTable that)
    {
        if (that == null) {
            throw new IllegalArgumentException("Cannot include a null runsTable");
        }

        if (that.orientation != orientation) {
            throw new IllegalArgumentException(
                    "Cannot include a runsTable of different orientation");
        }

        if (that.width != width) {
            throw new IllegalArgumentException("Cannot include a runsTable of different width");
        }

        if (that.height != height) {
            throw new IllegalArgumentException("Cannot include a runsTable of different height");
        }

        for (int row = 0; row < getSize(); row++) {
            RunSequence thisSeq = this.getSequence(row);
            RunSequence thatSeq = that.getSequence(row);

            for (Run thatRun : thatSeq) {
                thisSeq.add(thatRun);
            }
        }
    }

    //-------------//
    // isIdentical //
    //-------------//
    /**
     * Field by field comparison (TODO: used by unit tests only!)
     *
     * @param that the other RunTable to compare with
     * @return true if identical
     */
    public boolean isIdentical (RunTable that)
    {
        // Check null entities
        if (that == null) {
            return false;
        }

        if ((this.orientation == that.orientation)
            && (this.width == that.width)
            && (this.height == that.height)) {
            // Check run sequences, row by row
            for (int row = 0; row < getSize(); row++) {
                RunSequence thisSeq = getSequence(row);
                RunSequence thatSeq = that.getSequence(row);

                if (!thisSeq.equals(thatSeq)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    //-----------//
    // lookupRun //
    //-----------//
    /**
     * Given an absolute point, retrieve the containing run if any
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

        for (Run run : getSequence(oPt.y)) {
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
     * Purge a runs table of all runs that match the provided predicate
     *
     * @param predicate the filter to detect runs to remove
     * @return this runs table, to allow easy chaining
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
     * @param removed   a table to be filled, if not null, with purged runs
     * @return this runs table, to allow easy chaining
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
            RunSequence seq = getSequence(i);

            for (Iterator<Run> it = seq.iterator(); it.hasNext();) {
                Run run = it.next();

                if (predicate.check(run)) {
                    it.remove();

                    if (removed != null) {
                        removed.getSequence(i).add(run);
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
     * Remove the provided run at indicated position
     *
     * @param pos the position where run is to be found
     * @param run the run to remove
     */
    public void removeRun (int pos,
                           Run run)
    {
        RunSequence seq = getSequence(pos);

        if (!seq.remove(run)) {
            throw new RuntimeException(this + " Cannot find " + run + " at pos " + pos);
        }
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
                RunSequence seq = getSequence(row);

                for (Run run : seq) {
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
                RunSequence seq = getSequence(row);

                for (Run run : seq) {
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

    //-------------//
    // setSequence //
    //-------------//
    /**
     * (package private) method meant to optimize the filling of a whole RunSequence.
     *
     * @param index position in sequences list
     * @param seq   the RunSequence already populated
     */
    void setSequence (int index,
                      RunSequence seq)
    {
        sequences[index] = seq;
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
}
