//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B a r s R e t r i e v e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;
import omr.glyph.Shape;
import omr.glyph.dynamic.CompoundFactory;
import omr.glyph.dynamic.CompoundFactory.CompoundConstructor;
import omr.glyph.dynamic.CurvedFilament;
import omr.glyph.dynamic.Filament;
import omr.glyph.dynamic.FilamentIndex;
import omr.glyph.dynamic.SectionCompound;
import omr.glyph.dynamic.StraightFilament;

import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;

import omr.math.AreaUtil;
import omr.math.AreaUtil.CoreData;
import omr.math.GeoPath;
import omr.math.GeoUtil;
import omr.math.Histogram;
import omr.math.PointUtil;
import static omr.run.Orientation.*;
import omr.run.RunTable;

import omr.sheet.Part;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.grid.PartGroup.Symbol;
import static omr.sheet.grid.StaffPeak.Attribute.*;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.inter.AbstractVerticalInter;
import omr.sig.inter.BarConnectorInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.BraceInter;
import omr.sig.inter.BracketConnectorInter;
import omr.sig.inter.BracketInter;
import omr.sig.inter.BracketInter.BracketKind;
import omr.sig.inter.Inter;
import omr.sig.relation.BarConnectionRelation;
import omr.sig.relation.BarGroupRelation;
import omr.sig.relation.Relation;

import omr.step.StepException;

import omr.ui.Colors;
import omr.ui.util.ItemRenderer;
import omr.ui.util.UIUtil;

import omr.util.Dumping;
import omr.util.Entities;
import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code BarsRetriever} focuses on the retrieval of vertical bar lines and
 * brackets.
 * <p>
 * Bar lines are used to:
 * <ul>
 * <li>Determine the gathering of staves into systems and parts</li>
 * <li>Define staff sides precisely.</li>
 * <li>Determine measures (later)</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class BarsRetriever
        implements ItemRenderer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BarsRetriever.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Lag of vertical runs. */
    private Lag vLag;

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Staff projectors. */
    private final List<StaffProjector> projectors = new ArrayList<StaffProjector>();

    /** All alignments found between bars across staves. */
    private final Set<BarAlignment> alignments = new LinkedHashSet<BarAlignment>();

    /** All (physical) connections found between bars across staves. */
    private final Set<BarConnection> connections = new LinkedHashSet<BarConnection>();

    /** Constructor for brace compound. */
    private final CompoundConstructor braceConstructor;

    /** Constructor for (bracket) serif compound. */
    private final CompoundConstructor serifConstructor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Retrieve the bar lines of all staves.
     *
     * @param sheet the sheet to process
     */
    public BarsRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        scale = sheet.getScale();

        // Scale-dependent parameters
        params = new Parameters(scale);

        // Companions
        staffManager = sheet.getStaffManager();

        // Specific constructors
        braceConstructor = new CompoundConstructor()
        {
            @Override
            public SectionCompound newInstance ()
            {
                return new CurvedFilament(scale.getInterline(), params.braceSegmentLength);
            }
        };
        serifConstructor = new CompoundConstructor()
        {
            @Override
            public SectionCompound newInstance ()
            {
                return new StraightFilament(scale.getInterline());
            }
        };
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //------------------//
    // buildVerticalLag //
    //------------------//
    /**
     * Build the underlying vertical lag, from the provided runs table.
     * This method must be called before building info.
     *
     * @param longVertTable the provided table of (long) vertical runs
     */
    public void buildVerticalLag (RunTable longVertTable)
    {
        vLag = sheet.getLagManager().buildVerticalLag(longVertTable);
    }

    //---------//
    // process //
    //---------//
    /**
     * Retrieve all bar lines, brackets and braces in the sheet and create systems,
     * groups and parts.
     *
     * @throws StepException raised if processing must stop
     */
    public void process ()
            throws StepException
    {
        // Individual staff analysis to find bar peaks on x-axis projection
        findBarPeaks();

        // Build core glyph for each peak
        buildBarSticks();

        // Remove braces
        purgeBracePeaks();

        // Find all bar (or bracket) alignments across staves
        findAlignments();

        // Find all concrete connections across staves
        findConnections();

        // Purge conflicting connections
        purgeConnections();

        // Purge alignments incompatible with connections
        purgeAlignments();

        // Detect top and bottom portions of brackets
        detectBracketEnds();

        // Detect middle portions of brackets
        detectBracketMiddles();

        // Detect long peaks that do not connect staves
        detectLongPeaks();

        // Purge long peaks (and delete their alignments/connections)
        purgeLongPeaks();

        // Create systems from bar connections
        SystemManager mgr = sheet.getSystemManager();
        mgr.setSystems(createSystems(getSystemTops()));
        logger.info("{}Systems: {}", sheet.getLogPrefix(), mgr.getSystemsString());

        // Purge alignments across systems, they are not relevant
        purgeCrossAlignments();

        // Define precisely all staff side abscissae
        refineSides();

        // Purge C-clef-based false barlines
        purgeCClefs();

        // In multi-staff systems, boost the aligned peaks, weaken/delete the isolated ones
        checkUnalignedPeaks();

        // Partition peaks between thin and thick
        partitionWidths();

        // Create barline and bracket interpretations within each system
        createInters();

        // Create bar and bracket connection across staves
        createConnectionInters();

        // Detect grouped bar lines
        groupBarlines();

        // Record bars in staff
        recordBars();

        // Look for brace portion just before staff left side
        detectBraceEnds();

        // Build braces across staves
        buildBraces();

        // Build parts and groups
        createParts();
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the axis of each bar line / bracket / connection.
     *
     * @param g graphics context
     */
    @Override
    public void renderItems (Graphics2D g)
    {
        if (!constants.showVerticalLines.isSet()) {
            return;
        }

        final Rectangle clip = g.getClipBounds();
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        final Color oldColor = g.getColor();
        g.setColor(Colors.ENTITY_MINOR);

        // Draw bar lines (only within staff height)
        for (Staff staff : staffManager.getStaves()) {
            for (StaffPeak peak : staff.getPeaks()) {
                if (peak instanceof StaffPeak.Brace) {
                    continue;
                }

                Rectangle peakBox = new Rectangle(
                        peak.getStart(),
                        peak.getTop(),
                        peak.getWidth(),
                        peak.getBottom() - peak.getTop());

                if (clip.intersects(peakBox)) {
                    double xMid = (peak.getStart() + peak.getStop()) / 2d;
                    Line2D line = new Line2D.Double(xMid, peak.getTop(), xMid, peak.getBottom());
                    g.draw(line);
                }
            }
        }

        // Draw Connections (outside of staff height)
        for (BarConnection connection : connections) {
            Line2D median = connection.getMedian();

            if (median.intersects(clip)) {
                g.draw(median);
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    //--------------//
    // alignedPeaks //
    //--------------//
    /**
     * Report the BarPeak instance(s) that are aligned with the provided peak, looking
     * on the provided vertical side.
     *
     * @param peak the peak to check from
     * @param side which side to look (from provided peak)
     * @return the collection of peaks found, perhaps empty
     */
    private List<StaffPeak.Bar> alignedPeaks (StaffPeak.Bar peak,
                                              VerticalSide side)
    {
        final List<StaffPeak.Bar> found = new ArrayList<StaffPeak.Bar>();

        for (BarAlignment alignment : alignmentsOf(peak, side.opposite())) {
            found.add(alignment.getPeak(side));
        }

        return found;
    }

    //--------------//
    // alignmentsOf //
    //--------------//
    /**
     * Report the collection of alignments for which the provided peak is involved on
     * desired vertical side.
     *
     * @param peak the peak to check for
     * @param side the desired vertical side
     * @return the collection found, perhaps empty.
     */
    private Set<BarAlignment> alignmentsOf (StaffPeak.Bar peak,
                                            VerticalSide side)
    {
        Set<BarAlignment> found = new HashSet<BarAlignment>();

        for (BarAlignment alignment : alignments) {
            if (alignment.getPeak(side) == peak) {
                found.add(alignment);
            }
        }

        return found;
    }

    //----------------//
    // buildBarSticks //
    //----------------//
    /**
     * Build the underlying stick of every peak.
     * <p>
     * These glyphs are needed to detect those peaks which go past staff height above,
     * below, or both, and may rather be stems.
     * They are used also to detect curly peaks that are due to brace portions.
     * <p>
     * For each peak, we take a vertical "slice" of the relevant sections using a lookup area.
     * We then run a dedicated factory on the sections and make it focus on the bar core area.
     */
    private void buildBarSticks ()
    {
        // Preselect sections of proper max width
        final int maxWidth = getMaxPeaksWidth();
        List<Section> allSections = getSectionsByWidth(maxWidth);
        logger.debug("sections:{}", allSections.size());

        BarFilamentFactory factory = new BarFilamentFactory(sheet.getScale());

        for (Staff staff : staffManager.getStaves()) {
            for (StaffPeak peak : staff.getPeaks()) {
                // Take proper slice of sections for this peak
                List<Section> sections = getPeakSections(peak, allSections);
                Filament filament = factory.buildBarFilament(sections, peak.getBounds());
                peak.setFilament(filament);
                logger.debug("Staff#{} {}", staff.getId(), peak);
            }
        }
    }

    //--------------------//
    // buildBraceFilament //
    //--------------------//
    /**
     * Build the brace filament that goes from top peak to bottom peak.
     *
     * @param topPeak    peak for top portion
     * @param bottomPeak peak for bottom portion
     * @return the brace filament
     */
    private Filament buildBraceFilament (StaffPeak topPeak,
                                         StaffPeak bottomPeak,
                                         List<Section> allSections)
    {
        // Define (perhaps slanted) area, slightly increased to the left
        final Path2D path = new Path2D.Double();
        path.moveTo(topPeak.getStart() - params.braceLeftMargin, topPeak.getTop()); // Upper left
        path.lineTo(topPeak.getStop() + 1, topPeak.getTop()); // Upper right
        path.lineTo(bottomPeak.getStop() + 1, bottomPeak.getBottom() + 1); // Lower right
        path.lineTo(bottomPeak.getStart() - params.braceLeftMargin, bottomPeak.getBottom() + 1); // Lower left
        path.closePath();

        final Area area = new Area(path);
        final Filament topFilament = topPeak.getFilament();
        final Filament bottomFilament = bottomPeak.getFilament();
        final List<Section> sections = getAreaSections(area, allSections);
        sections.removeAll(topFilament.getMembers());
        sections.removeAll(bottomFilament.getMembers());

        // Now we have two end glyphs and a few sections in the middle
        Filament compound = (Filament) CompoundFactory.buildCompoundFromParts(
                Arrays.asList(topFilament, bottomFilament),
                braceConstructor);

        boolean expanding;

        do {
            expanding = false;

            for (Iterator<Section> it = sections.iterator(); it.hasNext();) {
                Section section = it.next();

                if (compound.touches(section)) {
                    compound.addSection(section);
                    it.remove();
                    expanding = true;

                    break;
                }
            }
        } while (expanding);

        return compound;
    }

    //-------------//
    // buildBraces //
    //-------------//
    /**
     * Retrieve concrete braces between staves with brace portions.
     * <p>
     * TODO: Implement and test a case with more than 2 staves embraced.
     */
    private void buildBraces ()
    {
        List<Section> allSections = null;
        final List<Staff> staves = staffManager.getStaves();
        final GlyphIndex nest = sheet.getGlyphIndex();

        StaffLoop:
        for (int iStaff = 0; iStaff < staves.size(); iStaff++) {
            Staff staff = staves.get(iStaff);
            final List<StaffPeak> peaks = staff.getPeaks();

            if (peaks.isEmpty()) {
                continue;
            }

            StaffPeak peak = peaks.get(0);

            if (peak.isSet(BRACE_TOP)) {
                for (Staff otherStaff : staves.subList(iStaff + 1, staves.size())) {
                    final List<StaffPeak> otherPeaks = otherStaff.getPeaks();

                    if (otherPeaks.isEmpty()) {
                        logger.warn("Staff#{} isolated brace top", staff.getId());

                        continue StaffLoop;
                    }

                    StaffPeak otherPeak = otherPeaks.get(0);

                    if (!otherPeak.isSet(BRACE_BOTTOM)) {
                        logger.warn("Staff#{} expected a brace bottom", otherStaff.getId());

                        continue StaffLoop;
                    }

                    // Retrieve the full brace glyph
                    if (allSections == null) {
                        allSections = getSectionsByWidth(params.maxBraceThickness);
                    }

                    Filament braceFilament = buildBraceFilament(peak, otherPeak, allSections);
                    Glyph glyph = nest.registerOriginal(braceFilament.toGlyph(null));
                    BraceInter braceInter = new BraceInter(glyph, Inter.intrinsicRatio * 1);
                    SIGraph sig = staff.getSystem().getSig();
                    sig.addVertex(braceInter);

                    // Skip the staves processed
                    iStaff = staves.indexOf(otherStaff);

                    continue StaffLoop;
                }
            }
        }
    }

    //--------------------//
    // buildSerifFilament //
    //--------------------//
    /**
     * Build the filament that may represent a bracket end serif.
     *
     * @param staff    containing staff
     * @param sections the population of candidate sections
     * @param side     top or bottom of staff
     * @param roi      the rectangular roi for the serif
     * @return the filament built
     */
    private Filament buildSerifFilament (Staff staff,
                                         Set<Section> sections,
                                         VerticalSide side,
                                         Rectangle roi)
    {
        // Retrieve all glyphs out of connected sections
        List<SectionCompound> compounds = CompoundFactory.buildCompounds(
                sections,
                serifConstructor);
        logger.debug("Staff#{} serif {}", staff.getId(), Entities.ids(compounds));

        if (compounds.size() > 1) {
            // Sort filaments according to their distance from bar/roi vertex
            final Point vertex = new Point(roi.x, roi.y + ((side == TOP) ? (roi.height - 1) : 0));
            Collections.sort(
                    compounds,
                    new Comparator<SectionCompound>()
            {
                @Override
                public int compare (SectionCompound g1,
                                    SectionCompound g2)
                {
                    double d1 = PointUtil.length(
                            GeoUtil.vectorOf(g1.getCentroid(), vertex));
                    double d2 = PointUtil.length(
                            GeoUtil.vectorOf(g2.getCentroid(), vertex));

                    return Double.compare(d1, d2);
                }
            });

            // Pickup the first ones and stop as soon as minimum weight is reached
            int totalWeight = 0;

            for (int i = 0; i < compounds.size(); i++) {
                SectionCompound compound = compounds.get(i);
                totalWeight += compound.getWeight();

                if (totalWeight >= params.serifMinWeight) {
                    compounds = compounds.subList(0, i + 1);

                    break;
                }
            }

            return (Filament) CompoundFactory.buildCompoundFromParts(compounds, serifConstructor);
        } else {
            return (Filament) compounds.get(0);
        }
    }

    //-----------------//
    // checkConnection //
    //-----------------//
    /**
     * Check whether the provided alignment is a true connection, that is with concrete
     * foreground pixels on the alignment line.
     * <p>
     * For this, we define a spline which goes through top & bottom points of each of the aligned
     * peaks and look for pixels in the area.
     *
     * @param alignment the alignment to check.
     * @return the connection if OK, null otherwise
     */
    private BarConnection checkConnection (BarAlignment alignment)
    {
        ByteProcessor pixelFilter = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
        StaffPeak.Bar p1 = alignment.topPeak;
        StaffPeak.Bar p2 = alignment.bottomPeak;

        // Theoretical lines on left and right sides
        final GeoPath leftLine = new GeoPath(
                new Line2D.Double(
                        new Point2D.Double(p1.getStart(), p1.getBottom()),
                        new Point2D.Double(p2.getStart(), p2.getTop())));
        final GeoPath rightLine = new GeoPath(
                new Line2D.Double(
                        new Point2D.Double(p1.getStop(), p1.getBottom()),
                        new Point2D.Double(p2.getStop(), p2.getTop())));

        final CoreData data = AreaUtil.verticalCore(pixelFilter, leftLine, rightLine);

        if (p1.getFilament().isVip() || p2.getFilament().isVip()) {
            logger.info(
                    "VIP checkConnection S#{} {} and S#{} {} {}",
                    p1.getStaff().getId(),
                    p1,
                    p2.getStaff().getId(),
                    p2,
                    data);
        }

        if ((data.gap <= params.maxConnectionGap)
            && (data.whiteRatio <= params.maxConnectionWhiteRatio)) {
            double whiteImpact = 1 - (data.whiteRatio / params.maxConnectionWhiteRatio);
            double gapImpact = 1 - ((double) data.gap / params.maxConnectionGap);
            double alignImpact = alignment.getImpacts().getGrade() / alignment.getImpacts()
                    .getIntrinsicRatio();
            GradeImpacts impacts = new BarConnection.Impacts(alignImpact, whiteImpact, gapImpact);
            double grade = impacts.getGrade();
            logger.debug("{} grade:{} impacts:{}", alignment, grade, impacts);

            if (grade >= params.minConnectionGrade) {
                return new BarConnection(alignment, impacts);
            }
        }

        return null;
    }

    //---------------------//
    // checkUnalignedPeaks //
    //---------------------//
    /**
     * Only for multi-staff systems, check the peaks alignments.
     * Give a bonus to every peak aligned (or connected) with a peak in a staff nearby and
     * weaken (or delete) the isolated ones.
     */
    private void checkUnalignedPeaks ()
    {
        final boolean deletion = constants.deleteUnalignedBars.isSet();

        for (SystemInfo system : sheet.getSystems()) {
            if (system.getStaves().size() > 1) {
                for (Staff staff : system.getStaves()) {
                    List<StaffPeak> toRemove = new ArrayList<StaffPeak>();

                    for (StaffPeak p : staff.getPeaks()) {
                        if (p instanceof StaffPeak.Bar) {
                            StaffPeak.Bar peak = (StaffPeak.Bar) p;
                            final AbstractVerticalInter inter = peak.getInter();

                            if (isAligned(peak, TOP) || isAligned(peak, BOTTOM)) {
                                peak.set(ALIGNED);
                            } else {
                                peak.set(UNALIGNED);

                                if (deletion) {
                                    toRemove.add(peak);
                                }
                            }
                        }
                    }

                    if (!toRemove.isEmpty()) {
                        logger.debug("Staff#{} removing isolated {}", staff.getId(), toRemove);
                        staff.removePeaks(toRemove);
                    }
                }
            }
        }
    }

    //------------------------//
    // createConnectionInters //
    //------------------------//
    /**
     * Populate all systems sigs with connection inters for bar line and brackets.
     */
    private void createConnectionInters ()
    {
        for (BarConnection connection : connections) {
            StaffPeak.Bar topPeak = connection.topPeak;
            SystemInfo system = topPeak.getStaff().getSystem();
            SIGraph sig = system.getSig();

            if (topPeak.isBracket()) {
                sig.addVertex(new BracketConnectorInter(connection, connection.getImpacts()));
            } else {
                sig.addVertex(
                        new BarConnectorInter(
                                connection,
                                topPeak.isSet(THICK) ? Shape.THICK_CONNECTOR : Shape.THIN_CONNECTOR,
                                connection.getImpacts()));
            }

            // Also, connected bars support each other
            Relation bcRel = new BarConnectionRelation(connection.getImpacts());
            StaffPeak.Bar bottomPeak = connection.bottomPeak;
            sig.addEdge(topPeak.getInter(), bottomPeak.getInter(), bcRel);
        }
    }

    //--------------//
    // createInters //
    //--------------//
    /**
     * Based on remaining peaks, populate each system sig with proper inters for bar
     * lines and for brackets.
     */
    private void createInters ()
    {
        final double up = constants.alignedIncreaseRatio.getValue();
        final double down = constants.unalignedDecreaseRatio.getValue();

        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Staff staff : system.getStaves()) {
                for (StaffPeak p : staff.getPeaks()) {
                    if (p instanceof StaffPeak.Brace) {
                        continue;
                    }

                    StaffPeak.Bar peak = (StaffPeak.Bar) p;
                    double x = (peak.getStart() + peak.getStop()) / 2d;
                    Line2D median = new Line2D.Double(
                            x,
                            peak.getTop(),
                            x,
                            peak.getBottom());

                    final Glyph glyph = sheet.getGlyphIndex().registerOriginal(
                            peak.getFilament().toGlyph(null));
                    final AbstractVerticalInter inter;

                    if (peak.isBracket()) {
                        BracketKind kind = getBracketKind(peak);
                        inter = new BracketInter(
                                glyph,
                                peak.getImpacts(),
                                median,
                                peak.getWidth(),
                                kind);
                    } else {
                        inter = new BarlineInter(
                                glyph,
                                peak.isSet(THICK) ? Shape.THICK_BARLINE : Shape.THIN_BARLINE,
                                peak.getImpacts(),
                                median,
                                peak.getWidth());

                        for (HorizontalSide side : HorizontalSide.values()) {
                            if (peak.isStaffEnd(side)) {
                                ((BarlineInter) inter).setStaffEnd(side);
                            }
                        }

                        if (peak.isSet(ALIGNED)) {
                            inter.increase(up);
                        }

                        if (peak.isSet(UNALIGNED)) {
                            inter.decrease(down);
                        }
                    }

                    sig.addVertex(inter);
                    inter.setStaff(staff);
                    peak.setInter(inter);
                }
            }
        }
    }

    //------------//
    // createPart //
    //------------//
    private Part createPart (SystemInfo system,
                             Staff first,
                             Staff last)
    {
        Part part = new Part(system);

        part.addStaff(first);

        if (last != first) {
            List<Staff> staves = system.getStaves();
            int iFirst = staves.indexOf(first);
            int iLast = staves.indexOf(last);

            for (Staff staff : staves.subList(iFirst + 1, iLast + 1)) {
                part.addStaff(staff);
            }
        }

        part.setId(1 + system.getParts().size());
        system.addPart(part);

        return part;
    }

    //-------------//
    // createParts //
    //-------------//
    /**
     * Within each system, retrieve all parts and groups.
     * <p>
     * A bracket defines a (bracket) group.
     * No staff connection implies different parts.
     * Braced staves represent a single part when not connected to other staves, otherwise it is a
     * group.
     */
    private void createParts ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            List<Staff> staves = system.getStaves();

            // All groups in this system
            List<PartGroup> allGroups = system.getPartGroups();

            // Current active groups
            Map<Integer, PartGroup> activeGroups = new TreeMap<Integer, PartGroup>();

            for (Staff staff : staves) {
                List<StaffPeak> peaks = staff.getPeaks();

                // Look for starting barline
                StaffPeak.Bar endBar = getPeakEnd(LEFT, peaks);

                if (endBar == null) {
                    // Staff = system, just one part
                    logger.debug("Staff#{} no starting barline", staff.getId());
                    createPart(system, staff, staff);

                    continue;
                }

                int endIndex = peaks.indexOf(endBar);

                if (endIndex == 0) {
                    // Isolated staff: just one part
                    logger.debug("Staff#{} nothing before starting barline", staff.getId());
                    createPart(system, staff, staff);

                    continue;
                }

                // Check for part connection below this staff
                boolean bottomConn = isPartConnected(staff, BOTTOM);

                // Going from endBar to the left, look for bracket(s) & brace
                int level = 0;

                for (int i = endIndex - 1; i >= 0; i--) {
                    level++;

                    StaffPeak peak = peaks.get(i);

                    if (peak.isBracket()) {
                        final PartGroup pg;

                        if (peak.isBracketEnd(TOP)) {
                            // Start bracket group
                            pg = new PartGroup(level, Symbol.bracket, bottomConn, staff);
                            allGroups.add(pg);
                            activeGroups.put(level, pg);
                            logger.debug("Staff#{} start bracket {}", staff.getId(), pg);
                        } else {
                            // Continue bracket group
                            pg = activeGroups.get(level);

                            if (pg != null) {
                                pg.setLastStaff(staff);

                                // Stop bracket group?
                                if (peak.isBracketEnd(BOTTOM)) {
                                    logger.debug("Staff#{} stop bracket {}", staff.getId(), pg);
                                    activeGroups.put(level, null);
                                } else {
                                    logger.debug("Staff#{} continue bracket {}", staff.getId(), pg);
                                }
                            } else {
                                logger.warn("Staff#{} no group level:{}", staff.getId(), level);
                            }
                        }

                        // All peaks browsed?
                        if (i == 0) {
                            createPart(system, staff, staff);
                        }
                    } else if (peak instanceof StaffPeak.Brace) {
                        if (peak.isBraceEnd(TOP)) {
                            // Start brace group
                            PartGroup pg = new PartGroup(level, Symbol.brace, bottomConn, staff);
                            allGroups.add(pg);
                            activeGroups.put(level, pg);
                            logger.debug("Staff#{} start brace {}", staff.getId(), pg);
                        } else {
                            // Continue brace group
                            PartGroup pg = activeGroups.get(level);

                            if (pg != null) {
                                pg.setLastStaff(staff);

                                // Stop brace group?
                                if (peak.isBraceEnd(BOTTOM)) {
                                    activeGroups.put(level, null);

                                    // Was this brace a real group?
                                    if (!bottomConn && !isPartConnected(pg.getFirstStaff(), TOP)) {
                                        // No, just a multi-staff instrument
                                        logger.debug(
                                                "Staff#{} end multi-staff instrument {}",
                                                staff.getId(),
                                                pg);
                                        allGroups.remove(pg);
                                        createPart(system, pg.getFirstStaff(), pg.getLastStaff());
                                    } else {
                                        int i1 = staves.indexOf(pg.getFirstStaff());
                                        int i2 = staves.indexOf(pg.getLastStaff());
                                        logger.debug("Staff#{} stop brace {}", staff.getId(), pg);

                                        for (Staff s : staves.subList(i1, i2 + 1)) {
                                            createPart(system, s, s);
                                        }
                                    }
                                }
                            } else {
                                logger.warn("Staff#{} no group level:{}", staff.getId(), level);
                            }
                        }
                    } else {
                        logger.warn("Staff#{} unexpected bar {}", staff.getId(), peak);
                    }
                }
            }

            if (!allGroups.isEmpty()) {
                logger.info(stringOf(allGroups, system));
            }
        }
    }

    //---------------//
    // createSystems //
    //---------------//
    /**
     * Build SystemInfo for each detected system.
     *
     * @param systems starting staves
     * @return the sequence of systems
     */
    private List<SystemInfo> createSystems (Integer[] systemTops)
    {
        final List<SystemInfo> newSystems = new ArrayList<SystemInfo>();
        Integer staffTop = null;
        int systemId = 0;
        SystemInfo system = null;

        for (int i = 0; i < staffManager.getStaffCount(); i++) {
            Staff staff = staffManager.getStaff(i);

            // System break?
            if ((staffTop == null) || (staffTop < systemTops[i])) {
                // Start of a new system
                staffTop = systemTops[i];

                system = new SystemInfo(++systemId, sheet, staffManager.getRange(staff, staff));
                newSystems.add(system);
            } else {
                // Continuing current system
                system.setStaves(staffManager.getRange(system.getFirstStaff(), staff));
            }
        }

        return newSystems;
    }

    //--------------//
    // detectBraces //
    //--------------//
    /**
     * Look for brace top or bottom portion just before left bar line (or bracket).
     */
    private void detectBraceEnds ()
    {
        // Preselect sections of proper max width
        final List<Section> allSections = getSectionsByWidth(params.maxBraceThickness);
        final FilamentIndex filamentIndex = sheet.getFilamentIndex();

        BarFilamentFactory factory = new BarFilamentFactory(sheet.getScale());

        for (StaffProjector projector : projectors) {
            Staff staff = projector.getStaff();
            List<StaffPeak> peaks = staff.getPeaks();

            if (peaks.isEmpty()) {
                continue;
            }

            // Check first peak is on left of staff
            final StaffPeak firstPeak = peaks.get(0);

            if (firstPeak.getStart() > staff.getAbscissa(LEFT)) {
                continue;
            }

            // Try to extract a brace-compatible peak before bar line or bracket
            final int maxRight = firstPeak.getStart() - 1;
            final int minLeft = maxRight - params.maxBraceWidth;
            final StaffPeak.Brace bracePeak = projector.findBracePeak(minLeft, maxRight);

            if (bracePeak == null) {
                continue;
            }

            // Take proper slice of sections for this peak
            List<Section> sections = getPeakSections(bracePeak, allSections);
            Filament filament = factory.buildBarFilament(sections, bracePeak.getBounds());
            filamentIndex.register(filament);
            bracePeak.setFilament(filament);

            // A few tests on glyph
            if (filament.getLength(VERTICAL) < ((staff.getLineCount() - 1) * scale.getInterline())) {
                continue;
            }

            double curvature = filament.getMeanCurvature();
            logger.debug(
                    "Staff#{} brace curvature:{} vs {}",
                    staff.getId(),
                    curvature,
                    params.maxBraceCurvature);

            if (curvature >= params.maxBraceCurvature) {
                continue;
            }

            boolean beyondTop = false;
            boolean beyondBottom = false;

            for (VerticalSide side : VerticalSide.values()) {
                double ext = extensionOf(bracePeak, side);

                if (ext > params.maxBraceExtension) {
                    if (side == TOP) {
                        beyondTop = true;
                    } else {
                        beyondBottom = true;
                    }
                }
            }

            if (beyondTop && beyondBottom) {
                bracePeak.set(BRACE_MIDDLE);
            } else if (beyondBottom) {
                bracePeak.set(BRACE_TOP);
            } else if (beyondTop) {
                bracePeak.set(BRACE_BOTTOM);
            }

            logger.debug("Staff#{} {}", staff.getId(), bracePeak);
            staff.insertBracePeak(bracePeak);
        }
    }

    //-------------------//
    // detectBracketEnds //
    //-------------------//
    /**
     * Detect the peaks that correspond to top or bottom end of brackets.
     * <p>
     * Such bracket end is characterized as follows:<ul>
     * <li>It is the first peak on staff left (since braces have been removed at this point).</li>
     * <li>It is a rather thick peak.</li>
     * <li>It sometimes (but not always) goes a bit beyond staff top or bottom line.</li>
     * <li>It has a serif shape at end.</li>
     * </ul>
     */
    private void detectBracketEnds ()
    {
        for (Staff staff : staffManager.getStaves()) {
            List<StaffPeak> peaks = staff.getPeaks();

            if (peaks.isEmpty()) {
                continue;
            }

            final StaffPeak p = peaks.get(0);

            if (p instanceof StaffPeak.Brace) {
                continue;
            }

            final StaffPeak.Bar peak = (StaffPeak.Bar) p;
            final StaffPeak.Bar nextPeak = (StaffPeak.Bar) ((peaks.size() > 1) ? peaks.get(1) : null);

            // Check first peak is on left of staff
            if (peak.getStart() > staff.getAbscissa(LEFT)) {
                continue;
            }

            // Sufficient width?
            if (peak.getWidth() < params.minBracketWidth) {
                continue;
            }

            // It cannot go too far beyond staff height
            for (VerticalSide side : VerticalSide.values()) {
                double ext = extensionOf(peak, side);

                // Check for serif shape
                //TODO: perhaps record serif glyph in peak (so that it can be easily erased)?
                if ((ext <= params.maxBracketExtension) && hasSerif(staff, peak, nextPeak, side)) {
                    logger.debug("Staff#{} {} bracket end", staff.getId(), side);

                    peak.setBracketEnd(side);
                }
            }
        }
    }

    //----------------------//
    // detectBracketMiddles //
    //----------------------//
    /**
     * Among peaks, flag the ones that correspond to brackets middle portions.
     */
    private void detectBracketMiddles ()
    {
        // Flag recursively any peak connected to a bracket end
        boolean modified;

        do {
            modified = false;

            for (BarConnection connection : connections) {
                StaffPeak.Bar top = connection.topPeak;
                StaffPeak.Bar bottom = connection.bottomPeak;

                if (top.isBracket()) {
                    if (!bottom.isBracket()) {
                        bottom.set(BRACKET_MIDDLE);
                        modified = true;
                    }
                } else if (bottom.isBracket()) {
                    top.set(BRACKET_MIDDLE);
                    modified = true;
                }
            }
        } while (modified == true);
    }

    //-----------------//
    // detectLongPeaks //
    //-----------------//
    /**
     * Detect long bars (those getting above or below the related staff).
     * <p>
     * Just after a bracket end, the glyph of following bar line may go slightly beyond staff.
     */
    private void detectLongPeaks ()
    {
        for (Staff staff : staffManager.getStaves()) {
            for (int index = 0; index < staff.getPeaks().size(); index++) {
                List<StaffPeak> peaks = staff.getPeaks();
                StaffPeak peak = peaks.get(index);

                // Check whether the glyph gets above and/or below the staff
                if (!peak.isBracket()) {
                    for (VerticalSide side : VerticalSide.values()) {
                        double ext = extensionOf(peak, side);

                        if (ext > params.maxBarExtension) {
                            if (!isJustAfterBracket(peaks, (StaffPeak.Bar) peak, side)) {
                                peak.setBeyond(side);
                            }
                        }
                    }
                }
            }
        }
    }

    //-------------//
    // extensionOf //
    //-------------//
    /**
     * Report how much a peak goes beyond a staff limit line.
     *
     * @param peak the peak to check
     * @param side TOP or BOTTOM
     * @return delta ordinate beyond staff
     */
    private double extensionOf (StaffPeak peak,
                                VerticalSide side)
    {
        final Rectangle box = peak.getFilament().getBounds();
        final double halfLine = scale.getMaxFore() / 2.0;

        if (side == TOP) {
            return (peak.getTop() - halfLine - box.y);
        } else {
            return ((box.y + box.height) - 1 - halfLine - peak.getBottom());
        }
    }

    //----------------//
    // findAlignments //
    //----------------//
    /**
     * Find all bar (or bracket) alignments across staves.
     */
    private void findAlignments ()
    {
        // Check for peaks aligned across staves
        for (Staff staff : staffManager.getStaves()) {
            for (VerticalSide side : VerticalSide.values()) {
                List<Staff> otherStaves = staffManager.vertNeighbors(staff, side);

                // Make sure there are other staves on this side and they are "short-wise compatible"
                // with current staff
                if (otherStaves.isEmpty() || (otherStaves.get(0).isShort() != staff.isShort())) {
                    continue;
                }

                // Look for all alignment/connection relations
                for (StaffPeak peak : staff.getPeaks()) {
                    // Look for a suitable partnering peak in stave(s) nearby
                    if (peak instanceof StaffPeak.Brace) {
                        continue;
                    }

                    for (Staff otherStaff : otherStaves) {
                        lookupPeaks((StaffPeak.Bar) peak, side, otherStaff);
                    }
                }
            }
        }
    }

    //--------------//
    // findBarPeaks //
    //--------------//
    /**
     * Use individual staff projections to retrieve bar peaks.
     */
    private void findBarPeaks ()
    {
        // Analysis staff per staff
        for (Staff staff : staffManager.getStaves()) {
            StaffProjector projector = new StaffProjector(sheet, staff);
            projectors.add(projector);
            projector.process();
        }
    }

    //-----------------//
    // findConnections //
    //-----------------//
    /**
     * Find all bar (or bracket) concrete connections across staves.
     */
    private void findConnections ()
    {
        // Check among the alignments for peaks connected across staves
        for (Iterator<BarAlignment> it = alignments.iterator(); it.hasNext();) {
            BarAlignment alignment = it.next();

            // Look for concrete connection
            BarConnection connection = checkConnection(alignment);

            if (connection != null) {
                connections.add(connection);
                // Remove the underlying alignment
                it.remove();
            }
        }
    }

    //-----------------//
    // getAreaSections //
    //-----------------//
    private List<Section> getAreaSections (Area area,
                                           List<Section> allSections)
    {
        final Rectangle areaBox = area.getBounds();
        final int xBreak = areaBox.x + areaBox.width;
        final List<Section> sections = new ArrayList<Section>();

        for (Section section : allSections) {
            final Rectangle sectionBox = section.getBounds();

            if (area.contains(sectionBox)) {
                sections.add(section);
            } else if (sectionBox.x >= xBreak) {
                break; // Since allSections are sorted by abscissa
            }
        }

        return sections;
    }

    //----------------//
    // getBracketKind //
    //----------------//
    private BracketKind getBracketKind (StaffPeak peak)
    {
        if (peak.isSet(BRACKET_MIDDLE)) {
            return BracketKind.NONE;
        }

        if (peak.isBracketEnd(TOP)) {
            if (peak.isBracketEnd(BOTTOM)) {
                return BracketKind.BOTH;
            } else {
                return BracketKind.TOP;
            }
        }

        if (peak.isBracketEnd(BOTTOM)) {
            return BracketKind.BOTTOM;
        } else {
            return null;
        }
    }

    //------------------//
    // getMaxPeaksWidth //
    //------------------//
    /**
     * Retrieve the maximum width among all peaks.
     *
     * @return the maximum width
     */
    private int getMaxPeaksWidth ()
    {
        int maxWidth = 0;

        for (Staff staff : staffManager.getStaves()) {
            for (StaffPeak peak : staff.getPeaks()) {
                if (!(peak instanceof StaffPeak.Brace)) {
                    maxWidth = Math.max(maxWidth, peak.getWidth());
                }
            }
        }

        return maxWidth;
    }

    //------------//
    // getPeakEnd //
    //------------//
    private StaffPeak.Bar getPeakEnd (HorizontalSide side,
                                      List<StaffPeak> peaks)
    {
        for (StaffPeak peak : peaks) {
            if (peak.isStaffEnd(side)) {
                return (StaffPeak.Bar) peak;
            }
        }

        return null;
    }

    //-----------------//
    // getPeakSections //
    //-----------------//
    /**
     * Select proper candidate sections for the peak at hand.
     *
     * @param peak        the peak to process
     * @param allSections pre-filtered sheet-level collection
     * @return the sub-collection of relevant sections
     */
    private List<Section> getPeakSections (StaffPeak peak,
                                           List<Section> allSections)
    {
        final Rectangle peakBox = new Rectangle(
                peak.getStart(),
                peak.getTop(),
                peak.getWidth(),
                peak.getBottom() - peak.getTop() + 1);

        // Increase height slightly beyond staff to allow detection of bracket ends
        peakBox.grow(0, params.bracketLookupExtension);

        final int xBreak = peakBox.x + peakBox.width;
        final List<Section> sections = new ArrayList<Section>();
        final int maxSectionWidth = peak.getWidth();

        for (Section section : allSections) {
            final Rectangle sectionBox = section.getBounds();

            if (sectionBox.intersects(peakBox)) {
                if (section.getLength(HORIZONTAL) <= maxSectionWidth) {
                    sections.add(section);
                }
            } else if (sectionBox.x >= xBreak) {
                break; // Since allSections are sorted by abscissa
            }
        }

        return sections;
    }

    //--------------------//
    // getSectionsByWidth //
    //--------------------//
    /**
     * Select relevant sections for bar sticks.
     * <p>
     * Both vertical and horizontal sections are OK if they are not wider than the maximum allowed.
     * The global collection is sorted on abscissa.
     *
     * @param maxWidth maximum section horizontal width
     * @return the abscissa-sorted list of compliant sections
     */
    private List<Section> getSectionsByWidth (int maxWidth)
    {
        List<Section> sections = new ArrayList<Section>();
        Lag hLag = sheet.getLagManager().getLag(Lags.HLAG);

        for (Lag lag : Arrays.asList(vLag, hLag)) {
            for (Section section : lag.getEntities()) {
                if (section.getLength(HORIZONTAL) <= maxWidth) {
                    sections.add(section);
                }
            }
        }

        Collections.sort(sections, Section.byAbscissa);

        return sections;
    }

    //---------------//
    // getSystemTops //
    //---------------//
    /**
     * Use connections across staves to gather staves into systems.
     *
     * @return the system top, per staff
     */
    private Integer[] getSystemTops ()
    {
        // For each staff, gives the staff that starts the containing system
        final Integer[] systemTops = new Integer[staffManager.getStaffCount()];

        // Connections are ordered per top staff then per abscissa.
        for (BarConnection connection : connections) {
            int top = connection.topPeak.getStaff().getId();
            int bottom = connection.bottomPeak.getStaff().getId();

            if (systemTops[top - 1] == null) {
                // First connection ever between the 2 staves
                systemTops[top - 1] = top;
            }

            systemTops[bottom - 1] = systemTops[top - 1];
        }

        // Complete assignments
        for (int i = 1; i <= systemTops.length; i++) {
            if (systemTops[i - 1] == null) {
                systemTops[i - 1] = i;
            }
        }

        final int[] ids = new int[staffManager.getStaffCount()];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = i + 1;
        }

        logger.debug("Staves:  {}", ids);
        logger.debug("Systems: {}", (Object) systemTops);

        return systemTops;
    }

    //-------------------//
    // getWidthHistogram //
    //-------------------//
    /**
     * Build an histogram on widths of bars.
     *
     * @return the width histogram
     */
    private Histogram<Integer> getWidthHistogram ()
    {
        final Histogram<Integer> histo = new Histogram<Integer>();

        for (Staff staff : staffManager.getStaves()) {
            for (StaffPeak peak : staff.getPeaks()) {
                if (!(peak instanceof StaffPeak.Brace) && !peak.isBracket()) {
                    int width = peak.getWidth();
                    histo.increaseCount(width, 1);
                }
            }
        }

        logger.debug("Bars width histogram {}", histo.dataString());

        return histo;
    }

    //---------------//
    // groupBarPeaks //
    //---------------//
    /**
     * Dispatch all bar peaks into either isolated or grouped ones.
     *
     * @param isolated (output) collection of isolated peaks
     * @param groups   (output) collection of grouped peaks
     */
    private void groupBarPeaks (List<StaffPeak.Bar> isolated,
                                List<List<StaffPeak.Bar>> groups)
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                List<StaffPeak.Bar> group = null;
                StaffPeak.Bar prevPeak = null;

                for (StaffPeak p : staff.getPeaks()) {
                    if (p instanceof StaffPeak.Brace || p.isBracket()) {
                        if ((group == null) && (prevPeak != null)) {
                            isolated.add(prevPeak);
                        }

                        group = null;
                        prevPeak = null;
                    } else {
                        StaffPeak.Bar peak = (StaffPeak.Bar) p;

                        if (prevPeak != null) {
                            int gap = peak.getStart() - prevPeak.getStop() - 1;

                            if (gap <= params.maxDoubleBarGap) {
                                // We are in a group with previous peak
                                if (group == null) {
                                    groups.add(group = new ArrayList<StaffPeak.Bar>());
                                    group.add(prevPeak);
                                }

                                group.add(peak);
                            } else if (group != null) {
                                group = null;
                            } else {
                                isolated.add(prevPeak);
                            }
                        }

                        prevPeak = peak;
                    }
                }

                // End of staff
                if ((group == null) && (prevPeak != null)) {
                    isolated.add(prevPeak);
                }
            }
        }
    }

    //---------------//
    // groupBarlines //
    //---------------//
    /**
     * Detect bar lines organized in groups.
     */
    private void groupBarlines ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Staff staff : system.getStaves()) {
                StaffPeak.Bar prevPeak = null;

                for (StaffPeak peak : staff.getPeaks()) {
                    if (peak instanceof StaffPeak.Brace || peak.isBracket()) {
                        continue;
                    }

                    if (prevPeak != null) {
                        int gap = peak.getStart() - prevPeak.getStop() - 1;

                        if (gap <= params.maxDoubleBarGap) {
                            BarGroupRelation rel = new BarGroupRelation(scale.pixelsToFrac(gap));
                            sig.addEdge(prevPeak.getInter(), peak.getInter(), rel);
                        }
                    }

                    prevPeak = (StaffPeak.Bar) peak;
                }
            }
        }
    }

    //----------//
    // hasSerif //
    //----------//
    /**
     * Check whether the provided peak glyph exhibits a serif on desired side.
     * Define a region of interest just beyond glyph end and look for sections contained in roi.
     * Build a glyph from connected sections and check its shape.
     *
     * @param peak     provided peak
     * @param nextPeak following peak, if any
     * @param side     TOP or BOTTOM
     * @return true if serif is detected
     */
    private boolean hasSerif (Staff staff,
                              StaffPeak.Bar peak,
                              StaffPeak.Bar nextPeak,
                              VerticalSide side)
    {
        // Constants
        final int halfLine = (int) Math.ceil(scale.getMaxFore() / 2.0);

        // Define lookup region for serif
        final Filament barFilament = peak.getFilament();
        final Rectangle glyphBox = barFilament.getBounds();
        final int yBox = (side == TOP)
                ? (Math.min(
                        glyphBox.y + params.serifThickness,
                        peak.getTop() - halfLine) - params.serifRoiHeight)
                : Math.max(
                        (glyphBox.y + glyphBox.height) - params.serifThickness,
                        peak.getBottom() + halfLine);
        final Rectangle roi = new Rectangle(
                peak.getStop() + 1,
                yBox,
                params.serifRoiWidth,
                params.serifRoiHeight);
        barFilament.addAttachment(((side == TOP) ? "t" : "b") + "Serif", roi);

        // Look for intersected sections
        // Remove sections from bar peak (and from next peak if any)
        Lag hLag = sheet.getLagManager().getLag(Lags.HLAG);
        Set<Section> sections = hLag.intersectedSections(roi);
        sections.addAll(vLag.intersectedSections(roi));
        sections.removeAll(barFilament.getMembers());

        if (nextPeak != null) {
            sections.removeAll(nextPeak.getFilament().getMembers());
        }

        if (sections.isEmpty()) {
            return false;
        }

        // Retrieve serif glyph from sections
        Filament serif = buildSerifFilament(staff, sections, side, roi);
        serif.computeLine();

        double slope = serif.getSlope();
        logger.debug(
                "Staff#{} {} {} serif#{} weight:{} slope:{}",
                staff.getId(),
                peak,
                side,
                serif.getId(),
                serif.getWeight(),
                slope);

        if (serif.getWeight() < params.serifMinWeight) {
            logger.info(
                    "Staff#{} serif normalized weight too small {} vs {}",
                    staff.getId(),
                    serif.getNormalizedWeight(scale.getInterline()),
                    constants.serifMinWeight.getValue());

            return false;
        }

        int dir = (side == TOP) ? (-1) : 1;

        if ((slope * dir) < params.serifMinSlope) {
            logger.info("Staff#{} serif slope too small {}", staff.getId(), slope * dir);

            return false;
        }

        return true;
    }

    //-----------//
    // isAligned //
    //-----------//
    /**
     * Report whether the provided peak is involved in an alignment or a connection on
     * the desired side.
     *
     * @param peak the peak to check
     * @param side which side to look
     * @return true if aligned or connected
     */
    private boolean isAligned (StaffPeak.Bar peak,
                               VerticalSide side)
    {
        for (BarAlignment alignment : alignments) {
            if (alignment.getPeak(side) == peak) {
                return true;
            }
        }

        for (BarAlignment alignment : connections) {
            if (alignment.getPeak(side) == peak) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // isConnected //
    //-------------//
    /**
     * Check whether the provided peak is connected on the provided vertical side.
     *
     * @param peak the peak to check
     * @param side which vertical side to look for from peak
     * @return true if a compliant connection was found, false otherwise
     */
    private boolean isConnected (StaffPeak.Bar peak,
                                 VerticalSide side)
    {
        final VerticalSide opposite = side.opposite();

        for (BarConnection connection : connections) {
            if (connection.getPeak(opposite) == peak) {
                return true;
            }
        }

        return false;
    }

    //--------------------//
    // isJustAfterBracket //
    //--------------------//
    /**
     * Check whether the provided peak is located just after a bracket end.
     * (if so, it is likely to go slightly beyond staff height)
     *
     * @param peaks sequence of peaks
     * @param peak  the provided peak
     * @param side  which vertical side is being considered
     * @return true if just after bracket end
     */
    private boolean isJustAfterBracket (List<StaffPeak> peaks,
                                        StaffPeak.Bar peak,
                                        VerticalSide side)
    {
        int index = peaks.indexOf(peak);

        if (index == 0) {
            return false;
        }

        StaffPeak prevPeak = peaks.get(index - 1);

        if (!prevPeak.isBracketEnd(side)) {
            return false;
        }

        return (peak.getStart() - prevPeak.getStop() - 1) <= params.maxDoubleBarGap;
    }

    //-----------------//
    // isPartConnected //
    //-----------------//
    /**
     * Check whether the provided staff has part-connection on the provided vertical side.
     * <p>
     * A part-connection is a connection between two staves, not counting the system-connection on
     * the left side of the staves.
     *
     * @param staff the staff to check
     * @param side  which vertical side to look for from staff
     * @return true if a compliant part connection was found, false otherwise
     */
    private boolean isPartConnected (Staff staff,
                                     VerticalSide side)
    {
        final VerticalSide opposite = side.opposite();
        final List<StaffPeak> peaks = staff.getPeaks();
        final StaffPeak.Bar leftBar = getPeakEnd(LEFT, peaks);

        if (leftBar == null) {
            return false;
        }

        for (BarConnection connection : connections) {
            StaffPeak peak = connection.getPeak(opposite);

            if (peak.getStaff() == staff) {
                // Is this a part-connection, rather than a system-connection?
                int gap = peak.getStart() - leftBar.getStop() - 1;

                if (gap > params.maxDoubleBarGap) {
                    return true;
                }
            } else if (peak.getStaff().getId() > staff.getId()) {
                break; // Since connections are sorted per staff, then abscissa
            }
        }

        return false;
    }

    //-------------//
    // lookupPeaks //
    //-------------//
    /**
     * Lookup in the provided staff for one or several peaks compliant with (de-skewed)
     * peak abscissa and peak kind.
     * This populates the 'alignments' set.
     *
     * @param peak       the reference peak
     * @param side       vertical side with respect to reference peak
     * @param otherStaff the other staff to be browsed for alignment with peak
     */
    private void lookupPeaks (StaffPeak.Bar peak,
                              VerticalSide side,
                              Staff otherStaff)
    {
        final Skew skew = sheet.getSkew();
        final int mid = (peak.getStart() + peak.getStop()) / 2;
        final double dsk = skew.deskewed(new Point(mid, peak.getOrdinate(side))).getX();

        for (StaffPeak op : otherStaff.getPeaks()) {
            StaffPeak.Bar otherPeak = (StaffPeak.Bar) op;

            //TODO: perhaps check that peaks widths are "compatible"?
            // Vertically aligned, taking sheet slope into account
            int otherMid = (otherPeak.getStart() + otherPeak.getStop()) / 2;
            Point otherPt = (side == TOP) ? new Point(otherMid, otherPeak.getBottom())
                    : new Point(otherMid, otherPeak.getTop());
            double otherDsk = skew.deskewed(otherPt).getX();
            double dx = scale.pixelsToFrac(otherDsk - dsk);

            if (Math.abs(dx) <= constants.maxAlignmentDx.getValue()) {
                double alignImpact = 1
                                     - (Math.abs(dx) / constants.maxAlignmentDx.getValue());
                GradeImpacts impacts = new BarAlignment.Impacts(alignImpact);
                final BarAlignment alignment;

                if (side == TOP) {
                    alignment = new BarAlignment(otherPeak, peak, -dx, impacts);
                } else {
                    alignment = new BarAlignment(peak, otherPeak, dx, impacts);
                }

                alignments.add(alignment);
            }
        }
    }

    //-----------------//
    // partitionWidths //
    //-----------------//
    /**
     * Assign each peak as being thin or thick.
     * <p>
     * We can discard the case of all page peaks being thick, so we simply have to detect whether we
     * do have some thick ones.
     * When this method is called, some peaks are still due to stems. We don't know the average
     * stem width, but typically stem peaks are thinner or equal to bar peaks.
     * <p>
     * Isolated bars are thin (TODO: unless we hit a thick bar but missed a thin candidate nearby,
     * however this could be fixed using the global sheet population of thin bars).
     * <p>
     * We look for grouped bars and compare widths within the same group. If significant difference
     * is found, then we discriminate thins & thicks, otherwise they are all considered as thin.
     */
    private void partitionWidths ()
    {
        // Dispatch peaks into isolated peaks and groups of peaks
        final List<StaffPeak.Bar> isolated = new ArrayList<StaffPeak.Bar>();
        final List<List<StaffPeak.Bar>> groups = new ArrayList<List<StaffPeak.Bar>>();
        groupBarPeaks(isolated, groups);

        // Isolated peaks are considered thin
        for (StaffPeak.Bar peak : isolated) {
            peak.set(THIN);
        }

        // Process groups is any
        for (List<StaffPeak.Bar> group : groups) {
            // Read maximum width difference within this group
            int minWidth = Integer.MAX_VALUE;
            int maxWidth = Integer.MIN_VALUE;

            for (StaffPeak.Bar peak : group) {
                int width = peak.getWidth();
                minWidth = Math.min(minWidth, width);
                maxWidth = Math.max(maxWidth, width);
            }

            double normedDelta = (maxWidth - minWidth) / (double) scale.getInterline();
            logger.debug("min:{} max:{} nDelta:{} {}", minWidth, maxWidth, normedDelta, group);

            if (normedDelta >= params.minNormedDeltaWidth) {
                // Hetero (thins & thicks)
                for (StaffPeak.Bar peak : group) {
                    int width = peak.getWidth();

                    if ((width - minWidth) <= (maxWidth - width)) {
                        peak.set(THIN);
                    } else {
                        peak.set(THICK);
                    }
                }
            } else {
                // Homo => all thins
                for (StaffPeak.Bar peak : group) {
                    peak.set(THIN);
                }
            }
        }
    }

    //-----------------//
    // purgeAlignments //
    //-----------------//
    /**
     * Purge the alignments collection.
     * <ul>
     * <li>Remove any alignment that conflicts with a connection.
     * Any connection is given priority against conflicting alignment (simply because connection was
     * validated by presence of enough black pixels in the inter-staff region)</li>
     * <li>Remove duplicates: in the collection of alignments a peak should appear at most once as
     * top and at most once as bottom. In case of conflict, use alignment quality to disambiguate.
     * TODO: A more complex approach to disambiguate could use detection of pair of bars aligned
     * with another pair of bars, and align left with left and right with right. But is it worth the
     * added complexity?</li>
     * </ul>
     */
    private void purgeAlignments ()
    {
        // Purge alignments vs connections
        for (BarConnection connection : connections) {
            for (VerticalSide side : VerticalSide.values()) {
                for (BarAlignment alignment : alignmentsOf(connection.getPeak(side), side)) {
                    alignments.remove(alignment);
                    logger.debug("Removed {}", alignment);
                }
            }
        }

        // Check duplicate alignments (looking to top & bottom)
        for (VerticalSide side : VerticalSide.values()) {
            Map<StaffPeak.Bar, BarAlignment> map = new HashMap<StaffPeak.Bar, BarAlignment>();
            Set<BarAlignment> toRemove = new HashSet<BarAlignment>();

            for (BarAlignment alignment : alignments) {
                StaffPeak.Bar peak = alignment.getPeak(side);
                BarAlignment otherAlignment = map.get(peak);

                if (otherAlignment != null) {
                    // We have a conflict here, make a decision
                    logger.debug("Conflict {} vs {}", alignment, otherAlignment);

                    if (Math.abs(otherAlignment.dx) <= Math.abs(alignment.dx)) {
                        toRemove.add(alignment);
                    } else {
                        toRemove.add(otherAlignment);
                        map.put(peak, alignment);
                    }
                } else {
                    map.put(peak, alignment);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Purging {}", toRemove);
                alignments.removeAll(toRemove);
            }
        }
    }

    //-----------------//
    // purgeBracePeaks //
    //-----------------//
    /**
     * Purge brace portions mistaken for bar lines peaks.
     * Wrong bar line peaks may result from mistakes on brace portion.
     * Such brace portions are characterized with:
     * - Short average curvature (we use this!)
     * - Low derivative
     * - Location on left side of the staff
     * - Small no-staff blank separation from rest of staff (but perhaps reduced to nothing)
     * - Significant thickness
     */
    private void purgeBracePeaks ()
    {
        for (Staff staff : staffManager.getStaves()) {
            List<StaffPeak> toRemove = new ArrayList<StaffPeak>();

            for (StaffPeak peak : staff.getPeaks()) {
                Filament glyph = (Filament) peak.getFilament();
                double curvature = glyph.getMeanCurvature();

                if (curvature < params.minBarCurvature) {
                    if (glyph.isVip()) {
                        logger.info("VIP removing curved {} glyph#{}", peak, glyph.getId());
                    }

                    peak.set(BRACE);
                    toRemove.add(peak);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Staff#{} removing curved {}", staff.getId(), toRemove);
                staff.removePeaks(toRemove);
            }
        }
    }

    //-------------//
    // purgeCClefs //
    //-------------//
    /**
     * Purge C-Clef portions mistaken for bar lines.
     * <p>
     * A C-clef exhibits a pair of peaks (a rather thick one followed by a rather thin one).
     * It should be looked for in two location kinds:
     * <ul>
     * <li>At the very beginning of staff with no initial bar line, with only a short chunk of staff
     * lines, so that first peak is not a staff end.</li>
     * <li>After a bar line, provided this bar line is not part of a thin + thick + thin group.
     * For this case the horizontal gap between bar line and start of C-clef must be larger than
     * maximum multi-bar gap.</li>
     * </ul>
     */
    private void purgeCClefs ()
    {
        for (Staff staff : staffManager.getStaves()) {
            final List<StaffPeak> peaks = staff.getPeaks();
            final int staffStart = staff.getAbscissa(LEFT);
            int measureStart = staffStart;

            for (int i = 0; i < peaks.size(); i++) {
                StaffPeak p = peaks.get(i);

                if (p.getStart() <= measureStart) {
                    continue;
                }

                // Look for a rather thick first peak
                if (!p.isStaffEnd(LEFT)
                    && !p.isStaffEnd(RIGHT)
                    && !(p instanceof StaffPeak.Brace)
                    && !p.isBracket()
                    && (p.getWidth() >= params.minPeak1WidthForCClef)) {
                    StaffPeak.Bar peak = (StaffPeak.Bar) p;

                    // Check gap is larger than multi-bar gap but smaller than measure
                    int gap = peak.getStart() - measureStart;

                    // Gap is not relevant for first measure, thanks to !peak.isStaffEnd() test
                    int minGap = (measureStart == staffStart) ? 0 : params.maxDoubleBarGap;

                    if ((gap > minGap)
                        && (gap < params.minMeasureWidth)
                        && !isConnected(peak, TOP)
                        && !isConnected(peak, BOTTOM)) {
                        if (logger.isDebugEnabled() || peak.getFilament().isVip()) {
                            logger.info("Got a C-Clef peak1 at {}", peak);
                        }

                        final List<StaffPeak> toRemove = new ArrayList<StaffPeak>();
                        peak.set(CCLEF_ONE);
                        toRemove.add(peak);

                        // Look for a rather thin second peak right after the first
                        if ((i + 1) < peaks.size()) {
                            final StaffPeak.Bar peak2 = (StaffPeak.Bar) peaks.get(i + 1);
                            int gap2 = peak2.getStart() - peak.getStop() - 1;

                            if ((peak2.getWidth() <= params.maxPeak2WidthForCClef)
                                && (gap2 <= params.maxDoubleBarGap)
                                && !isConnected(peak2, TOP)
                                && !isConnected(peak2, BOTTOM)) {
                                if (logger.isDebugEnabled()
                                    || peak.getFilament().isVip()
                                    || peak2.getFilament().isVip()) {
                                    logger.info("Got a C-Clef peak2 at {}", peak2);
                                }

                                peak2.set(CCLEF_TWO);
                                toRemove.add(peak2);
                                logger.debug("Staff#{} purging C-Clef {}", staff.getId(), toRemove);
                                i++; // Don't re-browse this peak

                                // Avoid false peaks before the end of C-Clef has been passed
                                if ((i + 1) < peaks.size()) {
                                    int mid2 = (peak2.getStart() + peak2.getStop()) / 2;
                                    int xBreak = mid2 + params.cClefTail;

                                    for (StaffPeak otherPeak : peaks.subList(i + 1, peaks.size())) {
                                        int otherMid = (otherPeak.getStart() + otherPeak.getStop()) / 2;

                                        if (otherMid < xBreak) {
                                            logger.debug(
                                                    "Staff#{} purging tail of C-Clef {}",
                                                    staff.getId(),
                                                    otherPeak);
                                            otherPeak.set(CCLEF_TAIL);
                                            toRemove.add(otherPeak);
                                            i++; // Don't re-browse this peak
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        staff.removePeaks(toRemove);
                    } else {
                        measureStart = peak.getStop() + 1;
                    }
                } else {
                    measureStart = p.getStop() + 1;
                }
            }
        }
    }

    //------------------//
    // purgeConnections //
    //------------------//
    /**
     * Purge the connections collection of duplicates.
     * <p>
     * In the collection of connections a peak should appear at most once as
     * top and at most once as bottom. In case of conflict, use connection quality to disambiguate.
     */
    private void purgeConnections ()
    {
        // Check duplicate connections (looking to top & bottom)
        for (VerticalSide side : VerticalSide.values()) {
            Map<StaffPeak.Bar, BarConnection> map = new HashMap<StaffPeak.Bar, BarConnection>();
            Set<BarConnection> toRemove = new HashSet<BarConnection>();

            for (BarConnection connection : connections) {
                StaffPeak.Bar peak = connection.getPeak(side);
                BarConnection otherConnection = map.get(peak);

                if (otherConnection != null) {
                    // We have a conflict here, make a decision
                    logger.debug("Conflict {} vs {}", connection, otherConnection);

                    if (otherConnection.getImpacts().getGrade() >= connection.getImpacts().getGrade()) {
                        toRemove.add(connection);
                    } else {
                        toRemove.add(otherConnection);
                        map.put(peak, connection);
                    }
                } else {
                    map.put(peak, connection);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Purging {}", toRemove);
                connections.removeAll(toRemove);
            }
        }
    }

    //----------------------//
    // purgeCrossAlignments //
    //----------------------//
    /**
     * Only alignments within a system are meaningful.
     * So, alignments across systems must be deleted.
     */
    private void purgeCrossAlignments ()
    {
        final Set<BarAlignment> toRemove = new HashSet<BarAlignment>();

        for (BarAlignment alignment : alignments) {
            final SystemInfo s1 = alignment.getPeak(TOP).getStaff().getSystem();
            final SystemInfo s2 = alignment.getPeak(BOTTOM).getStaff().getSystem();

            if (s1 != s2) {
                toRemove.add(alignment);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Purging cross-system alignments{}", toRemove);
            alignments.removeAll(toRemove);
        }
    }

    //----------------//
    // purgeLongPeaks //
    //----------------//
    /**
     * Purge long thin bars (those getting above or below the related staff) that do not
     * connect staves.
     * <p>
     * Thick bars are not concerned by this test, because they cannot be mistaken for stems and can
     * appear to be extended because of brackets.
     * <p>
     * The check is relaxed for a bar which is aligned with another bar that exhibits no such length
     * problem.
     */
    private void purgeLongPeaks ()
    {
        for (Staff staff : staffManager.getStaves()) {
            final Set<StaffPeak.Bar> toRemove = new LinkedHashSet<StaffPeak.Bar>();

            PeakLoop:
            for (StaffPeak p : staff.getPeaks()) {
                StaffPeak.Bar peak = (StaffPeak.Bar) p;

                if (peak.getFilament().isVip()) {
                    logger.info("VIP purgeLongPeaks on staff#{} {}", staff.getId(), peak);
                }

                // Check whether peak goes beyond staff
                for (VerticalSide side : VerticalSide.values()) {
                    if (peak.isBeyond(side) && !isConnected(peak, side)) {
                        // Relax check. TODO: is this OK?
                        List<StaffPeak.Bar> partners = alignedPeaks(peak, side);

                        if (partners.size() == 1) {
                            StaffPeak.Bar partner = partners.get(0);

                            if (!partner.isBeyond()) {
                                // Consider this bar as safe
                                continue PeakLoop;
                            }
                        }

                        if (peak.getFilament().isVip()) {
                            logger.info(
                                    "VIP removed long peak on staff#{} {}",
                                    staff.getId(),
                                    peak);
                        }

                        toRemove.add(peak);
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Staff#{} removing longs {}", staff.getId(), toRemove);
                staff.removePeaks(toRemove);

                // Delete the alignments or connections that involved those peaks
                purgeRelations(toRemove, alignments);
                purgeRelations(toRemove, connections);
            }
        }
    }

    //----------------//
    // purgeRelations //
    //----------------//
    /**
     * Due to peaks being removed, delete the relations (alignments, connections) where
     * those peaks were involved.
     *
     * @param removedPeaks the peaks removed
     * @param rels         the collection to purge
     */
    private void purgeRelations (Set<StaffPeak.Bar> removedPeaks,
                                 Set<? extends BarAlignment> rels)
    {
        for (Iterator<? extends BarAlignment> it = rels.iterator(); it.hasNext();) {
            BarAlignment alignment = it.next();

            for (VerticalSide side : VerticalSide.values()) {
                StaffPeak.Bar peak = alignment.getPeak(side);

                if (removedPeaks.contains(peak)) {
                    it.remove();

                    break;
                }
            }
        }
    }

    //------------//
    // recordBars //
    //------------//
    private void recordBars ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                List<StaffPeak> peaks = staff.getPeaks();

                // All bars
                List<BarlineInter> bars = new ArrayList<BarlineInter>();

                for (StaffPeak peak : peaks) {
                    Inter inter = peak.getInter();

                    if (inter instanceof BarlineInter) {
                        bars.add((BarlineInter) inter);
                    }
                }

                staff.setBars(bars);
            }
        }
    }

    //-------------//
    // refineSides //
    //-------------//
    private void refineSides ()
    {
        for (StaffProjector projector : projectors) {
            projector.refineStaffSides();
        }
    }

    //----------//
    // stringOf //
    //----------//
    private String stringOf (List<PartGroup> groups,
                             SystemInfo system)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("System#").append(system.getId());

        for (PartGroup group : groups) {
            sb.append("\n   ").append(group);
        }

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean showVerticalLines = new Constant.Boolean(
                false,
                "Should we show the vertical grid lines?");

        private final Scale.Fraction minThinThickDelta = new Scale.Fraction(
                0.2,
                "Minimum difference between THIN/THICK width values");

        private final Scale.Fraction maxBraceThickness = new Scale.Fraction(
                1.0,
                "Maximum thickness of a brace");

        private final Scale.Fraction maxBraceWidth = new Scale.Fraction(
                3.0,
                "Maximum width of a brace");

        private final Scale.Fraction maxBraceExtension = new Scale.Fraction(
                1.0,
                "Maximum extension for a brace above or below staff line");

        private final Scale.Fraction maxAlignmentDx = new Scale.Fraction(
                0.5,
                "Max abscissa shift for bar alignment");

        private final Scale.Fraction maxConnectionGap = new Scale.Fraction(
                2.0,
                "Max vertical gap when connecting bar lines");

        private final Constant.Ratio maxConnectionWhiteRatio = new Constant.Ratio(
                0.25,
                "Max white ratio when connecting bar lines");

        private final Constant.Ratio minConnectionGrade = new Constant.Ratio(
                0.5,
                "Minimum grade for a true connection");

        private final Scale.Fraction maxBarExtension = new Scale.Fraction(
                0.3,
                "Maximum extension for a bar line above or below staff line");

        private final Scale.Fraction minBarCurvature = new Scale.Fraction(
                20,
                "Minimum mean curvature for a bar line (rather than a brace)");

        private final Scale.Fraction maxBraceCurvature = new Scale.Fraction(
                40,
                "Maximum mean curvature for a brace");

        private final Scale.Fraction maxDoubleBarGap = new Scale.Fraction(
                0.75,
                "Max horizontal gap between two members of a double bar");

        private final Scale.Fraction minMeasureWidth = new Scale.Fraction(
                2.0,
                "Minimum width for a measure");

        private final Constant.Ratio alignedIncreaseRatio = new Constant.Ratio(
                0.30,
                "Boost ratio for aligned bar lines");

        private final Constant.Ratio unalignedDecreaseRatio = new Constant.Ratio(
                0.30,
                "Penalty ratio for unaligned bar lines (in multi-staff systems)");

        private final Constant.Boolean deleteUnalignedBars = new Constant.Boolean(
                true,
                "Should unaligned bar lines be deleted? (in multi-staff systems)");

        // For C-clefs -----------------------------------------------------------------------------
        //
        private final Scale.Fraction minPeak1WidthForCClef = new Scale.Fraction(
                0.3,
                "Minimum width for first peak of C-Clef");

        private final Scale.Fraction maxPeak2WidthForCClef = new Scale.Fraction(
                0.3,
                "Maximum width for second peak of C-Clef");

        private final Scale.Fraction cClefTail = new Scale.Fraction(
                2.0,
                "Typical width for tail of C-Clef, from second peak to right end");

        // For braces ------------------------------------------------------------------------------
        //
        private final Scale.Fraction braceLeftMargin = new Scale.Fraction(
                0.5,
                "Margin on left side of brace peak to retrieve full glyph");

        private final Scale.Fraction braceSegmentLength = new Scale.Fraction(
                1.0,
                "Typical distance between brace points");

        // For brackets ----------------------------------------------------------------------------
        //
        private final Scale.Fraction minBracketWidth = new Scale.Fraction(
                0.4,
                "Minimum width for a bracket peak");

        private final Scale.Fraction maxBracketExtension = new Scale.Fraction(
                1.25,
                "Maximum extension for bracket end above or below staff line");

        private final Scale.Fraction bracketLookupExtension = new Scale.Fraction(
                2.0,
                "Lookup height for bracket end above or below staff line");

        private final Scale.Fraction serifSegmentLength = new Scale.Fraction(
                1.0,
                "Typical distance between bracket serif points");

        private final Scale.Fraction serifRoiWidth = new Scale.Fraction(
                2.0,
                "Width of lookup ROI for bracket serif");

        private final Scale.Fraction serifRoiHeight = new Scale.Fraction(
                2.0,
                "Height of lookup ROI for bracket serif");

        private final Scale.Fraction serifThickness = new Scale.Fraction(
                0.3,
                "Typical thickness of bracket serif");

        private final Scale.AreaFraction serifMinWeight = new Scale.AreaFraction(
                0.15,
                "Minimum weight for bracket serif");

        private final Constant.Double serifMinSlope = new Constant.Double(
                "tangent",
                0.25,
                "Minimum absolute slope for bracket serif");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double minNormedDeltaWidth;

        final int maxBraceThickness;

        final int maxBraceWidth;

        final int maxBraceExtension;

        final int maxAlignmentDx;

        final double maxBarExtension;

        final int minBarCurvature;

        final int maxBraceCurvature;

        final int maxConnectionGap;

        final double maxConnectionWhiteRatio;

        final double minConnectionGrade;

        final int maxDoubleBarGap;

        final int minMeasureWidth;

        final int minPeak1WidthForCClef;

        final int maxPeak2WidthForCClef;

        final int cClefTail;

        final int braceLeftMargin;

        final int braceSegmentLength;

        final int serifSegmentLength;

        final int minBracketWidth;

        final int maxBracketExtension;

        final int bracketLookupExtension;

        final int serifRoiWidth;

        final int serifRoiHeight;

        final int serifThickness;

        final int serifMinWeight;

        final double serifMinSlope;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minNormedDeltaWidth = constants.minThinThickDelta.getValue();
            maxBraceThickness = scale.toPixels(constants.maxBraceThickness);
            maxBraceWidth = scale.toPixels(constants.maxBraceWidth);
            maxBraceExtension = scale.toPixels(constants.maxBraceExtension);
            maxAlignmentDx = scale.toPixels(constants.maxAlignmentDx);
            maxBarExtension = scale.toPixels(constants.maxBarExtension);
            minBarCurvature = scale.toPixels(constants.minBarCurvature);
            maxBraceCurvature = scale.toPixels(constants.maxBraceCurvature);
            maxConnectionGap = scale.toPixels(constants.maxConnectionGap);
            maxConnectionWhiteRatio = constants.maxConnectionWhiteRatio.getValue();
            minConnectionGrade = constants.minConnectionGrade.getValue();
            maxDoubleBarGap = scale.toPixels(constants.maxDoubleBarGap);
            minMeasureWidth = scale.toPixels(constants.minMeasureWidth);

            cClefTail = scale.toPixels(constants.cClefTail);
            minPeak1WidthForCClef = scale.toPixels(constants.minPeak1WidthForCClef);
            maxPeak2WidthForCClef = scale.toPixels(constants.maxPeak2WidthForCClef);

            braceLeftMargin = scale.toPixels(constants.braceLeftMargin);
            braceSegmentLength = scale.toPixels(constants.braceSegmentLength);
            serifSegmentLength = scale.toPixels(constants.serifSegmentLength);
            minBracketWidth = scale.toPixels(constants.minBracketWidth);
            maxBracketExtension = scale.toPixels(constants.maxBracketExtension);
            bracketLookupExtension = scale.toPixels(constants.bracketLookupExtension);
            serifRoiWidth = scale.toPixels(constants.serifRoiWidth);
            serifRoiHeight = scale.toPixels(constants.serifRoiHeight);
            serifThickness = scale.toPixels(constants.serifThickness);
            serifMinWeight = scale.toPixels(constants.serifMinWeight);
            serifMinSlope = constants.serifMinSlope.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
