//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a s i c S e c t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.lag;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.GeoUtil;
import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;
import omr.run.Run;

import omr.ui.Colors;
import omr.ui.util.UIUtil;

import omr.util.AbstractEntity;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicSection} is a basic implementation of {@link Section}.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "section")
public class BasicSection
        extends AbstractEntity
        implements Section
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            BasicSection.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Position of first run */
    @XmlAttribute(name = "first-pos")
    private int firstPos;

    /** Section orientation */
    @XmlAttribute(name = "orientation")
    private Orientation orientation;

    /** The collection of runs that make up the section */
    @XmlElement(name = "run")
    private final List<Run> runs = new ArrayList<Run>();

    /** Containing lag, if any. */
    private Lag lag;

    /** Oriented bounding rectangle */
    protected Rectangle orientedBounds;

    /** Absolute mass center */
    private Point centroid;

    /** Length of longest run */
    private int maxRunLength;

    /** Number of foreground pixels. */
    private int weight;

    /** Absolute contour points */
    private Polygon polygon;

    /** Approximating oriented line for this section */
    protected Line orientedLine;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicSection.
     *
     * @param orientation provided orientation for the section
     */
    public BasicSection (Orientation orientation)
    {
        this.orientation = orientation;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // allocateTable //
    //---------------//
    /**
     * For basic print out, allocate a drawing table, to be later filled
     * with section pixels
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
    @Override
    public void append (Run run)
    {
        run = new Run(run);
        runs.add(run);
        addRun(run);

        logger.debug("Appended {} to {}", run, this);
    }

    //-------------------//
    // computeParameters //
    //-------------------//
    @Override
    public void computeParameters ()
    {
        // weight & maxRunLength
        weight = 0;
        maxRunLength = 0;

        // maxRunLength
        for (Run run : runs) {
            computeRunContribution(run);
        }

        // Invalidate cached data
        invalidateCache();

        logger.debug(
                "Parameters of {} maxRunLength={} meanRunLength={}" + " weight={}",
                this,
                getMaxRunLength(),
                getMeanRunLength(),
                weight);
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (int x,
                             int y)
    {
        return getPolygon().contains(x, y);
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        return contains(point.x, point.y);
    }

    //----------//
    // cumulate //
    //----------//
    @Override
    public void cumulate (Barycenter barycenter,
                          Rectangle absRoi)
    {
        if (barycenter == null) {
            throw new IllegalArgumentException("Barycenter is null");
        }

        if (absRoi == null) {
            // Take all run pixels
            int pos = firstPos - 1;

            for (Run run : runs) {
                double coord = run.getStart() + (run.getLength() / 2d);
                pos++;

                if (orientation == Orientation.HORIZONTAL) {
                    barycenter.include(run.getLength(), coord, pos);
                } else {
                    barycenter.include(run.getLength(), pos, coord);
                }
            }
        } else {
            Rectangle oRoi = orientation.oriented(absRoi);

            // Take only the pixels contained by the oriented roi
            int pos = firstPos - 1;
            int posMax = Math.min(firstPos + runs.size(), oRoi.y + oRoi.height) - 1;
            int coordMax = (oRoi.x + oRoi.width) - 1;

            for (Run run : runs) {
                pos++;

                if (pos < oRoi.y) {
                    continue;
                }

                if (pos > posMax) {
                    break;
                }

                final int roiStart = Math.max(run.getStart(), oRoi.x);
                final int roiStop = Math.min(run.getStop(), coordMax);

                for (int coord = roiStart; coord <= roiStop; coord++) {
                    if (orientation == Orientation.HORIZONTAL) {
                        barycenter.include(coord, pos);
                    } else {
                        barycenter.include(pos, coord);
                    }
                }
            }
        }
    }

    //----------//
    // cumulate //
    //----------//
    @Override
    public void cumulate (PointsCollector collector)
    {
        final Rectangle roi = collector.getRoi();

        if (roi == null) {
            int p = firstPos;

            for (Run run : runs) {
                final int start = run.getStart();

                for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                    if (orientation == Orientation.HORIZONTAL) {
                        collector.include(start + ic, p);
                    } else {
                        collector.include(p, start + ic);
                    }
                }

                p++;
            }
        } else {
            // Take only the pixels contained by the absolute roi
            Rectangle oRoi = orientation.oriented(roi);
            final int pMin = oRoi.y;
            final int pMax = -1 + Math.min(firstPos + runs.size(), oRoi.y + oRoi.height);
            final int cMin = oRoi.x;
            final int cMax = (oRoi.x + oRoi.width) - 1;
            int p = firstPos - 1;

            for (Run run : runs) {
                p++;

                if (p < pMin) {
                    continue;
                }

                if (p > pMax) {
                    break;
                }

                final int roiStart = Math.max(run.getStart(), cMin);
                final int roiStop = Math.min(run.getStop(), cMax);
                final int length = roiStop - roiStart + 1;

                if (length > 0) {
                    for (int c = roiStart; c <= roiStop; c++) {
                        if (orientation == Orientation.HORIZONTAL) {
                            collector.include(c, p);
                        } else {
                            collector.include(p, c);
                        }
                    }
                }
            }
        }
    }

    //-----------//
    // drawAscii //
    //-----------//
    @Override
    public void drawAscii ()
    {
        System.out.println("Section#" + getId());

        // Determine the absolute bounds
        Rectangle box = getBounds();

        char[][] table = allocateTable(box);
        fillTable(table, box);
        drawingOfTable(table, box);
    }

    //----------------//
    // drawingOfTable //
    //----------------//
    /**
     * Printout the filled drawing table
     *
     * @param table the filled table
     * @param box   the table limits in the image
     * @return the drawing as a string
     */
    public static String drawingOfTable (char[][] table,
                                         Rectangle box)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n"));

        sb.append(String.format("xMin=%d, xMax=%d%n", box.x, (box.x + box.width) - 1));
        sb.append(String.format("yMin=%d, yMax=%d%n", box.y, (box.y + box.height) - 1));

        for (int iy = 0; iy < table.length; iy++) {
            sb.append(String.format("%d:", iy + box.y));

            char[] line = table[iy];

            for (int ix = 0; ix < line.length; ix++) {
                sb.append(line[ix]);
            }

            sb.append(String.format("%n"));
        }

        return sb.toString();
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof Section) {
            return byFullAbscissa.compare(this, (Section) obj) == 0;
        }

        return false;
    }

    //------------//
    // fillBuffer //
    //------------//
    @Override
    public void fillBuffer (ByteProcessor buffer,
                            Point offset)
    {
        if (isVertical()) {
            int x = getFirstPos() - offset.x;

            for (Run run : runs) {
                for (int y = run.getStart(); y <= run.getStop(); y++) {
                    buffer.set(x, y - offset.y, 0);
                }

                x += 1;
            }
        } else {
            int y = getFirstPos() - offset.y;

            for (Run run : runs) {
                for (int x = run.getStart(); x <= run.getStop(); x++) {
                    buffer.set(x - offset.x, y, 0);
                }

                y += 1;
            }
        }
    }

    //-----------//
    // fillTable //
    //-----------//
    @Override
    public void fillTable (char[][] table,
                           Rectangle box)
    {
        // Determine the bounds
        getPolygon(); // Make sure the polygon is available

        int xPrev = 0;
        int yPrev = 0;
        int x;
        int y;

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

    //-----------------//
    // getAbsoluteLine //
    //-----------------//
    @Override
    public Line getAbsoluteLine ()
    {
        getOrientedLine();

        return orientation.switchRef(orientedLine);
    }

    //---------------//
    // getAreaCenter //
    //---------------//
    @Override
    public Point getAreaCenter ()
    {
        Rectangle box = getBounds();

        return new Point(box.x + (box.width / 2), box.y + (box.height / 2));
    }

    //-----------//
    // getAspect //
    //-----------//
    @Override
    public double getAspect (Orientation orientation)
    {
        return (double) getLength(orientation) / (double) getThickness(orientation);
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        return getPolygon().getBounds(); // This is always a fresh copy of rectangle
    }

    //-------------//
    // getCentroid //
    //-------------//
    @Override
    public Point getCentroid ()
    {
        if (centroid == null) {
            Point orientedPoint = new Point(0, 0);
            int y = firstPos;

            for (Run run : runs) {
                final int length = run.getLength();
                orientedPoint.y += (length * (2 * y));
                orientedPoint.x += (length * ((2 * run.getStart()) + length));
                y++;
            }

            orientedPoint.x /= (2 * getWeight());
            orientedPoint.y /= (2 * getWeight());

            centroid = orientation.absolute(orientedPoint);
            logger.debug("Centroid of {} is {}", this, centroid);
        }

        return centroid;
    }

    //-------------//
    // getFirstPos //
    //-------------//
    @Override
    public int getFirstPos ()
    {
        return firstPos;
    }

    //-------------//
    // getFirstRun //
    //-------------//
    @Override
    public Run getFirstRun ()
    {
        return runs.get(0);
    }

    //--------//
    // getLag //
    //--------//
    @Override
    public Lag getLag ()
    {
        return lag;
    }

    //------------//
    // getLastPos //
    //------------//
    @Override
    public int getLastPos ()
    {
        return (firstPos + getRunCount()) - 1;
    }

    //------------//
    // getLastRun //
    //------------//
    @Override
    public Run getLastRun ()
    {
        return runs.get(runs.size() - 1);
    }

    //-----------//
    // getLength //
    //-----------//
    @Override
    public int getLength (Orientation orientation)
    {
        if (orientation == Orientation.HORIZONTAL) {
            return getBounds().width;
        } else {
            return getBounds().height;
        }
    }

    //-----------------//
    // getMaxRunLength //
    //-----------------//
    @Override
    public int getMaxRunLength ()
    {
        return maxRunLength;
    }

    //---------------//
    // getMeanAspect //
    //---------------//
    @Override
    public double getMeanAspect (Orientation orientation)
    {
        return getLength(orientation) / getMeanThickness(orientation);
    }

    //------------------//
    // getMeanRunLength //
    //------------------//
    @Override
    public int getMeanRunLength ()
    {
        return weight / getRunCount();
    }

    //------------------//
    // getMeanThickness //
    //------------------//
    @Override
    public double getMeanThickness (Orientation orientation)
    {
        return (double) getWeight() / getLength(orientation);
    }

    //----------------//
    // getOrientation //
    //----------------//
    @Override
    public Orientation getOrientation ()
    {
        return orientation;
    }

    //-------------------//
    // getOrientedBounds //
    //-------------------//
    @Override
    public Rectangle getOrientedBounds ()
    {
        if (orientedBounds == null) {
            orientedBounds = new Rectangle(orientation.oriented(getBounds()));
        }

        return orientedBounds;
    }

    //-----------------//
    // getOrientedLine //
    //-----------------//
    @Override
    public Line getOrientedLine ()
    {
        if (orientedLine == null) {
            // Compute the section line
            orientedLine = new BasicLine();

            int y = getFirstPos();

            for (Run run : getRuns()) {
                int stop = run.getStop();

                for (int x = run.getStart(); x <= stop; x++) {
                    orientedLine.includePoint((double) x, (double) y);
                }

                y++;
            }
        }

        return orientedLine;
    }

    //-----------------//
    // getPathIterator //
    //-----------------//
    @Override
    public PathIterator getPathIterator ()
    {
        return getPolygon().getPathIterator(null);
    }

    //------------//
    // getPolygon //
    //------------//
    @Override
    public Polygon getPolygon ()
    {
        if (polygon == null) {
            polygon = computePolygon();
        }

        return polygon;
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    @Override
    public Point getRectangleCentroid (Rectangle absRoi)
    {
        if (absRoi == null) {
            throw new IllegalArgumentException("Rectangle of Interest is null");
        }

        Barycenter barycenter = new Barycenter();
        cumulate(barycenter, absRoi);

        if (barycenter.getWeight() != 0) {
            return new Point(
                    (int) Math.rint(barycenter.getX()),
                    (int) Math.rint(barycenter.getY()));
        } else {
            return null;
        }
    }

    //-------------//
    // getRunCount //
    //-------------//
    @Override
    public int getRunCount ()
    {
        return runs.size();
    }

    //---------//
    // getRuns //
    //---------//
    @Override
    public List<Run> getRuns ()
    {
        return runs;
    }

    //---------------//
    // getStartCoord //
    //---------------//
    @Override
    public int getStartCoord ()
    {
        return getOrientedBounds().x;
    }

    //--------------//
    // getStopCoord //
    //--------------//
    @Override
    public int getStopCoord ()
    {
        Rectangle bounds = getOrientedBounds();

        return bounds.x + (bounds.width - 1);
    }

    //--------------//
    // getThickness //
    //--------------//
    @Override
    public int getThickness (Orientation orientation)
    {
        if (orientation == Orientation.HORIZONTAL) {
            return getBounds().height;
        } else {
            return getBounds().width;
        }
    }

    //-----------//
    // getWeight //
    //-----------//
    @Override
    public int getWeight ()
    {
        if (weight == 0) {
            computeParameters();
        }

        return weight;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (43 * hash) + Objects.hashCode(this.orientation);
        hash = (43 * hash) + this.weight;

        return hash;
    }

    //------------//
    // intersects //
    //------------//
    @Override
    public boolean intersects (Rectangle rect)
    {
        return getPolygon().intersects(rect);
    }

    //------------//
    // intersects //
    //------------//
    @Override
    public boolean intersects (java.awt.Shape shape)
    {
        int pos = getFirstPos();

        for (Run run : getRuns()) {
            final int start = run.getStart();
            final Rectangle runBox = (orientation == Orientation.HORIZONTAL)
                    ? new Rectangle(start, pos, run.getLength(), 1)
                    : new Rectangle(pos, start, 1, run.getLength());

            if (shape.intersects(runBox)) {
                return true;
            }

            pos++;
        }

        return false;
    }

    //------------//
    // intersects //
    //------------//
    @Override
    public boolean intersects (Section that)
    {
        return intersects(that.getPolygon());
    }

    //------------//
    // isVertical //
    //------------//
    @Override
    public boolean isVertical ()
    {
        return orientation == Orientation.VERTICAL;
    }

    //---------//
    // prepend //
    //---------//
    @Override
    public void prepend (Run run)
    {
        run = new Run(run);
        logger.debug("Prepending {} to {}", run, this);

        firstPos--;
        runs.add(0, run);
        addRun(run);

        logger.debug("Prepended {}", this);
    }

    //--------//
    // render //
    //--------//
    @Override
    public boolean render (Graphics g,
                           boolean drawBorders,
                           Color specificColor)
    {
        final Rectangle clip = g.getClipBounds();
        final Rectangle rect = getBounds();

        if ((clip != null) && !clip.intersects(rect)) {
            return false;
        }

        // Which color to be used?
        Color oldColor = g.getColor();

        if (specificColor != null) {
            if (oldColor != specificColor) {
                g.setColor(specificColor);
            }
        } else {
            // Default section color
            Color color = isVertical() ? Colors.GRID_VERTICAL : Colors.GRID_HORIZONTAL;

            if (color != oldColor) {
                g.setColor(color);
            }
        }

        // Fill polygon
        getPolygon();
        g.fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

        // Draw polygon borders if so desired
        if (drawBorders) {
            g.setColor(Color.black);
            g.drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
        }

        if (g.getColor() != oldColor) {
            g.setColor(oldColor);
        }

        return true;
    }

    //----------------//
    // renderSelected //
    //----------------//
    @Override
    public boolean renderSelected (Graphics g)
    {
        Rectangle clip = g.getClipBounds();
        Rectangle rect = getBounds();

        if ((clip == null) || clip.intersects(rect)) {
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g;
                final Stroke oldStroke = UIUtil.setAbsoluteStroke(g2, 1f);
                g.setColor(Color.white);
                g.fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                g.setColor(Color.black);
                g.drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                g2.setStroke(oldStroke);
            }

            return true;
        } else {
            return false;
        }
    }

    //-------------//
    // setFirstPos //
    //-------------//
    @Override
    public void setFirstPos (int firstPos)
    {
        this.firstPos = firstPos;
    }

    //--------//
    // setLag //
    //--------//
    /**
     * (package access from lag?)
     *
     * @param lag the containing lag
     */
    @Override
    public void setLag (Lag lag)
    {
        this.lag = lag;

        if (lag != null) {
            orientation = lag.getOrientation();
        }
    }

    //---------//
    // touches //
    //---------//
    @Override
    public boolean touches (Section that)
    {
        Rectangle thatFatBox = that.getBounds();
        thatFatBox.grow(1, 1);

        // Very rough test
        if (!getPolygon().intersects(thatFatBox)) {
            return false;
        }

        int pos = getFirstPos();

        for (Run run : getRuns()) {
            final int start = run.getStart();
            final Rectangle r1 = (orientation == Orientation.HORIZONTAL)
                    ? new Rectangle(start, pos, run.getLength(), 1)
                    : new Rectangle(pos, start, 1, run.getLength());

            if (thatFatBox.intersects(r1)) {
                // Check contact between this run and one of that runs
                int thatPos = that.getFirstPos();

                for (Run thatRun : that.getRuns()) {
                    final int thatStart = thatRun.getStart();
                    final int thatLength = thatRun.getLength();
                    final Rectangle r2 = (that.getOrientation() == Orientation.HORIZONTAL)
                            ? new Rectangle(thatStart, thatPos, thatLength, 1)
                            : new Rectangle(thatPos, thatStart, 1, thatLength);

                    if (GeoUtil.touch(r1, r2)) {
                        return true;
                    }

                    thatPos++;
                }
            }

            pos++;
        }

        return false;
    }

    //-----------//
    // translate //
    //-----------//
    @Override
    public void translate (Point vector)
    {
        // Get the coord/pos equivalent of dx/dy vector
        Point cp = orientation.oriented(vector);
        int dc = cp.x;
        int dp = cp.y;

        // Apply the needed modifications
        firstPos += dp;

        for (Run run : runs) {
            run.translate(dc);
        }

        // Force update
        invalidateCache();
    }

    //----------------//
    // computePolygon //
    //----------------//
    /**
     * Compute the arrays of points needed to draw the section runs.
     * This is an absolute definition.
     *
     * @return the created polygon that represents the section geometry
     */
    protected Polygon computePolygon ()
    {
        final int maxNb = 1 + (4 * getRunCount()); // Upper value
        final int[] xx = new int[maxNb];
        final int[] yy = new int[maxNb];
        int idx = 0; // Current filling index in xx & yy arrays

        if (isVertical()) {
            idx = populatePolygon(yy, xx, idx, 1);
            idx = populatePolygon(yy, xx, idx, -1);
        } else {
            idx = populatePolygon(xx, yy, idx, 1);
            idx = populatePolygon(xx, yy, idx, -1);
        }

        Polygon poly = new Polygon(xx, yy, idx);

        return poly;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return orientation.isVertical() ? "V" : "H";
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    protected void invalidateCache ()
    {
        orientedBounds = null;
        centroid = null;
        polygon = null;
        orientedLine = null;
    }

    //--------//
    // addRun //
    //--------//
    /**
     * Compute incrementally the cached parameters.
     */
    private void addRun (Run run)
    {
        // Invalidate cached data
        invalidateCache();

        // Compute contribution of this run
        computeRunContribution(run);
    }

    //------------------------//
    // computeRunContribution //
    //------------------------//
    private void computeRunContribution (Run run)
    {
        final int length = run.getLength();
        weight += length;
        maxRunLength = Math.max(maxRunLength, length);
    }

    //-----------------//
    // populatePolygon //
    //-----------------//
    /**
     * Compute the arrays of points needed to draw the section runs
     *
     * @param xpoints to receive abscissae
     * @param ypoints to receive coordinates
     * @param dir     direction for browsing runs
     * @param index   first index available in arrays
     * @return last index value
     */
    private int populatePolygon (int[] xpoints,
                                 int[] ypoints,
                                 int index,
                                 int dir)
    {
        // Precise delimitating points
        int runNb = getRunCount();
        int iStart = (dir > 0) ? 0 : (runNb - 1);
        int iBreak = (dir > 0) ? runNb : (-1);
        int y = (dir > 0) ? getFirstPos() : (getFirstPos() + runNb);
        int xPrev = -1;

        for (int i = iStart; i != iBreak; i += dir) {
            Run run = runs.get(i);

            // +----------------------------+
            // +--+-------------------------+
            //    +----------------------+--+
            //    +----------------------+
            //
            // Order of the 4 angle points for a run is
            // Vertical lag:    Horizontal lag:
            //     1 2              1 4
            //     4 3              2 3
            int x = (dir > 0) ? run.getStart() : (run.getStop() + 1);

            if (x != xPrev) {
                if (xPrev != -1) {
                    // Insert last vertex
                    xpoints[index] = xPrev;
                    ypoints[index] = y;
                    index++;
                }

                // Insert new vertex
                xpoints[index] = x;
                ypoints[index] = y;
                index++;
                xPrev = x;
            }

            y += dir;
        }

        // Complete the sequence, with a new vertex
        xpoints[index] = xPrev;
        ypoints[index] = y;
        index++;

        if (dir < 0) {
            // Finish with starting point
            xpoints[index] = runs.get(0).getStart();
            ypoints[index] = getFirstPos();
            index++;
        }

        return index;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of Section interface.
     */
    public static class Adapter
            extends XmlAdapter<BasicSection, Section>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicSection marshal (Section s)
        {
            return (BasicSection) s;
        }

        @Override
        public Section unmarshal (BasicSection s)
        {
            return s;
        }
    }
}
//
//    //---------------//
//    // inNextSibling //
//    //---------------//
//    @Override
//    public Section inNextSibling ()
//    {
//        // Check we have sources
//        if (getInDegree() == 0) {
//            return null;
//        }
//
//        // Proper source section
//        Section source = getSources().get(getInDegree() - 1);
//
//        // Browse till we get to this as target
//        for (Iterator<Section> li = source.getTargets().iterator(); li.hasNext();) {
//            Section section = li.next();
//
//            if (section == this) {
//                if (li.hasNext()) {
//                    return li.next();
//                } else {
//                    return null;
//                }
//            }
//        }
//
//        logger.error("inNextSibling inconsistent graph");
//
//        return null;
//    }
//
//    //-------------------//
//    // inPreviousSibling //
//    //-------------------//
//    @Override
//    public Section inPreviousSibling ()
//    {
//        if (getInDegree() == 0) {
//            return null;
//        }
//
//        // Proper source section
//        Section source = getSources().get(0);
//
//        // Browse till we get to this as target
//        for (ListIterator<Section> li = source.getTargets().listIterator(source.getOutDegree());
//                li.hasPrevious();) {
//            Section section = li.previous();
//
//            if (section == this) {
//                if (li.hasPrevious()) {
//                    return li.previous();
//                } else {
//                    return null;
//                }
//            }
//        }
//
//        logger.error("inPreviousSibling inconsistent graph");
//
//        return null;
//    }
//
//
//    //----------------//
//    // outNextSibling //
//    //----------------//
//    @Override
//    public Section outNextSibling ()
//    {
//        if (getOutDegree() == 0) {
//            return null;
//        }
//
//        // Proper target section
//        Section target = getTargets().get(getOutDegree() - 1);
//
//        // Browse till we get to this as source
//        for (Iterator<Section> li = target.getSources().iterator(); li.hasNext();) {
//            Section section = li.next();
//
//            if (section == this) {
//                if (li.hasNext()) {
//                    return li.next();
//                } else {
//                    return null;
//                }
//            }
//        }
//
//        logger.error("outNextSibling inconsistent graph");
//
//        return null;
//    }
//
//    //--------------------//
//    // outPreviousSibling //
//    //--------------------//
//    @Override
//    public Section outPreviousSibling ()
//    {
//        if (getOutDegree() == 0) {
//            return null;
//        }
//
//        // Proper target section
//        Section target = getTargets().get(getOutDegree() - 1);
//
//        // Browse till we get to this as source
//        for (ListIterator<Section> li = target.getSources().listIterator(target.getInDegree());
//                li.hasPrevious();) {
//            Section section = li.previous();
//
//            if (section == this) {
//                if (li.hasPrevious()) {
//                    return li.previous();
//                } else {
//                    return null;
//                }
//            }
//        }
//
//        logger.error("outPreviousSibling inconsistent graph");
//
//        return null;
//    }
//
