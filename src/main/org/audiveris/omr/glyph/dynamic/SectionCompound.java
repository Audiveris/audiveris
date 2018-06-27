//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S e c t i o n C o m p o u n d                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.glyph.AbstractWeightedEntity;
import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.Barycenter;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;

import static org.audiveris.omr.run.Orientation.*;

import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.util.ByteUtil;
import org.audiveris.omr.util.Entities;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code SectionCompound} represents a dynamic collection of sections, allowing
 * to define a growing compound out of these sections.
 *
 * @author Hervé Bitteur
 */
public class SectionCompound
        extends AbstractWeightedEntity
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Sections that compose this compound.
     * The collection is kept sorted on natural Section order (abscissa then ordinate then id, even
     * with mixed section orientations).
     */
    private final SortedSet<Section> members = new TreeSet<Section>(Section.byFullAbscissa);

    /** Link to the compound, if any, this one is a part of. */
    private SectionCompound partOf;

    /** Cached weight. */
    private Integer weight;

    /** Cached bounds. */
    protected Rectangle bounds;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new {@code SectionCompound} object.
     */
    public SectionCompound ()
    {
    }

    /**
     * Creates a new {@code SectionCompound} object.
     *
     * @param interline ignored!
     */
    public SectionCompound (int interline)
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // addSection //
    //------------//
    /**
     * Add a section as a member of this compound.
     *
     * @param section The section to be included
     */
    public void addSection (Section section)
    {
        Objects.requireNonNull(section, "Cannot add a null section");
        members.add(section);
        invalidateCache();
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        // Rough test
        checkBounds();

        if (!bounds.contains(point)) {
            return false;
        }

        for (Section section : getMembers()) {
            if (section.contains(point)) {
                return true;
            }
        }

        return false;
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this);
        sb.append(String.format("   sections=%s%n", Entities.ids(members)));

        if (partOf != null) {
            sb.append(String.format("   partOf=%s%n", partOf));
        }

        return sb.toString();
    }

    //-------------//
    // getAncestor //
    //-------------//
    /**
     * Report the top ancestor of this compound.
     * This is this compound itself, when it has no parent (i.e. not been included into another one)
     *
     * @return the compound ancestor
     */
    public SectionCompound getAncestor ()
    {
        SectionCompound cpd = this;

        while (cpd.partOf != null) {
            cpd = cpd.partOf;
        }

        return cpd;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        checkBounds();

        return new Rectangle(bounds);
    }

    //-----------//
    // getCenter //
    //-----------//
    @Override
    public Point getCenter ()
    {
        checkBounds();

        return GeoUtil.centerOf(bounds);
    }

    //-------------//
    // getCentroid //
    //-------------//
    @Override
    public Point getCentroid ()
    {
        return PointUtil.rounded(getCentroid(null));
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the glyph absolute centroid (mass center) of all pixels found in the
     * provided absolute ROI if any.
     *
     * @param roi the region of interest, if null all symbol pixels are considered
     * @return the absolute mass center point
     */
    public Point2D getCentroid (Rectangle roi)
    {
        Barycenter barycenter = new Barycenter();

        for (Section section : getMembers()) {
            section.cumulate(barycenter, roi);
        }

        if (barycenter.getWeight() != 0) {
            return new Point2D.Double(barycenter.getX(), barycenter.getY());
        } else {
            return null;
        }
    }

    //-----------------//
    // getFirstSection //
    //-----------------//
    /**
     * Report the first section in the ordered collection of members.
     *
     * @return the first section of the compound
     */
    public Section getFirstSection ()
    {
        return members.first();
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        checkBounds();

        return bounds.height;
    }

    //---------//
    // getLeft //
    //---------//
    @Override
    public int getLeft ()
    {
        checkBounds();

        return bounds.x;
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the length of the compound, along the provided orientation.
     *
     * @param orientation the general orientation reference
     * @return the compound length in pixels
     */
    @Override
    public int getLength (Orientation orientation)
    {
        checkBounds();

        if (orientation == HORIZONTAL) {
            return bounds.width;
        } else {
            return bounds.height;
        }
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report the set of member sections.
     *
     * @return member sections
     */
    public SortedSet<Section> getMembers ()
    {
        return Collections.unmodifiableSortedSet(members);
    }

    //-----------//
    // getPartOf //
    //-----------//
    /**
     * Report the containing compound, if any, which has "stolen" the sections of this
     * compound.
     *
     * @return the containing compound if any
     */
    public SectionCompound getPartOf ()
    {
        return partOf;
    }

    //--------//
    // getTop //
    //--------//
    @Override
    public int getTop ()
    {
        checkBounds();

        return bounds.y;
    }

    //------------//
    // getTopLeft //
    //------------//
    @Override
    public Point getTopLeft ()
    {
        checkBounds();

        return bounds.getLocation();
    }

    //-----------//
    // getWeight //
    //-----------//
    @Override
    public int getWeight ()
    {
        if (weight == null) {
            weight = 0;

            for (Section section : members) {
                weight += section.getWeight();
            }
        }

        return weight;
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        checkBounds();

        return bounds.width;
    }

    //---------------//
    // removeSection //
    //---------------//
    /**
     * Remove a section from the compound members
     *
     * @param section the section to remove
     * @param linked  should we update the link from section to compound?
     * @return true if the section was actually found and removed
     */
    public boolean removeSection (Section section,
                                  boolean linked)
    {
        boolean bool = members.remove(section);
        invalidateCache();

        return bool;
    }

    //-----------//
    // setBounds //
    //-----------//
    /**
     * Force the compound contour bounds (when start and stop points are forced).
     *
     * @param bounds the forced contour box
     */
    public void setBounds (Rectangle bounds)
    {
        this.bounds = bounds;
    }

    //-----------//
    // setPartOf //
    //-----------//
    /**
     * Record the link to the compound which has "stolen" the sections of this compound.
     *
     * @param compound the containing compound, if any
     */
    public void setPartOf (SectionCompound compound)
    {
        partOf = compound;
    }

    //---------------//
    // stealSections //
    //---------------//
    /**
     * Include the sections from another compound into this one, and make
     * its sections point into this one.
     * Doing so, the other compound becomes inactive.
     *
     * @param that the compound to swallow
     */
    public void stealSections (SectionCompound that)
    {
        for (Section section : that.getMembers()) {
            addSection(section);
        }

        that.setPartOf(this);
    }

    //----------//
    // toBuffer //
    //----------//
    public ByteProcessor toBuffer ()
    {
        checkBounds();

        final Point offset = bounds.getLocation();
        final ByteProcessor buffer = new ByteProcessor(bounds.width, bounds.height);
        ByteUtil.raz(buffer); // buffer.invert();

        for (Section section : getMembers()) {
            section.fillBuffer(buffer, offset);
        }

        return buffer;
    }

    //---------//
    // toGlyph //
    //---------//
    /**
     * Build a (fixed) glyph, based on current content of this section compound.
     *
     * @param group targeted group, perhaps null
     * @return a glyph made of compound pixels
     */
    public Glyph toGlyph (GlyphGroup group)
    {
        // Fill buffer with section pixels
        final ByteProcessor buffer = toBuffer();

        // Allocate and populate properly oriented run table
        final RunTableFactory factory = new RunTableFactory(
                (buffer.getWidth() > buffer.getHeight()) ? HORIZONTAL : VERTICAL,
                null);
        final RunTable runTable = factory.createTable(buffer);

        // Allocate glyph with proper offset
        final Glyph glyph = new BasicGlyph(bounds.x, bounds.y, runTable);
        glyph.addGroup(group);

        return glyph;
    }

    //---------//
    // touches //
    //---------//
    /**
     * Test whether compound touches the provided section
     *
     * @param section provided section
     * @return true if there is contact
     */
    public boolean touches (Section section)
    {
        for (Section member : members) {
            if (section.touches(member)) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // checkBounds //
    //-------------//
    protected void checkBounds ()
    {
        if (bounds == null) {
            Rectangle theBounds = null;

            for (Section section : members) {
                if (theBounds == null) {
                    theBounds = section.getBounds(); // Already a copy of section bounds
                } else {
                    theBounds.add(section.getBounds());
                }
            }

            bounds = theBounds;
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (partOf != null) {
            sb.append(" anc:").append(getAncestor());
        }

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    protected void invalidateCache ()
    {
        weight = null;
        bounds = null;
    }
}
