//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L i n k e d S e c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.glyph.dynamic.SectionCompound;

import omr.math.Barycenter;
import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;
import omr.run.Run;

import omr.util.HorizontalSide;

import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class {@code LinkedSection} encapsulates a section together with links to source
 * sections and links to target sections.
 *
 * @author Hervé Bitteur
 */
public class LinkedSection
        implements Section
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final Section section;

    private Set<LinkedSection> sources;

    private Set<LinkedSection> targets;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code LinkedSection} object.
     *
     * @param section the underlying section
     */
    public LinkedSection (Section section)
    {
        this.section = section;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // addSource //
    //-----------//
    public void addSource (LinkedSection source)
    {
        if (sources == null) {
            sources = new LinkedHashSet<LinkedSection>();
        }

        sources.add(source);
    }

    //-----------//
    // addTarget //
    //-----------//
    public void addTarget (LinkedSection target)
    {
        if (targets == null) {
            targets = new LinkedHashSet<LinkedSection>();
        }

        targets.add(target);
    }

    @Override
    public void append (Run run)
    {
        section.append(run);
    }

    @Override
    public void computeParameters ()
    {
        section.computeParameters();
    }

    @Override
    public boolean contains (int x,
                             int y)
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
    public SectionCompound getCompound ()
    {
        return section.getCompound();
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
    public String getId ()
    {
        return section.getId();
    }

    @Override
    public Lag getLag ()
    {
        return section.getLag();
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

    //-------------//
    // getLinkedOn //
    //-------------//
    public Set<LinkedSection> getLinkedOn (HorizontalSide side)
    {
        Objects.requireNonNull(side, "LinkedSection.getLinkedOn requires a non-null side");

        if (side == HorizontalSide.LEFT) {
            return getSources();
        } else {
            return getTargets();
        }
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
    public Set<LinkedSection> getSources ()
    {
        if (sources == null) {
            return Collections.EMPTY_SET;
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
    public Set<LinkedSection> getTargets ()
    {
        if (targets == null) {
            return Collections.EMPTY_SET;
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

    @Override
    public boolean isCompoundMember ()
    {
        return section.isCompoundMember();
    }

    @Override
    public Boolean isFat ()
    {
        return section.isFat();
    }

    @Override
    public boolean isProcessed ()
    {
        return section.isProcessed();
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
    public void prepend (Run run)
    {
        section.prepend(run);
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

    @Override
    public void resetFat ()
    {
        section.resetFat();
    }

    @Override
    public void setCompound (SectionCompound compound)
    {
        section.setCompound(compound);
    }

    @Override
    public void setFat (boolean fat)
    {
        section.setFat(fat);
    }

    @Override
    public void setFirstPos (int firstPos)
    {
        section.setFirstPos(firstPos);
    }

    @Override
    public void setId (String id)
    {
        section.setId(id);
    }

    @Override
    public void setLag (Lag lag)
    {
        section.setLag(lag);
    }

    @Override
    public void setProcessed (boolean processed)
    {
        section.setProcessed(processed);
    }

    @Override
    public void setVip (boolean vip)
    {
        section.setVip(vip);
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
    public void translate (Point vector)
    {
        section.translate(vector);
    }
}
