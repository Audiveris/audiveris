//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           G l y p h                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.glyph;

import ij.process.ByteProcessor;

import org.audiveris.omr.math.BasicLine;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointsCollector;
import org.audiveris.omr.moments.ARTMoments;
import org.audiveris.omr.moments.GeometricMoments;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Table;

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

/**
 * Class {@code Glyph} is a symbol made of a fixed set of pixels.
 * <p>
 * A glyph is un-mutable, meaning one cannot add or remove pixels to/from an existing instance,
 * although one can always create another glyph instance with the proper collection of pixels.
 * Instead of Glyph, see {@link org.audiveris.omr.glyph.dynamic.SectionCompound} class to deal with
 * growing
 * compounds of sections.
 * <p>
 * A glyph is implemented and persisted as a run table located at a given absolute origin.
 * <p>
 * A glyph has no intrinsic orientation, hence some methods such as {@link #getLength} require that
 * view orientation be provided as a parameter.
 * <p>
 * A glyph has no shape, see {@link org.audiveris.omr.sig.inter.Inter} class for glyph
 * interpretation and {@link org.audiveris.omr.classifier.Sample} class for training of shape
 * classifiers.
 * <p>
 * Additional features are made available via the {@link Glyphs} class.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "glyph")
public class Glyph
        extends AbstractWeightedEntity
        implements NearLine
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Glyph.class);

    //~ Instance fields ----------------------------------------------------------------------------
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
    public Glyph (int left,
                  int top,
                  RunTable runTable)
    {
        this.left = left;
        this.top = top;

        ///Objects.requireNonNull(runTable, "Glyph created with null runTable");
        // NOTA: We must accept null RunTable for the Sample no-arg constructor
        this.runTable = runTable;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private Glyph ()
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
            sb.append(String.format("   vip%n"));
        }

        sb.append(String.format("   bounds=%s%n", getBounds()));
        sb.append(String.format("   groups=%s%n", groups));

        ///sb.append(String.format("   nest=%s%n", getIndex()));
        // Display
        if (attachments != null) {
            sb.append(String.format("   attachments=%s%n", attachments));
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

        final Glyph other = (Glyph) obj;

        if (this.left != other.left) {
            return false;
        }

        if (this.top != other.top) {
            return false;
        }

        return Objects.equals(this.runTable, other.runTable);
    }

    /**
     * Fill the provided table with glyph foreground pixels.
     *
     * @param table       (output) the table to fill
     * @param tableOrigin absolute origin of table
     * @param fat         true to add touching locations
     */
    public void fillTable (Table.UnsignedByte table,
                           Point tableOrigin,
                           boolean fat)
    {
        runTable.fillTable(table, tableOrigin, getTopLeft(), fat);
    }

    /**
     * Report the glyph ART moments.
     *
     * @return the glyph ART moments
     */
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

    /**
     * Report a buffer of the glyph (which can be handed to the OCR)
     *
     * @return a black &amp; white buffer (contour box size)
     */
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
    public Point2D getCenter2D ()
    {
        return new Point2D.Double(left + (runTable.getWidth() / 2.0),
                                  top + (runTable.getHeight() / 2.0));
    }

    @Override
    public Point getCentroid ()
    {
        if (centroid == null) {
            centroid = runTable.computeCentroid(left, top);
        }

        return centroid;
    }

    public Point2D getCentroidDouble ()
    {
        return runTable.computeCentroidDouble(left, top);
    }

    /**
     * Report the glyph geometric moments.
     *
     * @param interline the global sheet interline
     * @return the glyph geometric moments
     */
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

    /**
     * Report the containing glyph index
     *
     * @return the containing index
     */
    public GlyphIndex getIndex ()
    {
        return index;
    }

    /**
     * The setter for glyph index.
     *
     * @param index the containing glyph index
     */
    public void setIndex (GlyphIndex index)
    {
        this.index = index;
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

        return new Line2D.Double(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    @Override
    public Line2D getCenterLine ()
    {
        checkLine();

        return basicLine.toCenterLine();
    }

    @Override
    public double getMeanDistance ()
    {
        checkLine();

        return basicLine.getMeanDistance();
    }

    /**
     * Report the underlying table of runs
     *
     * @return the glyph runTable
     */
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

    /**
     * Report a short glyph reference
     *
     * @return glyph reference
     */
    public String idString ()
    {
        return "glyph#" + id;
    }

    /**
     * Report whether the glyph has a pixel in common with the provided table.
     *
     * @param table       the table of pixels
     * @param tableOrigin top-left corner of table
     * @return true if connection found
     */
    public boolean intersects (Table.UnsignedByte table,
                               Point tableOrigin)
    {
        return runTable.intersects(table, tableOrigin, getTopLeft());
    }

    /**
     * Report whether the glyph intersects the provided AWT shape.
     *
     * @param shape the provided AWT shape
     * @return true if intersection is not empty, false otherwise
     */
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

    /**
     * Report whether this glyph is identical to that glyph
     *
     * @param that the other glyph
     * @return true if their pixels are identical
     */
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
        return this.runTable.equals(that.runTable);
    }

    /**
     * Report whether this glyph is a vertical seed.
     *
     * @return true if flagged as VERTICAL_SEED group
     */
    public boolean isVerticalSeed ()
    {
        return getGroups().contains(GlyphGroup.VERTICAL_SEED);
    }

    @Override
    public void renderLine (Graphics2D g)
    {
        Rectangle clip = g.getClipBounds();

        if ((clip == null) || clip.intersects(getBounds())) {
            checkLine(); // To make sure the line has been computed

            if (line != null) {
                g.draw(line);
            }
        }
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
        switch (basicLine.getNumberOfPoints()) {
        case 0:
            throw new IllegalStateException("Glyph has no pixel, cannot compute line.");

        case 1:
            slope = 0d; // we just need a value.

            break;

        default:
            slope = basicLine.getSlope();

            break;
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
}
