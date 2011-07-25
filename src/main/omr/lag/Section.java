//----------------------------------------------------------------------------//
//                                                                            //
//                               S e c t i o n                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.graph.Vertex;

import omr.lag.ui.LagView;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.PointsCollector;

import omr.run.Run;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.picture.Picture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Section</code> handles a section of contiguous and
 * compatible instances of class {@link Run}.
 *
 * <p> A section does not carry orientation information, only the containing
 * {@link Lag} has this information.  Thus all runs of a given lag (and
 * consequently all sections made of these runs) share the same orientation.
 *
 * <ol> <li> Positions increase in parallel with run numbers, so the thickness
 * of a section is defined as the delta between last and first positions, in
 * other words its number of runs. </li>
 *
 * <li> Coordinates increase along any section run, so the section start is the
 * minimum of all run starting coordinates, and the section stop is the maximum
 * of all run stopping coordinates. We define section length as the value: stop
 * - start +1 </li> </ol>
 *
 * @param <L> precise lag (sub)type
 * @param <S> precise section (sub)type
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Section<L extends Lag, S extends Section<L, S>>
    extends Vertex<L, S, SectionSignature>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Section.class);

    /** A section comparator, using section id */
    public static final Comparator<Section> idComparator = new Comparator<Section>() {
        public int compare (Section s1,
                            Section s2)
        {
            return Integer.signum(s1.getId() - s2.getId());
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Position of first run */
    @XmlAttribute(name = "first-pos")
    private int firstPos;

    /** The collection of runs that make up the section */
    @XmlElement(name = "run")
    private final List<Run> runs = new ArrayList<Run>();

    /** Oriented bounding rectangle */
    protected Rectangle orientedBounds;

    /** Absolute mass center */
    private PixelPoint centroid;

    /** Approximate absolute starting point */
    private PixelPoint startPoint;

    /** Approximate absolute stopping point */
    private PixelPoint stopPoint;

    /** Contribution to the foreground */
    private int foreWeight;

    /** Length of longest run */
    private int maxRunLength;

    /** Number of pixels, whatever the gray level */
    private int weight;

    /** Absolute contour points */
    private Polygon contour;

    /** Absolute contour box */
    private PixelRectangle contourBox;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Section //
    //---------//
    /**
     * Creates a new Section.
     */
    protected Section ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getAreaCenter //
    //---------------//
    /**
     * Report the section area absolute center.
     *
     * @return the area absolute center
     */
    public PixelPoint getAreaCenter ()
    {
        PixelRectangle box = getContourBox();

        return new PixelPoint(
            box.x + (box.width / 2),
            box.y + (box.height / 2));
    }

    //-----------//
    // getAspect //
    //-----------//
    /**
     * Report the ratio of length over thickness
     *
     * @return the "slimness" of the section
     */
    public double getAspect ()
    {
        return (double) getLength() / (double) getThickness();
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Return the absolute point which is at the mass center of the section,
     * with all pixels considered of equal weight.
     *
     * @return the mass center of the section, as a absolute point
     */
    public PixelPoint getCentroid ()
    {
        if (centroid == null) {
            Point orientedPoint = new Point(0, 0);
            int   y = firstPos;

            for (Run run : runs) {
                final int length = run.getLength();
                orientedPoint.y += (length * (2 * y));
                orientedPoint.x += (length * ((2 * run.getStart()) + length));
                y++;
            }

            orientedPoint.x /= (2 * getWeight());
            orientedPoint.y /= (2 * getWeight());

            centroid = getGraph()
                           .absolute(orientedPoint);

            if (logger.isFineEnabled()) {
                logger.fine("Centroid of " + this + " is " + centroid);
            }
        }

        return centroid;
    }

    //------------//
    // getContour //
    //------------//
    /**
     * Return the absolute polygon that defines the display contour.
     *
     * @return the absolute perimeter contour
     */
    public Polygon getContour ()
    {
        if (contour == null) {
            contour = computeContour();
        }

        return contour;
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return a COPY of the absolute bounding box.
     * Useful to quickly check if the section needs to be repainted.
     *
     * @return the absolute bounding box
     */
    public PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            contourBox = new PixelRectangle(getContour().getBounds());
        }

        return new PixelRectangle(contourBox);
    }

    //-------------------//
    // getFirstAdjacency //
    //-------------------//
    /**
     * Return the adjacency ratio on the incoming junctions of the section.
     * This is computed as the ratio to the length of the first run, of the
     * sum of run overlapping lengths of the incoming junctions. In other
     * words, this is a measure of how much the section at hand is
     * overlapped with runs.
     *
     * <ul> <li> An isolated section/vertex, such as the one related to a
     * barline, will exhibit a very low adjacency ratio. </li>
     *
     * <li> On the contrary, a section which is just a piece of a larger glyph,
     * such as a treble clef or a brace, will have a higher adjacency. </li>
     * </ul>
     *
     * @return the percentage of overlapped run length
     * @see #getLastAdjacency
     */
    public double getFirstAdjacency ()
    {
        Run run = getFirstRun();
        int runStart = run.getStart();
        int runStop = run.getStop();
        int adjacency = 0;

        for (S source : getSources()) {
            Run lastRun = source.getLastRun();
            int start = Math.max(runStart, lastRun.getStart());
            int stop = Math.min(runStop, lastRun.getStop());

            if (stop >= start) {
                adjacency += (stop - start + 1);
            }
        }

        return (double) adjacency / (double) run.getLength();
    }

    //-------------//
    // setFirstPos //
    //-------------//
    /**
     * Set the position of the first run in the section.
     *
     * @param firstPos position of the first run, abscissa for a vertical run,
     *                 ordinate for a horizontal run.
     */
    public void setFirstPos (int firstPos)
    {
        this.firstPos = firstPos;
    }

    //-------------//
    // getFirstPos //
    //-------------//
    /**
     * Return the position (x for vertical runs, y for horizontal runs) of the
     * first run in the section
     *
     * @return the position
     */
    public int getFirstPos ()
    {
        return firstPos;
    }

    //-------------//
    // getFirstRun //
    //-------------//
    /**
     * Return the first run within the section
     *
     * @return the run, which always exists
     */
    public Run getFirstRun ()
    {
        return runs.get(0);
    }

    //---------------//
    // getForeWeight //
    //---------------//
    /**
     * Return the contribution of the section to the foreground
     *
     * @return the section foreground weight
     */
    public int getForeWeight ()
    {
        return foreWeight;
    }

    //----------//
    // getGraph //
    //----------//
    /**
     * Report the containing graph (lag) of this vertex (section)
     *
     * @return the containing graph
     */
    @Override
    public L getGraph ()
    {
        return graph;
    }

    //------------------//
    // getLastAdjacency //
    //------------------//
    /**
     * Return the adjacency ratio at the end of the section/vertex at hand.  See
     * getFirstAdjacency for explanation of the role of adjacency.
     *
     * @return the percentage of overlapped run length
     * @see #getFirstAdjacency
     */
    public double getLastAdjacency ()
    {
        Run run = getLastRun();
        int runStart = run.getStart();
        int runStop = run.getStop();
        int adjacency = 0;

        for (S target : getTargets()) {
            Run firstRun = target.getFirstRun();
            int start = Math.max(runStart, firstRun.getStart());
            int stop = Math.min(runStop, firstRun.getStop());

            if (stop >= start) {
                adjacency += (stop - start + 1);
            }
        }

        return (double) adjacency / (double) run.getLength();
    }

    //------------//
    // getLastPos //
    //------------//
    /**
     * Return the position of the last run of the section
     *
     * @return the position of last run
     */
    public int getLastPos ()
    {
        return (firstPos + getRunNb()) - 1;
    }

    //------------//
    // getLastRun //
    //------------//
    /**
     * Return the last run of the section
     *
     * @return this last run (rightmost run for vertical section)
     */
    public Run getLastRun ()
    {
        return runs.get(runs.size() - 1);
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Return the length of the section, along the runs direction
     *
     * @return stop - start +1
     */
    public int getLength ()
    {
        return getOrientedBounds().width;
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Return the mean gray level of the section
     *
     * @return the section foreground level (0 -> 255)
     */
    public int getLevel ()
    {
        return (int) Math.rint((double) foreWeight / (double) weight);
    }

    //-----------------//
    // getMaxRunLength //
    //-----------------//
    /**
     * Return the size of the longest run in the section
     *
     * @return the maximum run length
     */
    public int getMaxRunLength ()
    {
        return maxRunLength;
    }

    //------------------//
    // getMeanRunLength //
    //------------------//
    /**
     * Return the average value for all run lengths in the section.
     *
     * @return the mean run length
     */
    public int getMeanRunLength ()
    {
        return weight / getRunNb();
    }

    //-------------------//
    // getOrientedBounds //
    //-------------------//
    /**
     * Return the section oriented bounding rectangle, so please clone it if you
     * want to modify it afterwards
     *
     * @return the section bounding rectangle
     */
    public Rectangle getOrientedBounds ()
    {
        if (orientedBounds == null) {
            orientedBounds = new Rectangle(
                graph.oriented(getContourBox()));
        }

        return orientedBounds;
    }

    //----------//
    // getRunAt //
    //----------//
    /**
     * Retrieves the run at the given position
     *
     * @param pos position of the desired run (x for vertical, y for horizontal)
     *
     * @return the proper Run
     */
    public Run getRunAt (int pos)
    {
        return runs.get(pos - firstPos);
    }

    //----------//
    // getRunNb //
    //----------//
    /**
     * Report the number of runs this sections contains
     *
     * @return the nb of runs in the section
     */
    public int getRunNb ()
    {
        return runs.size();
    }

    //---------//
    // getRuns //
    //---------//
    /**
     * Return the list of all runs in this section
     *
     * @return the section runs
     */
    public List<Run> getRuns ()
    {
        return runs;
    }

    //----------//
    // getStart //
    //----------//
    /**
     * Return the smallest run starting coordinate, which means the smallest y
     * value (ordinate) for a section of vertical runs.
     *
     * @return the starting coordinate of the section
     */
    public int getStart ()
    {
        return getOrientedBounds().x;
    }

    //---------------//
    // getStartPoint //
    //---------------//
    /**
     * Return the approximate absolute point which starts the section
     * (left point for horizontal section, top point for vertical section)
     * @return the approximate absolute starting point
     */
    public PixelPoint getStartPoint ()
    {
        if (startPoint == null) {
            Rectangle roi = new Rectangle(getOrientedBounds());
            roi.width = 3;

            Point pt = getRectangleCentroid(roi);
            startPoint = graph.absolute(new Point(getStart(), pt.y));
        }

        return startPoint;
    }

    //---------//
    // getStop //
    //---------//
    /**
     * Return the largest run stopping coordinate, which is the largest y value
     * (ordinate) for a section of vertical runs.
     *
     * @return the stopping coordinate of the section
     */
    public int getStop ()
    {
        return (getStart() + getLength()) - 1;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    /**
     * Return the approximate absolute point which stops the section
     * (right point for horizontal section, bottom point for vertical section)
     * @return the approximate absolute stopping point
     */
    public PixelPoint getStopPoint ()
    {
        if (stopPoint == null) {
            Rectangle roi = new Rectangle(getOrientedBounds());
            roi.x += (roi.width - 3);
            roi.width = 3;

            Point pt = getRectangleCentroid(roi);
            stopPoint = graph.absolute(new Point(getStop(), pt.y));
        }

        return stopPoint;
    }

    //--------------//
    // getThickness //
    //--------------//
    /**
     * Return the thickness of the section, which is just the number of runs.
     *
     * @return the nb of runs in this section
     */
    public int getThickness ()
    {
        return getOrientedBounds().height;
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Return the thickness of the section at the provided coord value
     * @param coord the coordinate (x for horizontal, y for vertical) around
     * which the thickness is to be measured
     * @param probeWidth the width of the probe to use
     * @return the thickness around this location
     */
    public int getThicknessAt (int coord,
                               int probeWidth)
    {
        getOrientedBounds();

        PixelRectangle roi = new PixelRectangle(
            coord,
            orientedBounds.y,
            0,
            orientedBounds.height);
        roi.grow(probeWidth / 2, 0);

        // Use a large-enough collector
        PointsCollector collector = new PointsCollector(roi);
        cumulate(collector);

        int count = collector.getCount();

        if (count == 0) {
            return 0;
        } else {
            // Find out min and max ordinates
            int[] yy = new int[count];
            System.arraycopy(collector.getYValues(), 0, yy, 0, count);
            Arrays.sort(yy);

            int yMin = yy[0];
            int yMax = yy[count - 1];

            return yMax - yMin + 1;
        }
    }

    //-----------//
    // getWeight //
    //-----------//
    /**
     * Return the total weight of the section, which is the sum of the weight
     * (length) of all runs.
     *
     * @return the section weight
     */
    public int getWeight ()
    {
        if (weight == 0) {
            computeParameters();
        }

        return weight;
    }

    //---------------//
    // allocateTable //
    //---------------//
    /**
     * For basic print out, allocate a drawing table, to be later filled with
     * section pixels
     *
     * @param box the limits of the drawing table
     * @return the table ready to be filled
     */
    public static char[][] allocateTable (Rectangle box)
    {
        char[][] table = new char[box.height + 1][box.width + 1];

        for (int i = 0; i < table.length; i++) {
            Arrays.fill(table[i], ' ');
        }

        return table;
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    /**
     * Report the absolute centroid of the section pixels found in the provided
     * absolute region of interest
     * @param roi the absolute rectangle that defines the region of interest
     * @return the absolute centroid
     */
    public PixelPoint getRectangleCentroid (PixelRectangle roi)
    {
        if (roi == null) {
            throw new IllegalArgumentException("Rectangle of Interest is null");
        }

        Rectangle rect = graph.oriented(roi);
        Point     point = getRectangleCentroid(rect);

        return graph.absolute(point);
    }

    //--------//
    // append //
    //--------//
    /**
     * Extend a section with the given run. This new run is assumed to be
     * contiguous to the current last run of the section, no check is performed.
     *
     * @param run the new last run
     */
    public void append (Run run)
    {
        runs.add(run);
        addRun(run);

        if (logger.isFineEnabled()) {
            logger.fine("Appended " + run + " to " + this);
        }
    }

    //----------//
    // complete //
    //----------//
    /**
     * Called when we have finished adding runs to the section, thus it is now
     * time (and safe) to compute section parameters such as contour, view,
     * etc...
     */
    @SuppressWarnings("unchecked")
    public void complete ()
    {
        // Create views in parallel with containing Lag
        for (Object obj : graph.getViews()) {
            LagView view = (LagView) obj; // !!!!!
            view.addSectionView(this); // Compiler warning here
        }
    }

    //-------------------//
    // computeParameters //
    //-------------------//
    /**
     * Compute the various cached parameters from scratch
     */
    public void computeParameters ()
    {
        // weight & foreWeight & maxRunLength
        weight = 0;
        foreWeight = 0;
        maxRunLength = 0;

        // maxRunLength
        for (Run run : runs) {
            computeRunContribution(run);
        }

        // Invalidate cached data
        invalidateCache();

        if (logger.isFineEnabled()) {
            logger.fine(
                "Parameters of " + this + " maxRunLength=" + getMaxRunLength() +
                " meanRunLength=" + getMeanRunLength() + " weight=" + weight +
                "foreWeight=" + foreWeight);
        }
    }

    //----------//
    // contains //
    //----------//
    /**
     * Predicate to check whether the given point falls within one of the
     * section runs.
     *
     * @param coord coordinate along section length
     * @param pos   run position
     *
     * @return true if point(coord,pos) is contained in the section
     */
    public boolean contains (int coord,
                             int pos)
    {
        // First check with the bounding rectangle
        if (!getOrientedBounds()
                 .contains(coord, pos)) {
            return false;
        }

        // Then a closer look
        Run run = getRunAt(pos);

        return (run.getStart() <= coord) && (run.getStop() >= coord);
    }

    //----------//
    // cumulate //
    //----------//
    /**
     * Cumulate in the provided Barycenter the section pixels that are contained
     * in the provided roi Rectangle. If the roi is null, all pixels are
     * cumulated into the barycenter.
     * @param barycenter the oriented point to populate
     * @param roi the oriented rectangle of interest
     */
    public void cumulate (Barycenter barycenter,
                          Rectangle  roi)
    {
        if (barycenter == null) {
            throw new IllegalArgumentException("Barycenter is null");
        }

        if (roi == null) {
            // Take all run pixels
            int y = firstPos - 1;

            for (Run run : runs) {
                y++;
                barycenter.include(
                    run.getLength(),
                    run.getStart() + (run.getLength() / 2d),
                    y);
            }
        } else {
            // Take only the pixels contained by the roi
            int y = firstPos - 1;
            int yMax = Math.min(firstPos + runs.size(), roi.y + roi.height) -
                       1;
            int xMax = (roi.x + roi.width) - 1;

            for (Run run : runs) {
                y++;

                if (y < roi.y) {
                    continue;
                }

                if (y > yMax) {
                    break;
                }

                final int roiStart = Math.max(run.getStart(), roi.x);
                final int roiStop = Math.min(run.getStop(), xMax);
                final int length = roiStop - roiStart + 1;

                if (length > 0) {
                    barycenter.include(length, roiStart + (length / 2d), y);
                }
            }
        }
    }

    //----------//
    // cumulate //
    //----------//
    /**
     * Cumulate all points that compose the runs of the section, into the
     * provided collector.
     *
     * @param collector the points collector to populate
     */
    public void cumulate (PointsCollector collector)
    {
        Rectangle roi = collector.getRoi();

        if (roi == null) {
            int p = firstPos;

            for (Run run : runs) {
                final int start = run.getStart();

                for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                    collector.include(start + ic, p);
                }

                p++;
            }
        } else {
            // Take only the pixels contained by the roi
            int y = firstPos - 1;
            int yMax = -1 +
                       Math.min(firstPos + runs.size(), roi.y + roi.height);
            int xMax = (roi.x + roi.width) - 1;

            for (Run run : runs) {
                y++;

                if (y < roi.y) {
                    continue;
                }

                if (y > yMax) {
                    break;
                }

                final int roiStart = Math.max(run.getStart(), roi.x);
                final int roiStop = Math.min(run.getStop(), xMax);
                final int length = roiStop - roiStart + 1;

                if (length > 0) {
                    for (int x = roiStart; x <= roiStop; x++) {
                        collector.include(x, y);
                    }
                }
            }
        }
    }

    //----------------//
    // cumulatePoints //
    //----------------//
    /**
     * Cumulate all points that compose the runs of the section, into the
     * provided arrays.
     *
     * @param coord array of abscissae
     * @param pos   array of ordinates
     * @param nb    initial index of first free array element
     *
     * @return final index
     */
    public int cumulatePoints (double[] coord,
                               double[] pos,
                               int      nb)
    {
        int p = firstPos;

        for (Run run : runs) {
            final int start = run.getStart();

            for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                coord[nb] = start + ic;
                pos[nb] = p;
                nb++;
            }

            p++;
        }

        return nb;
    }

    //-----------//
    // drawAscii //
    //-----------//
    /**
     * Draws a basic representation of the section, using ascii characters
     */
    public void drawAscii ()
    {
        System.out.println("Section#" + getId());
        
        // Determine the absolute bounds
        Rectangle box = getContourBox();

        char[][]  table = allocateTable(box);
        fillTable(table, box);
        drawTable(table, box);
    }

    //-----------//
    // drawTable //
    //-----------//
    /**
     * Printout the filled drawing table
     *
     * @param table the filled table
     * @param box the table limits in the image
     */
    public static void drawTable (char[][]  table,
                                  Rectangle box)
    {
        System.out.println(
            "xMin=" + box.x + ", xMax=" + ((box.x + box.width) - 1));
        System.out.println(
            "yMin=" + box.y + ", yMax=" + ((box.y + box.height) - 1));

        for (int iy = 0; iy < table.length; iy++) {
            System.out.print((iy + box.y) + ": ");
            System.out.println(table[iy]);
        }
    }

    //-----------//
    // fillImage //
    //-----------//
    public void fillImage (BufferedImage im,
                           Rectangle     box)
    {
        WritableRaster raster = im.getRaster();
        int            x = getFirstPos() - box.x;

        for (Run run : runs) {
            for (int y = run.getStart(); y <= run.getStop(); y++) {
                raster.setSample(x, y - box.y, 0, 255);
            }

            x += 1;
        }
    }

    //-----------//
    // fillTable //
    //-----------//
    /**
     * Draws the section, into the provided table
     */
    public void fillTable (char[][]  table,
                           Rectangle box)
    {
        // Determine the bounds
        Polygon polygon = getContour();

        int     xPrev = 0;
        int     yPrev = 0;
        int     x;
        int     y;

        for (int i = 0; i <= polygon.npoints; i++) {
            if (i == polygon.npoints) { // Last point
                x = polygon.xpoints[0] - box.x;
                y = polygon.ypoints[0] - box.y;
            } else {
                x = polygon.xpoints[i] - box.x;
                y = polygon.ypoints[i] - box.y;
            }

            if (i > 0) {
                if (x != xPrev) { // Horizontal

                    int x1 = Math.min(x, xPrev);
                    int x2 = Math.max(x, xPrev);

                    for (int ix = x1 + 1; ix < x2; ix++) {
                        table[y][ix] = '-';
                    }
                } else { // Vertical

                    int y1 = Math.min(y, yPrev);
                    int y2 = Math.max(y, yPrev);

                    for (int iy = y1 + 1; iy < y2; iy++) {
                        table[iy][x] = '|';
                    }
                }
            }

            table[y][x] = '+';
            xPrev = x;
            yPrev = y;
        }
    }

    //---------------//
    // inNextSibling //
    //---------------//
    /**
     * Return the next sibling section, both linked by source of last incoming
     * edge
     *
     * @return the next sibling or null
     */
    public S inNextSibling ()
    {
        // Check we have sources
        if (getInDegree() == 0) {
            return null;
        }

        // Proper source section
        S source = getSources()
                       .get(getInDegree() - 1);

        // Browse till we get to this as target
        for (Iterator<S> li = source.getTargets()
                                    .iterator(); li.hasNext();) {
            S section = li.next();

            if (section == this) {
                if (li.hasNext()) {
                    return li.next();
                } else {
                    return null;
                }
            }
        }

        logger.severe("inNextSibling inconsistent graph");

        return null;
    }

    //-------------------//
    // inPreviousSibling //
    //-------------------//
    /**
     * Return the previous sibling section, both linked by source of first
     * incoming edge
     *
     * @return the previous sibling or null
     */
    public S inPreviousSibling ()
    {
        if (getInDegree() == 0) {
            return null;
        }

        // Proper source section
        S source = getSources()
                       .get(0);

        // Browse till we get to this as target
        for (ListIterator<S> li = source.getTargets()
                                        .listIterator(source.getOutDegree());
             li.hasPrevious();) {
            S section = li.previous();

            if (section == this) {
                if (li.hasPrevious()) {
                    return li.previous();
                } else {
                    return null;
                }
            }
        }

        logger.severe("inPreviousSibling inconsistent graph");

        return null;
    }

    //-------//
    // merge //
    //-------//
    /**
     * Merge this section with the other provided section, which is not
     * affected, and must generally be destroyed.
     *
     * It is assumed (and not checked) that the two sections are
     * contiguous.
     *
     * @param other
     */
    public void merge (S other)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Merging " + this + " with " + other);
        }

        runs.addAll(other.getRuns());
        computeParameters();

        if (logger.isFineEnabled()) {
            logger.fine("Merged " + this);
        }
    }

    //----------------//
    // outNextSibling //
    //----------------//
    /**
     * Return the next sibling section, both linked by target of the last
     * outgoing edge
     *
     * @return the next sibling or null
     */
    public S outNextSibling ()
    {
        if (getOutDegree() == 0) {
            return null;
        }

        // Proper target section
        S target = getTargets()
                       .get(getOutDegree() - 1);

        // Browse till we get to this as source
        for (Iterator<S> li = target.getSources()
                                    .iterator(); li.hasNext();) {
            S section = li.next();

            if (section == this) {
                if (li.hasNext()) {
                    return li.next();
                } else {
                    return null;
                }
            }
        }

        logger.severe("outNextSibling inconsistent graph");

        return null;
    }

    //--------------------//
    // outPreviousSibling //
    //--------------------//
    /**
     * Return the previous sibling section, both linked by target of the first
     * outgoing edge
     *
     * @return the previous sibling or null
     */
    public S outPreviousSibling ()
    {
        if (getOutDegree() == 0) {
            return null;
        }

        // Proper target section
        S target = getTargets()
                       .get(getOutDegree() - 1);

        // Browse till we get to this as source
        for (ListIterator<S> li = target.getSources()
                                        .listIterator(target.getInDegree());
             li.hasPrevious();) {
            S section = li.previous();

            if (section == this) {
                if (li.hasPrevious()) {
                    return li.previous();
                } else {
                    return null;
                }
            }
        }

        logger.severe("outPreviousSibling inconsistent graph");

        return null;
    }

    //---------//
    // prepend //
    //---------//
    /**
     * Add a run at the beginning rather than at the end of the section
     *
     * @param run the new first run
     */
    public void prepend (Run run)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Prepending " + run + " to " + this);
        }

        firstPos--;
        runs.add(0, run);
        addRun(run);

        if (logger.isFineEnabled()) {
            logger.fine("Prepended " + this);
        }
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation vector to this section
     * @param vector the translation vector
     */
    public void translate (PixelPoint vector)
    {
        // Get the coord/pos equivalent of dx/dy vector
        Point cp = graph.oriented(vector);
        int   dc = cp.x;
        int   dp = cp.y;

        // Apply the needed modifications
        firstPos += dp;

        for (Run run : runs) {
            run.translate(dc);
        }

        // Force update
        invalidateCache();
    }

    //-------//
    // write //
    //-------//
    /**
     * Write the pixels of the section in the given picture
     *
     * @param picture the picture to update
     * @param pixel   the color to be used for the pixels
     */
    public void write (Picture picture,
                       int     pixel)
    {
        Point pt = new Point();
        pt.y = getFirstPos();

        for (Run run : runs) {
            for (pt.x = run.getStart(); pt.x <= run.getStop(); pt.x++) {
                setPixel(picture, pt, pixel);
            }

            pt.y++;
        }
    }

    //----------//
    // setPixel //
    //----------//
    /**
     * This method allows to write a specific pixel at given
     * coordinates in the given picture.
     *
     * @param picture the picture to be updated
     * @param cp      the (coord,pos) coordinates of the specified point
     * @param val     the color value for the pixel
     */
    protected void setPixel (Picture picture,
                             Point   cp,
                             int     val)
    {
        picture.setPixel(graph.absolute(cp), val);
    }

    //----------------//
    // computeContour //
    //----------------//
    /**
     * Compute the arrays of points needed to draw the section runs. This is
     * an absolute definition.
     */
    protected Polygon computeContour ()
    {
        final int pointNb = 4 * getRunNb();
        Polygon   polygon = new Polygon(
            new int[pointNb],
            new int[pointNb],
            pointNb);

        // Here, we assume that a section with no graph is vertical
        // This trick is needed when JAXB unmarshalls the section since it needs
        // the contour in order to determine the section position in the set container
        if ((graph == null) || graph.isVertical()) {
            computeContour(polygon.ypoints, polygon.xpoints);
        } else {
            computeContour(polygon.xpoints, polygon.ypoints);
        }

        return polygon;
    }

    //------------------//
    // computeSignature //
    //------------------//
    /**
     * Compute the signature of this section
     * @return the section signature, based on weight and bounds
     */
    protected SectionSignature computeSignature ()
    {
        return new SectionSignature(getWeight(), getOrientedBounds());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        sb.append(" fPos=")
          .append(firstPos)
          .append(" ");
        sb.append(getFirstRun());

        if (getRunNb() > 1) {
            sb.append("-")
              .append(getRunNb())
              .append("-")
              .append(getLastRun());
        }

        sb.append(" Wt=")
          .append(weight);
        sb.append(" lv=")
          .append(getLevel());
        sb.append(" fW=")
          .append(foreWeight);

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    protected void invalidateCache ()
    {
        orientedBounds = null;
        centroid = null;
        contour = null;
        contourBox = null;
        startPoint = null;
        stopPoint = null;
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    /**
     * Report the oriented mass center of the section runs intersected by the
     * provided oriented rectangle
     * @param roi the oriented rectangle of interest (roi)
     * @return the oriented centroid of the intersected points, or null
     * @throws IllegalArgumentException if provided roi is null
     */
    private Point getRectangleCentroid (Rectangle roi)
    {
        if (roi == null) {
            throw new IllegalArgumentException("Rectangle of Interest is null");
        }

        Barycenter barycenter = new Barycenter();
        cumulate(barycenter, roi);

        if (barycenter.getWeight() != 0) {
            return new Point(
                (int) Math.rint(barycenter.getX()),
                (int) Math.rint(barycenter.getY()));
        } else {
            return null;
        }
    }

    //--------//
    // addRun //
    //--------//
    /**
     * Compute incrementally the cached parameters
     */
    private void addRun (Run run)
    {
        // Invalidate cached data
        invalidateCache();

        // Compute contribution of this run
        computeRunContribution(run);
    }

    //----------------//
    // computeContour //
    //----------------//
    /**
     * Compute the arrays of points needed to draw the section runs
     *
     * @param xpoints to receive abscissae
     * @param ypoints to receive coordinates
     */
    private void computeContour (int[] xpoints,
                                 int[] ypoints)
    {
        // Precise delimitating points
        int i = 0;
        int runNb = getRunNb();
        int y = getFirstPos();

        for (Run run : runs) {
            // Order of the 4 angle points for a run is
            // Vertical lag:    Horizontal lag:
            //     1 2              1 4
            //     4 3              2 3
            xpoints[i] = run.getStart();
            xpoints[i + 1] = run.getStart();
            xpoints[(4 * runNb) - i - 2] = run.getStop() + 1;
            xpoints[(4 * runNb) - i - 1] = run.getStop() + 1;

            ypoints[i] = y;
            ypoints[i + 1] = y + 1;
            ypoints[(4 * runNb) - i - 2] = y + 1;
            ypoints[(4 * runNb) - i - 1] = y;

            i += 2;
            y += 1;
        }
    }

    //------------------------//
    // computeRunContribution //
    //------------------------//
    private void computeRunContribution (Run run)
    {
        final int length = run.getLength();
        weight += length;
        foreWeight += (length * run.getLevel());
        maxRunLength = Math.max(maxRunLength, length);
    }
}
