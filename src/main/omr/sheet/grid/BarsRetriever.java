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
import omr.sheet.grid.BarColumn.Chain;
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
import omr.ui.ViewParameters;
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

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleDirectedGraph;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code BarsRetriever} focuses on the retrieval of vertical barlines, brackets
 * and braces.
 * <p>
 * Barlines are used to:
 * <ul>
 * <li>Determine the gathering of staves into systems and parts</li>
 * <li>Define staff sides precisely.</li>
 * <li>Determine measures (later)</li>
 * </ul>
 * <p>
 * Staff by staff, peaks of projection on x-axis of every pixel found within staff height lead to
 * candidate barlines. Note that a barline is defined only within staff height. This strict
 * limitation in height allows to use projection even when the sheet is (slightly) skewed.
 * <p>
 * From one staff to the other, candidate barlines can be abscissa-wise aligned (taking sheet skew
 * into account).
 * Aligned barlines are vertically connected when there is a concrete vertical stick (a connector)
 * between them.
 * Connections are key to determine the gathering of staves into separate systems.
 * <p>
 * Once systems are determined, all barline candidates are gathered into columns of aligned
 * barlines, and further processing is performed on (system) column rather than (staff) barline.
 * <p>
 * Among all columns in a system, one column may play the specific role of system "start column".
 * This column is found as the right-most column of first column group and it determines the precise
 * starting abscissa of each staff within its containing system.
 * <p>
 * On right side of the start column, all columns must have the same system height.
 * On left side of the start column, we may find braces, brackets and square groups, exhibiting
 * various heights and shapes.
 * <p>
 * For braces, every staff start is looked up for any brace peak and whether the peak filament goes
 * beyond top or beyond bottom or both.
 * Then brace symbols are looked up between a top portion, [middle portion] and a bottom portion.
 * If a left bar peak is overlapped by a brace symbol, it is removed. Otherwise, the bar peak is
 * considered as part of a square group.
 *
 * @author Hervé Bitteur
 */
public class BarsRetriever
        implements ItemRenderer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BarsRetriever.class);

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

    /** Staff projectors. (sequence parallel to staves in sheet) */
    private final List<StaffProjector> projectors = new ArrayList<StaffProjector>();

    /** Graph of all peaks, linked by alignment/connection relationships. */
    private final SimpleDirectedGraph<StaffPeak, BarAlignment> peakGraph = new SimpleDirectedGraph<StaffPeak, BarAlignment>(
            BarAlignment.class);

    /** Columns of barlines, organized by system. */
    private final SortedMap<SystemInfo, List<BarColumn>> columnMap = new TreeMap<SystemInfo, List<BarColumn>>();

    /** Constructor for brace compound. */
    private final CompoundConstructor braceConstructor;

    /** Constructor for (bracket) serif compound. */
    private final CompoundConstructor serifConstructor;

    /** Specific factory for peak-based filaments. */
    private final BarFilamentFactory factory;

    /** Index of filaments. */
    private final FilamentIndex filamentIndex;

    /** All sections suitable for a brace. */
    private List<Section> allBraceSections;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Retrieve the bar lines of all staves.
     *
     * @param sheet the sheet to process
     */
    public BarsRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        // Scale-dependent parameters
        scale = sheet.getScale();
        params = new Parameters(scale);

        // Companions
        staffManager = sheet.getStaffManager();
        factory = new BarFilamentFactory(scale);
        filamentIndex = sheet.getFilamentIndex();

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
        findBarPeaks(); // Individual staff analysis to find raw bar peaks on x-axis projection

        buildBarSticks(); // Build core filament for each peak

        purgeCurvedPeaks(); // Purge any peak that looks like a curved entity (weak filter)

        findAlignments(); // Find all peak alignments across staves

        findConnections(); // Find all concrete connections across staves

        purgeAlignments(); // Purge conflicting connections & alignments

        // Create systems from bar connections
        SystemManager mgr = sheet.getSystemManager();
        mgr.setSystems(createSystems(getSystemTops()));

        if (mgr.getSystems().isEmpty()) {
            logger.warn("No system found");
            throw new StepException("No system found");
        }

        logger.info("Systems: {}", mgr.getSystemsString());

        purgeCrossAlignments(); // Purge alignments across systems, they are not relevant

        purgeExtendingPeaks(); // Purge peaks extending beyond system staves

        buildColumns(); // Within each system, organize peaks into system-based columns

        detectStartColumns(); // Detect start columns and purge following ones if not full

        detectBracePortions(); // Detect brace portions at start of staff

        buildBraces(); // Build braces across staves out of brace portions

        detectBracketEnds(); // Detect top and bottom portions of brackets

        detectBracketMiddles(); // Detect middle portions of brackets

        refineRightEnds(); // Define precise right end of each staff

        purgeCClefs(); // Purge C-clef-based false barlines

        partitionWidths(); // Partition peaks between thin and thick

        createInters(); // Create barline and bracket interpretations within each system

        createConnectionInters(); // Create barline and bracket connection across staves

        groupBarlines(); // Detect grouped barlines

        recordBars(); // Record barlines in staff

        createParts(); // Build parts and groups
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
        // Display staff peaks?
        if (ViewParameters.getInstance().isStaffPeakPainting()) {
            g.setColor(Colors.STAFF_PEAK);

            for (StaffProjector projector : projectors) {
                for (StaffPeak peak : projector.getPeaks()) {
                    peak.render(g);
                }
            }
        }

        if (!constants.showVerticalLines.isSet()) {
            return;
        }

        final Rectangle clip = g.getClipBounds();
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        final Color oldColor = g.getColor();
        g.setColor(Colors.ENTITY_MINOR);

        // Draw bar lines (only within staff height)
        for (StaffProjector projector : projectors) {
            for (StaffPeak peak : projector.getPeaks()) {
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
        for (BarAlignment align : peakGraph.edgeSet()) {
            if (align instanceof BarConnection) {
                BarConnection connection = (BarConnection) align;
                Line2D median = connection.getMedian();

                if (median.intersects(clip)) {
                    g.draw(median);
                }
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    //----------------//
    // buildBarSticks //
    //----------------//
    /**
     * Build the underlying stick of every peak.
     * <p>
     * These sticks are needed to detect those peaks which go past staff height above,
     * below, or both, and may rather be stems.
     * They are used also to detect curly peaks that are due to brace portions (not reliable).
     * <p>
     * For each peak, we take a vertical "slice" of the relevant sections using a lookup area.
     * We then run a dedicated factory on the sections and make it focus on the bar core area.
     */
    private void buildBarSticks ()
    {
        // Preselect sections of proper max width
        final int maxWidth = getMaxPeaksWidth();
        final List<Section> allSections = getSectionsByWidth(maxWidth);
        logger.debug("sections:{}", allSections.size());

        for (StaffProjector projector : projectors) {
            List<StaffPeak> toRemove = new ArrayList<StaffPeak>();

            for (StaffPeak peak : projector.getPeaks()) {
                // Build filament from proper slice of sections for this peak
                final List<Section> sections = getPeakSections(peak, allSections);
                final Filament filament = factory.buildBarFilament(sections, peak.getBounds());

                if (filament != null) {
                    filamentIndex.register(filament);
                    peak.setFilament(filament);
                    logger.debug("{}", peak);
                } else {
                    toRemove.add(peak);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Staff#{} no filament {}", projector.getStaff().getId(), toRemove);
                projector.removePeaks(toRemove);
            }
        }
    }

    //--------------------//
    // buildBraceFilament //
    //--------------------//
    /**
     * Build the brace filament that goes through all provided portions.
     *
     * @param portions the vertical sequence of brace portions
     * @return the brace filament
     */
    private Filament buildBraceFilament (List<StaffPeak.Brace> portions)
    {
        final StaffPeak topPeak = portions.get(0);

        // Define (perhaps slanted) area, slightly increased to the left
        final Path2D path = new Path2D.Double();
        path.moveTo(topPeak.getStart() - params.braceLeftMargin, topPeak.getTop()); // Start point

        // Left (top down)
        for (StaffPeak peak : portions) {
            path.lineTo(peak.getStop() + 1, peak.getTop());
            path.lineTo(peak.getStop() + 1, peak.getBottom() + 1);
        }

        // Right (bottom up)
        for (ListIterator<StaffPeak.Brace> it = portions.listIterator(portions.size());
                it.hasPrevious();) {
            StaffPeak peak = it.previous();
            path.lineTo(peak.getStart() - params.braceLeftMargin, peak.getBottom() + 1);

            if (it.hasPrevious()) {
                path.lineTo(peak.getStart() - params.braceLeftMargin, peak.getTop());
            }
        }

        path.closePath();

        final Area area = new Area(path);

        // Select sections that could be added to filaments
        final List<Filament> filaments = new ArrayList<Filament>();
        final List<Section> sections = getAreaSections(area, allBraceSections);

        for (StaffPeak peak : portions) {
            Filament filament = peak.getFilament();
            filaments.add(filament);
            sections.removeAll(filament.getMembers());
        }

        // Now we have several filaments and a few sections around
        Filament compound = (Filament) CompoundFactory.buildCompoundFromParts(
                filaments,
                braceConstructor);

        // Expand compound as much as possible
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
     */
    private void buildBraces ()
    {
        final List<Staff> staves = staffManager.getStaves();
        final GlyphIndex nest = sheet.getGlyphIndex();

        StaffLoop:
        for (int iStaff = 0; iStaff < staves.size(); iStaff++) {
            Staff staff = staves.get(iStaff);
            final StaffPeak.Brace bracePeak = projectorOf(staff).getBracePeak();

            if (bracePeak == null) {
                continue;
            }

            if (bracePeak.isSet(BRACE_TOP)) {
                List<StaffPeak.Brace> portions = new ArrayList<StaffPeak.Brace>();
                portions.add(bracePeak);

                // Look down for compatible brace portion(s)
                for (Staff otherStaff : staves.subList(iStaff + 1, staves.size())) {
                    StaffPeak.Brace otherBrace = projectorOf(otherStaff).getBracePeak();

                    if (otherBrace == null) {
                        logger.warn("Staff#{} isolated brace top", staff.getId());

                        break;
                    }

                    if (!otherBrace.isSet(BRACE_MIDDLE) && !otherBrace.isSet(BRACE_BOTTOM)) {
                        logger.warn("Staff#{} expected brace middle/bottom", otherStaff.getId());

                        break;
                    }

                    portions.add(otherBrace);

                    if (otherBrace.isSet(BRACE_BOTTOM)) {
                        break;
                    }
                }

                if (portions.size() > 1) {
                    // Retrieve the full brace filament
                    Filament braceFilament = buildBraceFilament(portions);
                    Glyph glyph = nest.registerOriginal(braceFilament.toGlyph(null));
                    BraceInter braceInter = new BraceInter(glyph, Inter.intrinsicRatio * 1);
                    SIGraph sig = staff.getSystem().getSig();
                    sig.addVertex(braceInter);
                }

                // Skip the staves processed
                iStaff = staves.indexOf(portions.get(portions.size() - 1).getStaff());
            }
        }
    }

    //--------------//
    // buildColumns //
    //--------------//
    /**
     * Within each system, organize peaks into system columns.
     * We use connections (and alignments when there is no connection)
     */
    private void buildColumns ()
    {
        ConnectivityInspector inspector = new ConnectivityInspector(peakGraph);
        List<Set<StaffPeak>> sets = inspector.connectedSets();
        logger.debug("sets: {}", sets.size());

        // Process system per system (we have already purged cross-system links)
        final SortedMap<SystemInfo, List<Chain>> chainMap = new TreeMap<SystemInfo, List<Chain>>();

        for (Set<StaffPeak> set : sets) {
            Chain chain = new Chain(set);
            SystemInfo system = chain.first().getStaff().getSystem();
            List<Chain> chainList = chainMap.get(system);

            if (chainList == null) {
                chainMap.put(system, chainList = new ArrayList<Chain>());
            }

            chainList.add(chain);
        }

        // Sort all chains within each system
        for (List<Chain> chains : chainMap.values()) {
            Collections.sort(chains);
        }

        // Try to aggregate chains into full-size columns
        final double maxDx = sheet.getScale().toPixelsDouble(constants.maxAlignmentDx);

        for (SystemInfo system : sheet.getSystems()) {
            List<BarColumn> columns = columnMap.get(system);

            if (columns == null) {
                columnMap.put(system, columns = new ArrayList<BarColumn>());
            }

            final List<Chain> chains = chainMap.get(system);

            if (chains != null) {
                for (Chain chain : chains) {
                    BarColumn column = null;

                    if (!columns.isEmpty()) {
                        column = columns.get(columns.size() - 1);

                        // Check dx and peak indices
                        double dx = chain.first().getDeskewedAbscissa() - column.getXDsk();

                        if ((Math.abs(dx) > maxDx) || !column.canInclude(chain)) {
                            column = null;
                        }
                    }

                    if (column == null) {
                        columns.add(column = new BarColumn(system, peakGraph));
                    }

                    column.addChain(chain);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.info("{}", system);

                for (BarColumn column : columns) {
                    logger.info("   {} {}", column.isFull() ? "---" : "   ", column);
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

        for (SectionCompound compound : compounds) {
            filamentIndex.register((Filament) compound);
        }

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
        final boolean vip = p1.isVip() || p2.isVip();

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

        if (vip) {
            logger.info("VIP checkConnection {} and {} {}", p1, p2, data);
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

            final double minGrade = params.minConnectionGrade * impacts.getIntrinsicRatio();

            if (grade >= minGrade) {
                BarConnection connection = new BarConnection(alignment, impacts);

                if (vip) {
                    logger.info("VIP {}", connection);
                }

                return connection;
            }
        }

        if (vip) {
            logger.info("VIP no connection between {} and {}", p1, p2);
        }

        return null;
    }

    //------------------------//
    // createConnectionInters //
    //------------------------//
    /**
     * Populate all systems SIGs with connection inters for barline and brackets.
     */
    private void createConnectionInters ()
    {
        for (BarAlignment align : peakGraph.edgeSet()) {
            if (align instanceof BarConnection) {
                BarConnection connection = (BarConnection) align;
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
    }

    //--------------//
    // createInters //
    //--------------//
    /**
     * Based on remaining peaks, populate each system sig with proper inters for bar
     * lines and for brackets.
     * <p>
     * We use a single underlying glyph for each "column" of barlines with connectors or
     * brackets with connectors.
     */
    private void createInters ()
    {
        final double up = constants.alignedIncreaseRatio.getValue();
        final double down = constants.unalignedDecreaseRatio.getValue();

        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Staff staff : system.getStaves()) {
                for (StaffPeak p : projectorOf(staff).getPeaks()) {
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
     * <ul>
     * <li>A bracket/square defines a (bracket/square) group.
     * <li>No staff connection implies different parts.
     * <li>Braced staves represent a single part when not connected to other staves, otherwise it is
     * a group.
     * </ul>
     */
    private void createParts ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            final List<Staff> staves = system.getStaves();
            final List<PartGroup> allGroups = system.getPartGroups(); // All groups in this system
            final Map<Integer, PartGroup> activeGroups = new TreeMap<Integer, PartGroup>(); // Active groups

            for (Staff staff : staves) {
                final StaffProjector projector = projectorOf(staff);
                final List<StaffPeak> peaks = projector.getPeaks();
                final StaffPeak.Bar startBar = getPeakEnd(LEFT, peaks); // Starting barline?

                if (startBar == null) {
                    logger.debug("Staff#{} one-staff system", staff.getId());
                    createPart(system, staff, staff); // Staff = system, just one part

                    continue;
                }

                // Going from startBar (excluded) to the left, look for bracket/square peaks
                final boolean botConn = isPartConnected(staff, BOTTOM); // Part connection below?
                int level = 0;

                for (int i = peaks.indexOf(startBar) - 1; i >= 0; i--) {
                    final StaffPeak peak = peaks.get(i);
                    level++;

                    if (peak.isBracket()) {
                        final PartGroup pg;

                        if (peak.isBracketEnd(TOP)) {
                            // Start bracket group
                            pg = new PartGroup(level, Symbol.bracket, botConn, staff.getId());
                            allGroups.add(pg);
                            activeGroups.put(level, pg);
                            logger.debug("Staff#{} start bracket {}", staff.getId(), pg);
                        } else {
                            // Continue bracket group
                            pg = activeGroups.get(level);

                            if (pg != null) {
                                pg.setLastStaffId(staff.getId());

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
                    } else {
                        // A simple (square) portion
                        StaffPeak.Bar bar = (StaffPeak.Bar) peak;

                        if (!isConnected(bar, TOP) && isConnected(bar, BOTTOM)) {
                            // Start square group
                            PartGroup pg = new PartGroup(
                                    level,
                                    Symbol.square,
                                    botConn,
                                    staff.getId());
                            allGroups.add(pg);
                            activeGroups.put(level, pg);
                            logger.debug("Staff#{} start square {}", staff.getId(), pg);
                        } else if (isConnected(bar, TOP)) {
                            // Continue square group
                            PartGroup pg = activeGroups.get(level);

                            if (pg != null) {
                                pg.setLastStaffId(staff.getId());

                                // Stop square group?
                                if (!isConnected(bar, BOTTOM)) {
                                    logger.debug("Staff#{} stop square {}", staff.getId(), pg);
                                    activeGroups.put(level, null);
                                } else {
                                    logger.debug("Staff#{} continue square {}", staff.getId(), pg);
                                }
                            } else {
                                logger.warn("Staff#{} no group level:{}", staff.getId(), level);
                            }
                        } else {
                            logger.warn("Staff#{} weird square portion", staff.getId());
                        }
                    }
                }

                final StaffPeak.Brace bracePeak = projector.getBracePeak(); // Leading brace peak?

                if (bracePeak == null) {
                    logger.debug("Staff#{} no brace before starting barline", staff.getId());
                    createPart(system, staff, staff);
                } else {
                    level++;

                    if (bracePeak.isBraceEnd(TOP)) {
                        // Start brace group
                        PartGroup pg = new PartGroup(level, Symbol.brace, botConn, staff.getId());
                        allGroups.add(pg);
                        activeGroups.put(level, pg);
                        logger.debug("Staff#{} start brace {}", staff.getId(), pg);
                    } else {
                        // Continue brace group
                        PartGroup pg = activeGroups.get(level);

                        if (pg != null) {
                            pg.setLastStaffId(staff.getId());

                            // Stop brace group?
                            if (bracePeak.isBraceEnd(BOTTOM)) {
                                activeGroups.put(level, null);

                                final int firstId = pg.getFirstStaffId();
                                final int lastId = pg.getLastStaffId();
                                final Staff firstStaff = staffManager.getStaff(firstId - 1);
                                final Staff lastStaff = staffManager.getStaff(lastId - 1);

                                // Was this brace a real group?
                                if (!botConn && !isPartConnected(firstStaff, TOP)) {
                                    // No, just a multi-staff instrument
                                    logger.debug(
                                            "Staff#{} end multi-staff instrument {}",
                                            staff.getId(),
                                            pg);
                                    allGroups.remove(pg);
                                    createPart(system, firstStaff, lastStaff);
                                } else {
                                    int i1 = staves.indexOf(firstStaff);
                                    int i2 = staves.indexOf(lastStaff);
                                    logger.debug("Staff#{} stop brace {}", staff.getId(), pg);

                                    for (Staff s : staves.subList(i1, i2 + 1)) {
                                        createPart(system, s, s);
                                    }
                                }
                            }
                        } else {
                            logger.warn("Staff#{} no group level:{}", staff.getId(), level);

                            // TODO:  We need to create part & systems anyway !!!
                        }
                    }
                }
            }

            if (!allGroups.isEmpty()) {
                dumpGroups(allGroups, system);
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

    //---------------------//
    // detectBracePortions //
    //---------------------//
    /**
     * Look for brace top, middle or bottom portion before start column.
     * Each detected brace portion is stored in staff projector.
     */
    private void detectBracePortions ()
    {
        allBraceSections = getSectionsByWidth(params.maxBraceThickness);

        for (StaffProjector projector : projectors) {
            final Staff staff = projector.getStaff();
            final List<StaffPeak> peaks = projector.getPeaks();
            final int iStart = projector.getStartPeakIndex();

            if (iStart == -1) {
                continue; // Start peak must exist
            }

            // Look for brace portion on left of first peak
            final StaffPeak firstPeak = peaks.get(0);
            int maxRight = firstPeak.getStart() - 1;
            int minLeft = maxRight - params.maxBraceWidth;
            StaffPeak.Brace bracePeak = lookForBracePeak(staff, minLeft, maxRight);

            if ((bracePeak == null) && (iStart >= 1)) {
                // First peak could itself be a brace portion (mistaken for a bar)
                final StaffPeak secondPeak = peaks.get(1);
                maxRight = secondPeak.getStart() - 1;
                minLeft = maxRight - params.maxBraceWidth;
                bracePeak = lookForBracePeak(staff, minLeft, maxRight);

                if (bracePeak != null) {
                    projector.removePeak(firstPeak); // Remove fake bar peak
                }
            }

            if (bracePeak != null) {
                projector.setBracePeak(bracePeak);
            }
        }
    }

    //-------------------//
    // detectBracketEnds //
    //-------------------//
    /**
     * Detect the peaks that correspond to top or bottom end of brackets.
     * <p>
     * Such bracket end is characterized as follows:<ul>
     * <li>It is located on left side of the start column.</li>
     * <li>It is a rather thick peak.</li>
     * <li>It sometimes (but not always) goes a bit beyond staff top or bottom line.</li>
     * <li>It has a serif shape at end.</li>
     * </ul>
     */
    private void detectBracketEnds ()
    {
        StaffLoop:
        for (StaffProjector projector : projectors) {
            final Staff staff = projector.getStaff();
            final List<StaffPeak> peaks = projector.getPeaks();
            final int iStart = projector.getStartPeakIndex();

            if (iStart == -1) {
                continue; // Start peak must exist
            }

            for (int i = iStart - 1; i >= 0; i--) {
                final StaffPeak.Bar peak = (StaffPeak.Bar) peaks.get(i);

                // Sufficient width?
                if (peak.getWidth() < params.minBracketWidth) {
                    continue;
                }

                final StaffPeak.Bar rightPeak = (StaffPeak.Bar) peaks.get(i + 1);

                // It cannot go too far beyond staff height
                for (VerticalSide side : VerticalSide.values()) {
                    double ext = extensionOf(peak, side);

                    // Check for serif shape
                    Filament serif;

                    if ((ext <= params.maxBracketExtension)
                        && (null != (serif = getSerif(staff, peak, rightPeak, side)))) {
                        logger.debug("Staff#{} {} bracket end", staff.getId(), side);

                        peak.setBracketEnd(side, serif);
                    }
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
        PeakLoop:
        for (StaffPeak peak : peakGraph.vertexSet()) {
            if (peak.isBracketEnd(TOP)) {
                // Flag all connected peaks below as MIDDLE until a BOTTOM is encountered
                while (true) {
                    Set<BarAlignment> aligns = peakGraph.outgoingEdgesOf(peak);
                    StaffPeak.Bar next = null;

                    for (BarAlignment align : aligns) {
                        if (align instanceof BarConnection) {
                            next = ((BarConnection) align).bottomPeak;

                            if (next.isBracketEnd(BOTTOM)) {
                                continue PeakLoop;
                            }

                            next.set(BRACKET_MIDDLE);
                        }
                    }

                    if (next == null) {
                        break;
                    }

                    peak = next;
                }
            }
        }
    }

    //--------------------//
    // detectStartColumns //
    //--------------------//
    /**
     * For each system, detect the first group of full columns, and use the right-most
     * (fully connected) one as actual "start column" with defines the precise left
     * limit of staff lines.
     * <p>
     * A brace cannot be used as a start column.
     * Unfortunately, we have yet no efficient way to recognize a given peak as a brace peak.
     * Methods detectBracePortions() and buildBraces() provide a limited recognition capability.
     * The risk is high on 2-staff systems, as a brace can be perceived as a fully-connected column.
     * <p>
     * Strategy is as follows: browse the fully-connected columns from left to right within maxGap
     * abscissa from previous column. The last column should be the right one.
     * We use a wider gap (brace-bar) for the very first couple of columns.
     * (This should be double-checked with gap being free of staff lines, but at this point staff
     * lines are not yet fully defined, and projection may be biased by brace touching next bar).
     * <p>
     * When there is no first bar group, or when this group is located within staff width, use only
     * the staff lines horizontal limits as the staff limit.
     */
    private void detectStartColumns ()
    {
        final Skew skew = sheet.getSkew();

        for (SystemInfo system : sheet.getSystems()) {
            List<BarColumn> columns = columnMap.get(system);
            BarColumn firstFull = null; // First full column encountered
            BarColumn startColumn = null; // Last full column (within same group as first column)

            for (int i = 0; i < columns.size(); i++) {
                final BarColumn column = columns.get(i);

                if (column.isFull()) {
                    if (firstFull == null) {
                        firstFull = column;
                    }

                    if (startColumn != null) {
                        double gap = (column.getXDsk() - (column.getWidth() / 2))
                                     - (startColumn.getXDsk() + (startColumn.getWidth() / 2));
                        int maxGap = (i == 1) ? params.maxBraceBarGap : params.maxDoubleBarGap;

                        if (gap > maxGap) {
                            break; // We are now too far on right
                        }
                    }

                    if (column.isFullyConnected()) {
                        startColumn = column;
                    }
                }
            }

            // Check location of last full column WRT staff lines limit
            double minDsk = Double.MAX_VALUE;

            for (Staff staff : system.getStaves()) {
                double xLeft = staff.getAbscissa(LEFT);
                double yLeft = (staff.getFirstLine().yAt(xLeft) + staff.getLastLine().yAt(xLeft)) / 2;
                minDsk = Math.min(minDsk, skew.deskewed(new Point2D.Double(xLeft, yLeft)).getX());
            }

            if ((firstFull == null)
                || ((firstFull.getXDsk() - minDsk) > params.maxBarToLinesLeftEnd)) {
                // Use staff lines as limit (already done)
            } else if (startColumn != null) {
                // Use start column as limit
                for (StaffPeak peak : startColumn.getPeaks()) {
                    Staff staff = peak.getStaff();
                    staff.setAbscissa(LEFT, peak.getStop());
                    peak.setStaffEnd(LEFT);
                }
            }

            // Remove all non-full columns located past staff left limit
            for (Iterator<BarColumn> it = columns.iterator(); it.hasNext();) {
                final BarColumn column = it.next();

                if (!column.isFull()) {
                    if ((startColumn == null) || (column.getXDsk() > startColumn.getXDsk())) {
                        // Delete this column, with its related peaks
                        for (StaffPeak peak : column.getPeaks()) {
                            if (peak != null) {
                                projectorOf(peak.getStaff()).removePeak(peak);
                            }
                        }

                        it.remove();
                    }
                }
            }

            if ((startColumn != null) && logger.isDebugEnabled()) {
                logger.debug("{} startColumn: {}", system, startColumn);
            }
        }
    }

    //------------//
    // dumpGroups //
    //------------//
    private void dumpGroups (List<PartGroup> groups,
                             SystemInfo system)
    {
        logger.info("System#{}", system.getId());

        for (PartGroup group : groups) {
            logger.info("   {}", group);
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
        for (StaffProjector projector : projectors) {
            final Staff staff = projector.getStaff();

            for (VerticalSide side : VerticalSide.values()) {
                List<Staff> otherStaves = staffManager.vertNeighbors(staff, side);

                // Make sure there are other staves on this side and they are "short-wise compatible"
                // with current staff
                if (otherStaves.isEmpty() || (otherStaves.get(0).isShort() != staff.isShort())) {
                    continue;
                }

                // Look for all alignment/connection relations
                for (StaffPeak peak : projector.getPeaks()) {
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
            StaffProjector projector = new StaffProjector(sheet, staff, peakGraph);
            projectors.add(projector);
            projector.process();
            Graphs.addAllVertices(peakGraph, projector.getPeaks());
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
        for (BarAlignment alignment : new ArrayList<BarAlignment>(peakGraph.edgeSet())) {
            // Look for concrete connection
            BarConnection connection = checkConnection(alignment);

            if (connection != null) {
                StaffPeak source = peakGraph.getEdgeSource(alignment);
                StaffPeak target = peakGraph.getEdgeTarget(alignment);
                peakGraph.removeEdge(alignment);
                peakGraph.addEdge(source, target, connection);
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

    //----------------//
    // getConnections //
    //----------------//
    private List<BarConnection> getConnections ()
    {
        List<BarConnection> list = new ArrayList<BarConnection>();

        for (BarAlignment align : peakGraph.edgeSet()) {
            if (align instanceof BarConnection) {
                list.add((BarConnection) align);
            }
        }

        Collections.sort(list);

        return list;
    }

    //----------------//
    // getConnections //
    //----------------//
    private List<BarConnection> getConnections (Staff staff,
                                                VerticalSide side)
    {
        final List<BarConnection> list = new ArrayList<BarConnection>();
        final VerticalSide opposite = side.opposite();
        final List<StaffPeak> peaks = projectorOf(staff).getPeaks();
        final StaffPeak.Bar startBar = getPeakEnd(LEFT, peaks);

        if (startBar == null) {
            return list;
        }

        for (BarAlignment align : peakGraph.edgeSet()) {
            if (align instanceof BarConnection) {
                BarConnection connection = (BarConnection) align;
                StaffPeak peak = connection.getPeak(opposite);

                if (peak.getStaff() == staff) {
                    // Is this a part-connection, rather than a system-connection?
                    if (peak.getStart() > startBar.getStart()) {
                        list.add(connection);
                    }
                } else if (peak.getStaff().getId() > staff.getId()) {
                    break; // Since connections are ordered by staff, then abscissa
                }
            }
        }

        return list;
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

        for (StaffProjector projector : projectors) {
            for (StaffPeak peak : projector.getPeaks()) {
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
        final int maxSectionWidth = peak.getWidth(); // Width of this particular peak

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

    //----------//
    // getSerif //
    //----------//
    /**
     * Check whether the provided peak glyph exhibits a serif on desired side.
     * <p>
     * Define a region of interest just beyond glyph end and look for sections contained in roi.
     * Build a glyph from connected sections and check its shape.
     *
     * @param peak      provided peak
     * @param rightPeak next peak on right, if any
     * @param side      TOP or BOTTOM
     * @return serif filament if serif was detected
     */
    private Filament getSerif (Staff staff,
                               StaffPeak.Bar peak,
                               StaffPeak.Bar rightPeak,
                               VerticalSide side)
    {
        // Constants
        final int halfLine = (int) Math.ceil(scale.getMaxFore() / 2.0);

        // Define lookup region for serif
        final Filament barFilament = peak.getFilament();
        final int yBox = (side == TOP) ? (peak.getTop() - halfLine - params.serifRoiHeight)
                : (peak.getBottom() + halfLine);
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

        if (rightPeak != null) {
            sections.removeAll(rightPeak.getFilament().getMembers());
        }

        if (sections.isEmpty()) {
            return null;
        }

        // Retrieve serif glyph from sections
        Filament serif = buildSerifFilament(staff, sections, side, roi);
        filamentIndex.register(serif);
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

            return null;
        }

        return serif;
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
        for (BarConnection connection : getConnections()) {
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

                for (StaffPeak p : projectorOf(staff).getPeaks()) {
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

                for (StaffPeak peak : projectorOf(staff).getPeaks()) {
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
        if (side == TOP) {
            return peakGraph.inDegreeOf(peak) > 0;
        } else {
            return peakGraph.outDegreeOf(peak) > 0;
        }
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
        Set<BarAlignment> edges = (side == TOP) ? peakGraph.incomingEdgesOf(peak)
                : peakGraph.outgoingEdgesOf(peak);

        for (BarAlignment edge : edges) {
            if (edge instanceof BarConnection) {
                return true;
            }
        }

        return false;
    }

    //-----------------//
    // isPartConnected //
    //-----------------//
    /**
     * Check whether the provided staff has part-connection on provided vertical side.
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
        return !getConnections(staff, side).isEmpty();
    }

    //------------------//
    // lookForBracePeak //
    //------------------//
    private StaffPeak.Brace lookForBracePeak (Staff staff,
                                              int minLeft,
                                              int maxRight)
    {
        final StaffProjector projector = projectorOf(staff);
        final StaffPeak.Brace bracePeak = projector.findBracePeak(minLeft, maxRight);

        if (bracePeak == null) {
            return null;
        }

        // Take proper slice of sections for this peak
        final List<Section> sections = getPeakSections(bracePeak, allBraceSections);
        Filament filament = factory.buildBarFilament(sections, bracePeak.getBounds());
        filamentIndex.register(filament);
        bracePeak.setFilament(filament);

        // A few tests on glyph
        if (filament.getLength(VERTICAL) < params.minBracePortionHeight) {
            return null;
        }

        double curvature = filament.getMeanCurvature();
        logger.debug(
                "Staff#{} curvature:{} vs {}",
                staff.getId(),
                curvature,
                params.maxBraceCurvature);

        if (curvature >= params.maxBraceCurvature) {
            return null;
        }

        final SystemInfo system = staff.getSystem();
        boolean beyondTop = false;
        boolean beyondBottom = false;

        for (VerticalSide side : VerticalSide.values()) {
            double ext = extensionOf(bracePeak, side);

            if (ext > params.maxBraceExtension) {
                if (side == TOP) {
                    if (staff != system.getFirstStaff()) {
                        beyondTop = true;
                    }
                } else if (staff != system.getLastStaff()) {
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

        if (bracePeak.isVip()) {
            logger.info("VIP {}", bracePeak);
        } else {
            logger.debug("{}", bracePeak);
        }

        return bracePeak;
    }

    //-------------//
    // lookupPeaks //
    //-------------//
    /**
     * Lookup in the provided staff for one or several peaks compliant with (de-skewed)
     * peak abscissa and peak kind.
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

        for (StaffPeak op : projectorOf(otherStaff).getPeaks()) {
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
                    peakGraph.addEdge(otherPeak, peak, alignment);
                } else {
                    alignment = new BarAlignment(peak, otherPeak, dx, impacts);
                    peakGraph.addEdge(peak, otherPeak, alignment);
                }
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

    //-------------//
    // projectorOf //
    //-------------//
    private StaffProjector projectorOf (Staff staff)
    {
        return projectors.get(staff.getId() - 1);
    }

    //------------------//
    // purgeConnections //
    //------------------//
    /**
     * Purge the alignments and connections.
     * <p>
     * In set of alignment / connections a peak should appear at most once as top and at most once
     * as bottom.
     * In case of conflict, use quality to disambiguate.
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
        Set<BarAlignment> toRemove = new HashSet<BarAlignment>();

        for (StaffPeak peak : peakGraph.vertexSet()) {
            if (peakGraph.inDegreeOf(peak) > 1) {
                Set<BarAlignment> edges = new HashSet(peakGraph.incomingEdgesOf(peak));
                edges.remove(BarAlignment.bestOf(edges));
                toRemove.addAll(edges);
            }

            if (peakGraph.outDegreeOf(peak) > 1) {
                Set<BarAlignment> edges = new HashSet(peakGraph.outgoingEdgesOf(peak));
                edges.remove(BarAlignment.bestOf(edges));
                toRemove.addAll(edges);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Purging alignments {}", toRemove);
            peakGraph.removeAllEdges(toRemove);
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
        for (StaffProjector projector : projectors) {
            final Staff staff = projector.getStaff();
            final List<StaffPeak> peaks = projector.getPeaks();
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
                        if (logger.isDebugEnabled() || peak.isVip()) {
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
                                if (logger.isDebugEnabled() || peak.isVip() || peak2.isVip()) {
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

                        if (!toRemove.isEmpty()) {
                            logger.debug("Staff#{} C-Clef peaks {}", staff.getId(), toRemove);
                            projector.removePeaks(toRemove);
                        }
                    } else {
                        measureStart = peak.getStop() + 1;
                    }
                } else {
                    measureStart = p.getStop() + 1;
                }
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

        for (BarAlignment alignment : peakGraph.edgeSet()) {
            final SystemInfo s1 = alignment.getPeak(TOP).getStaff().getSystem();
            final SystemInfo s2 = alignment.getPeak(BOTTOM).getStaff().getSystem();

            if (s1 != s2) {
                toRemove.add(alignment);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Purging cross-system alignments{}", toRemove);
            peakGraph.removeAllEdges(toRemove);
        }
    }

    //------------------//
    // purgeCurvedPeaks //
    //------------------//
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
    private void purgeCurvedPeaks ()
    {
        for (StaffProjector projector : projectors) {
            List<StaffPeak> toRemove = new ArrayList<StaffPeak>();

            for (StaffPeak peak : projector.getPeaks()) {
                Filament fil = (Filament) peak.getFilament();
                double curvature = fil.getMeanCurvature();

                if (curvature < params.minBarCurvature) {
                    if (fil.isVip()) {
                        logger.info("VIP removing curved {}", peak);
                    }

                    toRemove.add(peak);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Staff#{} removing curved {}", projector.getStaff().getId(), toRemove);
                projector.removePeaks(toRemove);
            }
        }
    }

    //---------------------//
    // purgeExtendingPeaks //
    //---------------------//
    /**
     * Purge bars extending above system top staff or below system bottom staff.
     */
    private void purgeExtendingPeaks ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (VerticalSide side : VerticalSide.values()) {
                final Staff staff = (side == TOP) ? system.getFirstStaff()
                        : system.getLastStaff();
                final StaffProjector projector = projectorOf(staff);
                final List<StaffPeak> peaks = projector.getPeaks();
                final int iStart = projector.getStartPeakIndex();
                final Set<StaffPeak.Bar> toRemove = new LinkedHashSet<StaffPeak.Bar>();

                for (int i = 0; i < peaks.size(); i++) {
                    if (i <= iStart) {
                        continue;
                    }

                    // Check whether the filament gets beyond the staff
                    StaffPeak.Bar peak = (StaffPeak.Bar) peaks.get(i);
                    double ext = extensionOf(peak, side);

                    if (ext > params.maxBarExtension) {
                        if (peak.isVip()) {
                            logger.info("VIP removed {} long {}", side, peak);
                        }

                        toRemove.add(peak);
                    }
                }

                if (!toRemove.isEmpty()) {
                    logger.debug("Staff#{} removing extendings {}", staff.getId(), toRemove);
                    projector.removePeaks(toRemove);
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
                // All bars
                List<BarlineInter> bars = new ArrayList<BarlineInter>();

                for (StaffPeak peak : projectorOf(staff).getPeaks()) {
                    Inter inter = peak.getInter();

                    if (inter instanceof BarlineInter) {
                        bars.add((BarlineInter) inter);
                    }
                }

                staff.setBars(bars);
            }
        }
    }

    //-----------------//
    // refineRightEnds //
    //-----------------//
    private void refineRightEnds ()
    {
        for (StaffProjector projector : projectors) {
            projector.refineRightEnd();
        }
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

        private final Scale.Fraction maxAlignmentDx = new Scale.Fraction(
                0.5,
                "Max abscissa shift for bar alignment");

        private final Scale.Fraction maxConnectionGap = new Scale.Fraction(
                2.0,
                "Max vertical gap when connecting bar lines");

        private final Constant.Ratio maxConnectionWhiteRatio = new Constant.Ratio(
                0.35,
                "Max white ratio when connecting bar lines");

        private final Constant.Ratio minConnectionGrade = new Constant.Ratio(
                0.5,
                "Minimum grade for a true connection");

        private final Scale.Fraction maxBarExtension = new Scale.Fraction(
                0.4,
                "Maximum extension for a bar line above or below staff line");

        private final Scale.Fraction maxBarToLinesLeftEnd = new Scale.Fraction(
                0.15,
                "Maximum dx between bar and left end of staff lines");

        private final Scale.Fraction minBarCurvature = new Scale.Fraction(
                15,
                "Minimum mean curvature for a bar line (rather than a brace)");

        private final Scale.Fraction maxDoubleBarGap = new Scale.Fraction(
                0.75,
                "Max horizontal gap between two members of a double bar");

        private final Scale.Fraction maxBraceBarGap = new Scale.Fraction(
                1.0,
                "Max horizontal gap between a brace peak and next bar");

        private final Scale.Fraction minMeasureWidth = new Scale.Fraction(
                2.0,
                "Minimum width for a measure");

        private final Constant.Ratio alignedIncreaseRatio = new Constant.Ratio(
                0.30,
                "Boost ratio for aligned bar lines");

        private final Constant.Ratio unalignedDecreaseRatio = new Constant.Ratio(
                0.30,
                "Penalty ratio for unaligned bar lines (in multi-staff systems)");

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

        private final Scale.Fraction minBracePortionHeight = new Scale.Fraction(
                3.0,
                "Minimum height for a brace portion");

        private final Scale.Fraction maxBraceThickness = new Scale.Fraction(
                1.0,
                "Maximum thickness of a brace");

        private final Scale.Fraction maxBraceWidth = new Scale.Fraction(
                3.0,
                "Maximum width of a brace");

        private final Scale.Fraction maxBraceExtension = new Scale.Fraction(
                1.0,
                "Maximum extension for a brace above or below staff line");

        private final Scale.Fraction maxBraceCurvature = new Scale.Fraction(
                35,
                "Maximum mean curvature for a brace");

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
                0.2,
                "Minimum weight for bracket serif");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double minNormedDeltaWidth;

        final int maxAlignmentDx;

        final double maxBarExtension;

        final int maxBarToLinesLeftEnd;

        final int minBarCurvature;

        final int maxConnectionGap;

        final double maxConnectionWhiteRatio;

        final double minConnectionGrade;

        final int maxDoubleBarGap;

        final int maxBraceBarGap;

        final int minMeasureWidth;

        final int minPeak1WidthForCClef;

        final int maxPeak2WidthForCClef;

        final int cClefTail;

        final int braceLeftMargin;

        final int braceSegmentLength;

        final int minBracePortionHeight;

        final int maxBraceThickness;

        final int maxBraceWidth;

        final int maxBraceExtension;

        final int maxBraceCurvature;

        final int serifSegmentLength;

        final int minBracketWidth;

        final int maxBracketExtension;

        final int bracketLookupExtension;

        final int serifRoiWidth;

        final int serifRoiHeight;

        final int serifThickness;

        final int serifMinWeight;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minNormedDeltaWidth = constants.minThinThickDelta.getValue();
            maxAlignmentDx = scale.toPixels(constants.maxAlignmentDx);
            maxBarExtension = scale.toPixels(constants.maxBarExtension);
            maxBarToLinesLeftEnd = scale.toPixels(constants.maxBarToLinesLeftEnd);
            minBarCurvature = scale.toPixels(constants.minBarCurvature);
            maxConnectionGap = scale.toPixels(constants.maxConnectionGap);
            maxConnectionWhiteRatio = constants.maxConnectionWhiteRatio.getValue();
            minConnectionGrade = constants.minConnectionGrade.getValue();
            maxDoubleBarGap = scale.toPixels(constants.maxDoubleBarGap);
            maxBraceBarGap = scale.toPixels(constants.maxBraceBarGap);
            minMeasureWidth = scale.toPixels(constants.minMeasureWidth);

            cClefTail = scale.toPixels(constants.cClefTail);
            minPeak1WidthForCClef = scale.toPixels(constants.minPeak1WidthForCClef);
            maxPeak2WidthForCClef = scale.toPixels(constants.maxPeak2WidthForCClef);

            braceLeftMargin = scale.toPixels(constants.braceLeftMargin);
            braceSegmentLength = scale.toPixels(constants.braceSegmentLength);
            minBracePortionHeight = scale.toPixels(constants.minBracePortionHeight);
            maxBraceThickness = scale.toPixels(constants.maxBraceThickness);
            maxBraceWidth = scale.toPixels(constants.maxBraceWidth);
            maxBraceExtension = scale.toPixels(constants.maxBraceExtension);
            maxBraceCurvature = scale.toPixels(constants.maxBraceCurvature);

            serifSegmentLength = scale.toPixels(constants.serifSegmentLength);
            minBracketWidth = scale.toPixels(constants.minBracketWidth);
            maxBracketExtension = scale.toPixels(constants.maxBracketExtension);
            bracketLookupExtension = scale.toPixels(constants.bracketLookupExtension);
            serifRoiWidth = scale.toPixels(constants.serifRoiWidth);
            serifRoiHeight = scale.toPixels(constants.serifRoiHeight);
            serifThickness = scale.toPixels(constants.serifThickness);
            serifMinWeight = scale.toPixels(constants.serifMinWeight);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
