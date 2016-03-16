//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B a s i c G l y p h                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.image.Table;

import omr.math.BasicLine;
import omr.math.LineUtil;
import omr.math.PointsCollector;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import omr.run.Orientation;
import static omr.run.Orientation.HORIZONTAL;
import omr.run.Run;
import omr.run.RunTable;

import omr.util.Navigable;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicGlyph} is the basis for Glyph implementation.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "glyph")
public class BasicGlyph
        extends AbstractSymbol
        implements Glyph
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            BasicGlyph.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Absolute abscissa of the glyph top left corner. */
    @XmlAttribute(name = "left")
    protected final int left;

    /** Absolute ordinate of the glyph top left corner. */
    @XmlAttribute(name = "top")
    protected final int top;

    /** Runs of pixels that compose the glyph. Gives all runs, thus width, height, etc... */
    @XmlElement(name = "run-table")
    protected final RunTable runTable;

    // Transient data
    //---------------
    //
    /** The containing glyph index, if any. */
    @Navigable(false)
    protected GlyphIndex index;

    /** Position with respect to nearest staff. */
    protected Double pitchPosition;

    /** Computed ART Moments. */
    protected ARTMoments artMoments;

    /** Computed geometric Moments. */
    protected GeometricMoments geoMoments;

    /** Mass center coordinates. */
    protected Point centroid;

    /** Box center coordinates. */
    protected Point center;

    /** Best straight line equation. */
    protected Line2D line;

    /** Line elements. */
    protected BasicLine basicLine;

    /** Absolute slope of the line WRT abscissa axis. */
    protected Double slope;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BasicGlyph} object.
     *
     * @param left     abscissa of top left corner
     * @param top      ordinate of top left corner
     * @param runTable table of runs (cannot be null)
     */
    public BasicGlyph (int left,
                       int top,
                       RunTable runTable)
    {
        this.left = left;
        this.top = top;

        ///Objects.requireNonNull(runTable, "BasicGlyph created with null runTable");
        // NOTA: We must accept null RunTable for the Sample no-arg constructor
        this.runTable = runTable;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BasicGlyph ()
    {
        this.left = 0;
        this.top = 0;
        this.runTable = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        if (getBounds().contains(point)) {
            final Point relPoint = new Point(point.x - left, point.y - top);

            return runTable.contains(relPoint);
        }

        return false;
    }

    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        // Admin
        sb.append(
                String.format(
                        "%s#%s @%s%n",
                        getClass().getSimpleName(),
                        id,
                        Integer.toHexString(hashCode())));

        if (isVip()) {
            sb.append(String.format("   vip%n", getId()));
        }

        sb.append(String.format("   bounds=%s%n", getBounds()));
        sb.append(String.format("   groups=%s%n", groups));

        ///sb.append(String.format("   nest=%s%n", getIndex()));
        // Display
        if (attachments != null) {
            sb.append(String.format("   attachments=%s%n", attachments));
        }

        // Environment
        if (pitchPosition != null) {
            sb.append(String.format("   pitchPosition=%s%n", pitchPosition));
        }

        return sb.toString();
    }

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

        final BasicGlyph other = (BasicGlyph) obj;

        if (this.left != other.left) {
            return false;
        }

        if (this.top != other.top) {
            return false;
        }

        if (!Objects.equals(this.runTable, other.runTable)) {
            return false;
        }

        return true;
    }

    @Override
    public void fillTable (Table.UnsignedByte table,
                           Point tableOrigin,
                           boolean fat)
    {
        runTable.fillTable(table, tableOrigin, getTopLeft(), fat);
    }

    @Override
    public ARTMoments getARTMoments ()
    {
        if (artMoments == null) {
            artMoments = runTable.computeArtMoments(left, top);
        }

        return artMoments;
    }

    @Override
    public Rectangle getBounds ()
    {
        return new Rectangle(left, top, runTable.getWidth(), runTable.getHeight());
    }

    @Override
    public ByteProcessor getBuffer ()
    {
        return runTable.getBuffer();
    }

    @Override
    public Point getCenter ()
    {
        if (center == null) {
            center = new Point(left + (runTable.getWidth() / 2), top + (runTable.getHeight() / 2));
        }

        return center;
    }

    @Override
    public Point getCentroid ()
    {
        if (centroid == null) {
            centroid = runTable.computeCentroid(left, top);
        }

        return centroid;
    }

    @Override
    public GeometricMoments getGeometricMoments (int interline)
    {
        if (geoMoments == null) {
            geoMoments = runTable.computeGeometricMoments(left, top, interline);
        }

        return geoMoments;
    }

    @Override
    public int getHeight ()
    {
        return runTable.getHeight();
    }

    @Override
    public GlyphIndex getIndex ()
    {
        return index;
    }

    @Override
    public double getInvertedSlope ()
    {
        checkLine();

        return LineUtil.getInvertedSlope(line);
    }

    @Override
    public int getLeft ()
    {
        return left;
    }

    @Override
    public int getLength (Orientation orientation)
    {
        if (orientation == HORIZONTAL) {
            return runTable.getWidth();
        } else {
            return runTable.getHeight();
        }
    }

    @Override
    public Line2D getLine ()
    {
        checkLine();

        return line;
    }

    @Override
    public double getMeanDistance ()
    {
        checkLine();

        return basicLine.getMeanDistance();
    }

    @Override
    public Double getPitchPosition ()
    {
        return pitchPosition;
    }

    @Override
    public RunTable getRunTable ()
    {
        return runTable;
    }

    @Override
    public double getSlope ()
    {
        if (slope == null) {
            checkLine();

            slope = LineUtil.getSlope(line);
        }

        return slope;
    }

    @Override
    public Point2D getStartPoint (Orientation orientation)
    {
        checkLine();

        if (orientation == HORIZONTAL) {
            // Use left side
            if (line.getX1() <= line.getX2()) {
                return line.getP1();
            } else {
                return line.getP2();
            }
        } else if (line.getY1() <= line.getY2()) {
            return line.getP1();
        } else {
            return line.getP2();
        }
    }

    @Override
    public Point2D getStopPoint (Orientation orientation)
    {
        checkLine();

        if (orientation == HORIZONTAL) {
            // Use right side
            if (line.getX2() >= line.getX1()) {
                return line.getP2();
            } else {
                return line.getP1();
            }
        } else if (line.getY2() >= line.getY1()) {
            return line.getP2();
        } else {
            return line.getP1();
        }
    }

    @Override
    public int getTop ()
    {
        return top;
    }

    @Override
    public Point getTopLeft ()
    {
        return new Point(left, top);
    }

    @Override
    public int getWeight ()
    {
        return runTable.getWeight();
    }

    @Override
    public int getWidth ()
    {
        return runTable.getWidth();
    }

    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (79 * hash) + this.left;
        hash = (79 * hash) + this.top;
        hash = (79 * hash) + Objects.hashCode(this.runTable);

        return hash;
    }

    @Override
    public String idString ()
    {
        return "glyph#" + id;
    }

    @Override
    public boolean intersects (Table.UnsignedByte table,
                               Point tableOrigin)
    {
        return runTable.intersects(table, tableOrigin, getTopLeft());
    }

    @Override
    public boolean intersects (Shape shape)
    {
        // First make a rough test
        Rectangle bounds = getBounds();

        if (shape.intersects(bounds)) {
            Rectangle clip = bounds.intersection(shape.getBounds());

            if (runTable.getOrientation() == HORIZONTAL) {
                final int minSeq = clip.y - top;
                final int maxSeq = (clip.y - top + clip.height) - 1;

                for (int iSeq = minSeq; iSeq <= maxSeq; iSeq++) {
                    for (Iterator<Run> it = runTable.iterator(iSeq); it.hasNext();) {
                        final Run run = it.next();

                        if (shape.intersects(left + run.getStart(), top + iSeq, run.getLength(), 1)) {
                            return true;
                        }
                    }
                }
            } else {
                final int minSeq = clip.x - left;
                final int maxSeq = (clip.x - left + clip.width) - 1;

                for (int iSeq = minSeq; iSeq <= maxSeq; iSeq++) {
                    for (Iterator<Run> it = runTable.iterator(iSeq); it.hasNext();) {
                        Run run = it.next();

                        if (shape.intersects(left + iSeq, top + run.getStart(), 1, run.getLength())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isIdentical (Glyph that)
    {
        if (this.getTop() != that.getTop()) {
            return false;
        }

        if (this.getLeft() != that.getLeft()) {
            return false;
        }

        if (this.getWeight() != that.getWeight()) {
            return false;
        }

        //TODO: we should accept different runTable orientations?
        return this.runTable.equals(((BasicGlyph) that).runTable);
    }

    @Override
    public boolean isTransient ()
    {
        return index == null;
    }

    @Override
    public boolean isVirtual ()
    {
        return false;
    }

    @Override
    public void renderLine (Graphics2D g)
    {
        Rectangle clip = g.getClipBounds();

        if ((clip == null) || clip.intersects(getBounds())) {
            checkLine(); // To make sure the line has been computed

            if (line != null) {
                ///g.draw(line);
                g.draw(
                        new Line2D.Double(
                                line.getX1(),
                                line.getY1() + 0.5,
                                line.getX2() + 1,
                                line.getY2() + 0.5));
            }
        }
    }

    @Override
    public void setIndex (GlyphIndex index)
    {
        this.index = index;
    }

    @Override
    public void setPitchPosition (double pitchPosition)
    {
        this.pitchPosition = pitchPosition;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getClass().getSimpleName()).append("{").append("#").append(getId());

        sb.append(internals());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    /**
     * Return the internals of this class, typically for inclusion in a toString.
     *
     * @return the string of internals
     */
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if ((groups != null) && !groups.isEmpty()) {
            sb.append(' ').append(groups);
        }

        return sb.toString();
    }

    //-----------//
    // checkLine //
    //-----------//
    /**
     * Make sure the approximating line is available
     */
    private void checkLine ()
    {
        if (line == null) {
            computeLine();
        }
    }

    //-------------//
    // computeLine //
    //-------------//
    private void computeLine ()
    {
        basicLine = new BasicLine();

        final boolean isHori = runTable.getOrientation() == HORIZONTAL;

        for (int iSeq = 0, iBreak = runTable.getSize(); iSeq < iBreak; iSeq++) {
            for (Iterator<Run> it = runTable.iterator(iSeq); it.hasNext();) {
                Run run = it.next();
                int start = run.getStart();

                for (int ic = run.getLength() - 1; ic >= 0; ic--) {
                    if (isHori) {
                        basicLine.includePoint(left + start + ic, top + iSeq);
                    } else {
                        basicLine.includePoint(left + iSeq, top + start + ic);
                    }
                }
            }
        }

        // We have a problem if glyph is just 0 or 1 pixel: no computable slope!
        if (basicLine.getNumberOfPoints() == 0) {
            throw new IllegalStateException("Glyph has no pixel, cannot compute line.");
        } else if (basicLine.getNumberOfPoints() == 1) {
            slope = 0d; // we just need a value.
        } else {
            slope = basicLine.getSlope();
        }

        line = basicLine.toDouble();
    }

    //--------------------//
    // getPointsCollector //
    //--------------------//
    /**
     * Cumulate <b>absolute</b> points from all runs.
     *
     * @return a populated point collector
     */
    private PointsCollector getPointsCollector ()
    {
        final PointsCollector collector = new PointsCollector(null, getWeight());
        runTable.cumulate(collector, new Point(left, top));

        return collector;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of Glyph interface.
     */
    public static class Adapter
            extends XmlAdapter<BasicGlyph, Glyph>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicGlyph marshal (Glyph glyph)
                throws Exception
        {
            return (BasicGlyph) glyph;
        }

        @Override
        public Glyph unmarshal (BasicGlyph basicGlyph)
                throws Exception
        {
            return basicGlyph;
        }
    }
}
