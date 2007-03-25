//----------------------------------------------------------------------------//
//                                                                            //
//                               S e c t i o n                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.graph.Vertex;

import omr.sheet.Picture;

import omr.util.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Section</code> is an class to handle a section of contiguous and
 * compatible instances of class {@link Run}.
 *
 * <p> A section does not carry orientation information, only the containing
 * {@link Lag} has this information.  Thus all runs of a given lag (and
 * consequently all sections made of these runs) have the same orientation.
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
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Section<L extends Lag, S extends Section<L, S>>
    extends Vertex<L, S>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Section.class);

    //~ Instance fields --------------------------------------------------------

    /** Position of first run */
    @XmlAttribute(name = "first-pos")
    private int firstPos;

    /** The collection of runs that make up the section */
    @XmlElement(name = "run")
    private final List<Run> runs = new ArrayList<Run>();

    /** Bounding rectangle (regardless of orientation) */
    protected Rectangle bounds;

    /** Mass center */
    private Point centroid;

    /** Model(unzoomed) contour points, which depend on orientation */
    private Polygon contour;

    /** Display contour points, which do not depend on orientation */
    private Rectangle contourBox;

    /** Contribution to the foreground */
    private int foreWeight;

    /** Length of longest run */
    private int maxRunLength;

    /** Number of pixels, whatever the grey level */
    private int weight;

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

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the section
     * collection
     *
     * @param sections the collection of sections
     * @return the string built
     */
    public static String toString (Collection<?extends Section> sections)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (Section section : sections) {
            sb.append('#')
              .append(section.getId());
        }

        sb.append("]");

        return sb.toString();
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

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the section bounding rectangle, so please clone it if you want to
     * modify it afterwards
     *
     * @return the section bounding rectangle
     */
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = new Rectangle(graph.switchRef(getContourBox(), null));
        }

        return bounds;
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Return the point which is at the mass center of the section, with all
     * pixels considered of equal weight.
     *
     * @return the mass center of the section, as a Point(coord,pos)
     */
    public Point getCentroid ()
    {
        if (centroid == null) {
            centroid = new Point(0, 0);

            int y = firstPos;

            for (Run run : runs) {
                final int length = run.getLength();
                centroid.y += (length * (2 * y));
                centroid.x += (length * ((2 * run.getStart()) + length));
                y++;
            }

            centroid.x /= (2 * getWeight());
            centroid.y /= (2 * getWeight());

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
     * Return the polygon that defines the display contour. Beware, this entity
     * depends on the lag orientation.
     *
     * @return the perimeter contour
     */
    public Polygon getContour ()
    {
        if (contour == null) {
            int pointNb = 4 * getRunNb();
            contour = new Polygon(new int[pointNb], new int[pointNb], pointNb);
            computeContour();
        }

        return contour;
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return the bounding box of the display polygon. Useful to quickly check
     * if the section needs to be repainted.
     *
     * @return the bounding contour rectangle box
     */
    public Rectangle getContourBox ()
    {
        if (contourBox == null) {
            contourBox = getContour()
                             .getBounds();
        }

        return contourBox;
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

        for (S source : sources) {
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

        for (S target : targets) {
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
        return getBounds().width;
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Return the mean grey level of the section
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
        return getBounds().x;
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
    // getThickness //
    //--------------//
    /**
     * Return the thickness of the section, which is just the number of runs.
     *
     * @return the nb of runs in this section
     */
    public int getThickness ()
    {
        return getBounds().height;
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
        if (!getBounds()
                 .contains(coord, pos)) {
            return false;
        }

        // Then a closer look
        Run run = getRunAt(pos);

        return (run.getStart() <= coord) && (run.getStop() >= coord);
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
    public int cumulatePoints (int[] coord,
                               int[] pos,
                               int   nb)
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
        // Determine the bounds
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
        System.out.println("xMin=" + box.x + ", xMax=" + (box.x + box.width));
        System.out.println("yMin=" + box.y + ", yMax=" + (box.y + box.height));

        for (int iy = 0; iy < table.length; iy++) {
            System.out.print((iy + box.y) + ": ");
            System.out.println(table[iy]);
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
        S source = sources.get(getInDegree() - 1);

        // Browse till we get to this as target
        for (Iterator<S> li = source.targets.iterator(); li.hasNext();) {
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
        S source = sources.get(0);

        // Browse till we get to this as target
        for (ListIterator<S> li = source.targets.listIterator(
            source.getOutDegree()); li.hasPrevious();) {
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

        runs.addAll(other.runs);
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
        S target = targets.get(getOutDegree() - 1);

        // Browse till we get to this as source
        for (Iterator<S> li = target.sources.iterator(); li.hasNext();) {
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
        S target = targets.get(getOutDegree() - 1);

        // Browse till we get to this as source
        for (ListIterator<S> li = target.sources.listIterator(
            target.getInDegree()); li.hasPrevious();) {
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

    //----------//
    // toString //
    //----------//
    /**
     * Return a readable description
     *
     * @return the descriptive string
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);

        sb.append(super.toString());

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

        if (this.getClass()
                .getName()
                .equals(Section.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
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
     * This abstract method allows to write a specific pixel at given
     * coordinates in the given picture. Any concrete subclass must of course
     * implement this method, in a manner consistent with the <B>orientation</B>
     * of the containing lag.
     *
     * @param picture the picture to be updated
     * @param cp      the (coord,pos) coordinates of the specified point
     * @param val     the color value for the pixel
     */
    protected void setPixel (Picture picture,
                             Point   cp,
                             int     val)
    {
        picture.setPixel(graph.switchRef(cp, null), val);
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString() for
     * example.
     *
     * @return the prefix string
     */
    @Override
    protected String getPrefix ()
    {
        return "Section";
    }

    //----------------//
    // computeContour //
    //----------------//
    /**
     * Compute the arrays of points needed to draw the section runs. This is
     * dependent upon the section orientation.
     */
    protected void computeContour ()
    {
        Polygon p = getContour();

        // Here, we assume that a section with no graph is vertical
        // This trick is needed when JAXB unmarshalls the section since it needs
        // the contour in order to determine the section position in the set container
        if ((graph == null) || graph.isVertical()) {
            computeContour(p.ypoints, p.xpoints);
        } else {
            computeContour(p.xpoints, p.ypoints);
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

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        bounds = null;
        centroid = null;
        contour = null;
        contourBox = null;
    }
}
