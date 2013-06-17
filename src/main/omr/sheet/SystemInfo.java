//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m I n f o                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.check.CheckSuite;

import omr.glyph.CompoundBuilder;
import omr.glyph.CompoundBuilder.CompoundAdapter;
import omr.glyph.GlyphInspector;
import omr.glyph.Glyphs;
import omr.glyph.GlyphsBuilder;
import omr.glyph.facets.Glyph;
import omr.glyph.pattern.PatternsChecker;
import omr.glyph.pattern.SlurInspector;

import omr.grid.BarAlignment;
import omr.grid.BarInfo;
import omr.grid.LineInfo;
import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.Section;

import omr.math.GeoPath;

import omr.score.SystemTranslator;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.step.StepException;

import omr.text.TextBuilder;
import omr.text.TextLine;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.Predicate;
import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class {@code SystemInfo} gathers information from the original
 * picture about a retrieved system.
 * Most of the physical processing is done in parallel at system level, and
 * thus is handled from this SystemInfo object.
 *
 * <p>Many processing tasks are actually handled by companion classes, but
 * SystemInfo is the interface of choice, with delegation to the proper
 * companion.
 *
 * @author Hervé Bitteur
 */
public class SystemInfo
        implements Comparable<SystemInfo>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SystemInfo.class);

    //~ Instance fields --------------------------------------------------------
    /** Related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Dedicated measure builder */
    private final MeasuresBuilder measuresBuilder;

    /** Dedicated text builder */
    private final TextBuilder textBuilder;

    /** Dedicated glyph builder */
    private final GlyphsBuilder glyphsBuilder;

    /** Dedicated compound builder */
    private final CompoundBuilder compoundBuilder;

    /** Dedicated verticals builder */
    private final VerticalsBuilder verticalsBuilder;

    /** Dedicated horizontals builder */
    private final HorizontalsBuilder horizontalsBuilder;

    /** Dedicated glyph inspector */
    private final GlyphInspector glyphInspector;

    /** Dedicated slur inspector */
    private final SlurInspector slurInspector;

    /** Dedicated system translator */
    private final SystemTranslator translator;

    /** Staves of this system */
    private List<StaffInfo> staves = new ArrayList<>();

    /** Parts in this system */
    private final List<PartInfo> parts = new ArrayList<>();

    /** Related System in Score hierarchy */
    private ScoreSystem scoreSystem;

    /** Left system bar, if any */
    private BarInfo leftBar;

    /** Right system bar, if any */
    private BarInfo rightBar;

    /** Left system limit (a filament or a straight line) */
    private Object leftLimit;

    /** Right system limit (a filament or a straight line) */
    private Object rightLimit;

    /** Bar alignments for this system */
    private List<BarAlignment> barAlignments;

    ///   HORIZONTALS   ////////////////////////////////////////////////////////
    /** Horizontal sections, assigned once for all to this system */
    private final List<Section> hSections = new ArrayList<>();

    /** Unmodifiable view of the horizontal section collection */
    private final Collection<Section> hSectionsView = Collections.
            unmodifiableCollection(
            hSections);

    /** Retrieved tenuto signs in this system */
    private final List<Glyph> tenutos = new ArrayList<>();

    /** Retrieved endings in this system */
    private final List<Glyph> endings = new ArrayList<>();

    ///   VERTICALS   //////////////////////////////////////////////////////////
    /** Vertical sections, assigned once for all to this system */
    private final List<Section> vSections = new ArrayList<>();

    /** Unmodifiable view of the vertical section collection */
    private final Collection<Section> vSectionsView = Collections.
            unmodifiableCollection(
            vSections);

    /** Collection of (active?) glyphs in this system */
    private final SortedSet<Glyph> glyphs = new ConcurrentSkipListSet<>(
            Glyph.byAbscissa);

    /** Unmodifiable view of the glyphs collection */
    private final SortedSet<Glyph> glyphsView = Collections.
            unmodifiableSortedSet(
            glyphs);

    /** Set of sentence made of text glyphs */
    private Set<TextLine> sentences = new LinkedHashSet<>();

    /** Used to assign a unique ID to system sentences */
    private int sentenceCount = 0;

    ////////////////////////////////////////////////////////////////////////////
    /** Unique Id for this system (in the sheet) */
    private final int id;

    /** Boundary that encloses all items of this system. */
    private SystemBoundary boundary;

    /** Ordinate of bottom of last staff of the system. */
    private int bottom;

    /** Delta ordinate between first line of first staff & first line of
     * last staff. */
    private int deltaY;

    /** Abscissa of beginning of system. */
    private int left;

    /** Ordinate of top of first staff of the system. */
    private int top;

    /** Width of the system. */
    private int width = -1;

    //~ Constructors -----------------------------------------------------------
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
        this.staves = staves;

        updateCoordinates();

        measuresBuilder = new MeasuresBuilder(this);
        textBuilder = new TextBuilder(this);
        glyphsBuilder = new GlyphsBuilder(this);
        compoundBuilder = new CompoundBuilder(this);
        verticalsBuilder = new VerticalsBuilder(this);
        horizontalsBuilder = new HorizontalsBuilder(this);
        glyphInspector = new GlyphInspector(this);
        slurInspector = new SlurInspector(this);
        translator = new SystemTranslator(this);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a brand new glyph as an active glyph in proper system
     * (and nest).
     * If the glyph is a compound, its parts are made pointing back to it and
     * are made no longer active glyphs. To just register a glyph (without
     * impacting its sections), use {@link #registerGlyph} instead.
     *
     * <p><b>Note</b>: The caller must use the returned glyph since it may be
     * different from the provided glyph (this happens when an original glyph
     * with same signature existed before this one)
     *
     * @param glyph the brand new glyph
     * @return the original glyph as inserted in the glyph nest. Use this entity
     *         instead of the provided one.
     * @see #registerGlyph
     */
    public Glyph addGlyph (Glyph glyph)
    {
        return glyphsBuilder.addGlyph(glyph);
    }

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
     * This is a private entry meant for GlyphsBuilder only.
     * The standard entry is {@link #addGlyph}
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
                        (int) Math.rint(staffInfo.getAbscissa(RIGHT) - left),
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
        return compoundBuilder.buildCompound(
                seed,
                includeSeed,
                suitables,
                adapter);
    }

    //---------------//
    // buildCompound //
    //---------------//
    public Glyph buildCompound (Collection<Glyph> parts)
    {
        return compoundBuilder.buildCompound(parts);
    }

    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build a glyph from a collection of sections, and make the
     * sections point back to the glyph.
     *
     * @param sections the provided members of the future glyph
     * @return the newly built glyph
     */
    public Glyph buildGlyph (Collection<Section> sections)
    {
        return glyphsBuilder.buildGlyph(sections);
    }

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Based on barlines found, build, check and cleanup score measures.
     */
    public void buildMeasures ()
    {
        measuresBuilder.buildMeasures();
    }

    //------------------------//
    // buildTransientCompound //
    //------------------------//
    /**
     * Make a new glyph out of a collection of (sub) glyphs,
     * by merging all their member sections.
     * This compound is transient, since until it is properly inserted by use
     * of {@link #addGlyph}, this building has no impact on either the
     * containing nest, the containing system, nor the contained sections
     * themselves.
     *
     * <p>If the newly built compound duplicates an original glyph, the original
     * glyph is used in place of the compound. Finally, the glyph features are
     * computed before the compound is returned.</p>
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
     * nest, the containing system, nor the contained sections themselves.
     *
     * <p>If the newly built compound duplicates an original glyph, the original
     * glyph is used in place of the compound. Finally, the glyph features are
     * computed before the compound is returned.</p>
     *
     * @param sections the collection of sections
     * @return the brand new transient glyph
     */
    public Glyph buildTransientGlyph (Collection<Section> sections)
    {
        return glyphsBuilder.buildTransientGlyph(sections);
    }

    //-----------------//
    // checkBoundaries //
    //-----------------//
    /**
     * Check this system for glyphs that cross the system boundaries.
     */
    public void checkBoundaries ()
    {
        glyphsBuilder.retrieveGlyphs(false);
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
     * @param o the other system to compare to
     * @return the comparison result
     */
    @Override
    public int compareTo (SystemInfo o)
    {
        return Integer.signum(id - o.id);
    }

    //----------------------//
    // computeGlyphFeatures //
    //----------------------//
    /**
     * Compute all the features that will be used to recognize the
     * glyph at hand (a mix of moments plus a few other characteristics).
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
     * Build a check suite for stem retrievals.
     *
     * @param isShort are we looking for short (vs standard) stems?
     * @return the newly built check suite
     */
    public CheckSuite<Glyph> createStemCheckSuite (boolean isShort)
            throws StepException
    {
        return verticalsBuilder.createStemCheckSuite(isShort);
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
                        + (glyph.isWellKnown() ? "wellKnown " : "          ")
                        + glyph.toString());
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
                System.out.println(
                        (section.isKnown() ? "known " : "      ")
                        + section.toString());
            }
        }
    }

    //------------------//
    // extractNewGlyphs //
    //------------------//
    /**
     * In the specified system, build new glyphs from unknown sections
     * (sections not linked to a known glyph).
     */
    public void extractNewGlyphs ()
    {
        removeInactiveGlyphs();
        retrieveGlyphs();
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
    public List<BarAlignment> getBarAlignments ()
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

    //-------------//
    // getBoundary //
    //-------------//
    /**
     * Report the precise boundary of this system.
     *
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
     * Report the rectangular bounds that enclose this system.
     *
     * @return the system rectangular bounds
     */
    public Rectangle getBounds ()
    {
        if (boundary != null) {
            return new Rectangle(boundary.getBounds());
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

    //------------//
    // getEndings //
    //------------//
    /**
     * Report the collection of endings found.
     *
     * @return the endings collection
     */
    public List<Glyph> getEndings ()
    {
        return endings;
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

    //-----------------------//
    // getHorizontalsBuilder //
    //-----------------------//
    public HorizontalsBuilder getHorizontalsBuilder ()
    {
        return horizontalsBuilder;
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

    //----------//
    // getLimit //
    //----------//
    /**
     * Report the system limit on the provided side.
     *
     * @param side proper horizontal side
     * @return the leftBar
     */
    public Object getLimit (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftLimit;
        } else {
            return rightLimit;
        }
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
     * @return the area vertical sections
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
                            - StaffInfo.getLedgerPitchPosition(
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

    //------------//
    // getTenutos //
    //------------//
    /**
     * Report the collection of tenutos found.
     *
     * @return the tenutos collection
     */
    public List<Glyph> getTenutos ()
    {
        return tenutos;
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
        List<Glyph> found = new ArrayList<>();

        for (Glyph glyph : getGlyphs()) {
            if (rect.contains(glyph.getBounds())) {
                found.add(glyph);
            }
        }

        return found;
    }

    //-------------------------//
    // lookupIntersectedGlyphs //
    //-------------------------//
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
        List<Glyph> found = new ArrayList<>();

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
     * Just register this glyph (as inactive) in order to persist glyph
     * info such as TextInfo.
     * Use {@link #addGlyph} to fully add the glpyh as active.
     *
     * @param glyph the glyph to just register
     * @return the proper (original) glyph
     * @see #addGlyph
     */
    public Glyph registerGlyph (Glyph glyph)
    {
        return glyphsBuilder.registerGlyph(glyph);
    }

    //----------------------------//
    // removeFromGlyphsCollection //
    //----------------------------//
    /**
     * Meant for access by GlyphsBuilder only,
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
        glyphsBuilder.removeGlyph(glyph);
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
        Collection<Glyph> toRemove = new ArrayList<>();

        for (Glyph glyph : getGlyphs()) {
            if (!glyph.isActive()) {
                toRemove.add(glyph);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("removeInactiveGlyphs: {} {}",
                    toRemove.size(), Glyphs.toString(toRemove));
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
        glyphsBuilder.retrieveGlyphs(true);
    }

    //---------------------//
    // retrieveHorizontals //
    //---------------------//
    /**
     * Retrieve ledgers (and tenuto, and horizontal endings).
     */
    public void retrieveHorizontals ()
            throws StepException
    {
        try {
            horizontalsBuilder.buildInfo();
        } catch (Exception ex) {
            logger.warn("Error in retrieveHorizontals", ex);
        }
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    /**
     * Retrieve stems (and vertical endings).
     *
     * @return the number of glyphs built
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
     * @param glyph   the glyph to segment along stems
     * @param isShort should we look for short (rather than standard) stems?
     */
    public void segmentGlyphOnStems (Glyph glyph,
                                     boolean isShort)
    {
        verticalsBuilder.segmentGlyphOnStems(glyph, isShort);
    }

    //--------------//
    // selectGlyphs //
    //--------------//
    /**
     * Select glyphs out of a provided collection of glyphs,for which
     * the provided predicate holds true.
     *
     * @param glyphs    the provided collection of glyphs candidates, or the
     *                  full
     *                  system collection if null
     * @param predicate the condition to be fulfilled to get selected
     * @return the sorted set of selected glyphs
     */
    public SortedSet<Glyph> selectGlyphs (Collection<Glyph> glyphs,
                                          Predicate<Glyph> predicate)
    {
        SortedSet<Glyph> selected = new TreeSet<>();

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
    public void setBarAlignments (List<BarAlignment> barAlignments)
    {
        this.barAlignments = barAlignments;
    }

    //-------------//
    // setBoundary //
    //-------------//
    /**
     * Define the precise boundary of this system.
     *
     * @param boundary the (new) boundary
     */
    public void setBoundary (SystemBoundary boundary)
    {
        logger.debug("{} setBoundary {}", idString(), boundary);
        this.boundary = boundary;

        updateBoundary();
    }

    //----------------//
    // updateBoundary //
    //----------------//
    /**
     * We have a new (or modified) system boundary.
     * So let's update the system boundary polygon as well as the limits of
     * the first and last staves.
     */
    public void updateBoundary ()
    {
        // Reset the system polygon
        boundary.update();

        // Update top limit of first staff
        GeoPath topPath = boundary.getLimit(VerticalSide.TOP).toGeoPath();
        getFirstStaff().setLimit(VerticalSide.TOP, topPath);

        // Update bottom limit of last staff
        GeoPath bottomPath = boundary.getLimit(VerticalSide.BOTTOM).toGeoPath();
        getLastStaff().setLimit(VerticalSide.BOTTOM, bottomPath);
    }

    //----------//
    // setLimit //
    //----------//
    /**
     * Record the system limit on the provided side.
     *
     * @param side  proper horizontal side
     * @param limit the limit to set
     */
    public void setLimit (HorizontalSide side,
                          Object limit)
    {
        if (side == HorizontalSide.LEFT) {
            this.leftLimit = limit;
        } else {
            this.rightLimit = limit;
        }
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * @param staves the range of staves
     */
    public void setStaves (List<StaffInfo> staves)
    {
        this.staves = staves;
        updateCoordinates();
    }

    //-----------//
    // stemBoxOf //
    //-----------//
    /**
     * Report a enlarged box of a given (stem) glyph.
     *
     * @param stem the stem
     * @return the enlarged stem box
     */
    public Rectangle stemBoxOf (Glyph stem)
    {
        return glyphsBuilder.stemBoxOf(stem);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the
     * system collection.
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
            sb.append("#").append(system.getId());
        }

        sb.append("]");

        return sb.toString();
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

        if (leftLimit != null) {
            sb.append(" leftLimit:").append(leftLimit);
        }

        if (rightLimit != null) {
            sb.append(" rightLimit:").append(rightLimit);
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
        Point2D topRight = firstLine.getEndPoint(RIGHT);
        StaffInfo lastStaff = getLastStaff();
        LineInfo lastLine = lastStaff.getLastLine();
        Point2D botLeft = lastLine.getEndPoint(LEFT);

        left = (int) Math.rint(topLeft.getX());
        top = (int) Math.rint(topLeft.getY());
        width = (int) Math.rint(topRight.getX() - topLeft.getX());
        deltaY = (int) Math.rint(
                lastStaff.getFirstLine().getEndPoint(LEFT).getY() - topLeft.
                getY());
        bottom = (int) Math.rint(botLeft.getY());
    }

    //----------------//
    // getTextBuilder //
    //----------------//
    public TextBuilder getTextBuilder ()
    {
        return textBuilder;
    }
}
