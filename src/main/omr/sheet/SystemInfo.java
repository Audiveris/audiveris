//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m I n f o                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.check.CheckSuite;

import omr.glyph.CompoundBuilder;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.GlyphsBuilder;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;
import omr.glyph.pattern.PatternsChecker;
import omr.glyph.pattern.SlurInspector;
import omr.glyph.text.Sentence;
import omr.glyph.text.SentencePattern;

import omr.log.Logger;

import omr.score.SystemTranslator;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.step.StepException;

import omr.util.Predicate;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class <code>SystemInfo</code> gathers information from the original picture
 * about a retrieved system. Most of the physical processing is done in parallel
 * at system level, and thus is handled from this SystemInfo object.
 *
 * <p>Many processing tasks are actually handled by companion classes, but
 * this SystemInfo is the interface of choice, with delegation to the proper
 * companion (such as {@link GlyphsBuilder}, {@link GlyphInspector},
 * {@link SlurInspector}, {@link SentencePattern}, {@link SystemTranslator}, etc)
 *
 * <p>Nota: All measurements are assumed in pixels.
 *
 * @author Hervé Bitteur
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

    /** Dedicated measure builder */
    private final MeasuresBuilder measuresBuilder;

    /** Dedicated glyph builder */
    private final GlyphsBuilder glyphsBuilder;

    /** Dedicated compound builder */
    private final CompoundBuilder compoundBuilder;

    /** Dedicated verticals builder */
    private final VerticalsBuilder verticalsBuilder;

    /** Dedicated glyph inspector */
    private final GlyphInspector glyphInspector;

    /** Dedicated slur inspector */
    private final SlurInspector slurInspector;

    /** Dedicated system translator */
    private final SystemTranslator translator;

    /** Staves of this system */
    private final List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Parts in this system */
    private final List<PartInfo> parts = new ArrayList<PartInfo>();

    /** Related System in Score hierarchy */
    private ScoreSystem scoreSystem;

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

    /** Collection of (active?) glyphs in this system */
    private final SortedSet<Glyph> glyphs = new ConcurrentSkipListSet<Glyph>(
        Glyph.globalComparator);

    /** Unmodifiable view of the glyphs collection */
    private final SortedSet<Glyph> glyphsView = Collections.unmodifiableSortedSet(
        glyphs);

    /** Set of sentences made of text glyphs */
    private Set<Sentence> sentences = new LinkedHashSet<Sentence>();

    /** Used to assign a unique ID to system sentences */
    private int sentenceCount = 0;

    ////////////////////////////////////////////////////////////////////////////

    /** Unique Id for this system (in the sheet) */
    private final int id;

    /** Boundary that encloses all items of this system */
    private SystemBoundary boundary;

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

        measuresBuilder = new MeasuresBuilder(this);
        glyphsBuilder = new GlyphsBuilder(this);
        compoundBuilder = new CompoundBuilder(this);
        verticalsBuilder = new VerticalsBuilder(this);
        glyphInspector = new GlyphInspector(this);
        slurInspector = new SlurInspector(this);
        translator = new SystemTranslator(this);
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
    public void setBoundary (SystemBoundary boundary)
    {
        this.boundary = boundary;
    }

    //-------------//
    // getBoundary //
    //-------------//
    /**
     * Report the precise boundary of this system
     * @return the precise system boundary
     */
    public SystemBoundary getBoundary ()
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
        if (boundary != null) {
            return new PixelRectangle(boundary.getBounds());
        } else {
            return null;
        }
    }

    //--------------------//
    // getCompoundBuilder //
    //--------------------//
    public CompoundBuilder getCompoundBuilder ()
    {
        return compoundBuilder;
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
    public SortedSet<Glyph> getGlyphs ()
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

    //--------------//
    // getLogPrefix //
    //--------------//
    /**
     * Report the proper prefix to use when logging a message
     * @return the proper prefix
     */
    public String getLogPrefix ()
    {
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());

        if (sb.length() > 1) {
            sb.insert(sb.length() - 1, "S" + id);
        } else {
            sb.append("S")
              .append(id)
              .append(" ");
        }

        return sb.toString();
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

    //------------------//
    // getNewSentenceId //
    //------------------//
    /**
     * Report the id for a new sentence
     * @return the next id
     */
    public int getNewSentenceId ()
    {
        return ++sentenceCount;
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Reports the parts of this system
     * @return the parts (non-null)
     */
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
    // getScoreSystem //
    //----------------//
    /**
     * Report the related logical score system
     *
     * @return the logical score System counterpart
     */
    public ScoreSystem getScoreSystem ()
    {
        return scoreSystem;
    }

    //------------------//
    // getSlurInspector //
    //------------------//
    public SlurInspector getSlurInspector ()
    {
        return slurInspector;
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
    /**
     * Add a brand new glyph as an active glyph in proper system and lag.
     * If the glyph is a compound, its parts are made pointing back to it and
     * are made no longer active glyphs.
     *
     * <p><b>Note</b>: The caller must use the returned glyph since it may be
     * different from the provided glyph (this happens when an original glyph
     * with same signature existed before this one)
     *
     * @param glyph the brand new glyph
     * @return the original glyph as inserted in the glyph lag. Use this entity
     * instead of the provided one.
     */
    public Glyph addGlyph (Glyph glyph)
    {
        return glyphsBuilder.addGlyph(glyph);
    }

    //---------//
    // addPart //
    //---------//
    /**
     * Add a part (set of staves) in this system
     * @param partInfo the part to add
     */
    public void addPart (PartInfo partInfo)
    {
        parts.add(partInfo);
    }

    //----------//
    // addStaff //
    //----------//
    /**
     * Add a staff into this system
     * @param idx index (in the global staves collection at sheet level) of the
     * staff to add
     */
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
            top = firstLine.yAt(firstLine.getLeft());
        }

        // Last staff (so far)
        stopIdx = idx;
        deltaY = firstLine.yAt(firstLine.getLeft()) - top;

        LineInfo lastLine = staff.getLastLine();
        bottom = lastLine.yAt(lastLine.getLeft());
    }

    //-----------------------//
    // addToGlyphsCollection //
    //-----------------------//
    /**
     * This is a private entry meant for GlyphsBuilder only.
     * The standard entry is {@link #addGlyph}
     * @param glyph the glyph to add to the system glyph collection
     */
    public void addToGlyphsCollection (Glyph glyph)
    {
        glyphs.add(glyph);
    }

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * Build the corresponding ScoreSystem entity with all its depending Parts
     * and Staves
     */
    public void allocateScoreStructure ()
    {
        // Allocate the score system
        scoreSystem = new ScoreSystem(
            this,
            sheet.getPage(),
            new PixelPoint(getLeft(), getTop()),
            new PixelDimension(getWidth(), getDeltaY()));

        // Allocate the parts in the system
        int id = 0;

        for (PartInfo partInfo : getParts()) {
            SystemPart part = new SystemPart(scoreSystem);
            part.setId(--id); // Temporary id

            // Allocate the staves in this part
            for (StaffInfo staffInfo : partInfo.getStaves()) {
                LineInfo firstLine = staffInfo.getFirstLine();
                LineInfo lastLine = staffInfo.getLastLine();
                new Staff(
                    staffInfo,
                    part,
                    new PixelPoint(left, firstLine.yAt(left)),
                    staffInfo.getRight() - left,
                    lastLine.yAt(left) - firstLine.yAt(left));
            }
        }
    }

    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build a glyph from a collection of sections, and make the sections point
     * back to the glyph
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public Glyph buildGlyph (Collection<GlyphSection> sections)
    {
        return glyphsBuilder.buildGlyph(sections);
    }

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Based on barlines found, build, check and cleanup score measures
     */
    public void buildMeasures ()
    {
        measuresBuilder.buildMeasures();
    }

    //------------------------//
    // buildTransientCompound //
    //------------------------//
    /**
     * Make a new glyph out of a collection of (sub) glyphs, by merging all
     * their member sections. This compound is transient, since until it is
     * properly inserted by use of {@link #addGlyph}, this building has no
     * impact on either the containing lag, the containing system, nor the
     * contained sections themselves.
     *
     * @param parts the collection of (sub) glyphs
     * @return the brand new (compound) glyph
     */
    public Glyph buildTransientCompound (Collection<Glyph> parts)
    {
        return glyphsBuilder.buildTransientCompound(parts);
    }

    //---------------------//
    // buildTransientGlyph //
    //---------------------//
    /**
     * Make a new glyph out of a collection of sections.
     * This glyph is transient, since until it is properly inserted by use of
     * {@link #addGlyph}, this building has no impact on either the containing
     * lag, the containing system, nor the contained sections themselves.
     *
     * @param sections the collection of sections
     * @return the brand new transient glyph
     */
    public Glyph buildTransientGlyph (Collection<GlyphSection> sections)
    {
        return glyphsBuilder.buildTransientGlyph(sections);
    }

    //-----------------//
    // checkBoundaries //
    //-----------------//
    /**
     * Check this system for glyphs that cross the system boundaries
     */
    public void checkBoundaries ()
    {
        glyphsBuilder.retrieveGlyphs(false);
    }

    //-------------//
    // clearGlyphs //
    //-------------//
    /**
     * Empty the system glyph collection
     */
    public void clearGlyphs ()
    {
        glyphs.clear();
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement natural SystemInfo sorting, based on system id
     * @param o the other system to compare to
     * @return the comparison result
     */
    public int compareTo (SystemInfo o)
    {
        return Integer.signum(id - o.id);
    }

    //----------------------//
    // computeGlyphFeatures //
    //----------------------//
    /**
     * Compute all the features that will be used to recognize the glyph at hand
     * (it's a mix of moments plus a few other characteristics)
     *
     * @param glyph the glyph at hand
     */
    public void computeGlyphFeatures (Glyph glyph)
    {
        glyphsBuilder.computeGlyphFeatures(glyph);
    }

    //----------------------//
    // createStemCheckSuite //
    //----------------------//
    /**
     * Build a check suite for stem retrievals
     * @param isShort are we looking for short (vs standard) stems?
     * @return the newly built check suite
     * @throws omr.step.StepException
     */
    public CheckSuite<Stick> createStemCheckSuite (boolean isShort)
        throws StepException
    {
        return verticalsBuilder.createStemCheckSuite(isShort);
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
                System.out.println(
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
                System.out.println(
                    (section.isKnown() ? "known " : "      ") +
                    section.toString());
            }
        }
    }

    //------------------//
    // extractNewGlyphs //
    //------------------//
    /**
     * In the specified system, build new glyphs from unknown sections (sections
     * not linked to a known glyph)
     */
    public void extractNewGlyphs ()
    {
        removeInactiveGlyphs();
        retrieveGlyphs();
    }

    //--------------//
    // fixLargeSlur //
    //--------------//
    /**
     * For large glyphs, we suspect a slur with a stuck object. So the strategy
     * is to rebuild the true Slur portions from the underlying sections. These
     * "good" sections are put into the "kept" collection. Sections left over
     * are put into the "left" collection in order to be used to rebuild the
     * stuck object(s).
     *
     * <p>The method by itself does not build the new slur glyph, this task must
     * be done by the caller.
     *
     * @param slur the spurious slur slur
     * @return the extracted slur glyph, if any
     */
    public Glyph fixLargeSlur (Glyph slur)
    {
        return slurInspector.fixLargeSlur(slur);
    }

    //-----------------//
    // fixSpuriousSlur //
    //-----------------//
    /**
     * Try to correct the slur glyphs (which have a too high circle distance) by
     * either adding a neigboring glyph (for small slurs) or removing stuck
     * glyph sections (for large slurs)
     *
     * @param glyph the spurious glyph at hand
     * @return true if the slur glyph has actually been fixed
     */
    public Glyph fixSpuriousSlur (Glyph glyph)
    {
        return slurInspector.fixSpuriousSlur(glyph);
    }

    //---------------//
    // inspectGlyphs //
    //---------------//
    /**
     * Process the given system, by retrieving unassigned glyphs, evaluating
     * and assigning them if OK, or trying compounds otherwise.
     *
     * @param maxDoubt the maximum acceptable doubt for this processing
     */
    public void inspectGlyphs (double maxDoubt)
    {
        glyphInspector.inspectGlyphs(maxDoubt);
    }

    //-----------------------//
    // lookupContainedGlyphs //
    //-----------------------//
    /**
     * Look up in system glyphs for the glyphs contained by a
     * provided rectangle
     *
     * @param rect the coordinates rectangle, in pixels
     * @return the glyphs found, which may be an empty list
     */
    public List<Glyph> lookupContainedGlyphs (PixelRectangle rect)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : getGlyphs()) {
            if (rect.contains(glyph.getContourBox())) {
                found.add(glyph);
            }
        }

        return found;
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

        for (Glyph glyph : getGlyphs()) {
            if (glyph != excluded) {
                if (glyph.intersects(rect)) {
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

    //----------------------------//
    // removeFromGlyphsCollection //
    //----------------------------//
    /**
     * Meant for access by GlypsBuilder only
     * Standard entry is {@link #removeGlyph}
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
     * Remove a glyph from the containing system glyph list, and make it
     * inactive by cutting the link from its member sections
     *
     * @param glyph the glyph to remove
     */
    public void removeGlyph (Glyph glyph)
    {
        glyphsBuilder.removeGlyph(glyph);
    }

    //----------------------//
    // removeInactiveGlyphs //
    //----------------------//
    /**
     * On a specified system, look for all inactive glyphs and remove them from
     * its glyphs collection (but leave them in the containing lag).
     * Purpose is to prepare room for a new glyph extraction
     */
    public void removeInactiveGlyphs ()
    {
        // To avoid concurrent modifs exception
        Collection<Glyph> toRemove = new ArrayList<Glyph>();

        for (Glyph glyph : getGlyphs()) {
            if (!glyph.isActive()) {
                toRemove.add(glyph);
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "removeInactiveGlyphs: " + toRemove.size() + " " +
                Glyphs.toString(toRemove));
        }

        for (Glyph glyph : toRemove) {
            // Remove glyph from system & cut sections links to it
            removeGlyph(glyph);
        }
    }

    //----------------//
    // resetSentences //
    //----------------//
    public void resetSentences ()
    {
        sentences.clear();
        sentenceCount = 0;
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * In a given system area, browse through all sections not assigned to known
     * glyphs, and build new glyphs out of connected sections
     */
    public void retrieveGlyphs ()
    {
        glyphsBuilder.retrieveGlyphs(true);
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    /**
     * Build new glyphs out of system suitable sections,
     * @return the number of glyphs built
     * @throws omr.step.StepException
     */
    public int retrieveVerticals ()
        throws StepException
    {
        return verticalsBuilder.retrieveVerticals();
    }

    //-------------//
    // runPatterns //
    //-------------//
    /**
     * Run the series of glyphs patterns
     * @return true if some progress has been made
     */
    public boolean runPatterns ()
    {
        return new PatternsChecker(this).runPatterns();
    }

    //---------------------//
    // segmentGlyphOnStems //
    //---------------------//
    /**
     * Process a glyph to retrieve its internal potential stems and leaves
     * @param glyph the glyph to segment along stems
     * @param isShort should we look for short (rather than standard) stems?
     */
    public void segmentGlyphOnStems (Glyph   glyph,
                                     boolean isShort)
    {
        verticalsBuilder.segmentGlyphOnStems(glyph, isShort);
    }

    //--------------//
    // selectGlyphs //
    //--------------//
    /**
     * Select glyphs out of a provided collection of glyphs,for which the
     * provided predicate holds true
     * @param glyphs the provided collection of glyphs candidates, or the full
     * system collection if null
     * @param predicate the condition to be fulfilled to get selected
     * @return the sorted set of selected glyphs
     */
    public SortedSet selectGlyphs (Collection<Glyph> glyphs,
                                   Predicate<Glyph>  predicate)
    {
        SortedSet<Glyph> selected = new TreeSet<Glyph>();

        if (glyphs == null) {
            glyphs = getGlyphs();
        }

        for (Glyph glyph : glyphs) {
            if (predicate.check((glyph))) {
                selected.add(glyph);
            }
        }

        return selected;
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
        StringBuilder sb = new StringBuilder(80);
        sb.append("{SystemInfo#")
          .append(id);
        sb.append(" T")
          .append(startIdx + 1);

        if (startIdx != stopIdx) {
            sb.append("..T")
              .append(stopIdx + 1);
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

    //--------------//
    // getSentences //
    //--------------//
    /**
     * Report the various sentences retrieved in this system.
     * @return the (perhaps empty) collection of sentences found
     */
    public Set<Sentence> getSentences ()
    {
        return sentences;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this system belongs to
     * @return the containing sheet
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //----------------//
    // translateFinal //
    //----------------//
    /**
     * Launch from this system the final processing of impacted systems to
     * translate them to score entities
     */
    public void translateFinal ()
    {
        translator.translateFinal();
    }

    //-----------------//
    // translateSystem //
    //-----------------//
    /**
     * Translate the physical Sheet system data into Score system entities
     */
    public void translateSystem ()
    {
        translator.translateSystem();
    }

    //-----------------//
    // boundaryUpdated //
    //-----------------//
    void boundaryUpdated ()
    {
        ///logger.warning("Update for " + this);
    }
}
