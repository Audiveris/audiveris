//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s i c S e c t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.graph.BasicVertex;

import omr.math.Barycenter;
import omr.math.BasicLine;
import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;
import omr.run.Run;

import omr.sheet.SystemInfo;

import omr.stick.SectionRole;
import omr.stick.StickRelation;

import omr.ui.Colors;

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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import omr.ui.util.UIUtil;

/**
 * Class {@code BasicSection} is a basic implementation of {@link Section}.
 *
 * <p>TODO: Get rid of StickRelation part ASAP?
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "section")
public class BasicSection
        extends BasicVertex<Lag, Section>
        implements Section
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BasicSection.class);

    //~ Instance fields --------------------------------------------------------
    /** Position of first run */
    @XmlAttribute(name = "first-pos")
    private int firstPos;

    /** Section orientation */
    @XmlAttribute(name = "orientation")
    private Orientation orientation;

    /** The collection of runs that make up the section */
    @XmlElement(name = "run")
    private final List<Run> runs = new ArrayList<>();

    /** Oriented bounding rectangle */
    protected Rectangle orientedBounds;

    /** Absolute mass center */
    private Point centroid;

    /** Contribution to the foreground */
    private int foreWeight;

    /** Length of longest run */
    private int maxRunLength;

    /** Number of pixels, whatever the gray level */
    private int weight;

    /** Absolute contour points */
    private Polygon polygon;

    /** Absolute contour box */
    private Rectangle bounds;

    /** Adjacent sections from the other orientation */
    private Set<Section> oppositeSections;

    /**
     * Glyph this section belongs to.
     * This reference is kept in sync with the containing GlyphLag activeMap.
     * Don't directly assign a value to 'glyph', use the setGlyph() method
     * instead.
     */
    private Glyph glyph;

    /** To flag sections too thick for staff line (null = don't know) */
    private Boolean fat = null;

    /** Flag to remember processing has been done */
    private boolean processed = false;

    /** (Debug) flag this section as VIP */
    private boolean vip;

    /** Relation between section and stick */
    protected StickRelation relation;

    /** Approximating oriented line for this section */
    protected Line orientedLine;

    /** The containing system, if any */
    private SystemInfo system;

    /**
     * Default color. This is the permanent default which is used when
     * the color is reset by {@link #resetColor}
     */
    protected Color defaultColor;

    /**
     * Color currently used.
     * By default, the color is the defaultColor chosen out of the palette.
     * But, temporarily, a section can be assigned a different color,
     * for example to highlight the section.
     */
    protected Color color;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // BasicSection //
    //--------------//
    /**
     * Creates a new BasicSection.
     */
    public BasicSection ()
    {
    }

    //~ Methods ----------------------------------------------------------------
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

    //----------------//
    // drawingOfTable //
    //----------------//
    /**
     * Printout the filled drawing table
     *
     * @param table the filled table
     * @param box   the table limits in the image
     */
    public static String drawingOfTable (char[][] table,
                                         Rectangle box)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n"));

        sb.append(String.format(
                "xMin=%d, xMax=%d%n", box.x, box.x + box.width - 1));
        sb.append(String.format(
                "yMin=%d, yMax=%d%n", box.y, box.y + box.height - 1));

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

    //--------------------//
    // addOppositeSection //
    //--------------------//
    @Override
    public void addOppositeSection (Section otherSection)
    {
        if (oppositeSections == null) {
            oppositeSections = new HashSet<>();
        }

        oppositeSections.add(otherSection);
    }

    //--------//
    // append //
    //--------//
    @Override
    public void append (Run run)
    {
        runs.add(run);
        addRun(run);

        logger.debug("Appended {} to {}", run, this);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement Comparable, sorting sections first by absolute
     * abscissa, then by absolute ordinate.
     *
     * @param other the other section to compare to
     * @return the result of ordering
     */
    @Override
    public int compareTo (Section other)
    {
        if (this == other) {
            return 0;
        }

        final Point ref = this.getBounds().getLocation();
        final Point otherRef = other.getBounds().getLocation();

        // Are x values different?
        final int dx = ref.x - otherRef.x;

        if (dx != 0) {
            return dx;
        }

        // Vertically aligned, so use ordinates
        final int dy = ref.y - otherRef.y;

        if (dy != 0) {
            return dy;
        }

        // Finally, use id. Note this should return zero since different
        // sections cannot overlap
        return this.getId() - other.getId();
    }

    //-------------------//
    // computeParameters //
    //-------------------//
    @Override
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

        logger.debug("Parameters of {} maxRunLength={} meanRunLength={}"
                     + " weight={} foreWeight={}",
                this, getMaxRunLength(), getMeanRunLength(),
                weight, foreWeight);
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
            int posMax = Math.min(firstPos + runs.size(), oRoi.y + oRoi.height)
                         - 1;
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
            final int pMax = -1
                             + Math.min(
                    firstPos + runs.size(),
                    oRoi.y + oRoi.height);
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

    //-----------//
    // fillImage //
    //-----------//
    @Override
    public void fillImage (BufferedImage im,
                           Rectangle box)
    {
        final WritableRaster raster = im.getRaster();

        if (isVertical()) {
            int x = getFirstPos() - box.x;

            for (Run run : runs) {
                for (int y = run.getStart(); y <= run.getStop(); y++) {
                    raster.setSample(x, y - box.y, 0, 255);
                }

                x += 1;
            }
        } else {
            int y = getFirstPos() - box.y;

            for (Run run : runs) {
                for (int x = run.getStart(); x <= run.getStop(); x++) {
                    raster.setSample(x - box.x, y, 0, 255);
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

        return new Point(
                box.x + (box.width / 2),
                box.y + (box.height / 2));
    }

    //-----------//
    // getAspect //
    //-----------//
    @Override
    public double getAspect (Orientation orientation)
    {
        return (double) getLength(orientation) / (double) getThickness(
                orientation);
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = new Rectangle(getPolygon().getBounds());
        }

        return new Rectangle(bounds); // Copy!
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

    //-----------------//
    // getDefaultColor //
    //-----------------//
    @Override
    public Color getDefaultColor ()
    {
        return defaultColor;
    }

    //-------------------//
    // getFirstAdjacency //
    //-------------------//
    @Override
    public double getFirstAdjacency ()
    {
        Run run = getFirstRun();
        int runStart = run.getStart();
        int runStop = run.getStop();
        int adjacency = 0;

        for (Section source : getSources()) {
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

    //---------------//
    // getForeWeight //
    //---------------//
    @Override
    public int getForeWeight ()
    {
        return foreWeight;
    }

    //----------//
    // getGlyph //
    //----------//
    @Override
    public Glyph getGlyph ()
    {
        return glyph;
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
    public Lag getGraph ()
    {
        return graph;
    }

    //------------------//
    // getLastAdjacency //
    //------------------//
    @Override
    public double getLastAdjacency ()
    {
        Run run = getLastRun();
        int runStart = run.getStart();
        int runStop = run.getStop();
        int adjacency = 0;

        for (Section target : getTargets()) {
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

    //----------//
    // getLevel //
    //----------//
    @Override
    public int getLevel ()
    {
        return (int) Math.rint((double) foreWeight / (double) weight);
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

    //---------------------//
    // getOppositeSections //
    //---------------------//
    @Override
    public Set<Section> getOppositeSections ()
    {
        if (oppositeSections != null) {
            return Collections.unmodifiableSet(oppositeSections);
        } else {
            return Collections.emptySet();
        }
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
    // getRelation //
    //-------------//
    @Override
    public StickRelation getRelation ()
    {
        return relation;
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

    //-----------//
    // getSystem //
    //-----------//
    @Override
    public SystemInfo getSystem ()
    {
        return system;
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

    //---------------//
    // inNextSibling //
    //---------------//
    @Override
    public Section inNextSibling ()
    {
        // Check we have sources
        if (getInDegree() == 0) {
            return null;
        }

        // Proper source section
        Section source = getSources().get(getInDegree() - 1);

        // Browse till we get to this as target
        for (Iterator<Section> li = source.getTargets().iterator(); li.hasNext();) {
            Section section = li.next();

            if (section == this) {
                if (li.hasNext()) {
                    return li.next();
                } else {
                    return null;
                }
            }
        }

        logger.error("inNextSibling inconsistent graph");

        return null;
    }

    //-------------------//
    // inPreviousSibling //
    //-------------------//
    @Override
    public Section inPreviousSibling ()
    {
        if (getInDegree() == 0) {
            return null;
        }

        // Proper source section
        Section source = getSources().get(0);

        // Browse till we get to this as target
        for (ListIterator<Section> li = source.getTargets().listIterator(
                source.getOutDegree()); li.hasPrevious();) {
            Section section = li.previous();

            if (section == this) {
                if (li.hasPrevious()) {
                    return li.previous();
                } else {
                    return null;
                }
            }
        }

        logger.error("inPreviousSibling inconsistent graph");

        return null;
    }

    //------------//
    // intersects //
    //------------//
    @Override
    public boolean intersects (Rectangle rect)
    {
        return getPolygon().intersects(rect);
    }

    //--------------//
    // isAggregable //
    //--------------//
    @Override
    public boolean isAggregable ()
    {
        if ((relation == null) || !relation.isCandidate()) {
            return false;
        }

        return !isKnown();
    }

    //-------------//
    // isColorized //
    //-------------//
    @Override
    public boolean isColorized ()
    {
        return defaultColor != null;
    }

    //-------//
    // isFat //
    //-------//
    @Override
    public Boolean isFat ()
    {
        return fat;
    }

    //---------------//
    // isGlyphMember //
    //---------------//
    @Override
    public boolean isGlyphMember ()
    {
        return glyph != null;
    }

    //---------//
    // isKnown //
    //---------//
    @Override
    public boolean isKnown ()
    {
        return (glyph != null)
               && (glyph.isSuccessful() || glyph.isWellKnown());
    }

    //-------------//
    // isProcessed //
    //-------------//
    @Override
    public boolean isProcessed ()
    {
        return processed;
    }

    //------------//
    // isVertical //
    //------------//
    @Override
    public boolean isVertical ()
    {
        return orientation == Orientation.VERTICAL;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //-------//
    // merge //
    //-------//
    @Override
    public void merge (Section other)
    {
        logger.debug("Merging {} with {}", this, other);

        runs.addAll(other.getRuns());
        computeParameters();

        logger.debug("Merged {}", this);
    }

    //----------------//
    // outNextSibling //
    //----------------//
    @Override
    public Section outNextSibling ()
    {
        if (getOutDegree() == 0) {
            return null;
        }

        // Proper target section
        Section target = getTargets().get(getOutDegree() - 1);

        // Browse till we get to this as source
        for (Iterator<Section> li = target.getSources().iterator(); li.hasNext();) {
            Section section = li.next();

            if (section == this) {
                if (li.hasNext()) {
                    return li.next();
                } else {
                    return null;
                }
            }
        }

        logger.error("outNextSibling inconsistent graph");

        return null;
    }

    //--------------------//
    // outPreviousSibling //
    //--------------------//
    @Override
    public Section outPreviousSibling ()
    {
        if (getOutDegree() == 0) {
            return null;
        }

        // Proper target section
        Section target = getTargets().get(getOutDegree() - 1);

        // Browse till we get to this as source
        for (ListIterator<Section> li = target.getSources().listIterator(
                target.getInDegree()); li.hasPrevious();) {
            Section section = li.previous();

            if (section == this) {
                if (li.hasPrevious()) {
                    return li.previous();
                } else {
                    return null;
                }
            }
        }

        logger.error("outPreviousSibling inconsistent graph");

        return null;
    }

    //---------//
    // prepend //
    //---------//
    @Override
    public void prepend (Run run)
    {
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
                           boolean drawBorders)
    {
        Rectangle clip = g.getClipBounds();
        Rectangle rect = getBounds();
        Color oldColor = g.getColor();

        if (clip.intersects(rect)) {
            // Default section color
            Color color = isVertical() ? Colors.GRID_VERTICAL
                    : Colors.GRID_HORIZONTAL;

            // Use color defined for section glyph shape, if any
            Glyph glyph = getGlyph();

            if (glyph != null) {
                Shape shape = glyph.getShape();

                if (shape != null) {
                    color = shape.getColor();
                }
            }

            g.setColor(color);

            // Fill polygon with proper color
            Polygon polygon = getPolygon();
            g.fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

            // Draw polygon borders if so desired
            if (drawBorders) {
                g.setColor(Color.black);
                g.drawPolygon(
                        polygon.xpoints,
                        polygon.ypoints,
                        polygon.npoints);
            }

            g.setColor(oldColor);

            return true;
        } else {
            return false;
        }
    }

    //----------------//
    // renderSelected //
    //----------------//
    @Override
    public boolean renderSelected (Graphics g)
    {
        Rectangle clip = g.getClipBounds();
        Rectangle rect = getBounds();

        if (clip.intersects(rect)) {
            Graphics2D g2 = (Graphics2D) g;
            final Stroke oldStroke = UIUtil.setAbsoluteStroke(g2, 1f);
            Polygon polygon = getPolygon();
            g.setColor(Color.white);
            g.fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
            g.setColor(Color.black);
            g.drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
            g2.setStroke(oldStroke);

            return true;
        } else {
            return false;
        }
    }

    //------------//
    // resetColor //
    //------------//
    @Override
    public void resetColor ()
    {
        setColor(defaultColor);
    }

    //----------//
    // resetFat //
    //----------//
    @Override
    public void resetFat ()
    {
        this.fat = null;
    }

    //----------//
    // setColor //
    //----------//
    @Override
    public void setColor (Color color)
    {
        this.color = color;
    }

    //-----------------//
    // setDefaultColor //
    //-----------------//
    @Override
    public void setDefaultColor (Color color)
    {
        defaultColor = color;
    }

    //--------//
    // setFat //
    //--------//
    @Override
    public void setFat (boolean fat)
    {
        this.fat = fat;
    }

    //-------------//
    // setFirstPos //
    //-------------//
    @Override
    public void setFirstPos (int firstPos)
    {
        this.firstPos = firstPos;
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        // Keep the activeMap of the containing Nest in sync!
        Nest nest = null;

        if ((glyph != null) && (glyph.getNest() != null)) {
            nest = glyph.getNest();
        } else if ((this.glyph != null) && (this.glyph.getNest() != null)) {
            nest = this.glyph.getNest();
        }

        this.glyph = glyph;

        if (nest != null) {
            nest.mapSection(this, glyph);
        }

        if (isVip()) {
            logger.info("{} linkedTo {}", this, glyph);

            if (glyph != null) {
                glyph.setVip();
            }
        }
    }

    //----------//
    // setGraph //
    //----------//
    /**
     * (package access from graph)
     */
    @Override
    public void setGraph (Lag lag)
    {
        super.setGraph(lag);

        if (lag != null) {
            orientation = lag.getOrientation();
        }
    }

    //-----------//
    // setParams //
    //-----------//
    /**
     * Assign major parameters (kind, layer and direction), since the enclosing
     * stick may be assigned later.
     *
     * @param role      the role of this section in stick elaboration
     * @param layer     the layer from stick core
     * @param direction the direction when departing from the stick core
     */
    public void setParams (SectionRole role,
                           int layer,
                           int direction)
    {
        if (relation == null) {
            relation = new StickRelation();
        }

        relation.setParams(role, layer, direction);
    }

    //--------------//
    // setProcessed //
    //--------------//
    @Override
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //-----------//
    // setSystem //
    //-----------//
    @Override
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Section");

        if (orientation != null) {
            sb.append(isVertical() ? "V" : "H");
        } else {
            sb.append("?");
        }

        sb.append("#").append(getId());

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
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

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        if (oppositeSections != null) {
            sb.append("/").append(oppositeSections.size());
        }

        //        sb.append(" fPos=")
        //          .append(firstPos)
        //          .append(" ");
        //        sb.append(getFirstRun());
        //
        //        if (getRunCount() > 1) {
        //            sb.append("-")
        //              .append(getRunCount())
        //              .append("-")
        //              .append(getLastRun());
        //        }
        //
        //        sb.append(" Wt=")
        //          .append(weight);

        //        sb.append(" lv=")
        //          .append(getLevel());
        //        sb.append(" fW=")
        //          .append(foreWeight);
        if ((isFat() != null) && isFat()) {
            sb.append(" fat");
        }

        if (relation != null) {
            sb.append(" ").append(relation);
        }

        if (glyph != null) {
            sb.append(" ").append(glyph.idString());

            if (glyph.getShape() != null) {
                sb.append(":").append(glyph.getShape());
            }
        }

        //        if (system != null) {
        //            sb.append(" syst:")
        //              .append(system.getId());
        //        }
        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    protected void invalidateCache ()
    {
        orientedBounds = null;
        centroid = null;
        polygon = null;
        bounds = null;
        orientedLine = null;
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

        // Link back from run to section
        run.setSection(this);

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
        foreWeight += (length * run.getLevel());
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

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of Section interface
     */
    public static class Adapter
            extends XmlAdapter<BasicSection, Section>
    {
        //~ Methods ------------------------------------------------------------

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
