//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m I n f o                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.glyph.Glyph;
import omr.glyph.GlyphSection;

import omr.score.System;

import omr.stick.Stick;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class <code>SystemInfo</code> gathers information from the original picture
 * about a retrieved system.
 *
 * <p>Nota: All measurements are assumed in pixels.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SystemInfo
{
    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Staves of this system */
    private final List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Parts in this system */
    private final List<PartInfo> parts = new ArrayList<PartInfo>();

    /** Related System in Score hierarchy */
    private System scoreSystem;

    ///   HORIZONTALS   ////////////////////////////////////////////////////////

    /** Retrieved endings in this system */
    private final List<Ending> endings = new ArrayList<Ending>();

    /** Retrieved ledgers in this system */
    private final List<Ledger> ledgers = new ArrayList<Ledger>();

    ///   VERTICALS   //////////////////////////////////////////////////////////

    /** Vertical sections, assigned once for all to this system */
    private final List<GlyphSection> vSections = new ArrayList<GlyphSection>();

    /** Active glyphs in this system, allowing concurrent access */
    private final SortedSet<Glyph> glyphs = new ConcurrentSkipListSet<Glyph>();

    ////////////////////////////////////////////////////////////////////////////

    /** Unique Id for this system (in the sheet) */
    private final int id;

    /** Bottom of system related area */
    private int areaBottom = -1;

    /** Top of system related area */
    private int areaTop = -1;

    /** Ordinate of bottom of last staff of the system. */
    private int bottom;

    /** Delta ordinate between first line of first staff & first line of last
       staff. */
    private int deltaY;

    /** Abscissa of beginning of system. */
    private int left = -1;

    /** Width of widest glyph in this system */
    private int maxGlyphWidth = -1;

    /** Width of widest Ledger in this system */
    private int maxLedgerWidth = -1;

    /** Index of first staff of the system, counted from 0 within all staves of
       the score */
    private int startIdx = -1;

    /** Index of last staff of the system, also counted from 0. */
    private int stopIdx;

    /** Ordinate of top of first staff of the system. */
    private int top = -1;

    /** Width of the system. */
    private int width = -1;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemInfo //
    //------------//
    /**
     * Create a SystemInfo entity, to register the provided parameters
     *
     * @param id       the unique identity
     * @param sheet    the containing sheet
     *
     * @throws omr.ProcessingException
     */
    public SystemInfo (int   id,
                       Sheet sheet)
    {
        this.id = id;
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // setAreaBottom //
    //---------------//
    /**
     * Set the ordinate of bottom of system area
     *
     * @param areaBottom ordinate of bottom of system area in pixels
     */
    public void setAreaBottom (int areaBottom)
    {
        this.areaBottom = areaBottom;
    }

    //---------------//
    // getAreaBottom //
    //---------------//
    /**
     * Report the ordinate of the bottom of the picture area whose all items are
     * assumed to belong to this system (the system related area)
     *
     * @return the related area bottom ordinate (in pixels)
     */
    public int getAreaBottom ()
    {
        return areaBottom;
    }

    //------------//
    // setAreaTop //
    //------------//
    /**
     * Set the ordinate of top of systemp area
     *
     * @param areaTop ordinate of top of system area in pixels
     */
    public void setAreaTop (int areaTop)
    {
        this.areaTop = areaTop;
    }

    //------------//
    // getAreaTop //
    //------------//
    /**
     * Report the ordinate of the top of the related picture area
     *
     * @return the related area top ordinate (in pixels)
     */
    public int getAreaTop ()
    {
        return areaTop;
    }

    //-----------//
    // getBottom //
    //-----------//
    /**
     * Report the ordinate of the bottom of the system, which is the ordinate of
     * the last line of the last staff of this system
     *
     * @return the system bottom, in pixels
     */
    public int getBottom ()
    {
        return bottom;
    }

    //-----------//
    // getDeltaY //
    //-----------//
    /**
     * Report the deltaY of the system, that is the difference in ordinate
     * between first and last staves of the system. This deltaY is of course 0
     * for a one-staff system.
     *
     * @return the deltaY value, expressed in pixels
     */
    public int getDeltaY ()
    {
        return deltaY;
    }

    //------------//
    // getEndings //
    //------------//
    /**
     * Report the collection of endings found
     *
     * @return the endings collection
     */
    public List<Ending> getEndings ()
    {
        return endings;
    }

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the unmodifiable collection of glyphs within the system area
     *
     * @return the unmodifiable collection of glyphs
     */
    public Collection<Glyph> getGlyphs ()
    {
        return Collections.unmodifiableCollection(glyphs);
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id (debugging info) of the system info
     *
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //------------//
    // getLedgers //
    //------------//
    /**
     * Report the collection of ledgers found
     *
     * @return the ledger collection
     */
    public List<Ledger> getLedgers ()
    {
        return ledgers;
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the left abscissa
     *
     * @return the left abscissa value, expressed in pixels
     */
    public int getLeft ()
    {
        return left;
    }

    //-------------------//
    // getMaxLedgerWidth //
    //-------------------//
    /**
     * Report the maximum width of ledgers within the system
     *
     * @return the maximum width in pixels
     */
    public int getMaxLedgerWidth ()
    {
        if (maxLedgerWidth == -1) {
            for (Ledger ledger : ledgers) {
                maxLedgerWidth = Math.max(
                    maxLedgerWidth,
                    ledger.getContourBox().width);
            }
        }

        return maxLedgerWidth;
    }

    //---------------------//
    // getModifiableGlyphs //
    //---------------------//
    /**
     * Report the modifiable collection of glyphs within the system area
     *
     * @return the modifiable collection of glyphs
     */
    public Collection<Glyph> getModifiableGlyphs ()
    {
        return glyphs;
    }

    //-------------------------------//
    // getModifiableVerticalSections //
    //-------------------------------//
    /**
     * Report the (modifiable)collection of vertical sections in the system
     * related area
     *
     * @return the area vertical sections
     */
    public Collection<GlyphSection> getModifiableVerticalSections ()
    {
        return vSections;
    }

    //----------//
    // getParts //
    //----------//
    public List<PartInfo> getParts ()
    {
        return parts;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * Report the abscissa of the end of the system
     *
     * @return the right abscissa, expressed in pixels
     */
    public int getRight ()
    {
        return left + width;
    }

    //----------------//
    // setScoreSystem //
    //----------------//
    /**
     * Set the link : physical sheet.SystemInfo -> logical score.System
     *
     * @param scoreSystem the logical score System counterpart
     */
    public void setScoreSystem (System scoreSystem)
    {
        this.scoreSystem = scoreSystem;
    }

    //----------------//
    // getScoreSystem //
    //----------------//
    /**
     * Report the related logical score system
     *
     * @return the logical score System counterpart
     */
    public System getScoreSystem ()
    {
        return scoreSystem;
    }

    //-------------//
    // getStaffAtY //
    //-------------//
    /**
     * Given an ordinate value, retrieve the closest staff within the system
     *
     * @param y the ordinate value
     * @return the "containing" staff
     */
    public StaffInfo getStaffAtY (int y)
    {
        for (StaffInfo staff : staves) {
            if (y <= staff.getAreaBottom()) {
                return staff;
            }
        }

        // Return the last staff
        return staves.get(staves.size() - 1);
    }

    //-------------//
    // setStartIdx //
    //-------------//
    /**
     * Set the index of the starting staff of this system
     *
     * @param startIdx the staff index, counted from 0
     */
    public void setStartIdx (int startIdx)
    {
        this.startIdx = startIdx;
    }

    //-------------//
    // getStartIdx //
    //-------------//
    /**
     * Report the index of the starting staff of this system
     *
     * @return the staff index, counted from 0
     */
    public int getStartIdx ()
    {
        return startIdx;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the list of staves that compose this system
     *
     * @return the staves
     */
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //------------//
    // getStopIdx //
    //------------//
    /**
     * Report the index of the terminating staff of this system
     *
     * @return the stopping staff index, counted from 0
     */
    public int getStopIdx ()
    {
        return stopIdx;
    }

    //--------//
    // getTop //
    //--------//
    /**
     * Report the ordinate of the top of this system
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
     * Report the (unmodifiable) collection of vertical sections in the system
     * related area
     *
     * @return the area vertical sections
     */
    public Collection<GlyphSection> getVerticalSections ()
    {
        return Collections.unmodifiableCollection(vSections);
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the system
     *
     * @return the width value, expressed in pixels
     */
    public int getWidth ()
    {
        return width;
    }

    //----------//
    // addGlyph //
    //----------//
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
    }

    //---------//
    // addPart //
    //---------//
    public void addPart (PartInfo partInfo)
    {
        parts.add(partInfo);
    }

    //----------//
    // addStaff //
    //----------//
    public void addStaff (int idx)
    {
        StaffInfo staff = sheet.getStaves()
                               .get(idx);
        LineInfo  firstLine = staff.getFirstLine();
        staves.add(staff);

        // Remember left side
        if (left == -1) {
            left = staff.getLeft();
        } else {
            left = Math.min(left, staff.getLeft());
        }

        // Remember width
        if (width == -1) {
            width = staff.getRight() - left + 1;
        } else {
            width = Math.max(width, staff.getRight() - left + 1);
        }

        // First staff ?
        if (startIdx == -1) {
            startIdx = idx;
            top = firstLine.getLine()
                           .yAt(firstLine.getLeft());
        }

        // Last staff (so far)
        stopIdx = idx;
        deltaY = firstLine.getLine()
                          .yAt(firstLine.getLeft()) - top;

        LineInfo lastLine = staff.getLastLine();
        bottom = lastLine.getLine()
                         .yAt(lastLine.getLeft());
    }

    //------------//
    // dumpGlyphs //
    //------------//
    /**
     * Dump all glyphs handled by this system
     */
    public void dumpGlyphs ()
    {
        dumpGlyphs(null);
    }

    //------------//
    // dumpGlyphs //
    //------------//
    /**
     * Dump the glyphs handled by this system and that are contained by the
     * provided rectangle
     *
     * @param rect the region of interest
     */
    public void dumpGlyphs (PixelRectangle rect)
    {
        for (Glyph glyph : getGlyphs()) {
            if ((rect == null) || (rect.contains(glyph.getContourBox()))) {
                java.lang.System.out.println(
                    (glyph.isActive() ? "active " : "       ") +
                    (glyph.isKnown() ? "known " : "      ") +
                    (glyph.isWellKnown() ? "wellKnown " : "          ") +
                    glyph.toString());
            }
        }
    }

    //--------------//
    // dumpSections //
    //--------------//
    /**
     * Dump all (vertical) sections handled by this system
     */
    public void dumpSections ()
    {
        dumpSections(null);
    }

    //--------------//
    // dumpSections //
    //--------------//
    /**
     * Dump the (vertical) sections handled by this system and that are
     * contained by the provided rectangle
     *
     * @param rect the region of interest
     */
    public void dumpSections (PixelRectangle rect)
    {
        for (GlyphSection section : getVerticalSections()) {
            if ((rect == null) || (rect.contains(section.getContourBox()))) {
                java.lang.System.out.println(
                    (section.isKnown() ? "known " : "      ") +
                    section.toString());
            }
        }
    }

    //-------------------------//
    // lookupIntersectedGlyphs //
    //-------------------------//
    /**
     * Look up in system glyphs for <b>all</b> glyphs, apart from the excluded
     * glyph, intersected by a provided rectangle
     *
     * @param rect the coordinates rectangle, in pixels
     * @param excluded the glyph to be excluded
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupIntersectedGlyphs (PixelRectangle rect,
                                                Glyph          excluded)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        // System glyphs are kept sorted on abscissa then ordinate of topLeft
        for (Glyph glyph : getGlyphs()) {
            if (glyph != excluded) {
                if (rect.intersects(glyph.getContourBox())) {
                    found.add(glyph);
                }
            }
        }

        return found;
    }

    //-------------------------//
    // lookupIntersectedGlyphs //
    //-------------------------//
    /**
     * Look up in system glyphs for <b>all</b> glyphs intersected by a
     * provided rectangle
     *
     * @param rect the coordinates rectangle, in pixels
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupIntersectedGlyphs (PixelRectangle rect)
    {
        return lookupIntersectedGlyphs(rect, null);
    }

    //-------------//
    // removeGlyph //
    //-------------//
    public boolean removeGlyph (Glyph glyph)
    {
        return glyphs.remove(glyph);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a description based on staff indices
     */
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(80);
        sb.append("{SystemInfo#")
          .append(id);
        sb.append(" ")
          .append(startIdx);

        if (startIdx != stopIdx) {
            sb.append("..")
              .append(stopIdx);
        }

        sb.append("}");

        return sb.toString();
    }
}
