//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R u n T a b l e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.image.PixelSource;
import static omr.image.PixelSource.BACKGROUND;
import omr.image.Table;

import omr.math.PointsCollector;

import omr.moments.ARTMoments;
import omr.moments.BasicARTExtractor;
import omr.moments.GeometricMoments;
import omr.moments.QuantizedARTMoments;
import static omr.run.Orientation.HORIZONTAL;

import omr.util.ByteUtil;
import omr.util.Predicate;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code RunTable} handles a rectangular assembly of oriented runs.
 * <p>
 * A RunTable is implemented as an array of run sequences, each run sequence being encoded as RLE
 * (Run Length Encoding) as follows:
 * <p>
 * The very first run is always considered to be foreground.
 * If a sequence starts with background, the very first (foreground) length must be zero.
 * So, the RLE array always has an odd number of cells, beginning and ending with foreground.
 * An empty sequence is encoded as null (although an array containing no value is also accepted).
 * <p>
 * No zero value should be found in the sequence (except in position 0, followed by a positive
 * background length and a positive foreground length).
 * <p>
 * We can have these various kinds of sequence, where 'F' stands for the length of a foreground run
 * and 'B' for the length of a background run:
 * <pre>
 * null    (for an empty sequence)
 * []      (for an empty sequence as well)
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
        implements Cloneable, PixelSource, Oriented
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            RunTable.class);

    private static JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //----------------
    /** Orientation, the same for this table and all contained runs. */
    @XmlAttribute
    private final Orientation orientation;

    /** Width of the table. */
    @XmlAttribute
    private final int width;

    /** Height of the table. */
    @XmlAttribute
    private final int height;

    /** Sequences of runs. */
    @XmlElement(name = "runs")
    private final RunSequence[] sequences;

    // Transient data
    //---------------
    /** Hosted event service for UI events related to this table (Runs), if any. */
    private RunService runService;

    /** Cached total weight. */
    private Integer weight;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RunTable object.
     *
     * @param orientation orientation of each run
     * @param width       table width
     * @param height      table height
     */
    public RunTable (Orientation orientation,
                     int width,
                     int height)
    {
        this.orientation = orientation;
        this.width = width;
        this.height = height;

        // Allocate the array of sequences, according to orientation
        final int seqNb = orientation.isVertical() ? width : height;
        sequences = new RunSequence[seqNb];
    }

    /**
     * No-arg constructor, needed for JAXB.
     */
    private RunTable ()
    {
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
        // Check run validity
        if (start < 0) {
            throw new RuntimeException("Illegal run start " + start);
        }

        if (length <= 0) {
            throw new RuntimeException("Illegal run length " + length);
        }

        weight = null; // Invalidate chached data

        // Look for background where foreground run is to take place
        // ...F(B)F... -> ...F(B1FB2)F...
        // .......^
        RunSequence sequence = sequences[index];

        if (sequence == null) {
            sequences[index] = sequence = new RunSequence();
        }

        short[] rle = sequence.rle;
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
                    sequence.rle = newRle;
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
                    sequence.rle = newRle;
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
                sequence.rle = newRle;
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

            sequence.rle = newRle;
        }

        return true;
    }

    //-------------------//
    // computeArtMoments //
    //-------------------//
    /**
     * Compute the Angular Radial Transform moments for this runTable
     *
     * @param left abscissa of topLeft corner (TODO: useful?)
     * @param top  ordinate of topLeft corner (TODO: useful?)
     * @return the ART moments
     */
    public ARTMoments computeArtMoments (int left,
                                         int top)
    {
        // Retrieve glyph foreground points
        final PointsCollector collector = new PointsCollector(null, getWeight());
        cumulate(collector, new Point(left, top));

        ///ARTMoments artMoments = new BasicARTMoments();
        ARTMoments artMoments = new QuantizedARTMoments();

        BasicARTExtractor extractor = new BasicARTExtractor();
        extractor.setDescriptor(artMoments);
        extractor.extract(collector.getXValues(), collector.getYValues(), collector.getSize());

        return artMoments;
    }

    //-----------------//
    // computeCentroid //
    //-----------------//
    public Point computeCentroid (int left,
                                  int top)
    {
        // Retrieve glyph foreground points
        getWeight(); // Make sure weight has been computed

        if (weight == 0) {
            return null;
        }

        final PointsCollector collector = new PointsCollector(null, weight);
        cumulate(collector, new Point(left, top));

        int[] xx = collector.getXValues();
        int[] yy = collector.getYValues();
        double x = 0;
        double y = 0;

        for (int i = weight - 1; i >= 0; i--) {
            x += xx[i];
            y += yy[i];
        }

        return new Point((int) Math.rint(x / weight), (int) Math.rint(y / weight));
    }

    //-------------------------//
    // computeGeometricMoments //
    //-------------------------//
    /**
     * Compute the geometric moments for this runTable
     *
     * @param left      abscissa of topLeft corner
     * @param top       ordinate of topLeft corner
     * @param interline scaling information
     * @return the geometric moments
     */
    public GeometricMoments computeGeometricMoments (int left,
                                                     int top,
                                                     int interline)
    {
        // Retrieve glyph foreground points
        final PointsCollector collector = new PointsCollector(null, getWeight());
        cumulate(collector, new Point(left, top));

        // Then compute the geometric moments with this collector
        return new GeometricMoments(
                collector.getXValues(),
                collector.getYValues(),
                collector.getSize(),
                interline);
    }

    //----------//
    // contains //
    //----------//
    /**
     * Report whether this table contains the provided relative point.
     *
     * @param relPoint provided point, relative to runTable top left corner
     * @return true if a run contains this point
     */
    public boolean contains (Point relPoint)
    {
        return null != getRunAt(relPoint.x, relPoint.y);
    }

    //------//
    // copy //
    //------//
    /**
     * Make a deep copy of the table.
     *
     * @return another table with runs content identical to this one
     */
    public RunTable copy ()
    {
        RunTable clone = new RunTable(orientation, width, height);

        for (int i = 0; i < sequences.length; i++) {
            RunSequence seq = sequences[i];

            if (seq != null) {
                short[] rle = new short[seq.rle.length];
                System.arraycopy(seq.rle, 0, rle, 0, seq.rle.length);
                clone.sequences[i] = new RunSequence(rle);
            }
        }

        return clone;
    }

    //----------//
    // cumulate //
    //----------//
    /**
     * Cumulate all points that compose the runs of the table, into the provided
     * <b>absolute</b> collector, which may exhibit a roi also with absolute coordinates.
     *
     * @param collector (output) the absolute points collector to populate
     * @param offset    offset to be added to run coordinates, or null
     */
    public void cumulate (PointsCollector collector,
                          Point offset)
    {
        final Rectangle roi = collector.getRoi();

        if (roi == null) {
            for (int p = 0, iBreak = getSize(); p < iBreak; p++) {
                for (Iterator<Run> it = iterator(p); it.hasNext();) {
                    Run run = it.next();
                    int start = run.getStart();

                    for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                        if (orientation == HORIZONTAL) {
                            collector.include(start + ic, p);
                        } else {
                            collector.include(p, start + ic);
                        }
                    }
                }
            }
        } else {
            // Take only the pixels contained by the absolute roi
            Rectangle oRoi = orientation.oriented(roi);
            final int pMin = oRoi.y;
            final int pMax = -1 + Math.min(getSize(), oRoi.y + oRoi.height);
            final int cMin = oRoi.x;
            final int cMax = (oRoi.x + oRoi.width) - 1;

            for (int p = pMin; p <= pMax; p++) {
                for (Iterator<Run> it = iterator(p); it.hasNext();) {
                    final Run run = it.next();
                    final int roiStart = Math.max(run.getStart(), cMin);
                    final int roiStop = Math.min(run.getStop(), cMax);
                    final int length = roiStop - roiStart + 1;

                    if (length > 0) {
                        for (int c = roiStart; c <= roiStop; c++) {
                            if (orientation == HORIZONTAL) {
                                collector.include(c, p);
                            } else {
                                collector.include(p, c);
                            }
                        }
                    }
                }
            }
        }

        // Translation if needed
        if (offset != null) {
            collector.translate(offset.x, offset.y);
        }
    }

    //--------//
    // dumpOf //
    //--------//
    /**
     * Report a drawing of the table.
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
            final RunSequence seq = sequences[i];
            System.out.printf("%4d:%s%n", i, (seq != null) ? seq.toString() : "null");
        }
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final RunTable other = (RunTable) obj;

        if (this.width != other.width) {
            return false;
        }

        if (this.height != other.height) {
            return false;
        }

        if (this.orientation != other.orientation) {
            return false;
        }

        return Arrays.deepEquals(this.sequences, other.sequences);
    }

    //-----------//
    // fillTable //
    //-----------//
    /**
     * Populate the provided table with run pixels.
     *
     * @param table       (output) the table to fill
     * @param tableOrigin top-left corner of table
     * @param offset      offset to be added to runTable coordinates, or null
     * @param fat         true to set adjacent pixels
     */
    public void fillTable (Table.UnsignedByte table,
                           Point tableOrigin,
                           Point offset,
                           boolean fat)
    {
        table.fill(255); // All white

        if (tableOrigin == null) {
            tableOrigin = new Point(0, 0);
        }

        if (offset == null) {
            offset = new Point(0, 0);
        }

        final int dx = tableOrigin.x - offset.x;
        final int dy = tableOrigin.y - offset.y;

        final int tableHeight = table.getHeight();
        final int tableWidth = table.getWidth();

        // Take only the pixels contained by the absolute roi
        if (orientation == HORIZONTAL) {
            final int pMin = Math.max(0, dy);
            final int pMax = -1 + Math.min(getSize(), dy + tableHeight);
            final int cMin = Math.max(0, dx);
            final int cMax = -1 + Math.min(width, dx + tableWidth);

            for (int p = pMin; p <= pMax; p++) {
                for (Iterator<Run> it = iterator(p); it.hasNext();) {
                    final Run run = it.next();
                    final int roiStart = Math.max(run.getStart(), cMin);
                    final int roiStop = Math.min(run.getStop(), cMax);
                    final int length = roiStop - roiStart + 1;

                    if (length > 0) {
                        final int y = p - dy;

                        for (int c = roiStart; c <= roiStop; c++) {
                            table.setValue(c - dx, y, 0);
                        }

                        if (fat) {
                            final int y1 = y - 1;

                            if (y1 >= 0) {
                                for (int c = roiStart; c <= roiStop; c++) {
                                    table.setValue(c - dx, y1, 0);
                                }
                            }

                            final int y2 = y + 1;

                            if (y2 < tableHeight) {
                                for (int c = roiStart; c <= roiStop; c++) {
                                    table.setValue(c - dx, y2, 0);
                                }
                            }

                            final int x1 = roiStart - cMin - 1;

                            if (x1 >= 0) {
                                table.setValue(x1, y, 0);
                            }

                            final int x2 = roiStop - cMin + 1;

                            if (x2 < tableWidth) {
                                table.setValue(x2, y, 0);
                            }
                        }
                    }
                }
            }
        } else {
            final int pMin = Math.max(0, dx);
            final int pMax = -1 + Math.min(getSize(), dx + tableWidth);
            final int cMin = Math.max(0, dy);
            final int cMax = -1 + Math.min(height, dy + tableHeight);

            for (int p = pMin; p <= pMax; p++) {
                for (Iterator<Run> it = iterator(p); it.hasNext();) {
                    final Run run = it.next();
                    final int roiStart = Math.max(run.getStart(), cMin);
                    final int roiStop = Math.min(run.getStop(), cMax);
                    final int length = roiStop - roiStart + 1;

                    if (length > 0) {
                        final int x = p - dx;

                        for (int c = roiStart; c <= roiStop; c++) {
                            table.setValue(x, c - dy, 0);
                        }

                        if (fat) {
                            final int x1 = x - 1;

                            if (x1 >= 0) {
                                for (int c = roiStart; c <= roiStop; c++) {
                                    table.setValue(x1, c - dy, 0);
                                }
                            }

                            final int x2 = x + 1;

                            if (x2 < tableWidth) {
                                for (int c = roiStart; c <= roiStop; c++) {
                                    table.setValue(x2, c - dy, 0);
                                }
                            }

                            final int y1 = roiStart - cMin - 1;

                            if (y1 >= 0) {
                                table.setValue(x, y1, 0);
                            }

                            final int y2 = roiStop - cMin + 1;

                            if (y2 < tableHeight) {
                                table.setValue(x, y2, 0);
                            }
                        }
                    }
                }
            }
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
        // Determine the bounding box
        final ByteProcessor buffer = new ByteProcessor(width, height);
        ByteUtil.raz(buffer); // buffer.invert();

        for (int iSeq = 0, size = getSize(); iSeq < size; iSeq++) {
            for (Itr it = new Itr(iSeq); it.hasNext();) {
                final Run run = it.next();

                for (int coord = run.getStart(), stop = run.getStop(); coord <= stop; coord++) {
                    if (orientation == HORIZONTAL) {
                        buffer.set(coord, iSeq, 0);
                    } else {
                        buffer.set(iSeq, coord, 0);
                    }
                }
            }
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
        render(g, new Point(0, 0));
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
     * Report the run found at given <b>relative</b> coordinates, if any.
     *
     * @param x abscissa, relative to runTable left
     * @param y ordinate, relative to runTable top
     * @return the run found, or null otherwise
     */
    public final Run getRunAt (int x,
                               int y)
    {
        final int iSeq = (orientation == HORIZONTAL) ? y : x;

        if ((iSeq < 0) || (iSeq >= sequences.length)) {
            return null;
        }

        final int coord = (orientation == HORIZONTAL) ? x : y;

        for (Itr it = new Itr(iSeq); it.hasNext();) {
            Run run = it.next();

            if (run.getStart() > coord) {
                return null;
            }

            if (run.getStop() >= coord) {
                return run;
            }
        }

        return null;
    }

    //---------------//
    // getRunService //
    //---------------//
    /**
     * Report the run service, if any, defined on this table
     *
     * @return the run service or null
     */
    public RunService getRunService ()
    {
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

        for (RunSequence seq : sequences) {
            if (seq != null) {
                total += seq.size();
            }
        }

        return total;
    }

    //-----------//
    // getWeight //
    //-----------//
    /**
     * Report the number of foreground pixels.
     *
     * @return the table weight
     */
    public int getWeight ()
    {
        if (weight == null) {
            weight = 0;

            for (int iSeq = 0, iBreak = getSize(); iSeq < iBreak; iSeq++) {
                for (Iterator<Run> it = iterator(iSeq); it.hasNext();) {
                    weight += it.next().getLength();
                }
            }
        }

        return weight;
    }

    //-----------//
    // unmarshal //
    //-----------//
    public static RunTable unmarshal (Path path)
    {
        logger.debug("RunTable unmarshalling {}", path);

        try {
            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);

            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(RunTable.class);
            }

            Unmarshaller um = jaxbContext.createUnmarshaller();
            RunTable runTable = (RunTable) um.unmarshal(is);
            is.close();
            logger.debug("Unmarshalled {}", runTable);

            return runTable;
        } catch (Exception ex) {
            logger.warn("RunTable. Error unmarshalling " + path + " " + ex, ex);

            return null;
        }
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

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        getWeight();

        int hash = 3;
        hash = (83 * hash) + Objects.hashCode(this.orientation);
        hash = (83 * hash) + this.width;
        hash = (83 * hash) + this.height;
        hash = (83 * hash) + Objects.hashCode(this.weight);

        return hash;
    }

    //--------------------//
    // persistentHashCode //
    //--------------------//
    /**
     * Provide a hash code value that PERSIST across application executions.
     *
     * @return the persistent hash code for this run table
     */
    public int persistentHashCode ()
    {
        getWeight();

        int hash = 3;
        hash = (83 * hash) + this.orientation.ordinal();
        hash = (83 * hash) + this.width;
        hash = (83 * hash) + this.height;
        hash = (83 * hash) + this.weight;

        return hash;
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

    //------------//
    // intersects //
    //------------//
    /**
     * Report whether this runTable has at least one pixel in common with the provided
     * table.
     *
     * @param table       the provided table
     * @param tableOrigin coordinates of table top-left corner
     * @param offset      offset to be added to run coordinates
     * @return true if non-null intersection found
     */
    public boolean intersects (Table.UnsignedByte table,
                               Point tableOrigin,
                               Point offset)
    {
        if (tableOrigin == null) {
            tableOrigin = new Point(0, 0);
        }

        if (offset == null) {
            offset = new Point(0, 0);
        }

        final int dx = tableOrigin.x - offset.x;
        final int dy = tableOrigin.y - offset.y;

        if (orientation == HORIZONTAL) {
            final int pMin = Math.max(0, dy);
            final int pMax = -1 + Math.min(getSize(), dy + table.getHeight());
            final int cMin = Math.max(0, dx);
            final int cMax = -1 + Math.min(width, dx + table.getWidth());

            for (int p = pMin; p <= pMax; p++) {
                for (Iterator<Run> it = iterator(p); it.hasNext();) {
                    final Run run = it.next();
                    final int roiStart = Math.max(run.getStart(), cMin);
                    final int roiStop = Math.min(run.getStop(), cMax);
                    final int length = roiStop - roiStart + 1;

                    if (length > 0) {
                        for (int c = roiStart; c <= roiStop; c++) {
                            if (table.getValue(c - dx, p - dy) == 0) {
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            final int pMin = Math.max(0, dx);
            final int pMax = -1 + Math.min(getSize(), dx + table.getWidth());
            final int cMin = Math.max(0, dy);
            final int cMax = -1 + Math.min(height, dy + table.getHeight());

            for (int p = pMin; p <= pMax; p++) {
                for (Iterator<Run> it = iterator(p); it.hasNext();) {
                    final Run run = it.next();
                    final int roiStart = Math.max(run.getStart(), cMin);
                    final int roiStop = Math.min(run.getStop(), cMax);
                    final int length = roiStop - roiStart + 1;

                    if (length > 0) {
                        for (int c = roiStart; c <= roiStop; c++) {
                            if (table.getValue(p - dx, c - dy) == 0) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
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
                weight = null;

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
     * @param g      target environment
     * @param offset absolute offset of runTable topLeft corner
     */
    public void render (Graphics2D g,
                        Point offset)
    {
        Objects.requireNonNull(offset, "Cannot render a RunTable at a null offset");

        // Potential clipping area (perhaps null)
        final Rectangle clip = g.getClipBounds();
        final Rectangle bounds = new Rectangle(offset.x, offset.y, width, height);

        if ((clip != null) && !clip.intersects(bounds)) {
            return;
        }

        if (orientation == HORIZONTAL) {
            final int minSeq = (clip != null) ? Math.max(clip.y - offset.y, 0) : 0;
            final int maxSeq = (clip != null)
                    ? (Math.min(((clip.y + clip.height) - offset.y), height) - 1)
                    : (height - 1);

            for (int iSeq = minSeq; iSeq <= maxSeq; iSeq++) {
                for (Itr it = new Itr(iSeq); it.hasNext();) {
                    final Run run = it.next();
                    g.fillRect(offset.x + run.getStart(), offset.y + iSeq, run.getLength(), 1);
                }
            }
        } else {
            final int minSeq = (clip != null) ? Math.max(clip.x - offset.x, 0) : 0;
            final int maxSeq = (clip != null)
                    ? (Math.min((clip.x + clip.width) - offset.x, width) - 1) : (width
                                                                                 - 1);

            for (int iSeq = minSeq; iSeq <= maxSeq; iSeq++) {
                for (Itr it = new Itr(iSeq); it.hasNext();) {
                    final Run run = it.next();
                    g.fillRect(offset.x + iSeq, offset.y + run.getStart(), 1, run.getLength());
                }
            }
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render this runTable at the provided offset location in output table, using the
     * provided value.
     *
     * @param table  output table to be filled
     * @param val    value to be used for every rendered pixel
     * @param offset target location relative to the output table
     */
    public void render (Table table,
                        int val,
                        Point offset)
    {
        Objects.requireNonNull(offset, "Cannot render a RunTable at a null offset");

        if (orientation == HORIZONTAL) {
            final int maxSeq = height - 1;

            for (int iSeq = 0; iSeq <= maxSeq; iSeq++) {
                for (Itr it = new Itr(iSeq); it.hasNext();) {
                    final Run run = it.next();
                    final int y = offset.y + iSeq;

                    for (int x = offset.x + run.getStart(); x <= (offset.x + run.getStop()); x++) {
                        table.setValue(x, y, val);
                    }
                }
            }
        } else {
            final int maxSeq = width - 1;

            for (int iSeq = 0; iSeq <= maxSeq; iSeq++) {
                for (Itr it = new Itr(iSeq); it.hasNext();) {
                    final Run run = it.next();
                    final int x = offset.x + iSeq;

                    for (int y = offset.y + run.getStart(); y <= (offset.y + run.getStop()); y++) {
                        table.setValue(x, y, val);
                    }
                }
            }
        }
    }

    //---------------//
    // setRunService //
    //---------------//
    /**
     * Assign a run service for this table
     *
     * @param runService the run service, perhaps null
     */
    public void setRunService (RunService runService)
    {
        this.runService = runService;
    }

    //-------------//
    // setSequence //
    //-------------//
    /**
     * set a whole run sequence.
     *
     * @param index position in sequences list
     * @param list  a list of runs
     */
    public void setSequence (int index,
                             List<? extends Run> list)
    {
        sequences[index] = encode(list);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(orientation);
        sb.append(" ").append(width).append("x").append(height);

        // Debug
        if (false) {
            sb.append(" runs:").append(getTotalRunCount());
        }

        sb.append("}");

        return sb.toString();
    }

    //------//
    // trim //
    //------//
    /**
     * Trim this run table, to come up with the smallest bounding box.
     *
     * @param offset (output) resulting offset WRT initial run table
     *
     * @return the resulting trimmed table
     */
    public RunTable trim (Point offset)
    {
        // Determine smallest bounding box
        int iSeqMin = 0;
        int iSeqMax = getSize();

        for (int iSeq = 0, size = getSize(); iSeq < size; iSeq++) {
            if (!isSequenceEmpty(iSeq)) {
                iSeqMin = iSeq;

                break;
            }
        }

        for (int iSeq = getSize() - 1; iSeq >= 0; iSeq--) {
            if (!isSequenceEmpty(iSeq)) {
                iSeqMax = iSeq;

                break;
            }
        }

        final boolean isVertical = orientation == Orientation.VERTICAL;
        int coordMin = isVertical ? height : width;
        int coordMax = 0;

        for (int iSeq = iSeqMin; iSeq <= iSeqMax; iSeq++) {
            for (Iterator<Run> it = iterator(iSeq); it.hasNext();) {
                final Run run = it.next();
                coordMin = Math.min(coordMin, run.getStart());
                coordMax = Math.max(coordMax, run.getStop());
            }
        }

        final int newWidth = isVertical ? (iSeqMax - iSeqMin + 1) : (coordMax - coordMin + 1);
        final int newHeight = isVertical ? (coordMax - coordMin + 1) : (iSeqMax - iSeqMin + 1);

        if ((newWidth == width) && (newHeight == height)) {
            offset.x = offset.y = 0;

            return this; // No modification
        }

        // Generate a shrunk table
        RunTable newTable = new RunTable(orientation, newWidth, newHeight);
        int i = -1; // Sequence index in newTable

        for (int iSeq = iSeqMin; iSeq <= iSeqMax; iSeq++) {
            RunSequence seq = getSequence(iSeq);
            i++;

            if ((seq != null) && (seq.rle != null)) {
                final short[] seqRle = seq.rle;
                final short[] rle;

                if (coordMin == 0) {
                    // Simply copy the rle
                    rle = new short[seqRle.length];
                    System.arraycopy(seqRle, 0, rle, 0, seqRle.length);
                } else {
                    int backLg = seqRle[1] & 0xFFFF; // backLg >= coordMin by definition of coordMin

                    if (backLg > coordMin) {
                        // Shorten the background length
                        rle = new short[seqRle.length];
                        System.arraycopy(seqRle, 0, rle, 0, seqRle.length);
                        rle[1] = (short) (backLg - coordMin);
                    } else {
                        // backLg == coordMin, hence skip the initial 0B pair of cells
                        rle = new short[seqRle.length - 2];
                        System.arraycopy(seqRle, 2, rle, 0, seqRle.length - 2);
                    }
                }

                newTable.sequences[i] = new RunSequence(rle);
            }
        }

        offset.x = isVertical ? iSeqMin : coordMin;
        offset.y = isVertical ? coordMin : iSeqMin;

        return newTable;
    }

    //-------//
    // write //
    //-------//
    /**
     * Write the table at proper offset in provided buffer
     *
     * @param buffer  the buffer to be written to
     * @param xOffset relative buffer abscissa for runTable topLeft corner
     * @param yOffset relative buffer ordinate for runTable topLeft corner
     */
    public void write (ByteProcessor buffer,
                       int xOffset,
                       int yOffset)
    {
        final boolean isVertical = orientation == Orientation.VERTICAL;

        for (int iSeq = 0, size = getSize(); iSeq < size; iSeq++) {
            for (Iterator<Run> it = iterator(iSeq); it.hasNext();) {
                final Run run = it.next();

                for (int coord = run.getStart(), stop = run.getStop(); coord <= stop; coord++) {
                    if (isVertical) {
                        buffer.set(xOffset + iSeq, yOffset + coord, 0);
                    } else {
                        buffer.set(xOffset + coord, yOffset + iSeq, 0);
                    }
                }
            }
        }
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
    static RunSequence encode (List<? extends Run> list)
    {
        if ((list == null) || list.isEmpty()) {
            return null;
        }

        short[] rle;
        int size = (2 * list.size()) - 1;
        int start = list.get(0).getStart();
        int cursor = 0;
        int length = 0;
        boolean injectBackground = false;

        if (start != 0) {
            // Insert an empty foreground length
            size += 2;
            rle = new short[size];
            rle[0] = 0;
            cursor = 1;
            injectBackground = true;
        } else {
            rle = new short[size];
        }

        for (Run run : list) {
            if (injectBackground) {
                // Inject background
                rle[cursor++] = (short) (run.getStart() - length);
                length = run.getStart();
            }

            // Inject foreground
            rle[cursor++] = (short) run.getLength();
            length += run.getLength();

            injectBackground = true;
        }

        return new RunSequence(rle);
    }

    //-------------//
    // getSequence //
    //-------------//
    /**
     * (package private) Report the sequence of runs at a given index
     *
     * @param index the desired index
     * @return the MODIFIABLE sequence of runs
     */
    final RunSequence getSequence (int index)
    {
        return sequences[index];
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
    final void setSequence (int index,
                            RunSequence seq)
    {
        sequences[index] = seq;
    }

    //--------------//
    // afterMarshal //
    //--------------//
    /**
     * Called immediately after marshalling of this object.
     * We reset any empty RunSequence to null.
     */
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        for (int i = 0, iBreak = sequences.length; i < iBreak; i++) {
            RunSequence seq = sequences[i];

            if ((seq != null) && ((seq.rle == null) || (seq.rle.length == 0))) {
                sequences[i] = null;
            }
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called immediately after unmarshalling of this object.
     * We reset any empty RunSequence to null.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller m,
                                 Object parent)
    {
        afterMarshal(null);
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     * We replace any null RunSequence by an empty RunSequence (to be properly marshalled).
     */
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        for (int i = 0, iBreak = sequences.length; i < iBreak; i++) {
            RunSequence seq = sequences[i];

            if (seq == null) {
                sequences[i] = new RunSequence(new short[0]);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // RunSequence //
    //-------------//
    /**
     * (package private) Sequence of runs, using run-length encoding.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "runs")
    static class RunSequence
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlValue
        private short[] rle;

        //~ Constructors ---------------------------------------------------------------------------
        public RunSequence (short[] rle)
        {
            this.rle = rle;
        }

        public RunSequence ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof RunSequence)) {
                return false;
            }

            final RunSequence that = (RunSequence) obj;

            return Arrays.equals(rle, that.rle);
        }

        @Override
        public int hashCode ()
        {
            int hash = 5;
            hash = (67 * hash) + Arrays.hashCode(this.rle);

            return hash;
        }

        /**
         * Report the number of foreground runs in this sequence
         *
         * @return count of (foreground) runs
         */
        public int size ()
        {
            if ((rle == null) || (rle.length == 0)) {
                return 0;
            }

            if (rle[0] == 0) {
                return (rle.length - 1) / 2; // Case of an initial background run
            } else {
                return (rle.length + 1) / 2; // Standard case of an initial foreground run
            }
        }

        @Override
        public String toString ()
        {
            return Arrays.toString(rle);
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

            // Check the case of an initial background run
            final RunSequence seq = sequences[index];

            if (seq != null) {
                final short[] rle = seq.rle;

                if ((rle != null) && (rle.length > 0)) {
                    if (rle[cursor] == 0) {
                        if (rle.length > 1) {
                            loc = rle[1];
                        }

                        cursor += 2;
                    }
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
            final RunSequence seq = sequences[index];

            if (seq == null) {
                return false;
            }

            final short[] rle = seq.rle;

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

            final short[] rle = sequences[index].rle;

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
            final short[] rle = sequences[index].rle;
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
                    sequences[index] = new RunSequence(newRle);
                }

                cursor = c;
            }
        }
    }
}
