//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t e m R e t r i e v e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker.VLinker;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker.CLinker;
import org.audiveris.omr.sig.GradeUtil;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BarConnectionRelation;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code StemsRetriever} processes a system to retrieve stems that connect to note
 * heads and perhaps beams.
 * <p>
 * This class defines the main component in charge of retrieving all stems in a system.
 * It uses companion classes:
 * <ul>
 * <li>{@link BeamLinker} handles all links from one beam.<br>
 * It is composed of one {@link BLinker} for each relevant location of the beam:
 * One on each beam side and one on each detected stump.
 * Depending on the configuration, a BLinker can have several {@link VLinker} dedicated to
 * vertical directions (0 for an ANCHORD with no stump, 1 for a stump pointing out of beam, 2 for
 * beam horizontal side with no stump).
 * <li>{@link HeadLinker} handles all links from one head.<br>
 * It is composed of one {@link SLinker} for each head horizontal side, each SLinker being composed
 * of one {@link CLinker} for each vertical side.
 * <li>{@link StemBuilder} handles the building of one stem for any vertical Linker
 * (VLinker or CLinker).
 * <li>{@link StemChecker} handles stem checking for StemBuilder.
 * </ul>
 * At this point in the OMR pipeline:
 * <ul>
 * <li>Beams have been identified (in BEAMS step) as well as void and black heads (in HEADS step),
 * but no flags yet (flags will be addressed later in the SYMBOLS step).
 * <li>Some stem seeds have been identified (in STEM_SEEDS step). A stem seed has a minimum length
 * and no internal vertical gap.
 * <li>We got a measurement for main and max stem thickness in the sheet (in STEM_SEEDS step).
 * </ul>
 *
 * <h3>Phase #1: one system at a time</h3>
 * <ul>
 * <li>{@link #inspectStems()}
 * <ul>
 * <li>Gather stem seeds for the system at hand.
 * <li>Allocate one BeamLinker for each beam.
 * <li>Allocate one HeadLinker for each head.
 * <li>For every beam, inspect every VLinker and populate its StemBuilder.
 * <li>For every head, inspect every CLinker and populate its StemBuilder.
 * </ul>
 * <li>{@link #linkStems()}
 * <ul>
 * <li>First, for every beam, link each of its horizontal sides, then each of its non-side stumps.
 * <li>Second, for every head not linked to a beam, try to link on both horizontal sides using
 * {@link HeadLinker#linkSides} with increasing profile values.
 * <li>Finally, for every still-not-linked head, try to connect to some existing stem.
 * </ul>
 * </ul>
 * <h3>Phase #2: once all systems have been processed</h3>
 * <ul>
 * <li>{@link #finalizeStems()}
 * <ul>
 * <li>Purge orphan beams located between two systems: a beam with no link is discarded if its
 * counterpart in the other system has links.
 * <li>Boost beam sides: beside stems linked to a beam side, boost the related heads as well, so
 * that they can survive and the beam with them.
 * <li>Check head stems: make sure that any head exhibits at most 2 stem links (in bottom left and
 * top right corners).
 * <li>Check needed stems: heads with no stem link at all are discarded.
 * </ul>
 * </ul>
 *
 * <h3>Possible evolutions</h3>
 * <ul>
 * <li>For rather long stems, full stem expansion driven only from the initial head or beam may be
 * difficult. In such cases, we could delegate stem expansion to some intermediate heads carefully
 * chosen.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class StemsRetriever
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StemsRetriever.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    @Navigable(false)
    private final SIGraph sig;

    /** Sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** Scale-dependent parameters. */
    private Parameters params;

    /** Stem-compatible heads for this system. */
    private List<Inter> systemHeads;

    /** Vertical seeds for this system. */
    private List<Glyph> systemSeeds;

    /** Beams and beam hooks for this system. */
    private List<Inter> systemBeams;

    /** Stems interpretations for this system. */
    private final HashMap<Glyph, StemInter> systemStems = new HashMap<>();

    /** Areas forbidden to stem candidates. */
    private final List<Area> noStemAreas;

    /** For stem validation. */
    private StemChecker stemChecker;

    /** System(s) immediately above our system. */
    private final List<SystemInfo> systemsAbove;

    /** System(s) immediately below our system. */
    private final List<SystemInfo> systemsBelow;

    /** Maximum vertical gap, per profile level. */
    private final TreeMap<Integer, Integer> gapMap;

    private StopWatch watch;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemsBuilder object.
     *
     * @param system the dedicated system
     */
    public StemsRetriever (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();

        Sheet sheet = system.getSheet();
        scale = sheet.getScale();

        gapMap = buildGapMap();

        noStemAreas = retrieveNoStemAreas();

        final SystemManager mgr = sheet.getSystemManager();
        systemsAbove = mgr.verticalNeighbors(system, VerticalSide.TOP);
        systemsBelow = mgr.verticalNeighbors(system, VerticalSide.BOTTOM);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // finalizeBeams //
    //---------------//
    /**
     * Only when all systems have been processed, we can finalize the processing.
     */
    public void finalizeBeams ()
    {
        if (params == null) {
            params = new Parameters(system, scale);
        }

        // The abscissa-sorted stem seeds for this system
        systemSeeds = system.getGroupedGlyphs(GlyphGroup.VERTICAL_SEED);
        purgeNoStemSeeds(systemSeeds);

        new Finalizer().process();
    }

    //---------//
    // process //
    //---------//
    public void process ()
    {
        watch = new StopWatch("StemsRetriever.process S#" + system.getId());

        if (params == null) {
            params = new Parameters(system, scale);
        }

        inspectStems();
        linkStems();
        finalizeStems();

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName())
                .append("{S#").append(system.getId()).append('}').toString();
    }

    //-------------//
    // getTargetPt //
    //-------------//
    /**
     * Compute the point where the (skewed) vertical from reference point crosses the
     * provided limit.
     *
     * @param refPt reference point for stem connection
     * @param limit the end of the white space (a rather horizontal line)
     * @param slope global sheet slope
     * @return the limit crossing point with skewed vertical at reference point
     */
    public static Point2D getTargetPt (Point2D refPt,
                                       Line2D limit,
                                       double slope)
    {
        final Point2D p2 = new Point2D.Double(refPt.getX() - (100 * slope), refPt.getY() + 100);

        return LineUtil.intersection(refPt, p2, limit.getP1(), limit.getP2());
    }

    //------------------//
    // sortBeamsFromRef //
    //------------------//
    /**
     * Sort the provided beams vertically from the reference point.
     *
     * @param refPt starting reference point
     * @param yDir  vertical direction from reference point
     * @param beams the beams to sort
     */
    public static void sortBeamsFromRef (Point2D refPt,
                                         int yDir,
                                         List<Inter> beams)
    {
        if (beams.isEmpty()) {
            return;
        }

        final double slope = beams.get(0).getSig().getSystem().getSheet().getSkew().getSlope();

        Comparator<Inter> fromRef = (i1, i2) -> {
            final AbstractBeamInter b1 = (AbstractBeamInter) i1;
            final AbstractBeamInter b2 = (AbstractBeamInter) i2;
            return Double.compare(
                    yDir * (getTargetPt(refPt, getLimit(b1, yDir), slope).getY() - refPt.getY()),
                    yDir * (getTargetPt(refPt, getLimit(b2, yDir), slope).getY() - refPt.getY()));
        };

        Collections.sort(beams, fromRef);
    }

    //--------------//
    // addStemInter //
    //--------------//
    /**
     * Record the provided stem into the system glyph-stem map.
     *
     * @param stem the provided stem
     */
    void addStemInter (StemInter stem)
    {
        if (stem.getGlyph() != null) {
            systemStems.put(stem.getGlyph(), stem);
        }
    }

    //-----------//
    // getGapMap //
    //-----------//
    /**
     * Report acceptable vertical gap per profile.
     *
     * @return the gapMap
     */
    TreeMap<Integer, Integer> getGapMap ()
    {
        return gapMap;
    }

    //---------------------------//
    // getMaxHeadContextualGrade //
    //---------------------------//
    /**
     * Report the maximum contextual grade for the provided head, assumed to be linked via
     * the provided head-stem relation to a perfect stem.
     *
     * @param head  head for which contextual grade is reported
     * @param hsRel relation that would stand between head and stem
     * @return head contextual grade (based only on support from a perfect stem)
     */
    double getMaxHeadContextualGrade (HeadInter head,
                                      HeadStemRelation hsRel)
    {
        final double maxStemGrade = Grades.intrinsicRatio;
        final double maxStemContrib = maxStemGrade * hsRel.getTargetRatio() - 1.0;
        final double maxCg = GradeUtil.contextual(head.getGrade(), maxStemContrib);

        return maxCg;
    }

    //----------------//
    // getNoStemAreas //
    //----------------//
    /**
     * @return the noStemAreas
     */
    List<Area> getNoStemAreas ()
    {
        return noStemAreas;
    }

    //-----------//
    // getParams //
    //-----------//
    /**
     * Give access to the whole set of parameters used by for stem retrieval.
     *
     * @return the parameters
     */
    Parameters getParams ()
    {
        return params;
    }

    //----------------------//
    // getNeighboringInters //
    //----------------------//
    /**
     * From the provided collection of interpretations, retrieve all those located
     * in some item vicinity.
     *
     * @param inters  the collection of interpretations to search
     * @param itemBox bounding box of item
     * @return the set of neighboring interpretations
     */
    List<Inter> getNeighboringInters (List<? extends Inter> inters,
                                      Rectangle itemBox)
    {
        // Retrieve neighboring inters, using a box of system height and sufficiently wide,
        // just to play with a limited number of inters.
        Rectangle systemBox = system.getBounds();
        Rectangle fatBox = new Rectangle(itemBox.x, systemBox.y, itemBox.width, systemBox.height);
        fatBox.grow(params.vicinityMargin, 0);

        return Inters.intersectedInters(inters, GeoOrder.BY_ABSCISSA, fatBox);
    }

    //---------------------//
    // getNeighboringSeeds //
    //---------------------//
    /**
     * Retrieve all vertical seeds in some item vicinity.
     *
     * @return the set of neighboring seeds
     */
    Set<Glyph> getNeighboringSeeds (Rectangle itemBox)
    {
        // Retrieve neighboring stem seeds, using a box of system height and sufficiently wide,
        // just to play with a limited number of seeds.
        Rectangle systemBox = system.getBounds();
        Rectangle fatBox = new Rectangle(itemBox.x, systemBox.y, itemBox.width, systemBox.height);
        fatBox.grow(params.vicinityMargin, 0);

        return Glyphs.intersectedGlyphs(systemSeeds, fatBox);
    }

    /**
     * @return the system
     */
    SystemInfo getSystem ()
    {
        return system;
    }

    /**
     * @return the system beams
     */
    List<Inter> getSystemBeams ()
    {
        return systemBeams;
    }

    /**
     * @return the system heads
     */
    List<Inter> getSystemHeads ()
    {
        return systemHeads;
    }

    /**
     * @return the stem checker
     */
    StemChecker getStemChecker ()
    {
        return stemChecker;
    }

    //--------------//
    // getStemInter //
    //--------------//
    /**
     * Report the stem interpretation if any for the glyph at hand.
     *
     * @param glyph the underlying glyph
     * @return the existing stem interpretation if any, or null
     */
    StemInter getStemInter (Glyph glyph)
    {
        return systemStems.get(glyph);
    }

    //-------------//
    // getTargetPt //
    //-------------//
    /**
     * Compute the point where the (skewed) vertical from reference point crosses the
     * provided limit.
     *
     * @param refPt reference point for stem connection
     * @param limit the end of the white space (a rather horizontal line)
     * @return the limit crossing point with skewed vertical at reference point
     */
    Point2D getTargetPt (Point2D refPt,
                         Line2D limit)
    {
        return getTargetPt(refPt, limit, system.getSheet().getSkew().getSlope());
    }

    //--------------------//
    // getTheoreticalLine //
    //--------------------//
    /**
     * Compute the (skewed) vertical line from reference point to the system limit.
     *
     * @param refPt starting reference point
     * @param yDir  vertical direction from reference point
     * @return the theoretical line oriented from ref point to system border
     */
    Line2D getTheoreticalLine (Point2D refPt,
                               int yDir)
    {
        final Rectangle systemBox = system.getBounds();
        final int sysY = (yDir > 0) ? (systemBox.y + systemBox.height) : systemBox.y;
        final Point2D sysPt = getTargetPt(refPt, new Line2D.Double(0, sysY, 100, sysY));
        return new Line2D.Double(refPt, sysPt);
    }

    //----------------//
    // checkHeadStems //
    //----------------//
    /**
     * A head can have links to two stems (non mutually exclusive) only when these
     * stems are compatible (head is on stem ends with one stem on bottom left and
     * one stem on top right).
     * <p>
     * Otherwise, we must clean up the configuration:
     * <ol>
     * <li>If there is a link with zero yGap, it has priority over non-zero yGap that are
     * generally due to stem extension. So cut the non-zero links.
     * <li>If there are two zero yGaps, if they are opposed on normal sides, it's OK.
     * <li>If not, cut the link to the one not on normal side.
     * <li>If there are two non-zero yGaps, cut the one with larger yGap.
     * </ol>
     *
     * @param head the note head to check
     */
    private void checkHeadStems (HeadInter head)
    {
        // Retrieve all stems connected to this head
        List<Inter> allStems = new ArrayList<>();

        for (Relation rel : sig.getRelations(head, HeadStemRelation.class)) {
            allStems.add(sig.getEdgeTarget(rel));
        }

        // List of non-conflicting stems ensembles
        List<List<Inter>> partners = sig.getPartitions(null, allStems);
        HeadStemsCleaner checker = null;

        for (List<Inter> ensemble : partners) {
            if (ensemble.size() <= 1) {
                continue;
            }

            if (checker == null) {
                checker = new HeadStemsCleaner(head);
            }

            checker.check(ensemble);
        }
    }

    //------------------//
    // checkNeededStems //
    //------------------//
    /**
     * For heads that need a stem, check there is at least one.
     *
     * @param systemHeads heads to check
     */
    private void checkNeededStems (List<Inter> systemHeads)
    {
        for (Inter head : systemHeads) {
            if (ShapeSet.StemHeads.contains(head.getShape())) {
                if (!sig.hasRelation(head, HeadStemRelation.class)) {
                    head.setAbnormal(true);
                }
            }
        }
    }

    //-------------//
    // buildGapMap //
    //-------------//
    private TreeMap<Integer, Integer> buildGapMap ()
    {
        final TreeMap<Integer, Integer> map = new TreeMap<>();
        map.put(-1, 0);

        for (int p = 0; p <= Profiles.MAX_VALUE; p++) {
            map.put(p, scale.toPixels(StemChecker.getMaxYGap(p)));
        }

        logger.debug("gapMap:{}", map);

        return map;
    }

    //----------//
    // getLimit //
    //----------//
    /**
     * Report closer beam limit, according to corner vertical direction.
     *
     * @param beam the beam or hook of interest
     * @param yDir vertical direction from reference point
     * @return the top or bottom beam limit, according to dir
     */
    private static Line2D getLimit (AbstractBeamInter beam,
                                    int yDir)
    {
        return beam.getBorder(VerticalSide.of(yDir));
    }

    //---------------//
    // finalizeStems //
    //---------------//
    /**
     * Final stems processing.
     */
    private void finalizeStems ()
    {
        // Check carefully multiple stem links on same head
        watch.start("checkHeadStems");
        for (Inter head : systemHeads) {
            checkHeadStems((HeadInter) head);
        }

        // Flag heads with no stem link as abnormal
        watch.start("checkNeededStems");
        checkNeededStems(systemHeads);
    }

    //--------------//
    // inspectStems //
    //--------------//
    /**
     * Inspect stems to suitable heads and beams in the system.
     * <p>
     * No link is performed, we just gather information about heads, beams, glyphs, vertical gaps.
     */
    private void inspectStems ()
    {
        logger.debug("\n{} inspectStems", system);
        stemChecker = new StemChecker(system.getSheet());

        // The abscissa-sorted stem seeds for this system
        watch.start("Seeds");
        systemSeeds = system.getGroupedGlyphs(GlyphGroup.VERTICAL_SEED);
        purgeNoStemSeeds(systemSeeds);

        // The abscissa-sorted beam (and beam hook) interpretations for this system
        watch.start("Beams linkers");
        systemBeams = sig.inters(AbstractBeamInter.class);
        Collections.sort(systemBeams, Inters.byAbscissa);

        for (Inter b : systemBeams) {
            AbstractBeamInter beam = (AbstractBeamInter) b;

            if (beam.getLinker() == null) {
                beam.setLinker(new BeamLinker(beam, this));
            }
        }

        // The abscissa-sorted head interpretations for this system
        watch.start("Heads linkers");
        systemHeads = sig.inters(ShapeSet.getStemTemplateNotes(system.getSheet()));
        Collections.sort(systemHeads, Inters.byAbscissa);

        for (Inter h : systemHeads) {
            HeadInter head = (HeadInter) h;
            head.setLinker(new HeadLinker(head, this)); // -> 1 SLinker/hSide (-> 1 CLinker/vSide)
        }

        // Inspect each beam stump or side to reachable heads (and other beams w/i group)
        watch.start("Beams inspection");
        for (Inter b : systemBeams) {
            AbstractBeamInter beam = (AbstractBeamInter) b;
            beam.getLinker().inspectVLinkers(); // -> 1 VLinker/beam hit if needed (+ heads)
        }

        // Inspect each head to other heads and beam
        watch.start("Heads inspection");
        for (Inter h : systemHeads) {
            HeadInter head = (HeadInter) h;
            head.getLinker().inspectCLinkers(); // -> 1 VLinker if beam hit (+ other heads)
        }
    }

    //-----------//
    // linkStems //
    //-----------//
    /**
     * Try to actually link from beams and from heads.
     */
    private void linkStems ()
    {
        logger.debug("\n{} linkStems", system);
        stemChecker = new StemChecker(system.getSheet());

        // Beams first
        watch.start("Beams linking");
        systemBeams = sig.inters(AbstractBeamInter.class);
        Collections.sort(systemBeams, Inters.byReverseWidth); // Link longest beams first

        for (Iterator<Inter> it = systemBeams.iterator(); it.hasNext();) {
            final AbstractBeamInter beam = (AbstractBeamInter) it.next();

            boolean ok = beam.getLinker().linkSides(system.getProfile());
            if (!ok) {
                logger.debug("Cannot link both sides of {}", beam);
                it.remove();
                continue;
            }

            beam.getLinker().linkStumps(system.getProfile());
        }

        // Heads second
        watch.start("Heads linking phase 1");
        systemHeads = sig.inters(ShapeSet.getStemTemplateNotes(system.getSheet()));
        Collections.sort(systemHeads, Inters.byReverseGrade);
        final List<HeadInter> unlinkedHeads = new ArrayList<>();

        for (Inter h : systemHeads) {
            final HeadInter head = (HeadInter) h;
            if (!head.getLinker().linkSides(Profiles.STANDARD, system.getProfile(), false)) {
                unlinkedHeads.add(head);
            }
        }

        watch.start("Heads linking phase 2");
        for (HeadInter head : unlinkedHeads) {
            head.getLinker().linkSides(Profiles.STANDARD, system.getProfile(), true);
        }
    }

    //------------------//
    // purgeNoStemSeeds //
    //------------------//
    /**
     * In the provided seeds collection, remove the ones that are located in so-called
     * noStemAreas.
     *
     * @param seeds the collection to purge
     */
    private void purgeNoStemSeeds (List<Glyph> seeds)
    {
        SeedLoop:
        for (Iterator<Glyph> it = seeds.iterator(); it.hasNext();) {
            final Glyph seed = it.next();
            final Rectangle seedBox = seed.getBounds();
            final double seedSize = seedBox.width * seedBox.height;

            for (Area noStem : noStemAreas) {
                if (noStem.intersects(seedBox)) {
                    // Compute intersection over noStem area
                    Area intersection = new Area(seedBox);
                    intersection.intersect(noStem);
                    Rectangle2D interBox = intersection.getBounds2D();
                    double interSize = interBox.getWidth() * interBox.getHeight();

                    Rectangle2D barBox = noStem.getBounds2D();
                    double barSize = barBox.getWidth() * barBox.getHeight();

                    final double ratio = interSize / Math.min(barSize, seedSize);

                    if (ratio > params.maxBarOverlap) {
                        it.remove();

                        continue SeedLoop;
                    }
                }
            }
        }
    }

    //---------------------//
    // retrieveNoStemAreas //
    //---------------------//
    /**
     * Remember those inters that candidate stems cannot compete with.
     * <p>
     * Typically, these are 2-staff barlines with their connector.
     *
     * @return list of no-stem areas, sorted by abscissa
     */
    private List<Area> retrieveNoStemAreas ()
    {
        final List<Area> areas = new ArrayList<>();

        for (Inter barline : sig.inters(BarlineInter.class)) {
            Set<Relation> connections = sig.getRelations(barline, BarConnectionRelation.class);

            for (Relation connection : connections) {
                BarlineInter source = (BarlineInter) sig.getEdgeSource(connection);

                if (source == barline) {
                    // Top area
                    areas.add(barline.getArea());

                    // Bottom area
                    BarlineInter target = (BarlineInter) sig.getEdgeTarget(connection);
                    areas.add(target.getArea());

                    // Middle area
                    Line2D median = new Line2D.Double(
                            source.getMedian().getP2(),
                            target.getMedian().getP1());
                    double width = 0.5 * (source.getWidth() + target.getWidth());
                    Area middle = AreaUtil.verticalRibbon(median, width);
                    areas.add(middle);
                }
            }
        }

        // Sort by abscissa
        Collections.sort(areas, (Area a1, Area a2) -> Double.compare(a1.getBounds2D().getMinX(),
                                                                     a2.getBounds2D().getMinX()));

        return areas;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Finalizer //
    //-----------//
    /**
     * This class is to be run after a pass of {@link #inspectStems} and
     * {@link #linkStems} on all systems.
     */
    private class Finalizer
    {

        private final StopWatch watch
                = new StopWatch("StemsRetriever.finalizer S#" + system.getId());

        /** Longest length of relevant beam stem as observed in our system. */
        int maxStemLength = Integer.MIN_VALUE;

        /** Collection of standard beams in current system. */
        final List<AbstractBeamInter> beams;

        ////** Typical abscissa gap among stem/heads along a beam. */
        ///final Integer typicalBeamGap;
        /** The abscissa-sorted head interpretations for this system. */
        final List<Inter> systemHeads = sig.inters(ShapeSet.getStemTemplateNotes(system.getSheet()));

        public Finalizer ()
        {
            watch.start("<init>");
            Collections.sort(systemHeads, Inters.byAbscissa);

            beams = getStandardBeams();
            ///typicalBeamGap = typicalBeamStemsDx();
        }

        public void process ()
        {
            // Purge cross-system orphan beams
            watch.start("beam purge");
            purgeOrphanBeams();

            // Boost beam sides
            watch.start("beam sides");
            for (AbstractBeamInter beam : beams) {
                boostBeamSides(beam);
            }

            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }

        //----------------//
        // boostBeamSides //
        //----------------//
        /**
         * Boost stems and heads, if any, found on each horizontal side of the provided
         * (good) beam.
         *
         * @param beam provided beam
         */
        private void boostBeamSides (Inter beam)
        {
            if (!beam.isGood()) {
                return;
            }

            for (Relation r : sig.getRelations(beam, BeamStemRelation.class)) {
                final BeamStemRelation bsRel = (BeamStemRelation) r;

                if (bsRel.getBeamPortion() != BeamPortion.CENTER) {
                    StemInter stem = (StemInter) sig.getEdgeTarget(bsRel);
                    // Nota: Stem is already boosted via specific BeamStemRelation

                    // Boost heads as well, at least the rather good ones
                    for (Relation rr : sig.getRelations(stem, HeadStemRelation.class)) {
                        final HeadStemRelation hsRel = (HeadStemRelation) rr;
                        final HeadInter head = (HeadInter) sig.getEdgeSource(hsRel);

                        if (!head.getShape().isSmall()) {
                            final double grade = 0.5 * (bsRel.getGrade() + hsRel.getGrade());
                            sig.addEdge(beam, head, new BeamHeadRelation(grade, true));
                        }
                    }
                }
            }
        }

        //--------------------//
        // getCrossSystemBeam //
        //--------------------//
        /**
         * Report another beam instance, if any, found in a system above or below with the
         * same underlying glyph.
         *
         * @param beam the beam at hand
         * @return the other beam (in a different system/sig) or null if not found
         */
        private AbstractBeamInter getCrossSystemBeam (AbstractBeamInter beam)
        {
            final Point center = beam.getCenter();
            final Glyph glyph = beam.getGlyph();

            if (glyph == null) {
                return null;
            }

            if (!systemsAbove.isEmpty()) {
                if (center.y < system.getFirstStaff().getFirstLine().yAt(center.x)) {
                    for (SystemInfo syst : systemsAbove) {
                        for (Inter b : syst.getSig().inters(BeamInter.class)) {
                            if (b.getGlyph() == glyph) {
                                return (AbstractBeamInter) b;
                            }
                        }
                    }
                }
            }

            if (!systemsBelow.isEmpty()) {
                if (center.y > system.getLastStaff().getLastLine().yAt(center.x)) {
                    for (SystemInfo syst : systemsBelow) {
                        for (Inter b : syst.getSig().inters(BeamInter.class)) {
                            if (b.getGlyph() == glyph) {
                                return (AbstractBeamInter) b;
                            }
                        }
                    }
                }
            }

            return null;
        }

        /**
         * Retrieve all the standard (non-small) beams in system.
         *
         * @return the collection of relevant beams
         */
        private List<AbstractBeamInter> getStandardBeams ()
        {
            final List<AbstractBeamInter> found = new ArrayList<>();

            for (Inter beam : sig.inters(AbstractBeamInter.class)) {
                if (beam instanceof BeamInter || beam instanceof BeamHookInter) {
                    found.add((AbstractBeamInter) beam);
                }
            }

            return found;
        }

        //------------------//
        // purgeOrphanBeams //
        //------------------//
        /**
         * Purge any beam with no stem linked if it has a counterpart in other system
         * and this counterpart beam has stems linked.
         * <p>
         * This method can be called only when all systems have been linked.
         */
        private void purgeOrphanBeams ()
        {
            for (Iterator<AbstractBeamInter> it = beams.iterator(); it.hasNext();) {
                final AbstractBeamInter beam = it.next();

                if (beam.isVip()) {
                    logger.info("VIP {} purgeOrphanBeams", beam);
                }

                if (beam.getStems().isEmpty()) {
                    // Beam located between systems?
                    AbstractBeamInter crossBeam = getCrossSystemBeam(beam);

                    if (crossBeam != null) {
                        if (!crossBeam.getStems().isEmpty()) {
                            if (beam.isVip()) {
                                logger.info("VIP {} discarding cross system {}", system, beam);
                            }

                            beam.remove();
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Scale.Fraction vicinityMargin = new Scale.Fraction(
                1.0,
                "Rough abscissa margin when looking for neighbors above and below in the system");

        private final Constant.Double slopeMargin = new Constant.Double(
                "tangent",
                0.015,
                "Margin around slope to define lookup areas");

        private final Constant.Ratio maxBarOverlap = new Constant.Ratio(
                0.25,
                "Maximum stem overlap area ratio on a connected barline");

        private final Scale.Fraction maxBeamSideDx = new Scale.Fraction(
                0.25,
                "Maximum horizontal dx around beam sides to catch sibling groups");

        private final Scale.Fraction maxBeamGroupDy = new Scale.Fraction(
                5.0,
                "Maximum vertical gap between two beam groups on same stem");

        private final Scale.Fraction minBeamHeadDy = new Scale.Fraction(
                1.0,
                "Minimum vertical distance between beam and head");

        private final Scale.Fraction maxBeamLinkerDx = new Scale.Fraction(
                0.25,
                "Maximum horizontal distance to reuse a beam linker");

        private final Scale.Fraction halfBeamLuDx = new Scale.Fraction(
                0.3,
                "Half dx for beam lookup area for stem and heads");

        private final Scale.Fraction minSeedContrib = new Scale.Fraction(
                0.5,
                "Minimum seed vertical contribution to stem");

        private final Scale.Fraction maxBeamSeedDx = new Scale.Fraction(
                0.1,
                "Maximum horizontal gap between beam and seed");

        private final Constant.Ratio maxBeamSeedDyRatio = new Constant.Ratio(
                0.5,
                "Maximum vertical gap between beam and seed as ratio of max gap with stem");

        private final Scale.Fraction minBeamStemsDx = new Scale.Fraction(
                1.0,
                "Minimum horizontal distance between two stems on the same beam");

        private final Scale.Fraction minBeamStumpDy = new Scale.Fraction(
                0.5,
                "Minimum stump length above and below beam group");

        private final Scale.Fraction maxLineSeedDx = new Scale.Fraction(
                0.15,
                "Maximum distance from stem seed to theoretical line");

        private final Scale.Fraction maxLineSectionDx = new Scale.Fraction(
                0.3,
                "Maximum distance from section center to target line");

        private final Scale.Fraction maxStemAlignmentDx = new Scale.Fraction(
                0.15,
                "Maximum dx between aligned stem seeds");

        private final Scale.Fraction maxStemAlignmentDy = new Scale.Fraction(
                4.0,
                "Maximum dy to check for aligned stem seeds");

        private final Scale.Fraction minHeadHeadDy = new Scale.Fraction(
                0.25,
                "Minimum vertical distance between two heads on a stem");

        private final Scale.Fraction maxHeadSeedDy = new Scale.Fraction(
                0.25,
                "Maximum vertical gap between seed & head reference point");

        private final Scale.Fraction stumpAreaDyHalf = new Scale.Fraction(
                0.2,
                "Half height of stump lookup area");

        private final Scale.Fraction stumpAreaDxIn = new Scale.Fraction(
                0.1,
                "Inside half width of stump lookup area");

        private final Scale.Fraction stumpAreaDxOut = new Scale.Fraction(
                0.1,
                "Outside half width of stump lookup area");

        private final Scale.Fraction minLinkerLength = new Scale.Fraction(
                0.85,
                "Minimum concrete length for a linker");

        private final Constant.Ratio artificialStemGrade = new Constant.Ratio(
                0.4,
                "Default grade for an artificial stem");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    static class Parameters
    {

        final double slopeMargin;

        final double maxBarOverlap;

        final int maxHeadOutDx;

        final int maxHeadInDx;

        final int vicinityMargin;

        final int minHeadHeadDy;

        final int minSeedContrib;

        final int maxHeadSeedDy;

        final int stumpAreaDyHalf;

        final double stumpAreaDxIn;

        final double stumpAreaDxOut;

        final int mainStemThickness;

        final int maxStemThickness;

        final int minBeamHeadDy;

        final int maxBeamLinkerDx;

        final double halfBeamLuDx;

        final int maxBeamSideDx;

        final int maxBeamGroupDy;

        final double maxBeamSeedDx;

        final int minBeamStumpDy;

        final double maxBeamSeedDyRatio;

        final int minBeamStemsDx;

        final double maxLineSeedDx;

        final double maxLineSectionDx;

        final int minChunkWeight;

        final int minStemTailLg;

        final int bestStemTailLg;

        final int minLinkerLength;

        final double maxStemAlignmentDx;

        final double maxStemAlignmentDy;

        final double artificialStemGrade;

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        Parameters (SystemInfo system,
                    Scale scale)
        {
            final int profile = system.getProfile();

            slopeMargin = constants.slopeMargin.getValue();
            maxBarOverlap = constants.maxBarOverlap.getValue();
            vicinityMargin = scale.toPixels(constants.vicinityMargin);

            minHeadHeadDy = scale.toPixels(constants.minHeadHeadDy);
            minSeedContrib = scale.toPixels(constants.minSeedContrib);
            maxHeadSeedDy = scale.toPixels(constants.maxHeadSeedDy);
            maxHeadOutDx = scale.toPixels(HeadStemRelation.getXOutGapMaximum(profile));
            maxHeadInDx = scale.toPixels(HeadStemRelation.getXInGapMaximum(profile));

            stumpAreaDyHalf = scale.toPixels(constants.stumpAreaDyHalf);
            stumpAreaDxIn = scale.toPixelsDouble(constants.stumpAreaDxIn);
            stumpAreaDxOut = scale.toPixelsDouble(constants.stumpAreaDxOut);

            minBeamHeadDy = scale.toPixels(constants.minBeamHeadDy);
            maxBeamLinkerDx = scale.toPixels(constants.maxBeamLinkerDx);
            halfBeamLuDx = scale.toPixelsDouble(constants.halfBeamLuDx);

            maxBeamSideDx = scale.toPixels(constants.maxBeamSideDx);
            maxBeamGroupDy = scale.toPixels(constants.maxBeamGroupDy);

            maxBeamSeedDx = scale.toPixels(constants.maxBeamSeedDx);
            maxBeamSeedDyRatio = constants.maxBeamSeedDyRatio.getValue();

            minBeamStemsDx = scale.toPixels(constants.minBeamStemsDx);

            minBeamStumpDy = scale.toPixels(constants.minBeamStumpDy);

            mainStemThickness = scale.getStemThickness();
            maxStemThickness = scale.getMaxStem();
            maxLineSeedDx = scale.toPixelsDouble(constants.maxLineSeedDx);
            maxLineSectionDx = scale.toPixelsDouble(constants.maxLineSectionDx);
            maxStemAlignmentDx = scale.toPixelsDouble(constants.maxStemAlignmentDx);
            maxStemAlignmentDy = scale.toPixelsDouble(constants.maxStemAlignmentDy);

            minChunkWeight = scale.getStemThickness(); // Not too stupid...

            minStemTailLg = scale.toPixels(StemInter.minTailLength());
            bestStemTailLg = scale.toPixels(StemInter.bestTailLength());

            minLinkerLength = scale.toPixels(constants.minLinkerLength);

            artificialStemGrade = constants.artificialStemGrade.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
//
//        //--------------------//
//        // typicalBeamStemsDx //
//        //--------------------//
//        /**
//         * Try to compute the typical abscissa gap between notes (head/stem) on same beam.
//         * <p>
//         * It also checks whether a beam does not have a stem relation too close to another.
//         * If so, these too close stems are flagged as mutually exclusive.
//         *
//         * @param beams system population of beams
//         * @return the typical abscissa gap, or null is not reliable enough
//         */
//        private Integer typicalBeamStemsDx ()
//        {
//            final List<Integer> values = new ArrayList<>();
//            Integer medianValue = null;
//            int count = 0; // Number of measure gaps
//
//            for (Iterator<AbstractBeamInter> it = beams.iterator(); it.hasNext();) {
//                final AbstractBeamInter beam = it.next();
//                final boolean beamIsGood = beam.isGood();
//                final List<BeamStemRelation> rels = new ArrayList<>();
//
//                for (Relation rel : sig.edgesOf(beam)) {
//                    if (rel instanceof BeamStemRelation) {
//                        rels.add((BeamStemRelation) rel);
//                    }
//                }
//
//                // Sort on abscissa
//                final int size = rels.size();
//                Collections.sort(rels, (BeamStemRelation o1, BeamStemRelation o2) -> Double.compare(
//                        o1.getExtensionPoint().getX(), o2.getExtensionPoint().getX()));
//
//                for (int i = 0; i < size; i++) {
//                    BeamStemRelation rel = rels.get(i);
//                    StemInter stem = (StemInter) sig.getEdgeTarget(rel);
//
//                    for (BeamStemRelation r : rels.subList(i + 1, size)) {
//                        final int dx = (int) Math.rint(
//                                r.getExtensionPoint().getX() - rel.getExtensionPoint().getX());
//
//                        if (dx < params.minBeamStemsDx) {
//                            // If stems are too close to one another, they are mutually exclusive.
//                            StemInter s = (StemInter) sig.getEdgeTarget(r);
//                            sig.insertExclusion(stem, s, Cause.INCOMPATIBLE);
//                        } else {
//                            if (beamIsGood) {
//                                count++;
//                                values.add(dx);
//                            }
//
//                            break;
//                        }
//                    }
//
//                    if (beamIsGood) {
//                        // Update max stem length, using only good beams
//                        maxStemLength = Math.max(maxStemLength, stem.getMedian().getBounds().height);
//                    }
//                }
//
//                if (!beamIsGood || beam instanceof BeamHookInter) {
//                    it.remove();
//                }
//            }
//
//            if (count > 0) {
//                Collections.sort(values);
//                medianValue = values.get(count / 2);
//                logger.debug("{} Median abscissa gap between beam stems: {}", system, medianValue);
//            }
//
//            return medianValue;
//        }
//
//
//    //-------------------------//
//    // performMutualExclusions //
//    //-------------------------//
//    /**
//     * Browse the system interpretations to insert mutual exclusions wherever possible.
//     * This is done for stems.
//     */
//    private void performMutualExclusions ()
//    {
//        final List<Inter> stems = sig.inters(Shape.STEM);
//        final int size = stems.size();
//        int count = 0;
//
//        try {
//            if (size < 2) {
//                return;
//            }
//
//            Collections.sort(stems, Inters.byAbscissa);
//
//            for (int i = 0; i < (size - 1); i++) {
//                final Inter one = stems.get(i);
//                final Rectangle oneBox = one.getGlyph().getBounds();
//                final int xBreak = oneBox.x + oneBox.width;
//
//                for (Inter two : stems.subList(i + 1, size)) {
//                    Rectangle twoBox = two.getGlyph().getBounds();
//
//                    // Is there an overlap between stems one & two?
//                    if (oneBox.intersects(twoBox)) {
//                        sig.insertExclusion(one, two, Cause.OVERLAP);
//                        count++;
//                    } else if (twoBox.x >= xBreak) {
//                        break;
//                    }
//                }
//            }
//        } finally {
//            logger.debug("S#{} stems: {} exclusions: {}", system.getId(), size, count);
//        }
//    }
