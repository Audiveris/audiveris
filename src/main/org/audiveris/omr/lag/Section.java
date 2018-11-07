//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S e c t i o n                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.lag;

import ij.process.ByteProcessor;

import org.audiveris.omr.math.Barycenter;
import org.audiveris.omr.math.Line;
import org.audiveris.omr.math.PointsCollector;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Oriented;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.util.Entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Section} describes an <b>immutable</b> section of contiguous and
 * compatible {@link Run} instances.
 * <p>
 * NOTA: Cross-section source/target relationship are not handled here, but in
 * {@link org.audiveris.omr.glyph.dynamic.LinkedSection} class.
 * <p>
 * A section carries orientation information, which is the orientation for all runs in this section.
 * <ol> <li> Positions increase in parallel with run numbers, so the thickness of a section is
 * defined as the delta between last and first positions, in other words its number of runs. </li>
 * <li> Coordinates increase along any section run, so the section start is the minimum of all run
 * starting coordinates, and the section stop is the maximum of all run stopping coordinates.
 * We define section length as the value: stop - start +1 </li> </ol>
 * <p>
 * <b>Beware</b>, the section orientation only governs the runs orientation.
 * It by no means implies that the section dimension is longer in the direction along the runs than
 * in the direction across.
 * To enforce this, the {@link #getLength(Orientation)} requires that an explicit orientation be
 * provided.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicSection.Adapter.class)
public interface Section
        extends Entity, Oriented
{

    /** A section comparator, using section id. */
    public static final Comparator<Section> idComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return Integer.compare(s1.getId(), s2.getId());
        }
    };

    /** For comparing Section instances on their decreasing weight. */
    public static final Comparator<Section> reverseWeightComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return Integer.signum(s2.getWeight() - s1.getWeight());
        }
    };

    /** For comparing Section instances on their start value. */
    public static final Comparator<Section> byCoordinate = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return s1.getStartCoord() - s2.getStartCoord();
        }
    };

    /** For comparing Section instances on their position value. */
    public static final Comparator<Section> byPosition = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return s1.getFirstPos() - s2.getFirstPos();
        }
    };

    /** For comparing Section instances on their position, then coordinate values. */
    public static final Comparator<Section> byFullPosition = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            final int p1 = s1.getFirstPos();
            final int p2 = s2.getFirstPos();

            if (p1 != p2) {
                return p1 - p2;
            }

            final int c1 = s1.getStartCoord();
            final int c2 = s2.getStartCoord();

            return c1 - c2;
        }
    };

    /** For comparing Section instances on their absolute abscissa. */
    public static final Comparator<Section> byAbscissa = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return Integer.compare(s1.getBounds().x, s2.getBounds().x);
        }
    };

    /** For comparing Section instances on their absolute abscissa, ordinate, id. */
    public static final Comparator<Section> byFullAbscissa = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            if (s1 == s2) {
                return 0;
            }

            final Point ref = s1.getBounds().getLocation();
            final Point otherRef = s2.getBounds().getLocation();

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

            // Finally, use id. Note this should return zero if different sections cannot overlap
            return Integer.compare(s1.getId(), s2.getId());
        }
    };

    /**
     * Predicate to check whether the given absolute point is located
     * inside the section.
     *
     * @param x absolute abscissa
     * @param y absolute ordinate
     * @return true if absolute point(x,y) is contained in the section
     */
    public boolean contains (int x,
                             int y);

    /**
     * Cumulate in the provided absolute Barycenter the section pixels
     * that are contained in the provided roi Rectangle.
     * If the roi is null, all pixels are cumulated into the barycenter.
     *
     * @param barycenter the absolute point to populate
     * @param absRoi     the absolute rectangle of interest
     */
    public void cumulate (Barycenter barycenter,
                          Rectangle absRoi);

    /**
     * Cumulate all points that compose the runs of the section, into
     * the provided <b>absolute</b> collector.
     *
     * @param collector the absolute points collector to populate
     */
    public void cumulate (PointsCollector collector);

    /**
     * Draws a basic representation of the section, using ascii chars.
     */
    public void drawAscii ();

    /**
     * Fill a buffer with the pixels of this section.
     *
     * @param buffer the buffer to populate with this section
     * @param offset absolute image offset
     */
    public void fillBuffer (ByteProcessor buffer,
                            Point offset);

    /**
     * Draws the section, into the provided table.
     *
     * @param table the table to populate
     * @param box   the region of interest
     */
    public void fillTable (char[][] table,
                           Rectangle box);

    /**
     * Return the <b>absolute</b> line which best approximates the section.
     *
     * @return the absolute fitted line
     * @see #getOrientedLine()
     */
    public Line getAbsoluteLine ();

    /**
     * Report the section area absolute center.
     *
     * @return the area absolute center
     */
    public Point getAreaCenter ();

    /**
     * Report the ratio of length over thickness, along provided orientation.
     *
     * @param orientation the desired orientation
     * @return the "slimness" of the section
     */
    public double getAspect (Orientation orientation);

    /**
     * Return a (COPY of) the absolute bounding box.
     *
     * @return the absolute bounding box
     */
    @Override
    public Rectangle getBounds ();

    /**
     * Return the absolute point which is at the mass center of the section, with all
     * pixels considered of equal weight.
     *
     * @return the mass center of the section, as a absolute point
     */
    public Point getCentroid ();

    /**
     * Return the absolute point which is at the mass center of the section, with all
     * pixels considered of equal weight.
     *
     * @return the mass center of the section, as a absolute point
     */
    public Point2D getCentroid2D ();

    /**
     * Return the position (x for vertical runs, y for horizontal runs)
     * of the first run of the section.
     *
     * @return the position
     */
    public int getFirstPos ();

    /**
     * Return the first run within the section.
     *
     * @return the run, which always exists
     */
    public Run getFirstRun ();

    /**
     * Report the containing lag
     *
     * @return the containing lag
     */
    public Lag getLag ();

    /**
     * Return the position of the last run of the section.
     *
     * @return the position of last run
     */
    public int getLastPos ();

    /**
     * Return the last run of the section.
     *
     * @return this last run (rightmost run for vertical section)
     */
    public Run getLastRun ();

    /**
     * Return the length of the section, using the provided orientation.
     *
     * @param orientation the desired orientation
     * @return the section length (along orientation)
     */
    public int getLength (Orientation orientation);

    /**
     * Return the size of the longest run in the section.
     *
     * @return the maximum run length
     */
    public int getMaxRunLength ();

    /**
     * Report the ratio of length over mean thickness, using the
     * provided orientation.
     *
     * @param orientation the desired orientation
     * @return the "slimness" of the section
     */
    public double getMeanAspect (Orientation orientation);

    /**
     * Return the average value for all run lengths in the section.
     *
     * @return the mean run length
     */
    public int getMeanRunLength ();

    /**
     * Report the average thickness of the section, using the provided
     * orientation.
     *
     * @param orientation the desired orientation
     * @return the average thickness of the section
     */
    public double getMeanThickness (Orientation orientation);

    /**
     * Return the section bounding rectangle, using the runs
     * orientation.
     * Please clone it if you want to modify it afterwards
     *
     * @return the section bounding rectangle
     */
    public Rectangle getOrientedBounds ();

    /**
     * Report the line which best approximates the section, using the
     * runs orientation.
     *
     * @return the oriented fitted line
     * @see #getAbsoluteLine()
     */
    public Line getOrientedLine ();

    /**
     * Create an iterator along the absolute polygon that represents the section contour.
     *
     * @return an iterator on the underlying polygon
     */
    public PathIterator getPathIterator ();

    /**
     * Return the absolute polygon that defines the display contour.
     *
     * @return the absolute perimeter contour
     */
    public Polygon getPolygon ();

    /**
     * Report the absolute centroid of the section pixels found in the
     * provided absolute region of interest.
     *
     * @param absRoi the absolute rectangle that defines the region of interest
     * @return the absolute centroid
     */
    public Point getRectangleCentroid (Rectangle absRoi);

    /**
     * Report the number of runs this sections contains.
     *
     * @return the nb of runs in the section
     */
    public int getRunCount ();

    /**
     * Return an unmodifiable list of all runs in this section.
     *
     * @return the section runs
     */
    public List<Run> getRuns ();

    /**
     * Return the smallest run starting coordinate, which is the
     * smallest y value (ordinate) for a section of vertical runs.
     *
     * @return the starting coordinate of the section
     */
    public int getStartCoord ();

    /**
     * Return the largest run stopping coordinate, which is the
     * largest y value (ordinate) for a section of vertical runs.
     *
     * @return the stopping coordinate of the section
     */
    public int getStopCoord ();

    /**
     * Return the thickness of the section, using the provided
     * orientation.
     *
     * @param orientation the desired orientation
     * @return the thickness across the provided orientation.
     */
    public int getThickness (Orientation orientation);

    /**
     * Return the total weight of the section, which is the sum of the
     * weight (length) of all runs.
     *
     * @return the section weight
     */
    public int getWeight ();

    /**
     * Check whether this section intersects the provided absolute rectangle.
     *
     * @param rectangle absolute rectangle
     * @return true if intersection is not empty
     */
    public boolean intersects (Rectangle rectangle);

    /**
     * Check whether this section intersects the provided shape.
     *
     * @param shape the shape to intersect
     * @return true if intersection is not empty
     */
    public boolean intersects (java.awt.Shape shape);

    /**
     * Check whether this section intersects that other section.
     *
     * @param that the other section
     * @return true if intersection is not empty
     */
    public boolean intersects (Section that);

    /**
     * Reports whether this section is organized in vertical runs.
     *
     * @return true if vertical, false otherwise
     */
    public boolean isVertical ();

    /**
     * Render the section
     *
     * @param g             the graphics context
     * @param drawBorders   should section borders be drawn
     * @param specificColor specific color
     * @return true if actually rendered, i.e. is displayed
     */
    public boolean render (Graphics g,
                           boolean drawBorders,
                           Color specificColor);

    /**
     * Render the section using the provided graphics object, while
     * showing that the section has been selected.
     *
     * @param g the graphics environment (which may be applying transformation such as scale)
     * @return true if the section is concerned by the clipping rectangle, which means if (part of)
     *         the section has been drawn
     */
    public boolean renderSelected (Graphics g);

    /**
     * Assign the containing lag
     *
     * @param lag the containing lag
     */
    public void setLag (Lag lag);

    /**
     * Check whether this section touches that other section
     *
     * @param that the other section
     * @return true if there is contact between the two sections
     *         (vertically or horizontally, but not in diagonal)
     */
    public boolean touches (Section that);

    /**
     * Apply an absolute translation to the section
     *
     * @param dx abscissa shift, regardless of section orientation
     * @param dy ordinate shift, regardless of section orientation
     */
    public void translateAbsolute (int dx,
                                   int dy);
}
