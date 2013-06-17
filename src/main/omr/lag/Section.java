//----------------------------------------------------------------------------//
//                                                                            //
//                               S e c t i o n                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.glyph.facets.Glyph;

import omr.graph.Vertex;

import omr.lag.ui.SectionView;

import omr.math.Barycenter;
import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;
import omr.run.Oriented;
import omr.run.Run;

import omr.sheet.SystemInfo;

import omr.stick.StickRelation;

import omr.util.Vip;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Section} handles a section of contiguous and
 * compatible {@link Run} instances.
 *
 * <p> A section carries orientation information, which is the orientation for
 * all runs in this section.
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
 * <p><b>Beware</b>, the section orientation only governs the runs orientation.
 * It by no means implies that the section dimension is longer in the direction
 * along the runs than in the direction across.
 * To enforce this, the {@link #getLength(Orientation)} requires that an
 * explicit orientation be provided, just like for {@link Glyph} instances.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicSection.Adapter.class)
public interface Section
        extends Vertex<Lag, Section>, Comparable<Section>, Oriented, SectionView, Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** A section comparator, using section id */
    public static final Comparator<Section> idComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return Integer.signum(s1.getId() - s2.getId());
        }
    };

    /** For comparing Section instances on their decreasing weight */
    public static final Comparator<Section> reverseWeightComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return Integer.signum(s2.getWeight() - s1.getWeight());
        }
    };

    /** For comparing Section instances on their start value */
    public static final Comparator<Section> startComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return s1.getStartCoord() - s2.getStartCoord();
        }
    };

    /** For comparing Section instances on their pos value */
    public static final Comparator<Section> posComparator = new Comparator<Section>()
    {
        @Override
        public int compare (Section s1,
                            Section s2)
        {
            return s1.getFirstPos() - s2.getFirstPos();
        }
    };

    //~ Methods ----------------------------------------------------------------
    /**
     * Register the adjacency of a section from the other orientation.
     *
     * @param otherSection the other section to remember
     */
    public void addOppositeSection (Section otherSection);

    /**
     * Extend a section with the given run.
     * This new run is assumed to be contiguous to the current last run of the
     * section, no check is performed.
     *
     * @param run the new last run
     */
    public void append (Run run);

    /**
     * Compute the various cached parameters from scratch.
     */
    public void computeParameters ();

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
     * Build an image with the pixels of this section.
     *
     * @param im  the image to populate with this section
     * @param box absolute bounding box (used as image coordinates reference)
     */
    public void fillImage (BufferedImage im,
                           Rectangle box);

    /**
     * Draws the section, into the provided table.
     */
    public void fillTable (char[][] table,
                           Rectangle box);

    /**
     * Return the <b>absolute</b> line which best approximates the
     * section.
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
     * Report the ratio of length over thickness, along provided
     * orientation.
     *
     * @return the "slimness" of the section
     */
    public double getAspect (Orientation orientation);

    /**
     * Return a COPY of the absolute bounding box.
     *
     * @return the absolute bounding box
     */
    @Override
    public Rectangle getBounds ();

    /**
     * Return the absolute point which is at the mass center of the
     * section, with all pixels considered of equal weight.
     *
     * @return the mass center of the section, as a absolute point
     */
    public Point getCentroid ();

    /**
     * Return the adjacency ratio on the incoming junctions.
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
    public double getFirstAdjacency ();

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
     * Return the contribution of the section to the foreground.
     *
     * @return the section foreground weight
     */
    public int getForeWeight ();

    /**
     * Report the glyph the section belongs to, if any.
     *
     * @return the glyph, which may be null
     */
    public Glyph getGlyph ();

    /**
     * Return the adjacency ratio at the end of the section at hand.
     * See getFirstAdjacency for explanation of the role of adjacency.
     *
     * @return the percentage of overlapped run length
     * @see #getFirstAdjacency
     */
    public double getLastAdjacency ();

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
     */
    public int getLength (Orientation orientation);

    /**
     * Return the mean gray level of the section.
     *
     * @return the section foreground level (0 -> 255)
     */
    public int getLevel ();

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
     * @return the average thickness of the section
     */
    public double getMeanThickness (Orientation orientation);

    /**
     * A read-only access to adjacent sections from opposite orientation
     *
     * @return the set of adjacent sections of the opposite orientation
     */
    public Set<Section> getOppositeSections ();

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
     * Create an iterator along the absolute polygon that represents
     * the section contour.
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

    //TODO:  REMOVE getRelation ASAP
    public StickRelation getRelation ();

    /**
     * Report the number of runs this sections contains.
     *
     * @return the nb of runs in the section
     */
    public int getRunCount ();

    /**
     * Return the list of all runs in this section.
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
     * Report the containing system.
     *
     * @return the system (may be null)
     */
    public SystemInfo getSystem ();

    /**
     * Return the thickness of the section, using the provided
     * orientation.
     *
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
     * Return the next sibling section, both linked by source of
     * last incoming edge.
     *
     * @return the next sibling or null
     */
    public Section inNextSibling ();

    /**
     * Return the previous sibling section, both linked by source of
     * first incoming edge.
     *
     * @return the previous sibling or null
     */
    public Section inPreviousSibling ();

    /**
     * Check that the section at hand is a candidate section not yet
     * aggregated to a recognized stick.
     *
     * @return true if aggregable (but not yet aggregated)
     */
    public boolean isAggregable ();

    /**
     * Report whether this section is "fat", according to the current
     * criteria and desired orientation.
     *
     * @return the fat flag, if any
     */
    public Boolean isFat ();

    /**
     * Checks whether the section is already a member of a glyph.
     *
     * @return the result of the test
     */
    public boolean isGlyphMember ();

    /**
     * Check that the section at hand is a member section, aggregated
     * to a known glyph.
     *
     * @return true if member of a known glyph
     */
    public boolean isKnown ();

    /**
     * Report whether this section has been "processed".
     *
     * @return the processed
     */
    public boolean isProcessed ();

    /**
     * Reports whether this section is organized in vertical runs.
     *
     * @return true if vertical, false otherwise
     */
    public boolean isVertical ();

    /**
     * Merge this section with the other provided section, which is not
     * affected, and must generally be destroyed.
     * TODO: rename as "include".
     * It is assumed (and not checked) that the two sections are
     * contiguous.
     *
     * @param other the other section to include into this one
     */
    public void merge (Section other);

    /**
     * Return the next sibling section, both linked by target of
     * the last outgoing edge.
     *
     * @return the next sibling or null
     */
    public Section outNextSibling ();

    /**
     * Return the previous sibling section, both linked by target of
     * the first outgoing edge.
     *
     * @return the previous sibling or null
     */
    public Section outPreviousSibling ();

    /**
     * Add a run at the beginning rather than at the end of the
     * section.
     *
     * @param run the new first run
     */
    public void prepend (Run run);

    /**
     * Nullify the fat sticky attribute.
     */
    public void resetFat ();

    /**
     * Record the current "fatness" value of this section.
     *
     * @param fat the fat flag
     */
    public void setFat (boolean fat);

    /**
     * Set the position of the first run of the section.
     *
     * @param firstPos position of the first run, abscissa for a vertical run,
     *                 ordinate for a horizontal run.
     */
    public void setFirstPos (int firstPos);

    /**
     * Assign the containing glyph, if any.
     *
     * @param glyph the containing glyph, perhaps null
     */
    public void setGlyph (Glyph glyph);

    /**
     * Set a flag to be used at caller's will.
     *
     * @param processed the processed to set
     */
    public void setProcessed (boolean processed);

    /**
     * Assign a containing system.
     *
     * @param system the system to set
     */
    public void setSystem (SystemInfo system);

    /**
     * Apply an absolute translation vector to this section.
     *
     * @param vector the translation vector
     */
    public void translate (Point vector);

    /**
     * Predicate to check whether the given absolute rectangle is
     * intersected by the section.
     *
     * @param rectangle absolute rectangle
     * @return true if intersection is not empty
     */
    boolean intersects (Rectangle rectangle);
}
