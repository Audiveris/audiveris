//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y s t e m I n f o                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.CompoundBuilder;
import omr.glyph.CompoundBuilder.CompoundAdapter;
import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.score.LogicalPart;
import omr.score.Page;
import omr.score.Score;
import omr.score.StaffPosition;

import omr.sheet.grid.LineInfo;
import omr.sheet.grid.PartGroup;
import omr.sheet.note.NotePosition;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;

import omr.sig.SIGraph;

import omr.text.TextBuilder;
import omr.text.TextLine;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class {@code SystemInfo} gathers information from the original picture about a
 * retrieved system.
 * <p>
 * Most of the OMR processing is done in parallel at system level.
 * <p>
 * This class is named {@code SystemInfo} to avoid name clash with ubiquitous
 * {@code java.lang.System} class.
 *
 * @author Hervé Bitteur
 */
public class SystemInfo
        implements Comparable<SystemInfo>
{
    //~ Static fields/initializers -----------------------------------------------------------------

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

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Symbol Interpretation Graph for this system. */
    private final SIGraph sig;

    /** Assigned page, if any. */
    private Page page;

    /** Dedicated text builder */
    private final TextBuilder textBuilder;

    /** Dedicated compound builder */
    private final CompoundBuilder compoundBuilder;

    /** Real staves of this system (no dummy staves included). */
    private List<Staff> staves = new ArrayList<Staff>();

    /** Real parts in this system (no dummy parts included). */
    private final List<Part> parts = new ArrayList<Part>();

    /** Measure stacks in this system. */
    private final List<MeasureStack> stacks = new ArrayList<MeasureStack>();

    /** PartGroups in this system */
    private final List<PartGroup> partGroups = new ArrayList<PartGroup>();

    /** Horizontal sections. */
    private final List<Section> hSections = new ArrayList<Section>();

    private final List<Section> ledgerSections = new ArrayList<Section>();

    /** Vertical sections. */
    private final List<Section> vSections = new ArrayList<Section>();

    /** Collection of (active?) glyphs in this system */
    private final SortedSet<Glyph> glyphs = new ConcurrentSkipListSet<Glyph>(Glyph.byAbscissa);

    /** Unmodifiable view of the glyphs collection */
    private final SortedSet<Glyph> glyphsView = Collections.unmodifiableSortedSet(glyphs);

    /** Set of text lines. */
    private final Set<TextLine> textLines = new LinkedHashSet<TextLine>();

    /** Unique Id (sequential vertical number starting from 1 in containing sheet). */
    private final int id;

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

    /** Indentation flag. */
    private boolean indented;

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
        textBuilder = new TextBuilder(this);
        compoundBuilder = new CompoundBuilder(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //-----------------------//
    // addToGlyphsCollection //
    //-----------------------//
    /**
     * This is a private entry meant for SystemGlyphsBuilder only.
     * The standard entry is {@link #registerGlyph}
     *
     * @param glyph the glyph to add to the system glyph collection
     */
    public void addToGlyphsCollection (Glyph glyph)
    {
        glyphs.add(glyph);
    }

    //---------------//
    // buildCompound //
    //---------------//
    public Glyph buildCompound (Glyph seed,
                                boolean includeSeed,
                                Collection<Glyph> suitables,
                                CompoundAdapter adapter)
    {
        return compoundBuilder.buildCompound(seed, includeSeed, suitables, adapter);
    }

    //-------------//
    // clearGlyphs //
    //-------------//
    /**
     * Empty the system glyph collection.
     */
    public void clearGlyphs ()
    {
        glyphs.clear();
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

    //-------------------------//
    // connectPageInitialSlurs //
    //-------------------------//
    /**
     * For this system, retrieve the connections between the (orphan) slurs at the
     * beginning of this page and the (orphan) slurs at the end of the previous page.
     *
     * @param score the containing score
     */
    public void connectPageInitialSlurs (Score score)
    {
        // Containing page
        final Page page = getPage();

        // Safer: check we are the very first system in page
        if (page.getFirstSystem() != this) {
            throw new IllegalArgumentException(
                    "connectPageInitialSlurs called for non-first system");
        }

        // If very first page, we are done
        if (score.getFirstPage() == page) {
            return;
        }

        SystemInfo precedingSystem = page.getPrecedingInScore(score).getLastSystem();

        if (precedingSystem != null) {
            // Examine every part in sequence
            for (int index = 0; index < parts.size(); index++) {
                final Part part = parts.get(index);

                // Find out the proper preceding part (across pages)
                Part precedingPart = precedingSystem.getParts().get(index);

                // Ending orphans in preceding system/part (if such part exists)
                part.connectSlursWith(precedingPart);
            }
        }
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
        if (area != null) {
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

    //--------------------//
    // getCompoundBuilder //
    //--------------------//
    /**
     * @return the compoundBuilder
     */
    public CompoundBuilder getCompoundBuilder ()
    {
        return compoundBuilder;
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
        final Page page = getPage();

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

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the unmodifiable collection of glyphs within the system
     * area.
     *
     * @return the unmodifiable collection of glyphs
     */
    public SortedSet<Glyph> getGlyphs ()
    {
        return glyphsView;
    }

    //-----------------------//
    // getHorizontalSections //
    //-----------------------//
    /**
     * Report the (unmodifiable) collection of horizontal sections in
     * the system related area.
     *
     * @return the area horizontal sections
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

    //-------------------//
    // getLedgerSections //
    //-------------------//
    /**
     * Report the (unmodifiable) collection of ledger sections in system related area.
     *
     * @return the area ledger sections
     */
    public List<Section> getLedgerSections ()
    {
        return Collections.unmodifiableList(ledgerSections);
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
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());

        if (sb.length() > 1) {
            sb.insert(sb.length() - 2, "-S" + id);
        } else {
            sb.append("S").append(id).append(" ");
        }

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

    //--------------------------//
    // getMutableLedgerSections //
    //--------------------------//
    /**
     * Report the (modifiable) collection of horizontal ledger sections
     * in the system related area.
     *
     * @return the ledger sections
     */
    public Collection<Section> getMutableLedgerSections ()
    {
        return ledgerSections;
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

    //--------------//
    // getPartAbove //
    //--------------//
    /**
     * Determine the (real) part which is above the given point.
     *
     * @param point the given point
     * @return the part above
     */
    public Part getPartAbove (Point point)
    {
        Staff staff = getStaffAbove(point);

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
        final Page page = getPage();

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

    //---------------//
    // getStaffAbove //
    //---------------//
    /**
     * Determine the (real) staff which is just above the given point.
     *
     * @param point the given point
     * @return the staff above
     */
    // Plain wrong!
    @Deprecated
    public Staff getStaffAbove (Point point)
    {
        Staff above = null;

        for (Staff staff : staves) {
            if (staff.getAreaBounds().getCenterY() <= point.y) {
                above = staff;
            } else {
                break;
            }
        }

        return above;
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

    //---------------//
    // getStaffBelow //
    //---------------//
    /**
     * Determine the (real) staff which is just below the given point.
     *
     * @param point the given point
     * @return the staff below
     */
    // Plain wrong!
    @Deprecated
    public Staff getStaffBelow (Point point)
    {
        for (Staff staff : staves) {
            if (staff.getAreaBounds().getCenterY() > point.y) {
                return staff;
            }
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
     * @return proper sublist of staves
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
        } else {
            // Point is below staff, add staff below if any
            if (last < (staves.size() - 1)) {
                last++;
            }
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
        return StaffManager.getStavesOf(point, staves, null);
    }

    //----------------//
    // getTextBuilder //
    //----------------//
    public TextBuilder getTextBuilder ()
    {
        return textBuilder;
    }

    //--------------//
    // getTextLines //
    //--------------//
    /**
     * Report the various text lines retrieved in this system.
     *
     * @return the (perhaps empty) collection of text lines found
     */
    public Set<TextLine> getTextLines ()
    {
        return textLines;
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
     * Report the (unmodifiable) collection of vertical sections in
     * the system related area.
     *
     * @return the area vertical sections
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
    // idString //
    //----------//
    /**
     * Convenient way to report a small system reference.
     *
     * @return system reference
     */
    public String idString ()
    {
        return "system#" + id;
    }

    //------------//
    // isIndented //
    //------------//
    /**
     * @return the indented
     */
    public boolean isIndented ()
    {
        return indented;
    }

    //-----------------------//
    // lookupContainedGlyphs //
    //-----------------------//
    /**
     * Look up in system glyphs for the glyphs contained by a
     * provided rectangle.
     *
     * @param rect the coordinates rectangle, in pixels
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupContainedGlyphs (Rectangle rect)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : getGlyphs()) {
            if (rect.contains(glyph.getBounds())) {
                found.add(glyph);
            }
        }

        return found;
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    /**
     * Look up in system glyphs for <b>all</b> glyphs, apart from the
     * excluded glyphs, intersected by a provided rectangle.
     *
     * @param rect     the coordinates rectangle, in pixels
     * @param excluded the glyphs to be excluded
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupIntersectedGlyphs (Rectangle rect,
                                                Glyph... excluded)
    {
        List<Glyph> exc = Arrays.asList(excluded);
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : getGlyphs()) {
            if (!exc.contains(glyph) && glyph.intersects(rect)) {
                found.add(glyph);
            }
        }

        return found;
    }

    //--------------------//
    // lookupShapedGlyphs //
    //--------------------//
    /**
     * Look up in system glyphs for those whose shape is the desired one.
     *
     * @param shape the desired shape
     * @return the glyphs found
     */
    public List<Glyph> lookupShapedGlyphs (Shape shape)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : getGlyphs()) {
            if (glyph.getShape() == shape) {
                found.add(glyph);
            }
        }

        return found;
    }

    //---------------//
    // registerGlyph //
    //---------------//
    /**
     * Register a brand new glyph in proper system (and nest).
     * <p>
     * <b>Note</b>: The caller must use the returned glyph since it may be
     * different from the provided glyph (this happens when an original glyph
     * with same signature existed before this one)
     *
     * @param glyph the brand new glyph
     * @return the original glyph as inserted in the glyph nest.
     *         Use the returned entity instead of the provided one.
     */
    public Glyph registerGlyph (Glyph glyph)
    {
        glyph = sheet.getGlyphNest().registerGlyph(glyph);
        glyphs.add(glyph);

        return glyph;
    }

    //----------------------------//
    // removeFromGlyphsCollection //
    //----------------------------//
    /**
     * Meant for access by SystemGlyphsBuilder only,
     * since standard entry is {@link #removeGlyph}.
     *
     * @param glyph the glyph to remove
     * @return true if the glyph was registered
     */
    public boolean removeFromGlyphsCollection (Glyph glyph)
    {
        return glyphs.remove(glyph);
    }

    //-------------//
    // removeGlyph //
    //-------------//
    /**
     * Remove a glyph from the containing system glyph list, and make
     * it inactive by cutting the link from its member sections.
     *
     * @param glyph the glyph to remove
     */
    public void removeGlyph (Glyph glyph)
    {
        glyphs.remove(glyph);

        // Cut link from its member sections, if pointing to this glyph
        glyph.cutSections();
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * In a given system area, browse through all sections not assigned
     * to known glyphs, and build new glyphs out of connected sections.
     */
    public void retrieveGlyphs ()
    {
        // Consider all unknown vertical & horizontal sections
        List<Section> allSections = new ArrayList<Section>();
        allSections.addAll(getVerticalSections());
        allSections.addAll(getHorizontalSections());

        final GlyphNest nest = sheet.getGlyphNest();
        List<Glyph> newGlyphs = nest.retrieveGlyphs(allSections, GlyphLayer.DEFAULT, false);

        // Record them into the system
        glyphs.addAll(newGlyphs);
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
        this.indented = indented;
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
        sb.append("{System#").append(id);
        sb.append(" T").append(getFirstStaff().getId());

        if (staves.size() > 1) {
            sb.append("..T").append(getLastStaff().getId());
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------------//
    // updateCoordinates //
    //-------------------//
    public final void updateCoordinates ()
    {
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
}
