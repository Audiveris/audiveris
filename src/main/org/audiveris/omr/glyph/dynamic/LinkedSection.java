//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L i n k e d S e c t i o n                                   //
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
package org.audiveris.omr.glyph.dynamic;

import ij.process.ByteProcessor;

import net.jcip.annotations.NotThreadSafe;

import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.Barycenter;
import org.audiveris.omr.math.Line;
import org.audiveris.omr.math.PointsCollector;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code LinkedSection} is a working wrapper around a section considered as
 * immutable, since all modifying operations on the underlying section raise
 * UnsupportedOperationException.
 * <p>
 * Additional transient data:
 * <ul>
 * <li>Links to source sections and target sections</li>
 * <li>Link to compound</li>
 * <li>Processed flag</li>
 * </ul>
 * <b>NOTA BENE</b>: This wrapper <b>CANNOT</b> be used by several threads!
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class LinkedSection
        implements Section
{

    /** Concrete underlying section, to which most features are delegated. */
    private final Section section;

    private List<LinkedSection> sources;

    private List<LinkedSection> targets;

    private boolean processed;

    private SectionCompound compound;

    /**
     * Creates a new {@code LinkedSection} object.
     *
     * @param section the underlying section
     */
    public LinkedSection (Section section)
    {
        this.section = section;
    }

    //-----------//
    // addSource //
    //-----------//
    /**
     * Add a source to the section.
     *
     * @param source a source section
     */
    public void addSource (LinkedSection source)
    {
        if (sources == null) {
            sources = new ArrayList<>();
        }

        sources.add(source);
    }

    //-----------//
    // addTarget //
    //-----------//
    /**
     * Add a target to the section.
     *
     * @param target a target section
     */
    public void addTarget (LinkedSection target)
    {
        if (targets == null) {
            targets = new ArrayList<>();
        }

        targets.add(target);
    }

    @Override
    public boolean contains (double x,
                             double y)
    {
        return section.contains(x, y);
    }

    @Override
    public boolean contains (Point point)
    {
        return section.contains(point);
    }

    @Override
    public void cumulate (Barycenter barycenter,
                          Rectangle absRoi)
    {
        section.cumulate(barycenter, absRoi);
    }

    @Override
    public void cumulate (PointsCollector collector)
    {
        section.cumulate(collector);
    }

    @Override
    public void drawAscii ()
    {
        section.drawAscii();
    }

    @Override
    public String dumpOf ()
    {
        return section.dumpOf();
    }

    @Override
    public void fillBuffer (ByteProcessor buffer,
                            Point offset)
    {
        section.fillBuffer(buffer, offset);
    }

    @Override
    public void fillTable (char[][] table,
                           Rectangle box)
    {
        section.fillTable(table, box);
    }

    @Override
    public Line getAbsoluteLine ()
    {
        return section.getAbsoluteLine();
    }

    @Override
    public Point getAreaCenter ()
    {
        return section.getAreaCenter();
    }

    @Override
    public double getAspect (Orientation orientation)
    {
        return section.getAspect(orientation);
    }

    @Override
    public Rectangle getBounds ()
    {
        return section.getBounds();
    }

    @Override
    public Point getCentroid ()
    {
        return section.getCentroid();
    }

    @Override
    public Point2D getCentroid2D ()
    {
        return section.getCentroid2D();
    }

    //-------------//
    // getCompound //
    //-------------//
    /**
     * Report the underlying compound.
     *
     * @return underlying section compound
     */
    public SectionCompound getCompound ()
    {
        return compound;
    }

    //-------------//
    // setCompound //
    //-------------//
    /**
     * Set the underlying compound section
     *
     * @param compound underlying section
     */
    public void setCompound (SectionCompound compound)
    {
        this.compound = compound;
    }

    @Override
    public int getFirstPos ()
    {
        return section.getFirstPos();
    }

    @Override
    public Run getFirstRun ()
    {
        return section.getFirstRun();
    }

    @Override
    public String getFullId ()
    {
        return section.getFullId();
    }

    @Override
    public int getId ()
    {
        return section.getId();
    }

    @Override
    public void setId (int id)
    {
        section.setId(id);
    }

    @Override
    public Lag getLag ()
    {
        return section.getLag();
    }

    @Override
    public void setLag (Lag lag)
    {
        section.setLag(lag);
    }

    @Override
    public int getLastPos ()
    {
        return section.getLastPos();
    }

    @Override
    public Run getLastRun ()
    {
        return section.getLastRun();
    }

    @Override
    public int getLength (Orientation orientation)
    {
        return section.getLength(orientation);
    }

    @Override
    public int getMaxRunLength ()
    {
        return section.getMaxRunLength();
    }

    @Override
    public double getMeanAspect (Orientation orientation)
    {
        return section.getMeanAspect(orientation);
    }

    @Override
    public int getMeanRunLength ()
    {
        return section.getMeanRunLength();
    }

    @Override
    public double getMeanThickness (Orientation orientation)
    {
        return section.getMeanThickness(orientation);
    }

    @Override
    public Orientation getOrientation ()
    {
        return section.getOrientation();
    }

    @Override
    public Rectangle getOrientedBounds ()
    {
        return section.getOrientedBounds();
    }

    @Override
    public Line getOrientedLine ()
    {
        return section.getOrientedLine();
    }

    @Override
    public PathIterator getPathIterator ()
    {
        return section.getPathIterator();
    }

    @Override
    public Polygon getPolygon ()
    {
        return section.getPolygon();
    }

    @Override
    public Point getRectangleCentroid (Rectangle absRoi)
    {
        return section.getRectangleCentroid(absRoi);
    }

    @Override
    public int getRunCount ()
    {
        return section.getRunCount();
    }

    @Override
    public List<Run> getRuns ()
    {
        return section.getRuns();
    }

    //------------//
    // getSources //
    //------------//
    /**
     * Report the source sections.
     *
     * @return the (non-null but perhaps empty) set of connected source sections
     */
    public List<LinkedSection> getSources ()
    {
        if (sources == null) {
            return Collections.emptyList();
        }

        return sources;
    }

    @Override
    public int getStartCoord ()
    {
        return section.getStartCoord();
    }

    @Override
    public int getStopCoord ()
    {
        return section.getStopCoord();
    }

    //------------//
    // getTargets //
    //------------//
    /**
     * Report the target sections.
     *
     * @return the (non-null but perhaps empty) set of connected target sections
     */
    public List<LinkedSection> getTargets ()
    {
        if (targets == null) {
            return Collections.emptyList();
        }

        return targets;
    }

    @Override
    public int getThickness (Orientation orientation)
    {
        return section.getThickness(orientation);
    }

    @Override
    public int getWeight ()
    {
        return section.getWeight();
    }

    @Override
    public boolean intersects (Rectangle rectangle)
    {
        return section.intersects(rectangle);
    }

    @Override
    public boolean intersects (Shape shape)
    {
        return section.intersects(shape);
    }

    @Override
    public boolean intersects (Section that)
    {
        return section.intersects(that);
    }

    //-------------//
    // isProcessed //
    //-------------//
    /**
     * Tell whether this LinkedSection has already been processed
     *
     * @return true if so
     */
    public boolean isProcessed ()
    {
        return processed;
    }

    @Override
    public boolean isVertical ()
    {
        return section.isVertical();
    }

    @Override
    public boolean isVip ()
    {
        return section.isVip();
    }

    @Override
    public void setVip (boolean vip)
    {
        section.setVip(vip);
    }

    @Override
    public boolean render (Graphics g,
                           boolean drawBorders,
                           Color specificColor)
    {
        return section.render(g, drawBorders, specificColor);
    }

    @Override
    public boolean renderSelected (Graphics g)
    {
        return section.renderSelected(g);
    }

    //--------------//
    // setProcessed //
    //--------------//
    /**
     * Flag the instance as processed.
     */
    public void setProcessed ()
    {
        processed = true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(section.toString());

        sb.append("-L:").append(getSources().size());

        sb.append("-R:").append(getTargets().size());

        return sb.toString();
    }

    @Override
    public boolean touches (Section that)
    {
        return section.touches(that);
    }

    @Override
    public void translateAbsolute (int dx,
                                   int dy)
    {
        section.translateAbsolute(dx, dy);
    }
}
