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

import omr.math.GeoUtil;
import omr.math.PointUtil;
import static omr.run.Orientation.*;

import omr.sheet.Part;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.grid.BarColumn.Chain;
import omr.sheet.grid.PartGroup.Symbol;
import static omr.sheet.grid.StaffPeak.Attribute.*;

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
import omr.util.StopWatch;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import org.jgrapht.alg.ConnectivityInspector;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

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

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Staff projectors. (sequence parallel to staves in sheet) */
    private final List<StaffProjector> projectors = new ArrayList<StaffProjector>();

    /** Graph of all peaks. */
    private final PeakGraph peakGraph;

    /** Columns of barlines, organized by system. */
    private final SortedMap<SystemInfo, List<BarColumn>> columnMap = new TreeMap<SystemInfo, List<BarColumn>>();

    /** Constructor for brace compound. */
    private final CompoundConstructor braceConstructor;

    /** Constructor for (bracket) serif compound. */
    private final CompoundConstructor serifConstructor;

    /** Specific builder for peak-based filaments. */
    private final BarFilamentBuilder filamentBuilder;

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

        // Companions
        staffManager = sheet.getStaffManager();
        filamentBuilder = new BarFilamentBuilder(sheet);
        filamentIndex = sheet.getFilamentIndex();
        peakGraph = new PeakGraph(sheet, projectors);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Retrieve all barlines, brackets and braces in the sheet and create systems,
     * groups and parts.
     *
     * @throws StepException raised if processing must stop
     */
    public void process ()
            throws StepException
    {
        final StopWatch watch = new StopWatch("BarRetriever");
        watch.start("buildSystems");
        peakGraph.buildSystems(); // Build graph of staff peaks, until systems are known

        watch.start("buildColumns");
        buildColumns(); // Within each system, organize peaks into system-based columns

        watch.start("detectStartColumns");
        detectStartColumns(); // Detect start columns

        watch.start("purgePartialColumns");
        purgePartialColumns(); // Purge partial columns after start

        watch.start("purgeTooLeft");
        purgeTooLeft(); // Purge any peak located too far on left of start peak

        watch.start("detectBracePortions");
        detectBracePortions(); // Detect brace portions at start of staff

        watch.start("buildBraces");
        buildBraces(); // Build brace interpretations across staves out of brace portions

        watch.start("purgeLeftOfBraces");
        purgeLeftOfBraces(); // Purge any peak located on left side of brace

        watch.start("verifyLinesRoot");
        verifyLinesRoot(); // Make sure lines don't start before first (bar/bracket) peak

        watch.start("detectBracketEnds");
        detectBracketEnds(); // Detect top and bottom portions of brackets

        watch.start("detectBracketMiddles");
        detectBracketMiddles(); // Detect middle portions of brackets

        watch.start("purgeUnalignedBars");
        purgeUnalignedBars(); // On multi-staff systems, purge unaligned bars

        watch.start("purgeExtendingPeaks");
        purgeExtendingPeaks(); // Purge peaks extending beyond system staves

        watch.start("refineRightEnd");
        refineRightEnds(); // Define precise right end of each staff

        watch.start("purgeCClefs");
        purgeCClefs(); // Purge C-clef-based false barlines

        watch.start("partitionWidths");
        partitionWidths(); // Partition peaks between thin and thick

        watch.start("createInters");
        createInters(); // Create barline and bracket interpretations within each system

        watch.start("createConnectionInters");
        createConnectionInters(); // Create barline and bracket connection across staves

        watch.start("groupBarlines");
        groupBarlines(); // Detect grouped barlines

        watch.start("recordBars");
        recordBars(); // Record barlines in staff

        watch.start("createGroups");
        createGroups(); // Build groups

        watch.start("createParts");
        createParts(); // Build parts

        if (constants.printWatch.isSet()) {
            watch.print();
        }
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
            for (StaffProjector projector : projectors) {
                final List<StaffPeak> peaks = projector.getPeaks();

                for (StaffPeak peak : peaks) {
                    peak.render(g);
                }

                StaffPeak bracePeak = projector.getBracePeak();

                if ((bracePeak != null) && !peaks.contains(bracePeak)) {
                    bracePeak.render(g);
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
                if (peak.isBrace()) {
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

    //--------------------//
    // buildBraceFilament //
    //--------------------//
    /**
     * Build the brace filament that goes through all provided portions.
     *
     * @param portions the vertical sequence of brace portions
     * @return the brace filament
     */
    private Filament buildBraceFilament (List<StaffPeak> portions)
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
        for (ListIterator<StaffPeak> it = portions.listIterator(portions.size()); it.hasPrevious();) {
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
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();

        for (SystemInfo system : sheet.getSystems()) {
            if (!system.isMultiStaff()) {
                continue;
            }

            final List<Staff> staves = system.getStaves();

            StaffLoop:
            for (int iStaff = 0; iStaff < staves.size(); iStaff++) {
                Staff staff = staves.get(iStaff);

                final StaffPeak bracePeak = projectorOf(staff).getBracePeak();

                if (bracePeak == null) {
                    continue;
                }

                if (bracePeak.isSet(BRACE_TOP)) {
                    List<StaffPeak> portions = new ArrayList<StaffPeak>();
                    portions.add(bracePeak);

                    // Look down for compatible brace portion(s):
                    // These portions are typically brace middle(s) if any followed by one brace bottom.
                    // In very long braces, middle portions may be rather straight and seen as "bars".
                    StaffPeak topPeak = bracePeak;

                    for (Staff otherStaff : staves.subList(iStaff + 1, staves.size())) {
                        StaffProjector otherProjector = projectorOf(otherStaff);
                        StaffPeak otherPeak = otherProjector.getBracePeak();

                        if (otherPeak == null) {
                            // Look for an aligned "bar" portion
                            StaffPeak p = otherProjector.getPeaks().get(0);
                            BarAlignment alignment = peakGraph.checkAlignment(
                                    topPeak,
                                    p,
                                    true, // Check on x
                                    false); // No check on width (some brace portions can be wide)

                            if (alignment != null) {
                                otherPeak = p;
                                otherPeak.set(BRACE_MIDDLE);
                                otherProjector.setBracePeak(otherPeak);
                            }
                        }

                        if (otherPeak == null) {
                            logger.warn("Staff#{} isolated brace top", staff.getId());

                            break;
                        }

                        if (!otherPeak.isSet(BRACE_MIDDLE) && !otherPeak.isSet(BRACE_BOTTOM)) {
                            logger.warn(
                                    "Staff#{} expected brace middle/bottom",
                                    otherStaff.getId());

                            break;
                        }

                        portions.add(otherPeak);

                        if (otherPeak.isSet(BRACE_BOTTOM)) {
                            break;
                        }

                        topPeak = otherPeak;
                    }

                    if (portions.size() > 1) {
                        // Retrieve the full brace filament
                        Filament braceFilament = buildBraceFilament(portions);
                        Glyph glyph = glyphIndex.registerOriginal(braceFilament.toGlyph(null));
                        BraceInter braceInter = new BraceInter(glyph, Inter.intrinsicRatio * 1);
                        SIGraph sig = staff.getSystem().getSig();
                        sig.addVertex(braceInter);
                    }

                    // Skip the staves processed
                    iStaff = staves.indexOf(portions.get(portions.size() - 1).getStaff());
                }
            }
        }
    }

    //--------------//
    // buildColumns //
    //--------------//
    /**
     * Within each system, organize peaks into system columns.
     * We use connections (or alignments when there is no connection)
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
        final double maxDx = sheet.getScale().toPixelsDouble(peakGraph.getMaxAlignmentDx());

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
                logger.info("{} columns:{}", system, columns.size());

                for (int i = 0; i < columns.size(); i++) {
                    BarColumn column = columns.get(i);
                    String prefix = column.isFull() ? "---" : "   ";
                    logger.info("{}  {} {}", i, prefix, column);
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
                StaffPeak topPeak = connection.topPeak;
                SystemInfo system = topPeak.getStaff().getSystem();
                SIGraph sig = system.getSig();

                if (topPeak.isBrace()) {
                    continue;
                }

                try {
                    if (topPeak.isBracket()) {
                        sig.addVertex(
                                new BracketConnectorInter(connection, connection.getImpacts()));
                    } else {
                        sig.addVertex(
                                new BarConnectorInter(
                                        connection,
                                        topPeak.isSet(THICK) ? Shape.THICK_CONNECTOR : Shape.THIN_CONNECTOR,
                                        connection.getImpacts()));
                    }

                    // Also, connected bars support each other
                    Relation bcRel = new BarConnectionRelation(connection.getImpacts());
                    StaffPeak bottomPeak = connection.bottomPeak;
                    sig.addEdge(topPeak.getInter(), bottomPeak.getInter(), bcRel);
                } catch (Exception ex) {
                    logger.warn("Cannot create connection for {} {}", align, ex.toString(), ex);
                }
            }
        }
    }

    //--------------//
    // createGroups //
    //--------------//
    /**
     * Within each system, retrieve all groups.
     * <ul>
     * <li>A bracket/square defines a (bracket/square) group.
     * <li>No staff connection implies different parts.
     * <li>Braced staves represent a single part when not connected to other staves and count of
     * braced staves is not more than 2, otherwise it is a group of separate parts.
     * </ul>
     * Code in this method is rather fragile, because it relies on presence of proper peaks
     * (especially brace top/bottom which are not always correct) and at proper level.
     */
    private void createGroups ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            logger.debug("createGroups {}", system);

            final List<PartGroup> allGroups = system.getPartGroups(); // All groups in this system
            final Map<Integer, PartGroup> activeGroups = new TreeMap<Integer, PartGroup>(); // Active groups

            for (Staff staff : system.getStaves()) {
                logger.debug("  Staff#{}", staff.getId());

                final StaffProjector projector = projectorOf(staff);
                final List<StaffPeak> peaks = projector.getPeaks();
                final int iStart = projector.getStartPeakIndex();

                if (iStart == -1) {
                    logger.debug("Staff#{} one-staff system", staff.getId());

                    continue;
                }

                // Going from startBar (excluded) to the left, look for bracket/square peaks
                // Braces are not handled in this loop
                final boolean botConn = isPartConnected(staff, BOTTOM); // Part connection below?
                int level = 0;

                for (int i = iStart - 1; i >= 0; i--) {
                    final StaffPeak peak = peaks.get(i);
                    logger.debug("    {}", peak);

                    if (peak.isBrace()) {
                        break;
                    }

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
                                    activeGroups.remove(level);
                                } else {
                                    logger.debug("Staff#{} continue bracket {}", staff.getId(), pg);
                                }
                            } else {
                                logger.warn("Staff#{} no group level:{}", staff.getId(), level);
                            }
                        }
                    } else if (!isConnected(peak, TOP) && isConnected(peak, BOTTOM)) {
                        // Start square group
                        PartGroup pg = new PartGroup(level, Symbol.square, botConn, staff.getId());
                        allGroups.add(pg);
                        activeGroups.put(level, pg);
                        logger.debug("Staff#{} start square {}", staff.getId(), pg);
                    } else if (isConnected(peak, TOP)) {
                        // Continue square group
                        PartGroup pg = activeGroups.get(level);

                        if (pg != null) {
                            pg.setLastStaffId(staff.getId());

                            // Stop square group?
                            if (!isConnected(peak, BOTTOM)) {
                                logger.debug("Staff#{} stop square {}", staff.getId(), pg);
                                activeGroups.remove(level);
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

                // Finally, here we handle braces if any
                final StaffPeak bracePeak = projector.getBracePeak(); // Leading brace peak?

                if (bracePeak == null) {
                    logger.debug("Staff#{} no brace before starting barline", staff.getId());
                } else {
                    level++;

                    if (bracePeak.isBraceEnd(TOP)) {
                        // (We may have a brace group on hold at this level if bottom was missed)
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
                                activeGroups.remove(level);
                                logger.debug("Staff#{} stop brace {}", staff.getId(), pg);
                            }
                        } else {
                            logger.info("No brace partner at level:{} for {}", level, bracePeak);
                        }
                    }
                }
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
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Staff staff : system.getStaves()) {
                StaffProjector projector = projectorOf(staff);

                for (StaffPeak peak : projector.getPeaks()) {
                    if (peak.isBrace()) {
                        continue;
                    }

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
        final List<Part> parts = system.getParts();
        final List<Staff> staves = system.getStaves();
        int iFirst = staves.indexOf(first);
        int iLast = staves.indexOf(last);

        // Dirty hack to prevent multiple part creation for same staff
        if (!parts.isEmpty()) {
            final Part latestPart = parts.get(parts.size() - 1);
            final Staff latestStaff = latestPart.getLastStaff();
            final int iLatest = staves.indexOf(latestStaff);

            if (iLast <= iLatest) {
                return null;
            }

            iFirst = Math.max(iLatest + 1, iFirst);
        }

        Part part = new Part(system);

        for (Staff staff : staves.subList(iFirst, iLast + 1)) {
            part.addStaff(staff);
        }

        part.setId(1 + parts.size());
        system.addPart(part);

        return part;
    }

    //-------------//
    // createParts //
    //-------------//
    /**
     * Within each system, create all parts.
     * <p>
     * By default, each staff corresponds to a separate part. The only exception is the case of a
     * "real" brace (multi-staff instrument) for which the embraced staves share a single part.
     * <p>
     * Such "real" brace is then removed from system groups.
     * <p>
     * NOTA: We have to make sure that every staff is somehow assigned to a part, without exception,
     * otherwise it would be lost when data is finally stored into project file.
     */
    private void createParts ()
    {
        final List<Staff> sheetStaves = staffManager.getStaves();

        for (SystemInfo system : sheet.getSystems()) {
            logger.debug("createParts {}", system);

            final List<PartGroup> allGroups = system.getPartGroups(); // All groups in this system

            // Look for "true" braced groups
            TreeSet<PartGroup> bracedGroups = new TreeSet<PartGroup>(PartGroup.byFirstId);

            for (PartGroup pg : allGroups) {
                if (isTrueBraceGroup(pg)) {
                    bracedGroups.add(pg);
                }
            }

            // Assign all staves
            int nextId = system.getFirstStaff().getId(); // ID of next staff to be assigned

            for (PartGroup braced : bracedGroups) {
                // Gap to fill?
                for (int id = nextId; id < braced.getFirstStaffId(); id++) {
                    Staff staff = sheetStaves.get(id - 1);
                    createPart(system, staff, staff);
                }

                // Brace itself
                createPart(
                        system,
                        sheetStaves.get(braced.getFirstStaffId() - 1),
                        sheetStaves.get(braced.getLastStaffId() - 1));
                nextId = braced.getLastStaffId() + 1;
            }

            // Final gap to fill?
            for (int id = nextId; id <= system.getLastStaff().getId(); id++) {
                Staff staff = sheetStaves.get(id - 1);
                createPart(system, staff, staff);
            }

            // These "true" braced groups are removed from system groups
            allGroups.removeAll(bracedGroups);

            // Print out purged system groups
            if (!allGroups.isEmpty()) {
                dumpGroups(allGroups, system);
            }
        }
    }

    //----------------------//
    // deleteRelatedColumns //
    //----------------------//
    /**
     * Delete the columns that are based on the provided removed peaks.
     *
     * @param system  the containing system
     * @param removed the provided removed peaks
     */
    private void deleteRelatedColumns (SystemInfo system,
                                       Collection<? extends StaffPeak> removed)
    {
        final List<BarColumn> columns = columnMap.get(system);

        if (columns != null) {
            final Set<BarColumn> columnsToRemove = new LinkedHashSet<BarColumn>();

            for (StaffPeak peak : removed) {
                BarColumn column = peak.getColumn();

                if (column != null) {
                    columnsToRemove.add(column);
                }
            }

            for (BarColumn column : columnsToRemove) {
                logger.debug("Deleting {}", column);

                for (StaffPeak peak : column.getPeaks()) {
                    if (peak != null) {
                        Staff staff = peak.getStaff();
                        StaffProjector projector = projectorOf(staff);
                        projector.removePeak(peak);
                    }
                }
            }

            columns.removeAll(columnsToRemove);
        }
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
        for (SystemInfo system : sheet.getSystems()) {
            if (!system.isMultiStaff()) {
                continue;
            }

            for (Staff staff : system.getStaves()) {
                StaffProjector projector = projectorOf(staff);

                final List<StaffPeak> peaks = projector.getPeaks();
                final int iStart = projector.getStartPeakIndex();

                if (iStart == -1) {
                    continue; // Start peak must exist
                }

                // Look for brace portion on left of first peak
                final StaffPeak firstPeak = peaks.get(0);
                int maxRight = firstPeak.getStart() - 1;
                int minLeft = maxRight - params.maxBraceWidth;
                StaffPeak bracePeak = lookForBracePeak(staff, minLeft, maxRight);

                if ((bracePeak == null) && (iStart >= 1)) {
                    // First peak could itself be a brace portion (mistaken for a bar)
                    final StaffPeak secondPeak = peaks.get(1);
                    maxRight = secondPeak.getStart() - 1;
                    minLeft = maxRight - params.maxBraceWidth;
                    bracePeak = lookForBracePeak(staff, minLeft, maxRight);

                    if (bracePeak != null) {
                        replacePeak(firstPeak, bracePeak);
                    }
                }

                if (bracePeak != null) {
                    projector.setBracePeak(bracePeak);
                }
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
        for (SystemInfo system : sheet.getSystems()) {
            if (!system.isMultiStaff()) {
                continue;
            }

            for (Staff staff : system.getStaves()) {
                StaffProjector projector = projectorOf(staff);

                final List<StaffPeak> peaks = projector.getPeaks();
                final int iStart = projector.getStartPeakIndex();

                if (iStart == -1) {
                    continue; // Start peak must exist
                }

                for (int i = iStart - 1; i >= 0; i--) {
                    final StaffPeak peak = peaks.get(i);

                    if (peak.isBrace()) {
                        break;
                    }

                    // Sufficient width?
                    if (peak.getWidth() < params.minBracketWidth) {
                        continue;
                    }

                    final StaffPeak rightPeak = peaks.get(i + 1);

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
    }

    //----------------------//
    // detectBracketMiddles //
    //----------------------//
    /**
     * Among peaks, flag the ones that correspond to brackets middle portions.
     */
    private void detectBracketMiddles ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            if (!system.isMultiStaff()) {
                continue;
            }

            for (Staff staff : system.getStaves()) {
                StaffProjector projector = projectorOf(staff);

                PeakLoop:
                for (StaffPeak peak : projector.getPeaks()) {
                    if (peak.isBracketEnd(TOP)) {
                        // Flag all connected peaks below as MIDDLE until a BOTTOM is encountered
                        while (true) {
                            Set<BarAlignment> aligns = peakGraph.outgoingEdgesOf(peak);
                            StaffPeak next = null;

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
     * When there is no first bar group, use only staff lines horizontal limits as the staff limit.
     */
    private void detectStartColumns ()
    {
        SystemLoop:
        for (SystemInfo system : sheet.getSystems()) {
            List<BarColumn> columns = columnMap.get(system);
            BarColumn startColumn = null; // Last full column (within same group as first full)

            for (int i = 0; i < columns.size(); i++) {
                final BarColumn column = columns.get(i);

                if (column.isFull()) {
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

            if (startColumn != null) {
                logger.debug("{} startColumn: {}", system, startColumn);

                // Check that this column is not too far right into staff
                StaffPeak[] startPeaks = startColumn.getPeaks();

                for (StaffPeak peak : startPeaks) {
                    Staff staff = peak.getStaff();
                    double xLeft = staff.getAbscissa(LEFT);

                    if ((peak.getStart() - xLeft) > params.maxLinesLeftToStartBar) {
                        if (peak.isVip() || logger.isDebugEnabled()) {
                            logger.info("start {} too far inside staff#{}", peak, staff.getId());
                        }

                        continue SystemLoop;
                    }
                }

                // Use start column as left limit
                for (StaffPeak peak : startColumn.getPeaks()) {
                    Staff staff = peak.getStaff();
                    staff.setAbscissa(LEFT, peak.getStop());
                    peak.setStaffEnd(LEFT);
                }
            } else if (system.isMultiStaff()) {
                logger.warn("No startColumn found for multi-staff {}", system);
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
        final Filament fil = peak.getFilament();
        final Rectangle box = fil.getBounds();
        final double halfLine = scale.getMaxFore() / 2.0;

        if (side == TOP) {
            return (peak.getTop() - halfLine - box.y);
        } else {
            return ((box.y + box.height) - 1 - halfLine - peak.getBottom());
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
    private List<BarConnection> getConnections (Staff staff,
                                                VerticalSide side)
    {
        final List<BarConnection> list = new ArrayList<BarConnection>();
        final VerticalSide opposite = side.opposite();
        final StaffProjector projector = projectorOf(staff);
        final int iStart = projector.getStartPeakIndex();

        if (iStart == -1) {
            return list;
        }

        final List<StaffPeak> peaks = projector.getPeaks();
        final StaffPeak startBar = peaks.get(iStart);

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

    //-----------//
    // getGroups //
    //-----------//
    /**
     * Report the peak groups as found in the provided sequence of peaks.
     *
     * @param peaks the horizontal sequence of peaks
     * @return the groups detected
     */
    private List<List<StaffPeak>> getGroups (List<StaffPeak> peaks)
    {
        List<List<StaffPeak>> groups = new ArrayList<List<StaffPeak>>();
        List<StaffPeak> group = new ArrayList<StaffPeak>();

        for (StaffPeak peak : peaks) {
            if (!group.isEmpty()) {
                int gap = peak.getStart() - group.get(group.size() - 1).getStop() - 1;

                if (gap > params.maxDoubleBarGap) {
                    if (group.size() > 1) {
                        groups.add(new ArrayList<StaffPeak>(group));
                    }

                    group.clear();
                }
            }

            group.add(peak);
        }

        // Last group?
        if (group.size() > 1) {
            groups.add(new ArrayList<StaffPeak>(group));
        }

        return groups;
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
        Lag vLag = sheet.getLagManager().getLag(Lags.VLAG);

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
                               StaffPeak peak,
                               StaffPeak rightPeak,
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
        Lag vLag = sheet.getLagManager().getLag(Lags.VLAG);
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
                    String.format("%.2f", serif.getNormalizedWeight(scale.getInterline())),
                    constants.serifMinWeight.getValue());

            return null;
        }

        return serif;
    }

    //---------------------//
    // getStartColumnIndex //
    //---------------------//
    private int getStartColumnIndex (List<BarColumn> columns)
    {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).isStart()) {
                return i;
            }
        }

        return -1;
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
    private void groupBarPeaks (List<StaffPeak> isolated,
                                List<List<StaffPeak>> groups)
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                List<StaffPeak> group = null;
                StaffPeak prevPeak = null;

                for (StaffPeak peak : projectorOf(staff).getPeaks()) {
                    if (peak.isBrace() || peak.isBracket()) {
                        if ((group == null) && (prevPeak != null)) {
                            isolated.add(prevPeak);
                        }

                        group = null;
                        prevPeak = null;
                    } else {
                        if (prevPeak != null) {
                            int gap = peak.getStart() - prevPeak.getStop() - 1;

                            if (gap <= params.maxDoubleBarGap) {
                                // We are in a group with previous peak
                                if (group == null) {
                                    groups.add(group = new ArrayList<StaffPeak>());
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
     * Detect barlines organized in groups.
     */
    private void groupBarlines ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Staff staff : system.getStaves()) {
                StaffPeak prevPeak = null;

                for (StaffPeak peak : projectorOf(staff).getPeaks()) {
                    if (peak.isBrace() || peak.isBracket()) {
                        continue;
                    }

                    if (prevPeak != null) {
                        int gap = peak.getStart() - prevPeak.getStop() - 1;

                        if (gap <= params.maxDoubleBarGap) {
                            BarGroupRelation rel = new BarGroupRelation(scale.pixelsToFrac(gap));
                            sig.addEdge(prevPeak.getInter(), peak.getInter(), rel);
                        }
                    }

                    prevPeak = peak;
                }
            }
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
    private boolean isConnected (StaffPeak peak,
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
    // isTrueBraceGroup //
    //------------------//
    /**
     * Check whether the provided group corresponds to a 1-instrument part
     * (such as a piano part).
     * Braced staves represent a single part when the count of braced staves is 2 (to be improved?)
     * and they are internally connected but not connected to external staves.
     *
     * @param pg the brace PartGroup to check
     * @return true if check is positive
     */
    private boolean isTrueBraceGroup (PartGroup pg)
    {
        if (!pg.isBrace()) {
            return false;
        }

        final int firstId = pg.getFirstStaffId();
        final int lastId = pg.getLastStaffId();
        final int staffCount = lastId - firstId + 1;

        if (staffCount != 2) {
            return false;
        }

        final Staff firstStaff = staffManager.getStaff(firstId - 1);
        final Staff lastStaff = staffManager.getStaff(lastId - 1);

        return !isPartConnected(firstStaff, TOP) // Not connected above
               && isPartConnected(firstStaff, BOTTOM) // Internally connected
               && !isPartConnected(lastStaff, BOTTOM); // Not connected below
    }

    //------------------//
    // lookForBracePeak //
    //------------------//
    private StaffPeak lookForBracePeak (Staff staff,
                                        int minLeft,
                                        int maxRight)
    {
        final StaffProjector projector = projectorOf(staff);
        final StaffPeak bracePeak = projector.findBracePeak(minLeft, maxRight);

        if (bracePeak == null) {
            return null;
        }

        if (bracePeak.getWidth() > params.maxBraceWidth) {
            logger.info("too wide bracePeak {}", bracePeak);

            return null;
        }

        if (allBraceSections == null) {
            allBraceSections = getSectionsByWidth(params.maxBraceThickness);
        }

        Filament filament = filamentBuilder.buildFilament(
                bracePeak,
                params.braceLookupExtension,
                allBraceSections);
        bracePeak.setFilament(filament);

        // A few tests on glyph
        if (filament == null) {
            return null;
        }

        if (filament.getLength(VERTICAL) < params.minBracePortionHeight) {
            return null;
        }

        double curvature = filament.getMeanCurvature(); // curvature radius value
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

            if (ext > params.braceLookupExtension) {
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
        final List<StaffPeak> isolated = new ArrayList<StaffPeak>();
        final List<List<StaffPeak>> groups = new ArrayList<List<StaffPeak>>();
        groupBarPeaks(isolated, groups);

        // Isolated peaks are considered thin
        for (StaffPeak peak : isolated) {
            peak.set(THIN);
        }

        // Process groups is any
        for (List<StaffPeak> group : groups) {
            // Read maximum width difference within this group
            int minWidth = Integer.MAX_VALUE;
            int maxWidth = Integer.MIN_VALUE;

            for (StaffPeak peak : group) {
                int width = peak.getWidth();
                minWidth = Math.min(minWidth, width);
                maxWidth = Math.max(maxWidth, width);
            }

            double normedDelta = (maxWidth - minWidth) / (double) scale.getInterline();
            logger.debug("min:{} max:{} nDelta:{} {}", minWidth, maxWidth, normedDelta, group);

            if (normedDelta >= params.minNormedDeltaWidth) {
                // Hetero (thins & thicks)
                for (StaffPeak peak : group) {
                    int width = peak.getWidth();

                    if ((width - minWidth) <= (maxWidth - width)) {
                        peak.set(THIN);
                    } else {
                        peak.set(THICK);
                    }
                }
            } else {
                // Homo => all thins
                for (StaffPeak peak : group) {
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
                StaffPeak peak = peaks.get(i);

                if (peak.getStart() <= measureStart) {
                    continue;
                }

                // Look for a rather thick first peak
                if (!peak.isStaffEnd(LEFT)
                    && !peak.isStaffEnd(RIGHT)
                    && !(peak.isBrace())
                    && !peak.isBracket()
                    && (peak.getWidth() >= params.minPeak1WidthForCClef)) {
                    // Check gap is larger than multi-bar gap but smaller than measure
                    int gap = peak.getStart() - measureStart;

                    // Gap is not relevant for first measure, thanks to !peak.isStaffEnd() test
                    int minGap = (measureStart == staffStart) ? 0 : params.maxDoubleBarGap;

                    if ((gap > minGap)
                        && (gap < params.minMeasureWidth)
                        && !isConnected(peak, TOP)
                        && !isConnected(peak, BOTTOM)) {
                        if (logger.isDebugEnabled() || peak.isVip()) {
                            logger.info("VIP Got a C-Clef peak1 at {}", peak);
                        }

                        final List<StaffPeak> toRemove = new ArrayList<StaffPeak>();
                        peak.set(CCLEF_ONE);
                        toRemove.add(peak);

                        // Look for a rather thin second peak right after the first
                        if ((i + 1) < peaks.size()) {
                            final StaffPeak peak2 = peaks.get(i + 1);
                            int gap2 = peak2.getStart() - peak.getStop() - 1;

                            if ((peak2.getWidth() <= params.maxPeak2WidthForCClef)
                                && (gap2 <= params.maxDoubleBarGap)
                                && !isConnected(peak2, TOP)
                                && !isConnected(peak2, BOTTOM)) {
                                if (logger.isDebugEnabled() || peak.isVip() || peak2.isVip()) {
                                    logger.info("VIP Got a C-Clef peak2 at {}", peak2);
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
                                            if (logger.isDebugEnabled() || otherPeak.isVip()) {
                                                logger.info(
                                                        "VIP Staff#{} purging tail of C-Clef {}",
                                                        staff.getId(),
                                                        otherPeak);
                                            }

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
                            deleteRelatedColumns(staff.getSystem(), toRemove);
                        }
                    } else {
                        measureStart = peak.getStop() + 1;
                    }
                } else {
                    measureStart = peak.getStop() + 1;
                }
            }
        }
    }

    //---------------------//
    // purgeExtendingPeaks //
    //---------------------//
    /**
     * Purge bars extending above system top staff or below system bottom staff.
     * <p>
     * This purge apply only to peaks on right of start column, since brackets and braces, as
     * well as the start column itself, can extend past top/bottom staves.
     */
    private void purgeExtendingPeaks ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            if (system.getStaves().size() >= params.largeSystemStaffCount) {
                continue; // We cannot not ruin a whole column of large system here!
            }

            for (VerticalSide side : VerticalSide.values()) {
                final Staff staff = (side == TOP) ? system.getFirstStaff()
                        : system.getLastStaff();
                final StaffProjector projector = projectorOf(staff);
                final List<StaffPeak> peaks = projector.getPeaks();
                final int iStart = projector.getStartPeakIndex();
                final Set<StaffPeak> toRemove = new LinkedHashSet<StaffPeak>();

                for (int i = iStart + 1; i < peaks.size(); i++) {
                    StaffPeak peak = peaks.get(i);
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
                    deleteRelatedColumns(system, toRemove);
                }
            }
        }
    }

    //-------------------//
    // purgeLeftOfBraces //
    //-------------------//
    /**
     * In every staff with a brace, purge any peak located on left of the brace.
     */
    private void purgeLeftOfBraces ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            if (!system.isMultiStaff()) {
                continue;
            }

            for (Staff staff : system.getStaves()) {
                final StaffProjector projector = projectorOf(staff);
                final StaffPeak bracePeak = projector.getBracePeak();

                if (bracePeak == null) {
                    continue;
                }

                final List<StaffPeak> peaks = projector.getPeaks();
                final int iStart = projector.getStartPeakIndex();
                final Set<StaffPeak> toRemove = new LinkedHashSet<StaffPeak>();

                for (int i = iStart - 1; i >= 0; i--) {
                    final StaffPeak peak = peaks.get(i);

                    if (peak.getStop() < bracePeak.getStart()) {
                        if (peak.isVip()) {
                            logger.info("VIP removing left {}", peak);
                        }

                        toRemove.add(peak);
                    }
                }

                if (!toRemove.isEmpty()) {
                    logger.debug("Staff#{} removing lefts {}", staff.getId(), toRemove);
                    projector.removePeaks(toRemove);
                    deleteRelatedColumns(system, toRemove);
                }
            }
        }
    }

    //---------------------//
    // purgePartialColumns //
    //---------------------//
    /**
     * Remove all non-full columns located on right side of staff left limit.
     */
    private void purgePartialColumns ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            final List<BarColumn> columns = columnMap.get(system);
            final int iStart = getStartColumnIndex(columns); // Start column (or -1)

            for (int i = iStart + 1; i < columns.size(); i++) {
                final BarColumn column = columns.get(i);

                if (!column.isFull()) {
                    logger.debug("Deleting partial {}", column);

                    // Delete this column, with its related peaks
                    for (StaffPeak peak : column.getPeaks()) {
                        if (peak != null) {
                            if (peak.isVip()) {
                                logger.info("VIP part of partial column {}", peak);
                            }

                            projectorOf(peak.getStaff()).removePeak(peak);
                        }
                    }

                    columns.remove(i--);
                }
            }
        }
    }

    //--------------//
    // purgeTooLeft //
    //--------------//
    /**
     * In every staff with a start peak, discard the peaks located too far on left.
     */
    private void purgeTooLeft ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                final StaffProjector projector = projectorOf(staff);
                int iStart = projector.getStartPeakIndex();

                if (iStart == -1) {
                    continue;
                }

                final List<StaffPeak> peaks = projector.getPeaks();
                final Set<StaffPeak> toRemove = new LinkedHashSet<StaffPeak>();
                StaffPeak prevPeak = peaks.get(iStart);

                for (int i = iStart - 1; i >= 0; i--) {
                    final StaffPeak peak = peaks.get(i);
                    final int gap = prevPeak.getStart() - peak.getStop() + 1;

                    if (gap > params.maxBraceBarGap) {
                        if (peak.isVip()) {
                            logger.info("VIP removing too left {}", peak);
                        }

                        toRemove.add(peak);
                    } else {
                        prevPeak = peak;
                    }
                }

                if (!toRemove.isEmpty()) {
                    logger.debug("Staff#{} removing too lefts {}", staff.getId(), toRemove);
                    projector.removePeaks(toRemove);
                    deleteRelatedColumns(system, toRemove);
                }
            }
        }
    }

    //--------------------//
    // purgeUnalignedBars //
    //--------------------//
    /**
     * On multi-staff system, purge isolated bars (bars aligned with nothing).
     */
    private void purgeUnalignedBars ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            if (!system.isMultiStaff()) {
                continue;
            }

            for (Staff staff : system.getStaves()) {
                final StaffProjector projector = projectorOf(staff);

                for (StaffPeak peak : new ArrayList<StaffPeak>(projector.getPeaks())) {
                    if (peakGraph.edgesOf(peak).isEmpty()) {
                        if (peak.isVip()) {
                            logger.info("VIP unaligned {}", peak);
                        }

                        projector.removePeak(peak);
                    }
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

    //-------------//
    // replacePeak //
    //-------------//
    /**
     * Replace a peak by another one.
     *
     * @param oldPeak the peak to be replaced
     * @param newPeak the new peak
     */
    private void replacePeak (StaffPeak oldPeak,
                              StaffPeak newPeak)
    {
        // PeakGraph
        for (BarAlignment edge : new ArrayList<BarAlignment>(peakGraph.incomingEdgesOf(oldPeak))) {
            StaffPeak source = peakGraph.getEdgeSource(edge);
            peakGraph.addEdge(source, newPeak, edge);
        }

        for (BarAlignment edge : new ArrayList<BarAlignment>(peakGraph.outgoingEdgesOf(oldPeak))) {
            StaffPeak target = peakGraph.getEdgeTarget(edge);
            peakGraph.addEdge(newPeak, target, edge);
        }

        // Column
        BarColumn column = oldPeak.getColumn();

        if (column != null) {
            column.addPeak(newPeak);
        }

        // Projector
        final Staff staff = oldPeak.getStaff();
        final StaffProjector projector = projectorOf(staff);
        projector.insertPeak(newPeak, oldPeak);
        projector.removePeak(oldPeak);
    }

    //-----------------//
    // verifyLinesRoot //
    //-----------------//
    /**
     * Make sure that there is no staff line portion right before first bar or bracket.
     * <p>
     * This may occur only on 1-staff systems.
     */
    private void verifyLinesRoot ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            if (system.isMultiStaff()) {
                continue;
            }

            final Staff staff = system.getFirstStaff();
            final StaffProjector projector = projectorOf(staff);
            projector.checkLinesRoot();
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

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean showVerticalLines = new Constant.Boolean(
                false,
                "Should we show the vertical grid lines?");

        private final Constant.Integer largeSystemStaffCount = new Constant.Integer(
                "staves",
                4,
                "Minimum number of staves for a system to be large");

        private final Scale.Fraction minThinThickDelta = new Scale.Fraction(
                0.2,
                "Minimum difference between THIN/THICK width values");

        private final Scale.Fraction maxBarExtension = new Scale.Fraction(
                1.0,
                "Maximum extension for a barline above or below staff line");

        private final Scale.Fraction maxLinesLeftToStartBar = new Scale.Fraction(
                0.15,
                "Maximum dx between left end of staff lines and start bar");

        private final Scale.Fraction maxDoubleBarGap = new Scale.Fraction(
                0.75,
                "Max horizontal gap between two members of a double bar");

        private final Scale.Fraction maxBraceBarGap = new Scale.Fraction(
                1.5,
                "Max horizontal gap between a brace peak and next bar");

        private final Scale.Fraction minMeasureWidth = new Scale.Fraction(
                2.0,
                "Minimum width for a measure");

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

        private final Scale.Fraction braceLookupExtension = new Scale.Fraction(
                1.0,
                "Lookup height for brace end above or below staff line");

        private final Scale.Fraction maxBraceCurvature = new Scale.Fraction(
                20, // 35,
                "Maximum mean curvature radius for a brace");

        // For brackets ----------------------------------------------------------------------------
        //
        private final Scale.Fraction minBracketWidth = new Scale.Fraction(
                0.25,
                "Minimum width for a bracket peak");

        private final Scale.Fraction maxBracketExtension = new Scale.Fraction(
                1.25,
                "Maximum extension for bracket trunk end above or below staff line");

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

        final int largeSystemStaffCount;

        final double minNormedDeltaWidth;

        final double maxBarExtension;

        final int maxLinesLeftToStartBar;

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

        final int braceLookupExtension;

        final int maxBraceCurvature;

        final int serifSegmentLength;

        final int minBracketWidth;

        final int maxBracketExtension;

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
            largeSystemStaffCount = constants.largeSystemStaffCount.getValue();
            minNormedDeltaWidth = constants.minThinThickDelta.getValue();
            maxBarExtension = scale.toPixels(constants.maxBarExtension);
            maxLinesLeftToStartBar = scale.toPixels(constants.maxLinesLeftToStartBar);
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
            braceLookupExtension = scale.toPixels(constants.braceLookupExtension);
            maxBraceCurvature = scale.toPixels(constants.maxBraceCurvature);

            serifSegmentLength = scale.toPixels(constants.serifSegmentLength);
            minBracketWidth = scale.toPixels(constants.minBracketWidth);
            maxBracketExtension = scale.toPixels(constants.maxBracketExtension);
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
