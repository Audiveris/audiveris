//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y s t e m I n f o                                    //
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
package omr.sheet;

import omr.glyph.BasicGlyph;
import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;
import omr.glyph.Symbol;
import omr.glyph.Symbol.Group;

import omr.lag.Section;

import omr.score.LogicalPart;
import omr.score.Page;
import omr.score.StaffPosition;

import omr.sheet.grid.LineInfo;
import omr.sheet.grid.PartGroup;
import omr.sheet.note.NotePosition;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;

import omr.sig.SIGraph;
import omr.sig.SigValue.InterSet;
import omr.sig.inter.Inter;
import omr.sig.inter.SentenceInter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;

/**
 * Class {@code SystemInfo} gathers information from the original picture about a
 * retrieved system.
 * <p>
 * Most of the OMR processing is done in parallel at system level.
 * <p>
 * This class is named {@code SystemInfo} to avoid continuous name clash with ubiquitous
 * {@code java.lang.System} class.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {
    /** NOTA: Sig must be marshalled last. */
    "id", "indented", "stacks", "parts", "partGroups", "freeGlyphs", "sig"}
)
public class SystemInfo
        implements Comparable<SystemInfo>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SystemInfo.class);

    /** To sort by system id. */
    public static final Comparator<SystemInfo> byId = new Comparator<SystemInfo>()
    {
        @Override
        public int compare (SystemInfo o1,
                            SystemInfo o2)
        {
            return Integer.compare(o1.id, o2.id);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Unique Id (sequential vertical number starting from 1 in containing sheet). */
    @XmlAttribute(name = "id")
    private final int id;

    /** Indentation flag. */
    @XmlAttribute(name = "indented")
    private Boolean indented;

    /** Measure stacks in this system. */
    @XmlElement(name = "stack")
    private final List<MeasureStack> stacks = new ArrayList<MeasureStack>();

    /** Real parts in this system (no dummy parts included). */
    @XmlElement(name = "part")
    private final List<Part> parts = new ArrayList<Part>();

    /** PartGroups in this system. */
    @XmlElement(name = "part-group")
    private final List<PartGroup> partGroups = new ArrayList<PartGroup>();

    /** Collection of stand-alone glyphs in this system.
     * This should be limited to glyphs not referenced elsewhere, to avoid garbage collection.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "free-glyphs")
    private final FreeGlyphs freeGlyphs = new FreeGlyphs();

    /**
     * Symbol Interpretation Graph for this system.
     * NOTA: sig must be marshalled AFTER parts hierarchy to separate IDs and IDREFs handling.
     */
    @XmlElement(name = "sig")
    private SIGraph sig;

    // Transient data
    //---------------
    //
    /** Containing sheet. */
    @Navigable(false)
    private Sheet sheet;

    /** Real staves of this system (no dummy staves included). */
    private List<Staff> staves = new ArrayList<Staff>();

    /** Assigned page, if any. */
    private Page page;

    /** Horizontal sections. */
    private final List<Section> hSections = new ArrayList<Section>();

    /** Vertical sections. */
    private final List<Section> vSections = new ArrayList<Section>();

    /** Area that encloses all items related to this system. */
    private Area area;

    /** Ordinate of bottom of last staff of the system. */
    private int bottom;

    /** Delta ordinate between first line of first staff & first line of last staff. */
    private int deltaY;

    /** Abscissa of beginning of system. */
    private int left;

    /** Abscissa of beginning of system area. */
    private int areaLeft;

    /** Abscissa of end of system area. */
    private int areaRight;

    /** Ordinate of top of first staff of the system. */
    private int top;

    /** Width of the system. */
    private int width = -1;

    /** Very temporary set, used only during SIG marshalling. */
    private InterSet interSet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a SystemInfo entity, to register the provided parameters.
     *
     * @param id     the unique identity
     * @param sheet  the containing sheet
     * @param staves the (initial) sequence of staves
     */
    public SystemInfo (int id,
                       Sheet sheet,
                       List<Staff> staves)
    {
        this.id = id;
        this.sheet = sheet;

        setStaves(staves);

        sig = new SIGraph(this);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private SystemInfo ()
    {
        this.id = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // addFreeGlyph //
    //--------------//
    /**
     * Add the provided glyph as a free glyph in this system.
     *
     * @param glyph the glyph to include
     */
    public void addFreeGlyph (Glyph glyph)
    {
        freeGlyphs.add((BasicGlyph) glyph);
    }

    //---------//
    // addPart //
    //---------//
    /**
     * Add a (real) part to this system.
     *
     * @param partInfo the part to add
     */
    public void addPart (Part partInfo)
    {
        parts.add(partInfo);
    }

    //-------------//
    // afterReload //
    //-------------//
    public void afterReload ()
    {
        try {
            // Populate system sig
            sig.afterReload(this);

            // Process staves upfront, so that their notes have their staff assigned.
            // Doing so, measure chords can determine which staves they belong to.
            for (Staff staff : staves) {
                staff.afterReload();
            }

            for (Part part : parts) {
                part.afterReload();
            }

            for (MeasureStack stack : stacks) {
                stack.afterReload(this);
            }

            for (Inter inter : sig.inters(SentenceInter.class)) {
                ((SentenceInter) inter).assignStaff(this);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //-----------------//
    // clearFreeGlyphs //
    //-----------------//
    /**
     * Clear the collection of free glyphs, when they are no longer useful.
     */
    public void clearFreeGlyphs ()
    {
        freeGlyphs.clear();
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement natural SystemInfo sorting, based on system id.
     *
     * @param that the other system to compare to
     * @return the comparison result
     */
    @Override
    public int compareTo (SystemInfo that)
    {
        return Integer.compare(id, that.id);
    }

    //----------------//
    // estimatedPitch //
    //----------------//
    /**
     * Make an estimate of pitch position for the provided point.
     * <p>
     * NOTA: this is really error-prone, since a pitch position is relevant only with respect to a
     * staff, and here we simply pick the "closest" staff which may not be the related staff.
     *
     * @param point the location to process
     * @return an estimate of point pitch position (WRT closest staff)
     */
    public double estimatedPitch (Point2D point)
    {
        final Staff closestStaff = getClosestStaff(point); // This is just an indication!

        return closestStaff.pitchPositionOf(point);
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the area of this system.
     *
     * @return the area of relevant entities
     */
    public Area getArea ()
    {
        if (area == null) {
            sheet.getSystemManager().computeSystemArea(this);
        }

        return area;
    }

    //------------//
    // getAreaEnd //
    //------------//
    public int getAreaEnd (HorizontalSide side)
    {
        if (side == LEFT) {
            return areaLeft;
        } else {
            return areaRight;
        }
    }

    //-----------//
    // getBottom //
    //-----------//
    /**
     * Report the ordinate of the bottom of this system.
     *
     * @return the bottom ordinate, expressed in pixels
     */
    public int getBottom ()
    {
        return bottom;
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the rectangular bounds that enclose this system.
     *
     * @return the system rectangular bounds
     */
    public Rectangle getBounds ()
    {
        if (getArea() != null) {
            return area.getBounds();
        } else {
            return null;
        }
    }

    //-----------------//
    // getClosestStaff //
    //-----------------//
    /**
     * Report the closest staff, <b>within</b> the system, from the provided point.
     *
     * @param point the provided point
     * @return the nearest staff, or null if none found
     */
    public Staff getClosestStaff (Point2D point)
    {
        return StaffManager.getClosestStaff(point, staves);
    }

    //-----------//
    // getDeltaY //
    //-----------//
    /**
     * Report the deltaY of the system, that is the difference in
     * ordinate between first and last staves of the system.
     * This deltaY is of course 0 for a one-staff system.
     *
     * @return the deltaY value, expressed in pixels
     */
    public int getDeltaY ()
    {
        return deltaY;
    }

    //----------------------//
    // getFirstMeasureStack //
    //----------------------//
    /**
     * Report the first measure stack in this part.
     *
     * @return the first measure stack
     */
    public MeasureStack getFirstMeasureStack ()
    {
        if (stacks.isEmpty()) {
            return null;
        }

        return stacks.get(0);
    }

    //--------------//
    // getFirstPart //
    //--------------//
    /**
     * Report the first (real) part in this system.
     *
     * @return the first part entity
     */
    public Part getFirstPart ()
    {
        for (Part part : parts) {
            if (!part.isDummy()) {
                return part;
            }
        }

        return null;
    }

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * Report the first staff of the system.
     *
     * @return the first staff
     */
    public Staff getFirstStaff ()
    {
        return staves.get(0);
    }

    //--------------------//
    // getFollowingInPage //
    //--------------------//
    /**
     * Report the following system, if any, in current page.
     *
     * @return the next system in page, or null
     */
    public SystemInfo getFollowingInPage ()
    {
        if (page == null) {
            return null;
        }

        final List<SystemInfo> pageSystems = page.getSystems();
        final int index = pageSystems.indexOf(this);

        if (index < (pageSystems.size() - 1)) {
            return pageSystems.get(index + 1);
        }

        return null;
    }

    //------------------//
    // getGroupedGlyphs //
    //------------------//
    /**
     * Report in system glyphs those assigned to the provided group.
     *
     * @param group the desired group
     * @return the glyphs found
     */
    public List<Glyph> getGroupedGlyphs (Symbol.Group group)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : freeGlyphs) {
            if (glyph.hasGroup(group)) {
                found.add(glyph);
            }
        }

        return found;
    }

    //-----------------------//
    // getHorizontalSections //
    //-----------------------//
    /**
     * Report the (unmodifiable) list of horizontal sections in the system area, ordered
     * by position (y) then coordinate (x).
     *
     * @return the ordered area horizontal sections
     */
    public List<Section> getHorizontalSections ()
    {
        return Collections.unmodifiableList(hSections);
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this system, a sequential number starting from 1 in containing
     * sheet, regardless of containing part.
     *
     * @return the system id within sheet
     */
    public int getId ()
    {
        return id;
    }

    //----------------//
    // getIndexInPage //
    //----------------//
    public int getIndexInPage ()
    {
        return getPage().getSystems().indexOf(this);
    }

    //-------------//
    // getInterSet //
    //-------------//
    /**
     * @return the interSet
     */
    public InterSet getInterSet ()
    {
        return interSet;
    }

    //---------------------//
    // getLastMeasureStack //
    //---------------------//
    /**
     * Report the last measure stack in this part.
     *
     * @return the last measure stack
     */
    public MeasureStack getLastMeasureStack ()
    {
        if (stacks.isEmpty()) {
            return null;
        }

        return stacks.get(stacks.size() - 1);
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * Report the last (real) staff in this system
     *
     * @return the last Staff in system
     */
    public Staff getLastStaff ()
    {
        return staves.get(staves.size() - 1);
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the left abscissa.
     *
     * @return the left abscissa value, expressed in pixels
     */
    public int getLeft ()
    {
        return left;
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    /**
     * Report the proper prefix to use when logging a message.
     *
     * @return the proper prefix
     */
    public String getLogPrefix ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("S").append(id).append(" ");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the IDs of the system collection.
     *
     * @param systems the collection of systems
     * @return the string built
     */
    public static String toString (Collection<SystemInfo> systems)
    {
        if (systems == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(" systems[");

        for (SystemInfo system : systems) {
            sb.append("#").append(system.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //-------------------//
    // getMeasureStackAt //
    //-------------------//
    /**
     * Report the measure stack that contains the provided point.
     *
     * @param point the provided point
     * @return the containing measure stack or null if none
     */
    public MeasureStack getMeasureStackAt (Point2D point)
    {
        final Staff staff = getStavesAround(point).get(0);
        final double x = point.getX();

        for (MeasureStack stack : stacks) {
            Measure measure = stack.getMeasureAt(staff);

            if ((x >= measure.getAbscissa(LEFT, staff))
                && (x <= measure.getAbscissa(RIGHT, staff))) {
                return stack;
            }
        }

        return null;
    }

    //------------------//
    // getMeasureStacks //
    //------------------//
    /**
     * @return the measureStacks
     */
    public List<MeasureStack> getMeasureStacks ()
    {
        return stacks;
    }

    //------------------------------//
    // getMutableHorizontalSections //
    //------------------------------//
    /**
     * Report the (modifiable) collection of horizontal sections in the
     * system related area.
     *
     * @return the area vertical sections
     */
    public Collection<Section> getMutableHorizontalSections ()
    {
        return hSections;
    }

    //----------------------------//
    // getMutableVerticalSections //
    //----------------------------//
    /**
     * Report the (modifiable) collection of vertical sections in the
     * system related area.
     *
     * @return the vertical sections
     */
    public Collection<Section> getMutableVerticalSections ()
    {
        return vSections;
    }

    //----------------//
    // getNoteStaffAt //
    //----------------//
    /**
     * Given a note, retrieve the proper related staff within the system, using ledgers
     * if any.
     *
     * @param point the center of the provided note entity
     * @return the proper note position (staff & pitch)
     */
    public NotePosition getNoteStaffAt (Point point)
    {
        Staff staff = getClosestStaff(point);
        NotePosition pos = staff.getNotePosition(point);

        logger.debug("{} -> {}", point, pos);

        double pitch = pos.getPitchPosition();

        if ((Math.abs(pitch) > 5) && (pos.getLedger() == null)) {
            // Delta pitch from reference line
            double dp = Math.abs(pitch) - 4;

            // Check with the other staff, if any
            int index = staves.indexOf(staff);
            Staff otherStaff = null;

            if ((pitch < 0) && (index > 0)) {
                otherStaff = staves.get(index - 1);
            } else if ((pitch > 0) && (index < (staves.size() - 1))) {
                otherStaff = staves.get(index + 1);
            }

            if (otherStaff != null) {
                NotePosition otherPos = otherStaff.getNotePosition(point);

                if (otherPos.getLedger() != null) {
                    // Delta pitch from closest reference ledger
                    double otherDp = Math.abs(
                            otherPos.getPitchPosition()
                            - Staff.getLedgerPitchPosition(otherPos.getLedger().index));

                    if (otherDp < dp) {
                        logger.debug("   otherPos: {}", pos);
                        pos = otherPos;
                    }
                }
            }
        }

        return pos;
    }

    //---------//
    // getPage //
    //---------//
    /**
     * Report the containing page, if any defined.
     *
     * @return the containing page or null
     */
    public Page getPage ()
    {
        return page;
    }

    //------------------//
    // getPartAtOrAbove //
    //------------------//
    /**
     * Determine the (real) part which is at or above the given point.
     *
     * @param point the given point
     * @return the part at or above
     */
    public Part getPartAtOrAbove (Point point)
    {
        Staff staff = getStaffAtOrAbove(point);

        if (staff == null) {
            return getFirstPart();
        } else {
            return getPartOf(staff);
        }
    }

    //-------------//
    // getPartById //
    //-------------//
    /**
     * Report the part with the provided id, if any.
     *
     * @param id the id of the desired part
     * @return the part found or null
     */
    public Part getPartById (int id)
    {
        for (Part part : parts) {
            if (part.getId() == id) {
                return part;
            }
        }

        logger.debug("{} No part with id {} found", this, id);

        return null;
    }

    //---------------//
    // getPartGroups //
    //---------------//
    /**
     * Reports the partGroups of this system.
     *
     * @return the partGroups (non-null)
     */
    public List<PartGroup> getPartGroups ()
    {
        return partGroups;
    }

    //-----------//
    // getPartOf //
    //-----------//
    public Part getPartOf (Staff staff)
    {
        if (staff == null) {
            return null;
        }

        for (Part part : parts) {
            if (part.getStaves().contains(staff)) {
                return part;
            }
        }

        return null;
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Reports the parts of this system.
     *
     * @return all the real parts
     */
    public List<Part> getParts ()
    {
        return parts;
    }

    //-----------------//
    // getPhysicalPart //
    //-----------------//
    /**
     * Report the system part which implements the provided LogicalPart in this system.
     *
     * @param logicalPart the provided part model
     * @return the corresponding system part, if any
     */
    public Part getPhysicalPart (LogicalPart logicalPart)
    {
        for (Part part : parts) {
            if (part.getLogicalPart() == logicalPart) {
                return part;
            }
        }

        logger.debug("{} No system part for {}", this, logicalPart);

        return null;
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the previous system, if any, in current page.
     *
     * @return the previous system in page, or null
     */
    public SystemInfo getPrecedingInPage ()
    {
        if (page == null) {
            return null;
        }

        final List<SystemInfo> pageSystems = page.getSystems();
        final int index = pageSystems.indexOf(this);

        if (index > 0) {
            return pageSystems.get(index - 1);
        }

        return null;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * Report the abscissa of the end of the system.
     *
     * @return the right abscissa, expressed in pixels
     */
    public int getRight ()
    {
        return left + width;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this system belongs to.
     *
     * @return the containing sheet
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //--------//
    // getSig //
    //--------//
    /**
     * @return the sig
     */
    public SIGraph getSig ()
    {
        return sig;
    }

    //---------//
    // getSkew //
    //---------//
    /**
     * Report the system specific skew (or the global sheet skew).
     *
     * @return the related skew
     */
    public Skew getSkew ()
    {
        return sheet.getSkew();
    }

    //-------------------//
    // getStaffAtOrAbove //
    //-------------------//
    public Staff getStaffAtOrAbove (Point2D point)
    {
        final Staff closest = getClosestStaff(point);

        if (closest == null) {
            return null;
        }

        final double toTop = closest.getFirstLine().yAt(point.getX()) - point.getY();

        if (toTop <= 0) {
            // Closest staff contains or is above point, so select it
            return closest;
        }

        // Closest staff is below point, so select previous staff if any
        final int index = staves.indexOf(closest);

        if (index > 0) {
            return staves.get(index - 1);
        }

        return null;
    }

    //-------------------//
    // getStaffAtOrBelow //
    //-------------------//
    public Staff getStaffAtOrBelow (Point2D point)
    {
        final Staff closest = getClosestStaff(point);

        if (closest == null) {
            return null;
        }

        final double toBottom = closest.getLastLine().yAt(point.getX()) - point.getY();

        if (toBottom >= 0) {
            // Closest staff contains or is below point, so select it
            return closest;
        }

        // Closest staff is above point, so select next staff if any
        final int index = staves.indexOf(closest);

        if (index < (staves.size() - 1)) {
            return staves.get(index + 1);
        }

        return null;
    }

    //------------------//
    // getStaffPosition //
    //------------------//
    /**
     * Report the vertical position of the provided point with respect to the system
     * real staves.
     *
     * @param point the point whose ordinate is to be checked
     * @return the StaffPosition value
     */
    public StaffPosition getStaffPosition (Point2D point)
    {
        if (point.getY() < getFirstStaff().getFirstLine().yAt(point.getX())) {
            return StaffPosition.ABOVE_STAVES;
        }

        if (point.getY() > getLastStaff().getLastLine().yAt(point.getX())) {
            return StaffPosition.BELOW_STAVES;
        }

        return StaffPosition.WITHIN_STAVES;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the list of (real) staves that compose this system.
     *
     * @return the staves
     */
    public List<Staff> getStaves ()
    {
        return staves;
    }

    //-----------------//
    // getStavesAround //
    //-----------------//
    /**
     * Report the staves just around the provided point.
     * <p>
     * If point lies within the core of a staff, just this staff is returned.
     * Otherwise, the staff just above if any in system is returned as well as the staff just below
     * if any in system.
     *
     * @param point
     * @return proper sublist of staves (top down)
     */
    public List<Staff> getStavesAround (Point2D point)
    {
        final Staff closest = getClosestStaff(point);
        final double toTop = closest.getFirstLine().yAt(point.getX()) - point.getY();
        final double toBottom = closest.getLastLine().yAt(point.getX()) - point.getY();

        int first = staves.indexOf(closest);
        int last = first;

        if ((toTop * toBottom) <= 0) {
            // Point is within staff core height, pick up just this staff
        } else if (toTop > 0) {
            // Point is above staff, add staff above if any
            if (first > 0) {
                first--;
            }
        } else if (last < (staves.size() - 1)) {
            last++;
        }

        return staves.subList(first, last + 1);
    }

    //-------------//
    // getStavesOf //
    //-------------//
    /**
     * Retrieve the real staves, <b>within</b> the system, whose area contains the
     * provided point.
     *
     * @param point the provided point
     * @return the list of "containing" staves
     */
    public List<Staff> getStavesOf (Point2D point)
    {
        return StaffManager.getStavesOf(point, staves);
    }

    //--------//
    // getTop //
    //--------//
    /**
     * Report the ordinate of the top of this system.
     *
     * @return the top ordinate, expressed in pixels
     */
    public int getTop ()
    {
        return top;
    }

    //---------------------//
    // getVerticalSections //
    //---------------------//
    /**
     * Report the (unmodifiable) list of vertical sections in system area, ordered by
     * position (x) then coordinate (y).
     *
     * @return the ordered area vertical sections
     */
    public Collection<Section> getVerticalSections ()
    {
        return Collections.unmodifiableCollection(vSections);
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the system.
     *
     * @return the width value, expressed in pixels
     */
    public int getWidth ()
    {
        return width;
    }

    //------------//
    // isIndented //
    //------------//
    /**
     * @return the indented
     */
    public boolean isIndented ()
    {
        return (indented != null) && indented;
    }

    //--------------//
    // isMultiStaff //
    //--------------//
    /**
     * Report whether the system contains several staves.
     *
     * @return true if multi-staff
     */
    public boolean isMultiStaff ()
    {
        return staves.size() > 1;
    }

    //----------------//
    // registerGlyphs //
    //----------------//
    /**
     * Make every glyph provided original, registered and included in freeGlyphs.
     *
     * @param parts the glyphs to register
     * @param group group to assign, or null
     */
    public void registerGlyphs (List<Glyph> parts,
                                Group group)
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();

        for (ListIterator<Glyph> li = parts.listIterator(); li.hasNext();) {
            Glyph glyph = li.next();
            glyph = glyphIndex.registerOriginal(glyph);
            glyph.addGroup(group);
            addFreeGlyph(glyph);
            li.set(glyph);
        }
    }

    //-----------------//
    // removeFreeGlyph //
    //-----------------//
    /**
     * Remove a glyph from the containing system collection of free glyphs.
     *
     * @param glyph the glyph to remove
     */
    public void removeFreeGlyph (Glyph glyph)
    {
        freeGlyphs.remove((BasicGlyph) glyph);
    }

    //---------------------//
    // removeGroupedGlyphs //
    //---------------------//
    /**
     * Remove all free glyphs that are assigned the provided group.
     *
     * @param group the group of glyphs to remove
     */
    public void removeGroupedGlyphs (Symbol.Group group)
    {
        for (Iterator<BasicGlyph> it = freeGlyphs.iterator(); it.hasNext();) {
            if (it.next().hasGroup(group)) {
                it.remove();
            }
        }
    }

    //---------//
    // setArea //
    //---------//
    public void setArea (Area area)
    {
        this.area = area;
    }

    //------------//
    // setAreaEnd //
    //------------//
    public void setAreaEnd (HorizontalSide side,
                            int x)
    {
        if (side == LEFT) {
            areaLeft = x;
        } else {
            areaRight = x;
        }
    }

    //-------------//
    // setIndented //
    //-------------//
    /**
     * @param indented the indented to set
     */
    public void setIndented (boolean indented)
    {
        this.indented = indented ? Boolean.TRUE : null;
    }

    //-------------//
    // setInterSet //
    //-------------//
    /**
     * @param interSet the interSet to set
     */
    public void setInterSet (InterSet interSet)
    {
        this.interSet = interSet;
    }

    //---------//
    // setPage //
    //---------//
    public void setPage (Page page)
    {
        this.page = page;
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * @param staves the range of staves
     */
    public final void setStaves (List<Staff> staves)
    {
        this.staves = staves;

        for (Staff staff : staves) {
            staff.setSystem(this);
        }

        updateCoordinates();
    }

    //
    //    //-------------//
    //    // swapVoiceId //
    //    //-------------//
    //    /**
    //     * Change the id of the provided voice to the provided id
    //     * (and change the other voice, if any, which owned the provided id).
    //     *
    //     * @param voice the voice whose id must be changed
    //     * @param id    the new id
    //     */
    //    public void swapVoiceId (Voice voice,
    //                             int id)
    //    {
    //        for (MeasureStack stack : stacks) {
    //            stack.swapVoiceId(voice, id);
    //        }
    //    }
    //
    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description.
     *
     * @return a description based on staff indices
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("System#").append(id);

        //        sb.append(" T").append(getFirstStaff().getId());
        //
        //        if (staves.size() > 1) {
        //            sb.append("..T").append(getLastStaff().getId());
        //        }
        //
        //sb.append("}");
        return sb.toString();
    }

    //-------------------//
    // updateCoordinates //
    //-------------------//
    public final void updateCoordinates ()
    {
        try {
            Staff firstStaff = getFirstStaff();
            LineInfo firstLine = firstStaff.getFirstLine();
            Point2D topLeft = firstLine.getEndPoint(LEFT);

            Staff lastStaff = getLastStaff();
            LineInfo lastLine = lastStaff.getLastLine();
            Point2D botLeft = lastLine.getEndPoint(LEFT);

            left = Integer.MAX_VALUE;

            int right = 0;

            for (Staff staff : staves) {
                left = Math.min(left, staff.getAbscissa(LEFT));
                right = Math.max(right, staff.getAbscissa(RIGHT));
            }

            top = (int) Math.rint(topLeft.getY());
            width = right - left + 1;
            deltaY = (int) Math.rint(
                    lastStaff.getFirstLine().getEndPoint(LEFT).getY() - topLeft.getY());
            bottom = (int) Math.rint(botLeft.getY());
        } catch (Exception ex) {
            logger.warn("Error updating coordinates for system#{}", id, ex);
        }
    }

    //-----------//
    // xOverlaps //
    //-----------//
    /**
     * Report whether this system horizontally overlaps that system
     *
     * @param that the other system
     * @return true if overlap
     */
    public boolean xOverlaps (SystemInfo that)
    {
        final int commonLeft = Math.max(this.left, that.left);
        final int commonRight = Math.min(
                (this.left + this.width) - 1,
                (that.left + that.width) - 1);

        return commonRight > commonLeft;
    }

    //-----------//
    // yOverlaps //
    //-----------//
    /**
     * Report whether this system vertically overlaps that system
     *
     * @param that the other system
     * @return true if overlap
     */
    public boolean yOverlaps (SystemInfo that)
    {
        final int commonTop = Math.max(this.top, that.top);
        final int commonBottom = Math.min(this.bottom, that.bottom);

        return commonBottom > commonTop;
    }

    //----------------//
    // initTransients //
    //----------------//
    void initTransients (BasicSheet sheet,
                         Page page)
    {
        this.sheet = sheet;
        this.page = page;
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        logger.debug("SystemInfo.beforeMarshal for {}", this);
        setInterSet(new InterSet());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // FreeGlyphs //
    //------------//
    /**
     * This is just a trick to present the right class type (BasicGlyph) to IDResolver.
     * Using plain LinkedHashSet&lt;BasicGlyph&gt; resulted in Object class being presented!
     *
     * @see
     * <a href="http://metro.1045641.n5.nabble.com/JAXB-custom-IDResolver-gets-wrong-target-type-using-Collections-td1058562.html">
     * This post</a>
     */
    private static class FreeGlyphs
            extends LinkedHashSet<BasicGlyph>
    {
    }
}
