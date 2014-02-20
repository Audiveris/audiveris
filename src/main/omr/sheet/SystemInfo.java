//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S y s t e m I n f o                                       //
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
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLayer;
import omr.glyph.GlyphNest;
import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;
import omr.glyph.pattern.PatternsChecker;
import omr.glyph.pattern.SlurInspector;

import omr.grid.BarInfo;
import omr.grid.LineInfo;
import omr.grid.OldBarAlignment;
import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.Section;

import omr.score.SystemTranslator;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sig.SIGraph;
import omr.sig.SigSolver;

import omr.text.TextBuilder;
import omr.text.TextLine;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
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
 * Class {@code SystemInfo} gathers information from the original
 * picture about a retrieved system.
 * Most of the physical processing is done in parallel at system level, and
 * thus is handled from this SystemInfo object.
 * <p>
 * Many processing tasks are actually handled by companion classes, but
 * SystemInfo is the interface of choice, with delegation to the proper
 * companion.
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
    /** Related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Symbol Interpretation Graph for this system. */
    private final SIGraph sig;

    /** Dedicated measure builder */
    private final MeasuresBuilder measuresBuilder;

    /** Dedicated text builder */
    private final TextBuilder textBuilder;

    /** Dedicated compound builder */
    private final CompoundBuilder compoundBuilder;

    /** Dedicated beams builder */
    public final BeamsBuilder beamsBuilder;

    /** Dedicated notes builder */
    public final NotesBuilder notesBuilder;

    /** Dedicated verticals builder */
    public final VerticalsBuilder verticalsBuilder;

    /** Dedicated stems builder */
    public final StemsBuilder stemsBuilder;

    /** Dedicated horizontals builder */
    public final HorizontalsBuilder horizontalsBuilder;

    /** Dedicated SIG processor */
    public final SigSolver sigSolver;

    /** Dedicated glyph inspector */
    private final GlyphInspector glyphInspector;

    /** Dedicated slur inspector */
    private final SlurInspector slurInspector;

    /** Dedicated system translator */
    private final SystemTranslator translator;

    /** Staves of this system */
    private List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Parts in this system */
    private final List<PartInfo> parts = new ArrayList<PartInfo>();

    /** Related System in Score hierarchy */
    private ScoreSystem scoreSystem;

    /** Left system bar, if any */
    private BarInfo leftBar;

    /** Right system bar, if any */
    private BarInfo rightBar;

    /** Bar alignments for this system */
    private List<OldBarAlignment> barAlignments;

    ///   HORIZONTALS   ////////////////////////////////////////////////////////
    /** Horizontal sections, assigned once for all to this system */
    private final List<Section> hSections = new ArrayList<Section>();

    private final List<Section> hFullSections = new ArrayList<Section>();

    /** Unmodifiable view of the horizontal section collection */
    private final Collection<Section> hSectionsView = Collections.unmodifiableCollection(hSections);

    private final Collection<Section> hFullSectionsView = Collections.unmodifiableCollection(
            hFullSections);

    ///   VERTICALS   //////////////////////////////////////////////////////////
    /** Vertical sections, assigned once for all to this system */
    private final List<Section> vSections = new ArrayList<Section>();

    /** Unmodifiable view of the vertical section collection */
    private final Collection<Section> vSectionsView = Collections.unmodifiableCollection(vSections);

    /** Collection of (active?) glyphs in this system */
    private final SortedSet<Glyph> glyphs = new ConcurrentSkipListSet<Glyph>(Glyph.byAbscissa);

    /** Unmodifiable view of the glyphs collection */
    private final SortedSet<Glyph> glyphsView = Collections.unmodifiableSortedSet(glyphs);

    /** Set of sentence made of text glyphs */
    private Set<TextLine> sentences = new LinkedHashSet<TextLine>();

    /** Used to assign a unique ID to system sentences */
    private int sentenceCount = 0;

    ////////////////////////////////////////////////////////////////////////////
    /** Unique Id for this system (in the sheet) */
    private final int id;

    /** Area that encloses all items of this system. */
    private Area area;

    /** Ordinate of bottom of last staff of the system. */
    private int bottom;

    /** Delta ordinate between first line of first staff & first line of
     * last staff. */
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

    //~ Constructors -------------------------------------------------------------------------------
    //------------//
    // SystemInfo //
    //------------//
    /**
     * Create a SystemInfo entity, to register the provided parameters.
     *
     * @param id     the unique identity
     * @param sheet  the containing sheet
     * @param staves the (initial) sequence of staves
     */
    public SystemInfo (int id,
                       Sheet sheet,
                       List<StaffInfo> staves)
    {
        this.id = id;
        this.sheet = sheet;

        setStaves(staves);

        sig = new SIGraph(this);
        sigSolver = new SigSolver(this, sig);
        measuresBuilder = new MeasuresBuilder(this);
        textBuilder = new TextBuilder(this);
        compoundBuilder = new CompoundBuilder(this);
        beamsBuilder = new BeamsBuilder(this);
        notesBuilder = new NotesBuilder(this);
        verticalsBuilder = new VerticalsBuilder(this);
        stemsBuilder = new StemsBuilder(this);
        horizontalsBuilder = new HorizontalsBuilder(this);
        glyphInspector = new GlyphInspector(this);
        slurInspector = new SlurInspector(this);
        translator = new SystemTranslator(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addPart //
    //---------//
    /**
     * Add a part (set of staves) in this system.
     *
     * @param partInfo the part to add
     */
    public void addPart (PartInfo partInfo)
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

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * Build the corresponding ScoreSystem entity with all its
     * depending Parts and Staves.
     */
    public void allocateScoreStructure ()
    {
        // Allocate the score system
        scoreSystem = new ScoreSystem(
                this,
                sheet.getPage(),
                new Point(getLeft(), getTop()),
                new Dimension(getWidth(), getDeltaY()));

        // Allocate the parts in the system
        int partId = 0;

        for (PartInfo partInfo : getParts()) {
            SystemPart part = new SystemPart(scoreSystem, partInfo);
            part.setId(--partId); // Temporary id

            // Allocate the staves in this part
            for (StaffInfo staffInfo : partInfo.getStaves()) {
                LineInfo firstLine = staffInfo.getFirstLine();
                LineInfo lastLine = staffInfo.getLastLine();
                new Staff(
                        staffInfo,
                        part,
                        new Point(left, firstLine.yAt(left)),
                        staffInfo.getAbscissa(RIGHT) - left,
                        lastLine.yAt(left) - firstLine.yAt(left));
            }
        }
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

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Based on bar lines found, build, check and cleanup score measures.
     */
    public void buildMeasures ()
    {
        measuresBuilder.buildMeasures();
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

    //------------//
    // dumpGlyphs //
    //------------//
    /**
     * Dump all glyphs handled by this system.
     */
    public void dumpGlyphs ()
    {
        dumpGlyphs(null);
    }

    //------------//
    // dumpGlyphs //
    //------------//
    /**
     * Dump the glyphs handled by this system and that are contained
     * by the provided rectangle.
     *
     * @param rect the region of interest
     */
    public void dumpGlyphs (Rectangle rect)
    {
        for (Glyph glyph : getGlyphs()) {
            if ((rect == null) || (rect.contains(glyph.getBounds()))) {
                System.out.println(
                        (glyph.isActive() ? "active " : "       ")
                        + (glyph.isKnown() ? "known " : "      ")
                        + (glyph.isWellKnown() ? "wellKnown " : "          ") + glyph.toString());
            }
        }
    }

    //--------------//
    // dumpSections //
    //--------------//
    /**
     * Dump all (vertical) sections handled by this system.
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
     * contained by the provided rectangle.
     *
     * @param rect the region of interest
     */
    public void dumpSections (Rectangle rect)
    {
        for (Section section : getVerticalSections()) {
            if ((rect == null) || (rect.contains(section.getBounds()))) {
                System.out.println((section.isKnown() ? "known " : "      ") + section.toString());
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

    //--------//
    // getBar //
    //--------//
    /**
     * Report the system barline on the provided side.
     *
     * @param side proper horizontal side
     * @return the system bar on this side, or null
     */
    public BarInfo getBar (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftBar;
        } else {
            return rightBar;
        }
    }

    //------------------//
    // getBarAlignments //
    //------------------//
    /**
     * Report the system bar alignments.
     *
     * @return the barAlignments
     */
    public List<OldBarAlignment> getBarAlignments ()
    {
        return barAlignments;
    }

    //-----------//
    // getBottom //
    //-----------//
    /**
     * Report the ordinate of the bottom of the system, which is the
     * ordinate of the last line of the last staff of this system.
     *
     * @return the system bottom, in pixels
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

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * Report the first staff of the system.
     *
     * @return the first staff
     */
    public StaffInfo getFirstStaff ()
    {
        return staves.get(0);
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

    //---------------------------//
    // getHorizontalFullSections //
    //---------------------------//
    /**
     * Report the (unmodifiable) collection of horizontal full
     * sections in the system related area.
     *
     * @return the area horizontal full sections
     */
    public Collection<Section> getHorizontalFullSections ()
    {
        return hFullSectionsView;
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
    public Collection<Section> getHorizontalSections ()
    {
        return hSectionsView;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id (debugging info) of the system info.
     *
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * @return the lastStaff
     */
    public StaffInfo getLastStaff ()
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
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());

        if (sb.length() > 1) {
            sb.insert(sb.length() - 2, "-S" + id);
        } else {
            sb.append("S").append(id).append(" ");
        }

        return sb.toString();
    }

    //----------------------------------//
    // getMutableHorizontalFullSections //
    //----------------------------------//
    /**
     * Report the (modifiable) collection of horizontal full sections
     * in the system related area.
     *
     * @return the full horizontal sections
     */
    public Collection<Section> getMutableHorizontalFullSections ()
    {
        return hFullSections;
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

    //------------------//
    // getNewSentenceId //
    //------------------//
    /**
     * Report the id for a new sentence.
     *
     * @return the next id
     */
    public int getNewSentenceId ()
    {
        return ++sentenceCount;
    }

    //----------------//
    // getNoteStaffAt //
    //----------------//
    /**
     * Given a note, retrieve the proper related staff within the
     * system, using ledgers if any.
     *
     * @param point the center of the provided note entity
     * @return the proper note position (staff & pitch)
     */
    public NotePosition getNoteStaffAt (Point point)
    {
        StaffInfo staff = getStaffAt(point);
        NotePosition pos = staff.getNotePosition(point);

        logger.debug("{} -> {}", point, pos);

        double pitch = pos.getPitchPosition();

        if ((Math.abs(pitch) > 5) && (pos.getLedger() == null)) {
            // Delta pitch from reference line
            double dp = Math.abs(pitch) - 4;

            // Check with the other staff, if any
            int index = staves.indexOf(staff);
            StaffInfo otherStaff = null;

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
                            - StaffInfo.getLedgerPitchPosition(otherPos.getLedger().index));

                    if (otherDp < dp) {
                        logger.debug("   otherPos: {}", pos);
                        pos = otherPos;
                    }
                }
            }
        }

        return pos;
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Reports the parts of this system.
     *
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
     * Report the abscissa of the end of the system.
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
     * Report the related logical score system.
     *
     * @return the logical score System counterpart
     */
    public ScoreSystem getScoreSystem ()
    {
        return scoreSystem;
    }

    //--------------//
    // getSentences //
    //--------------//
    /**
     * Report the various sentences retrieved in this system.
     *
     * @return the (perhaps empty) collection of sentences found
     */
    public Set<TextLine> getSentences ()
    {
        return sentences;
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

    //------------------//
    // getSlurInspector //
    //------------------//
    public SlurInspector getSlurInspector ()
    {
        return slurInspector;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Retrieve the staff, <b>within</b> the system, whose area
     * contains the provided point.
     *
     * @param point the provided point
     * @return the "containing" staff
     */
    public StaffInfo getStaffAt (Point2D point)
    {
        return StaffManager.getStaffAt(point, staves);
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the list of staves that compose this system.
     *
     * @return the staves
     */
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //----------------//
    // getTextBuilder //
    //----------------//
    public TextBuilder getTextBuilder ()
    {
        return textBuilder;
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
        return vSectionsView;
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

    //---------------//
    // inspectGlyphs //
    //---------------//
    /**
     * Process the given system, by retrieving unassigned glyphs,
     * evaluating and assigning them if OK, or trying compounds
     * otherwise.
     *
     * @param minGrade the minimum acceptable grade for this processing
     * @param wide     flag for extra wide compound box
     */
    public void inspectGlyphs (double minGrade,
                               boolean wide)
    {
        glyphInspector.inspectGlyphs(minGrade, wide);
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

    //---------------//
    // registerGlyph //
    //---------------//
    /**
     * Register a brand new glyph in proper system (and nest).
     *
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
        glyph = sheet.getNest().registerGlyph(glyph);
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

    //----------------------//
    // removeInactiveGlyphs //
    //----------------------//
    /**
     * On a specified system, look for all inactive glyphs and remove
     * them from its glyphs collection (but leave them in the
     * containing nest).
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

        if (logger.isDebugEnabled()) {
            logger.debug("removeInactiveGlyphs: {} {}", toRemove.size(), Glyphs.toString(toRemove));
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
     * In a given system area, browse through all sections not assigned
     * to known glyphs, and build new glyphs out of connected sections.
     */
    public void retrieveGlyphs ()
    {
        // Consider all unknown vertical & horizontal sections
        List<Section> allSections = new ArrayList<Section>();
        allSections.addAll(getVerticalSections());
        allSections.addAll(getHorizontalSections());

        final GlyphNest nest = sheet.getNest();
        List<Glyph> newGlyphs = nest.retrieveGlyphs(
                allSections,
                GlyphLayer.DEFAULT,
                false,
                Glyph.Linking.NO_LINK);

        // Record them into the system
        glyphs.addAll(newGlyphs);
    }

    //-------------//
    // runPatterns //
    //-------------//
    /**
     * Run the series of glyphs patterns.
     *
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
     * Process a glyph to retrieve its internal potential stems and
     * leaves.
     *
     * @param glyph the glyph to segment along stems
     */
    public void segmentGlyphOnStems (Glyph glyph)
    {
        verticalsBuilder.segmentGlyphOnStems(glyph);
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

    //--------//
    // setBar //
    //--------//
    /**
     * Assign system barline on the provided side.
     *
     * @param side proper horizontal side
     * @param bar  the bar to set
     */
    public void setBar (HorizontalSide side,
                        BarInfo bar)
    {
        if (side == HorizontalSide.LEFT) {
            this.leftBar = bar;
        } else {
            this.rightBar = bar;
        }
    }

    //------------------//
    // setBarAlignments //
    //------------------//
    /**
     * Record the various bar alignments for this system.
     *
     * @param barAlignments the barAlignments to set
     */
    public void setBarAlignments (List<OldBarAlignment> barAlignments)
    {
        this.barAlignments = barAlignments;
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * @param staves the range of staves
     */
    public final void setStaves (List<StaffInfo> staves)
    {
        this.staves = staves;

        for (StaffInfo staff : staves) {
            staff.setSystem(this);
        }

        updateCoordinates();
    }

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
        sb.append("{SystemInfo#").append(id);
        sb.append(" T").append(getFirstStaff().getId());

        if (staves.size() > 1) {
            sb.append("..T").append(getLastStaff().getId());
        }

        if (leftBar != null) {
            sb.append(" leftBar:").append(leftBar);
        }

        if (rightBar != null) {
            sb.append(" rightBar:").append(rightBar);
        }

        sb.append("}");

        return sb.toString();
    }

    //----------------//
    // translateFinal //
    //----------------//
    /**
     * Launch from this system the final processing of impacted systems
     * to translate them to score entities.
     */
    public void translateFinal ()
    {
        translator.translateFinal();
    }

    //----------//
    // trimSlur //
    //----------//
    /**
     * Rebuild true Slur from some underlying sections.
     *
     * @param slur the spurious slur
     * @return the extracted slur glyph, if any
     */
    public Glyph trimSlur (Glyph slur)
    {
        return slurInspector.trimSlur(slur);
    }

    //-------------------//
    // updateCoordinates //
    //-------------------//
    public final void updateCoordinates ()
    {
        StaffInfo firstStaff = getFirstStaff();
        LineInfo firstLine = firstStaff.getFirstLine();
        Point2D topLeft = firstLine.getEndPoint(LEFT);

        StaffInfo lastStaff = getLastStaff();
        LineInfo lastLine = lastStaff.getLastLine();
        Point2D botLeft = lastLine.getEndPoint(LEFT);

        left = Integer.MAX_VALUE;

        int right = 0;

        for (StaffInfo staff : staves) {
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
     * Convenient method, to build a string with just the ids of the
     * system collection.
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

    //-----------------//
    // translateSystem //
    //-----------------//
    /**
     * Translate the physical Sheet system data into Score system
     * entities.
     */
    public void translateSystem ()
    {
        translator.translateSystem();
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
