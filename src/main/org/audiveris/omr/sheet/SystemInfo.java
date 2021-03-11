//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y s t e m I n f o                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.StaffPosition;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.grid.PartGroup;
import org.audiveris.omr.sheet.note.NotePosition;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.SigListener;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

    private static final Logger logger = LoggerFactory.getLogger(SystemInfo.class);

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

    // Persistent data
    //----------------
    //
    /** Unique Id (sequential vertical number starting from 1 in containing sheet). */
    @XmlAttribute(name = "id")
    private int id;

    /** Indentation flag. */
    @XmlAttribute(name = "indented")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean indented;

    /** Horizontal sequence of measure stacks in this system. */
    @XmlElement(name = "stack")
    private final List<MeasureStack> stacks = new ArrayList<>();

    /** Vertical sequence of real parts in this system (no dummy parts included). */
    @XmlElement(name = "part")
    private final List<Part> parts = new ArrayList<>();

    /** PartGroups in this system. */
    @XmlElement(name = "part-group")
    private final List<PartGroup> partGroups = new ArrayList<>();

    /**
     * Collection of stand-alone glyphs in this system.
     * This should be limited to glyphs not referenced elsewhere, to avoid garbage collection.
     */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "free-glyphs")
    private Set<Glyph> freeGlyphs;

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
    private final List<Staff> staves = new ArrayList<>();

    /** Assigned page, if any. */
    private Page page;

    /** Horizontal sections. */
    private final List<Section> hSections = new ArrayList<>();

    /** Vertical sections. */
    private final List<Section> vSections = new ArrayList<>();

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
        sig.addGraphListener(new SigListener(sig));
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private SystemInfo ()
    {
    }

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
        if (freeGlyphs == null) {
            freeGlyphs = new LinkedHashSet<>();
        }

        freeGlyphs.add(glyph);
    }

    //---------//
    // addPart //
    //---------//
    /**
     * Add a (real) part to this system.
     *
     * @param part the part to add
     */
    public void addPart (Part part)
    {
        parts.add(part);
    }

    //---------//
    // addPart //
    //---------//
    /**
     * Add a (real) part to this system at provided index.
     *
     * @param index insertion index
     * @param part  the part to add
     */
    public void addPart (int index,
                         Part part)
    {
        parts.add(index, part);

        // Update MeasureStack's
        for (Measure measure : part.getMeasures()) {
            // Stack O--- Measure
            measure.getStack().addMeasure(index, measure);
        }
    }

    //----------//
    // addStack //
    //----------//
    /**
     * Add stack at end of system stacks
     *
     * @param stack stack to add
     */
    public void addStack (MeasureStack stack)
    {
        stacks.add(stack);
    }

    //----------//
    // addStack //
    //----------//
    /**
     * Add stack at provided index
     *
     * @param index provided index in system stacks
     * @param stack stack to add
     */
    public void addStack (int index,
                          MeasureStack stack)
    {
        stacks.add(index, stack);
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     */
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

            {
                // Support for OldStaffBarline
                // (In part left PartBarline and in measures PartBarlines)
                boolean upgraded = false;

                for (Part part : parts) {
                    final PartBarline lpb = part.getLeftPartBarline();

                    if (lpb != null) {
                        upgraded |= lpb.upgradeOldStuff();
                    }

                    for (Measure measure : part.getMeasures()) {
                        for (PartBarline pb : measure.getContainedPartBarlines()) {
                            upgraded |= pb.upgradeOldStuff();
                        }
                    }
                }

                if (upgraded) {
                    sheet.getStub().setUpgraded(true);
                }
            }

            for (Part part : parts) {
                part.afterReload();
            }

            for (MeasureStack stack : stacks) {
                stack.afterReload(this);
            }

            for (Inter inter : sig.inters(SentenceInter.class)) {
                SentenceInter sentence = (SentenceInter) inter;
                sentence.assignStaff(this, sentence.getLocation());
            }

            // Listen to sig modifications
            sig.addGraphListener(new SigListener(sig));
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
        freeGlyphs = null;
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
        return Integer.compare(id, that.id); // This is a total ordering
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SystemInfo) {
            return compareTo((SystemInfo) obj) == 0;
        }

        return false;
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

    //---------//
    // setArea //
    //---------//
    /**
     * Assign the system area.
     *
     * @param area the underlying system area
     */
    public void setArea (Area area)
    {
        this.area = area;
    }

    //------------//
    // getAreaEnd //
    //------------//
    /**
     * Report area side abscissa.
     *
     * @param side desired side
     * @return abscissa value
     */
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
    // getFirstStack //
    //---------------//
    /**
     * Report the first measure stack in this part.
     *
     * @return the first measure stack
     */
    public MeasureStack getFirstStack ()
    {
        if (stacks.isEmpty()) {
            return null;
        }

        return stacks.get(0);
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
    public List<Glyph> getGroupedGlyphs (GlyphGroup group)
    {
        List<Glyph> found = new ArrayList<>();

        if (freeGlyphs != null) {
            for (Glyph glyph : freeGlyphs) {
                if (glyph.hasGroup(group)) {
                    found.add(glyph);
                }
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

    //-------//
    // setId //
    //-------//
    /**
     * Assign a new ID to this system.
     *
     * @param id the new ID value
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //----------------//
    // getIndexInPage //
    //----------------//
    /**
     * Report 0-based index of this system within containing page.
     *
     * @return index in page
     */
    public int getIndexInPage ()
    {
        return getPage().getSystems().indexOf(this);
    }

    //--------------//
    // getLastStack //
    //--------------//
    /**
     * Report the last measure stack in this part.
     *
     * @return the last measure stack
     */
    public MeasureStack getLastStack ()
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

    //---------------//
    // getLyricLines //
    //---------------//
    /**
     * Report all the lyric lines in this system, sorted by ordinate
     *
     * @return the sequence of lyric lines in system
     */
    public List<LyricLineInter> getLyricLines ()
    {
        if (sig == null) {
            return Collections.emptyList();
        }

        List<Inter> lyricInters = sig.inters(LyricLineInter.class);

        if (lyricInters.isEmpty()) {
            return Collections.emptyList();
        }

        List<LyricLineInter> lines = new ArrayList<>();

        for (Inter inter : lyricInters) {
            lines.add((LyricLineInter) inter);
        }

        Collections.sort(lines, SentenceInter.byOrdinate);

        return lines;
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
     * @return the proper note position (staff and pitch) or null
     */
    public NotePosition getNoteStaffAt (Point point)
    {
        Staff staff = getClosestStaff(point);

        if (staff == null) {
            return null;
        }

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
                            otherPos.getPitchPosition() - Staff.getLedgerPitchPosition(
                            otherPos.getLedger().index));

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

    //---------//
    // setPage //
    //---------//
    /**
     * Assign the containing page.
     *
     * @param page the containing page
     */
    public void setPage (Page page)
    {
        this.page = page;
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
            return staff.getPart();
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

    //------------//
    // getProfile //
    //------------//
    /**
     * Convenient method to report the sheet processing profile based on poor switch.
     *
     * @return sheet processing profile
     */
    public int getProfile ()
    {
        return getSheet().getStub().getProfile();
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

    //------------//
    // getStackAt //
    //------------//
    /**
     * Report the measure stack that contains the provided point.
     *
     * @param point the provided point
     * @return the containing measure stack or null if none
     */
    public MeasureStack getStackAt (Point2D point)
    {
        if (point != null) {
            final List<Staff> stavesAround = getStavesAround(point);

            if (!stavesAround.isEmpty()) {
                final Staff staff = stavesAround.get(0);
                final double x = point.getX();

                for (MeasureStack stack : stacks) {
                    final Measure measure = stack.getMeasureAt(staff);

                    if ((measure != null)
                                && (x >= measure.getAbscissa(LEFT, staff))
                                && (x <= measure.getAbscissa(RIGHT, staff))) {
                        return stack;
                    }
                }
            }
        }

        return null;
    }

    //-----------//
    // getStacks //
    //-----------//
    /**
     * @return a unmodifiable view on stacks
     */
    public List<MeasureStack> getStacks ()
    {
        return Collections.unmodifiableList(stacks);
    }

    //-------------------//
    // getStaffAtOrAbove //
    //-------------------//
    /**
     * Report the staff (within this system) which either embraces or is above the
     * provided point.
     *
     * @param point provided point
     * @return staff here or above (within system), null otherwise
     */
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
    /**
     * Report the staff (within this system) which either embraces or is below the
     * provided point.
     *
     * @param point provided point
     * @return staff here or below (within system), null otherwise
     */
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
     * Report the list of (real) staves that compose this system, tablatures included.
     *
     * @return the staves
     */
    public List<Staff> getStaves ()
    {
        return staves;
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * @param staves the range of staves
     */
    public final void setStaves (List<Staff> staves)
    {
        this.staves.clear();

        if (staves != null) {
            this.staves.addAll(staves);

            for (Staff staff : staves) {
                staff.setSystem(this);
            }

            updateCoordinates();
        }
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
     * @param point the provided point
     * @return proper sub-list of staves (top down)
     */
    public List<Staff> getStavesAround (Point2D point)
    {
        final Staff closest = getClosestStaff(point);

        if (closest == null) {
            return Collections.emptyList();
        }

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

    //---------------//
    // getTablatures //
    //---------------//
    /**
     * Report the list of tablature staves.
     *
     * @return the tablatures list, perhaps empty but not null
     */
    public List<Staff> getTablatures ()
    {
        List<Staff> tablatures = null;

        for (Staff staff : staves) {
            if (tablatures == null) {
                tablatures = new ArrayList<>();
            }
            tablatures.add(staff);
        }

        return (tablatures != null) ? tablatures : Collections.emptyList();
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

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (67 * hash) + this.id;

        return hash;
    }

    //------------//
    // isIndented //
    //------------//
    /**
     * Report whether this system is indented, WRT other systems in sheet.
     *
     * @return true if indented
     */
    public boolean isIndented ()
    {
        return indented;
    }

    //-------------//
    // setIndented //
    //-------------//
    /**
     * @param indented the indented to set
     */
    public void setIndented (boolean indented)
    {
        this.indented = indented;
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
    // mergeWithBelow //
    //----------------//
    /**
     * Merge this system with the next one (just below).
     * <p>
     * This feature can be used manually when the left bar connection is too damaged, thus wrongly
     * leading to two separate systems.
     *
     * @return the PageRef removed, if any
     * @see #unmergeWith(SystemInfo, PageRef)
     */
    public PageRef mergeWithBelow ()
    {
        final List<SystemInfo> systems = sheet.getSystems();
        final SystemInfo systemBelow = systems.get(1 + systems.indexOf(this));

        // Remove systemBelow from sheet structure
        PageRef removedPageRef = sheet.getSystemManager().removeSystem(systemBelow);

        // parts
        parts.addAll(systemBelow.parts);

        for (Part partBelow : systemBelow.parts) {
            partBelow.setSystem(this);
            partBelow.setId(1 + parts.indexOf(partBelow));
        }

        // partGroups
        partGroups.addAll(systemBelow.partGroups);

        // freeGlyphs
        if (systemBelow.freeGlyphs != null) {
            for (Glyph glyph : systemBelow.freeGlyphs) {
                addFreeGlyph(glyph);
            }
        }

        // staves
        for (Staff staff : systemBelow.staves) {
            staff.setSystem(this);
        }
        staves.addAll(systemBelow.staves);

        // sections
        hSections.addAll(systemBelow.hSections);
        vSections.addAll(systemBelow.vSections);

        // bottom, deltaY, left, top, width
        updateCoordinates();

        // area, areaLeft, areaRight
        area = null;
        getArea();

        // stacks
        for (int i = 0; i < stacks.size(); i++) {
            MeasureStack stack = stacks.get(i);
            MeasureStack stackBelow = systemBelow.stacks.get(i);
            stack.mergeWithBelow(stackBelow);
        }

        // sig
        sig.includeSig(systemBelow.getSig());

        return removedPageRef;
    }

    //------------------//
    // numberLyricLines //
    //------------------//
    /**
     * Number the system lyric lines per part, above and below.
     */
    public void numberLyricLines ()
    {
        for (Part part : parts) {
            part.sortLyricLines();
        }
    }

    //-------------//
    // numberParts //
    //-------------//
    /**
     * (Re-)assign sequential numbers to parts within this system.
     */
    public void numberParts ()
    {
        int id = 1;

        for (Part part : parts) {
            part.setId(id++);
        }
    }

    //---------------//
    // registerGlyph //
    //---------------//
    /**
     * Make glyph original, registered and included in freeGlyphs.
     *
     * @param glyph the glyph to register
     * @param group group to assign, or null
     * @return the (perhaps new) registered glyph
     */
    public Glyph registerGlyph (Glyph glyph,
                                GlyphGroup group)
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();

        glyph = glyphIndex.registerOriginal(glyph);
        glyph.addGroup(group);
        addFreeGlyph(glyph);

        return glyph;
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
                                GlyphGroup group)
    {
        for (ListIterator<Glyph> li = parts.listIterator(); li.hasNext();) {
            li.set(registerGlyph(li.next(), group));
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
        if (freeGlyphs != null) {
            freeGlyphs.remove(glyph);

            if (freeGlyphs.isEmpty()) {
                freeGlyphs = null;
            }
        }
    }

    //---------------------//
    // removeGroupedGlyphs //
    //---------------------//
    /**
     * Remove all free glyphs that are <b>only</b> assigned the provided group.
     *
     * @param group the group of glyphs to remove
     */
    public void removeGroupedGlyphs (GlyphGroup group)
    {
        if (freeGlyphs != null) {
            for (Iterator<Glyph> it = freeGlyphs.iterator(); it.hasNext();) {
                final Glyph glyph = it.next();
                final EnumSet<GlyphGroup> glyphGroups = glyph.getGroups();

                if ((glyphGroups.size() == 1) && glyphGroups.contains(group)) {
                    it.remove();
                }
            }
        }
    }

    //------------//
    // removePart //
    //------------//
    /**
     * Remove the provided part as well as its measures from system stacks
     *
     * @param part the part to remove
     */
    public void removePart (Part part)
    {
        // Update MeasureStack's
        for (Measure measure : part.getMeasures()) {
            // Stack O--- Measure
            measure.getStack().removeMeasure(measure);
        }

        // System O--- Part
        parts.remove(part);
    }

    //-------------//
    // removeStack //
    //-------------//
    /**
     * Remove the provided stack as well as its measures from system parts.
     *
     * @param stack the stack to remove
     */
    public void removeStack (MeasureStack stack)
    {
        for (Measure measure : stack.getMeasures()) {
            // Part O--- Measure
            measure.getPart().removeMeasure(measure);
        }

        // System O--- Stack
        stacks.remove(stack);
    }

    //------------//
    // setAreaEnd //
    //------------//
    /**
     * Set the abscissa value of the area side
     *
     * @param side desired side
     * @param x    side abscissa value
     */
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
    // unmergeWith //
    //-------------//
    /**
     * Revert the merge of this system with the former system below.
     *
     * @param systemBelow former system below
     * @param pageRef     the removed PageRef, if any
     * @see #mergeWithBelow()
     */
    public void unmergeWith (SystemInfo systemBelow,
                             PageRef pageRef)
    {
        // Re-insert system (and its PageRef if needed)
        sheet.getSystemManager().unremoveSystem(systemBelow, pageRef);

        // parts
        parts.removeAll(systemBelow.parts);

        for (Part part : systemBelow.parts) {
            part.setSystem(systemBelow);
        }

        // partGroups
        partGroups.removeAll(systemBelow.partGroups);

        // freeGlyphs
        if (systemBelow.freeGlyphs != null) {
            freeGlyphs.removeAll(systemBelow.freeGlyphs);

            if (freeGlyphs.isEmpty()) {
                freeGlyphs = null;
            }
        }

        // staves
        staves.removeAll(systemBelow.staves);

        // sections
        hSections.removeAll(systemBelow.hSections);
        vSections.removeAll(systemBelow.vSections);

        // bottom, deltaY, left, top, width
        updateCoordinates();

        // area, areaLeft, areaRight
        area = null;
        getArea();

        // stacks
        for (int i = 0; i < stacks.size(); i++) {
            MeasureStack stack = stacks.get(i);
            MeasureStack stackBelow = systemBelow.stacks.get(i);
            stack.unmergeWith(stackBelow);
        }

        // sig
        sig.excludeSig(systemBelow.getSig());
    }

    //-------------------//
    // updateCoordinates //
    //-------------------//
    /**
     *
     */
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
    //--------------//
    // toLongString //
    //--------------//
    /**
     * Report a readable description.
     *
     * @return a description based on staff indices
     */
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this);
        sb.append(" {");

        if (staves.size() == 1) {
            sb.append("staff:").append(getFirstStaff().getId());
        } else {
            sb.append("staves:");

            for (int i = 0; i < staves.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }

                sb.append(staves.get(i).getId());
            }
        }

        sb.append("}");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "System#" + id;
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
    void initTransients (Sheet sheet,
                         Page page)
    {
        this.sheet = sheet;
        this.page = page;
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
}
