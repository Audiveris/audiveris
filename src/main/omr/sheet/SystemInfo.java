//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m I n f o                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.glyph.Glyph;
import omr.glyph.GlyphSection;
import omr.glyph.TextArea;
import omr.glyph.TextGlyphLine;

import omr.lag.HorizontalOrientation;

import omr.score.common.PixelRectangle;
import omr.score.entity.System;

import omr.util.Boundary;
import omr.util.Logger;

import java.awt.Rectangle;
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
    implements Comparable<SystemInfo>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemInfo.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Related System in Score hierarchy */
    private System scoreSystem;

    /** Staves of this system */
    private final List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Parts in this system */
    private final List<PartInfo> parts = new ArrayList<PartInfo>();

    ///   HORIZONTALS   ////////////////////////////////////////////////////////

    /** Retrieved endings in this system */
    private final List<Ending> endings = new ArrayList<Ending>();

    /** Retrieved ledgers in this system */
    private final List<Ledger> ledgers = new ArrayList<Ledger>();

    ///   VERTICALS   //////////////////////////////////////////////////////////

    /** Vertical sections, assigned once for all to this system */
    private final List<GlyphSection> vSections = new ArrayList<GlyphSection>();

    /** Unmodifiable view of the vertical section collection */
    private final Collection<GlyphSection> vSectionsView = Collections.unmodifiableCollection(
        vSections);

    /** Active glyphs in this system */
    private final SortedSet<Glyph> glyphs = new ConcurrentSkipListSet<Glyph>();

    /** Unmodifiable view of the glyphs collection */
    private final Collection<Glyph> glyphsView = Collections.unmodifiableCollection(
        glyphs);

    /** Ordered collection of lines of text glyphs */
    private SortedSet<TextGlyphLine> textLines = new TreeSet<TextGlyphLine>();

    ////////////////////////////////////////////////////////////////////////////

    /** Unique Id for this system (in the sheet) */
    private final int id;

    /** Boundary that encloses all items of this system */
    private Boundary boundary;

    /** Ordinate of bottom of last staff of the system. */
    private int bottom;

    /** Delta ordinate between first line of first staff & first line of last
       staff. */
    private int deltaY;

    /** Abscissa of beginning of system. */
    private int left = -1;

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
     */
    public SystemInfo (int   id,
                       Sheet sheet)
    {
        this.id = id;
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

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

    //-------------//
    // setBoundary //
    //-------------//
    /**
     * Define the precise boundary of this system
     * @param boundary the (new) boundary
     */
    public void setBoundary (Boundary boundary)
    {
        this.boundary = boundary;
    }

    //-------------//
    // getBoundary //
    //-------------//
    /**
     * Report the precise boundary of this system
     * @return the precise polygon boundary
     */
    public Boundary getBoundary ()
    {
        return boundary;
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the rectangular bounds that enclose this system
     * @return the system rectangular bounds
     */
    public PixelRectangle getBounds ()
    {
        return new PixelRectangle(boundary.getBounds());
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
        return glyphsView;
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

    //----------------------------//
    // getMutableVerticalSections //
    //----------------------------//
    /**
     * Report the (modifiable) collection of vertical sections in the system
     * related area
     *
     * @return the area vertical sections
     */
    public Collection<GlyphSection> getMutableVerticalSections ()
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
        return vSectionsView;
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

    //-------------//
    // clearGlyphs //
    //-------------//
    public void clearGlyphs ()
    {
        glyphs.clear();
    }

    //-----------//
    // compareTo //
    //-----------//
    public int compareTo (SystemInfo o)
    {
        return Integer.signum(id - o.id);
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
    @Override
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

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the system
     * collection
     *
     * @param systems the collection of glysystemsphs
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
            sb.append("#")
              .append(system.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // getSheet //
    //----------//
    public Sheet getSheet ()
    {
        return sheet;
    }

    //--------------//
    // getTextLines //
    //--------------//
    /**
     * Report the various lines of text retrieved in this system, ordered by
     * their ordinate. Each such line contains one or more sentences that are
     * sequences of text glyphs.
     * @return the (perhaps empty) ordered collection of text lines found
     */
    public SortedSet<TextGlyphLine> getTextLines ()
    {
        return textLines;
    }

    //-----------------//
    // alignTextGlyphs //
    //-----------------//
    /**
     * Align the various text glyphs in horizontal text lines
     */
    public void alignTextGlyphs ()
    {
        try {
            // Keep the previous work! No textLines.clear();
            for (Glyph glyph : getGlyphs()) {
                if ((glyph.getShape() != null) && glyph.getShape()
                                                       .isText()) {
                    TextGlyphLine.feed(glyph, this, textLines);
                }
            }

            // (Re)assign an id to each line
            if (logger.isFineEnabled()) {
                logger.fine("System#" + id);
            }

            int index = 0;

            for (TextGlyphLine line : textLines) {
                line.setId(++index);

                if (logger.isFineEnabled()) {
                    logger.fine(line.toString());
                }

                line.processGlyphs();
            }
        } catch (Error error) {
            logger.warning("Error in TextArea.alignTexts: " + error);
        } catch (Exception ex) {
            logger.warning("Exception in TextArea.alignTexts", ex);
        }
    }

    //--------------------//
    // retrieveTextGlyphs //
    //--------------------//
    /**
     * Retrieve the various glyphs and series of glyphs that could represent
     * text portions in the system at hand
     */
    public void retrieveTextGlyphs ()
    {
        TextArea area = new TextArea(
            null,
            sheet.getVerticalLag().createAbsoluteRoi(getBounds()),
            new HorizontalOrientation());

        // Subdivide the area, to find and build text glyphs (words most likely)
        area.subdivide(sheet);

        // Process alignments of text items
        alignTextGlyphs();
    }
}
