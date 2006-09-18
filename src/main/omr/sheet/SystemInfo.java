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
import omr.stick.StickSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    implements java.io.Serializable
{
    //~ Instance fields --------------------------------------------------------

    // Retrieved bar lines in this system
    private List<Stick>                  bars = new ArrayList<Stick>();

    // Retrieved endings in this system
    private List<Ending>                 endings = new ArrayList<Ending>();

    // Sections
    private transient List<GlyphSection> hSections = new ArrayList<GlyphSection>();

    // Glyphs & Sticks
    private List<Glyph>           glyphs = new ArrayList<Glyph>();
    private transient List<Stick> hSticks = new ArrayList<Stick>();

    // Retrieved ledgers in this system
    private List<Ledger>       ledgers = new ArrayList<Ledger>();

    // Staves of this system
    private List<StaffInfo>    staves;
    private List<GlyphSection> vSections = new ArrayList<GlyphSection>();
    private List<Stick>        vSticks = new ArrayList<Stick>();

    // Related System in Score hierarchy
    private System scoreSystem;

    // SYSTEMS step
    // ============
    //
    // Bottom of system related area
    private int areaBottom = -1;

    // Top of system related area
    private int areaTop = -1;

    // Ordinate of bottom of last staff of the system.
    private int bottom;

    // Delta ordinate between first line of first staff & first line of
    // last staff.
    private int deltaY;

    // Unique Id for this system (in the sheet)
    private int id;

    // Abscissa of beginning of system.
    private int left = -1;

    // Width of widest glyph in this system
    private int maxGlyphWidth = -1;
    private int maxLedgerWidth = -1;

    // NOTA: Following items are listed in the chronological order of their
    // filling/computation step
    //
    // BARS step
    // =========
    //

    // Index of first staff of the system, counted from 0 within all staves of
    // the score
    private int startIdx;

    // Index of last staff of the system, also counted from 0.
    private int stopIdx;

    // Ordinate of top of first staff of the system.
    private int top;

    // Width of the system.
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
     * @param startIdx the index of the starting staff
     * @param stopIdx  the index of the terminating staff
     *
     * @throws omr.ProcessingException
     */
    public SystemInfo (int   id,
                       Sheet sheet,
                       int   startIdx,
                       int   stopIdx)
    {
        this.id = id;
        this.startIdx = startIdx;
        this.stopIdx = stopIdx;

        // Compute size
        staves = new ArrayList<StaffInfo>();
        staves.addAll(sheet.getStaves().subList(startIdx, stopIdx + 1));

        for (StaffInfo set : staves) {
            if (left == -1) {
                left = set.getLeft();
            } else {
                left = Math.min(left, set.getLeft());
            }

            if (width == -1) {
                width = set.getRight() - left + 1;
            } else {
                width = Math.max(width, set.getRight() - left + 1);
            }
        }

        // First staff
        StaffInfo set = staves.get(0);
        LineInfo  line = set.getFirstLine();
        top = line.getLine()
                  .yAt(line.getLeft());

        // Last staff
        set = staves.get(staves.size() - 1);
        line = set.getFirstLine();
        deltaY = line.getLine()
                     .yAt(line.getLeft()) - top;
        line = set.getLastLine();
        bottom = line.getLine()
                     .yAt(line.getLeft());
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

    //---------//
    // getBars //
    //---------//
    /**
     * Report the list of bar lines in this system
     *
     * @return the (abscissa ordered) collection of bar lines
     */
    public List<Stick> getBars ()
    {
        return bars;
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
     * Report the list of glyphs within the system area
     *
     * @return the list of glyphs
     */
    public List<Glyph> getGlyphs ()
    {
        return glyphs;
    }

    //-----------------------//
    // getHorizontalSections //
    //-----------------------//
    /**
     * Report the collection of horizontal sections in the system related area
     *
     * @return the area horizontal sections
     */
    public List<GlyphSection> getHorizontalSections ()
    {
        return hSections;
    }

    //---------------------//
    // getHorizontalSticks //
    //---------------------//
    /**
     * Report the collection of horizontal sticks left over in the system
     * related area
     *
     * @return the area horizontal sticks
     */
    public List<Stick> getHorizontalSticks ()
    {
        return hSticks;
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

    //------------------//
    // getMaxGlyphWidth //
    //------------------//
    /**
     * Report the maximum width of glyphs found within the system
     *
     * @return the maximum width in pixels
     */
    public int getMaxGlyphWidth ()
    {
        if (maxGlyphWidth == -1) {
            for (Glyph glyph : glyphs) {
                maxGlyphWidth = Math.max(
                    maxGlyphWidth,
                    glyph.getContourBox().width);
            }
        }

        return maxGlyphWidth;
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
    // setStaves //
    //-----------//
    /**
     * Assign list of staves that compose this system
     *
     * @param staves the list of staves
     */
    public void setStaves (List<StaffInfo> staves)
    {
        this.staves = staves;
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
    // setStopIdx //
    //------------//
    /**
     * Set the index of the terminating staff of this system
     *
     * @param stopIdx the stopping staff index, counted from 0
     */
    public void setStopIdx (int stopIdx)
    {
        this.stopIdx = stopIdx;
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
     * Report the collection of vertical sections in the system related area
     *
     * @return the area vertical sections
     */
    public List<GlyphSection> getVerticalSections ()
    {
        return vSections;
    }

    //-------------------//
    // getVerticalSticks //
    //-------------------//
    /**
     * Report the collection of vertical sticks left over in the system related
     * area
     *
     * @return the area vertical sticks clutter
     */
    public List<Stick> getVerticalSticks ()
    {
        return vSticks;
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

    //------------//
    // sortGlyphs //
    //------------//
    /**
     * Sort all glyphs in the system, according to the left abscissa of their
     * contour box
     */
    public void sortGlyphs ()
    {
        Collections.sort(
            glyphs,
            new Comparator<Glyph>() {
                    public int compare (Glyph o1,
                                        Glyph o2)
                    {
                        return o1.getContourBox().x - o2.getContourBox().x;
                    }
                });
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
