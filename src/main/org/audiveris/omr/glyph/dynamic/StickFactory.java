//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t i c k F a c t o r y                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.SectionTally;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.Predicate;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import static java.lang.Boolean.TRUE;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code StickFactory} builds a set of straight filaments (stem seeds / ledgers).
 * <p>
 * As opposed to wavy filaments meant for staff lines, a stem seed or a ledger is rather short,
 * straight and really vertical (stem) or horizontal (ledger).
 * <p>
 * We aim at stem seeds rather than full stems, hence we can focus on sticks made of connected
 * sections, with no vertical gap. Seed merging will take place only later during STEMS step.
 * <p>
 * We know the most frequent value of stem width in the sheet, and also a maximum value.
 * For any given stem, the width must be rather constant all along the stem, hence we can add side
 * sections to a seed only if they are limited and consistent in width and rather significant
 * in their cumulated height along the seed.
 * <p>
 * Selection and aggregation are done with vertical sections only.
 * <p>
 * At the end, isolated stickers can be integrated into the seed, provided they are slim (1 pixel)
 * and not connected to any external section. Stickers can be vertical or horizontal sections,
 * provided their thickness is limited (1 pixel)
 * <p>
 * A similar approach applies for ledgers, except all sections are horizontal. Hence, we don't have
 * opposite stickers for ledgers.
 *
 * @author Hervé Bitteur
 */
public class StickFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StickFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sticks orientation. */
    private final Orientation orientation;

    /** Related system. */
    private final SystemInfo system;

    /** Related sheet. */
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Where filaments are to be stored. */
    private final FilamentIndex index;

    /** Section predicate, if any. */
    private final Predicate<Section> predicate;

    /** Map (position -> opposite sections (1-pixel wide)) of opposite stickers. */
    private Map<Integer, List<Section>> oppStickers;

    /** A specific view (with adjacency links) of main sections. */
    private List<LinkedSection> allSections;

    /** Scale-dependent constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StickFactory} object.
     *
     * @param orientation          sticks orientation (VERTICAL for stems, HORIZONTAL for ledgers)
     * @param system               the system to process
     * @param index                the index to host created filaments
     * @param predicate            optional predicate to check section, or null
     * @param maxStickThickness    maximum expected thickness for a stick
     * @param minCoreSectionLength minimum length for a core section
     * @param minSideRatio         minimum ratio of filament length to enlarge it
     */
    public StickFactory (Orientation orientation,
                         SystemInfo system,
                         FilamentIndex index,
                         Predicate<Section> predicate,
                         int maxStickThickness,
                         int minCoreSectionLength,
                         double minSideRatio)
    {
        this.orientation = orientation;
        this.system = system;
        this.index = index;
        this.predicate = predicate;

        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(maxStickThickness, minCoreSectionLength, minSideRatio);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * At system level, retrieve all candidate sticks (stem seeds / ledgers).
     *
     * @param systemSections   all sections to process (vertical for stem, horizontal for ledgers)
     *                         already ordered by full position.
     * @param oppositeStickers opposite stickers if any (some horizontal sections for stems)
     * @return the collection of seeds built.
     */
    public List<StraightFilament> retrieveSticks (List<Section> systemSections,
                                                  List<Section> oppositeStickers)
    {
        StopWatch watch = new StopWatch("StickFactory S#" + system.getId());

        try {
            // All sections in desired orientation
            watch.start("buildSectionGraph");
            this.allSections = buildSectionGraph(systemSections);

            // Additional external stickers, organized by position
            if (oppositeStickers != null) {
                watch.start("getOppositeStickers");
                oppStickers = getOppositeStickers(oppositeStickers);
            }

            // Pick up core sections (slim and long enough)
            watch.start("getCoreSections");

            List<LinkedSection> coreSections = getCoreSections();

            // Build sticks from the core sections
            watch.start("buildSticks");

            return buildSticks(coreSections);
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-------------//
    // isProcessed //
    //-------------//
    private final boolean isProcessed (LinkedSection section)
    {
        return section.isProcessed();
    }

    //--------------//
    // setProcessed //
    //--------------//
    private final void setProcessed (LinkedSection section)
    {
        section.setProcessed();
    }

    //-------------//
    // addStickers //
    //-------------//
    /**
     * Final processing, to add slim stickers on either side of the filament.
     *
     * @param fil the filament to process
     */
    private void addStickers (Filament fil)
    {
        final Set<Section> members = fil.getMembers();
        final Set<Section> stickers = new LinkedHashSet<Section>();

        for (boolean reverse : new boolean[]{true, false}) {
            for (Section s : members) {
                LinkedSection ls = (LinkedSection) s;

                // Main orientation
                for (LinkedSection linked : getNeighbors(ls, reverse)) {
                    // Must be thin & isolated on the border
                    if ((linked.getCompound() == null)
                        && (linked.getRunCount() == 1)
                        && getNeighbors(linked, reverse).isEmpty()) {
                        stickers.add(linked);
                    }
                }

                // External stickers?
                if (oppStickers != null) {
                    Run endRun = reverse ? s.getFirstRun() : s.getLastRun();
                    int x = reverse ? (s.getFirstPos() - 1) : (s.getLastPos() + 1);
                    Rectangle luArea = new Rectangle(
                            x,
                            endRun.getStart(),
                            1,
                            endRun.getLength());
                    List<Section> col = oppStickers.get(x);

                    if (col != null) {
                        for (Section hs : col) {
                            // Must touch the vertical section
                            // Must not touch another vertical section on other end (TODO: implement!)
                            if (hs.intersects(luArea)) {
                                stickers.add(hs);
                            }
                        }
                    }
                }
            }
        }

        for (Section sticker : stickers) {
            fil.addSection(sticker);

            if (sticker instanceof LinkedSection) {
                LinkedSection linked = (LinkedSection) sticker;
                linked.setCompound(fil);
                setProcessed(linked);
            }
        }
    }

    //-------------------//
    // buildSectionGraph //
    //-------------------//
    private List<LinkedSection> buildSectionGraph (List<Section> sections)
    {
        StopWatch watch = new StopWatch(
                "buildSectionGraph S#" + system.getId() + " size:" + sections.size());
        watch.start("create list");

        List<LinkedSection> list = new ArrayList<LinkedSection>();

        for (Section section : sections) {
            list.add(new LinkedSection(section));
        }

        watch.start("populate starts");

        final int posCount = orientation.isVertical() ? sheet.getWidth()
                : sheet.getHeight();
        final SectionTally<LinkedSection> tally = new SectionTally<LinkedSection>(posCount, list);

        // Detect and record connections
        watch.start("connections");

        for (int i = 0, iBreak = list.size(); i < iBreak; i++) {
            final LinkedSection source = list.get(i);
            final Run predRun = source.getLastRun();
            final int predStart = predRun.getStart();
            final int predStop = predRun.getStop();
            final int nextPos = source.getFirstPos() + source.getRunCount();

            for (LinkedSection target : tally.getSubList(nextPos)) {
                final Run succRun = target.getFirstRun();

                if (succRun.getStart() > predStop) {
                    break; // Since sublist is sorted on coord
                }

                if (succRun.getStop() >= predStart) {
                    // Record connection, both ways
                    source.addTarget(target);
                    target.addSource(source);
                }
            }
        }

        ///watch.print();
        return list;
    }

    //-------------//
    // buildSticks //
    //-------------//
    /**
     * Build sticks out of core sections.
     *
     * @param cores collection of core sections
     * @return the list of sticks built
     */
    private List<StraightFilament> buildSticks (List<LinkedSection> cores)
    {
        final int interline = scale.getInterline();
        final List<StraightFilament> fils = new ArrayList<StraightFilament>();

        for (LinkedSection core : cores) {
            if (core.isVip()) {
                logger.info("VIP buildSticks on core {}");
            }

            if (isProcessed(core)) {
                continue;
            }

            StraightFilament fil = new StraightFilament(interline);
            fil.addSection(core);
            core.setCompound(fil);
            setProcessed(core);
            fils.add(fil);
            index.register(fil);

            // Grow this filament as much as possible
            growFilament(fil);

            // Finally, aggregate isolated stickers (from main and from external sections if any)
            addStickers(fil);
        }

        return fils;
    }

    //-----------------//
    // getCoreSections //
    //-----------------//
    /**
     * Filter all sections to come out with core sections, suitable to grow sticks from.
     *
     * @return the core sections, sorted by decreasing length
     */
    private List<LinkedSection> getCoreSections ()
    {
        List<LinkedSection> candidates = new ArrayList<LinkedSection>();

        // Discard too thick or too short sections
        for (LinkedSection ls : allSections) {
            if ((ls.getRunCount() <= params.maxStickThickness)
                && (ls.getLength(orientation) >= params.minCoreSectionLength)) {
                if ((predicate == null) || predicate.check(ls)) {
                    candidates.add(ls);
                }
            }
        }

        // Sort candidates by decreasing length
        Collections.sort(
                candidates,
                new Comparator<Section>()
        {
            @Override
            public int compare (Section ls1,
                                Section ls2)
            {
                return Integer.compare(
                        ls2.getLength(orientation),
                        ls1.getLength(orientation));
            }
        });

        return candidates;
    }

    //--------------//
    // getNeighbors //
    //--------------//
    private List<LinkedSection> getNeighbors (LinkedSection section,
                                              boolean reverse)
    {
        //        return reverse ? Graphs.predecessorListOf(graph, section)
        //                : Graphs.successorListOf(graph, section);
        return reverse ? section.getSources() : section.getTargets();
    }

    //---------------------//
    // getOppositeStickers //
    //---------------------//
    /**
     * Organize the provided opposite sections (they are 1 pixel thick) per their
     * related position.
     *
     * @param externalStickers the collection of slim opposite sections
     * @return the map: coordinate -> sections for this coordinate value
     */
    private Map<Integer, List<Section>> getOppositeStickers (List<Section> externalStickers)
    {
        Map<Integer, List<Section>> map = new TreeMap<Integer, List<Section>>();
        Collections.sort(externalStickers, Section.byCoordinate);

        int iStart = -1;
        int coordStart = -1;

        for (int i = 0, iBreak = externalStickers.size(); i < iBreak; i++) {
            final Section section = externalStickers.get(i);
            final int coord = section.getStartCoord();

            if (coord > coordStart) {
                // Finish previous coord value
                if (coordStart != -1) {
                    map.put(coordStart, externalStickers.subList(iStart, i));
                }

                // Start new coord
                iStart = i;
                coordStart = coord;
            }
        }

        return map;
    }

    //-----------------//
    // getSideSections //
    //-----------------//
    /**
     * Report the sections that currently end the filament on the provided side
     *
     * @param fil     filament to process
     * @param reverse TRUE for source, FALSE for target
     * @return the set of side sections
     */
    private Set<LinkedSection> getSideSections (Filament fil,
                                                boolean reverse)
    {
        Set<LinkedSection> sideSections = new LinkedHashSet<LinkedSection>();

        // Look for members with no included section yet on desired side
        // TODO: this may be too restrictive?
        MemberLoop:
        for (Section s : fil.getMembers()) {
            LinkedSection ls = (LinkedSection) s;
            Collection<LinkedSection> neighbors = getNeighbors(ls, reverse);

            for (LinkedSection neighbor : neighbors) {
                if (neighbor.getCompound() == fil) {
                    continue MemberLoop; // Not on border!
                }
            }

            sideSections.add(ls);
        }

        return sideSections;
    }

    //--------------//
    // growFilament //
    //--------------//
    /**
     * Incrementally grow the provided filament, initially composed of just one section,
     * with suitable sections on both sides.
     *
     * @param fil the filament to grow
     */
    private void growFilament (Filament fil)
    {
        boolean grown;

        // Map: reverse -> finished
        Map<Boolean, Boolean> finished = new HashMap<Boolean, Boolean>();

        if (fil.isVip()) {
            index.publish(fil);
            logger.info("VIP growFilament with {}", fil);
        }

        do {
            grown = false;

            // Look on source side, then on target side
            SideLoop:
            for (boolean reverse : new boolean[]{true, false}) {
                if (TRUE.equals(finished.get(reverse))) {
                    continue;
                }

                final int filMeanThickness = (int) Math.rint(
                        fil.getMeanThickness(orientation));

                // Determine the section(s) on this side of filament
                Set<LinkedSection> sideSections = getSideSections(fil, reverse);
                Map<Section, Integer> contribs = new HashMap<Section, Integer>();

                // Look for possible extensions on this side
                final TreeSet<LinkedSection> allNeighbors = new TreeSet<LinkedSection>(
                        Section.byCoordinate);
                int total = 0;
                int count = 0;

                for (LinkedSection sideSection : sideSections) {
                    Run sideRun = reverse ? sideSection.getFirstRun()
                            : sideSection.getLastRun();
                    List<LinkedSection> neighbors = getNeighbors(sideSection, reverse);

                    for (Iterator<LinkedSection> it = neighbors.iterator(); it.hasNext();) {
                        final LinkedSection neighbor = it.next();

                        if (isProcessed(neighbor)) {
                            it.remove();
                        } else {
                            final int thickness = neighbor.getRunCount();

                            if (((thickness + filMeanThickness) > params.maxStickThickness)
                                || ((predicate != null) && !predicate.check(neighbor))) {
                                it.remove();
                            } else {
                                int length = neighbor.getLength(orientation);
                                count += length;
                                total += (thickness * length);

                                Integer contrib = contribs.get(neighbor);
                                int common = sideRun.getCommonLength(
                                        reverse ? neighbor.getLastRun() : neighbor.getFirstRun());
                                contribs.put(
                                        neighbor,
                                        (contrib != null) ? (contrib + common) : common);
                            }
                        }
                    }

                    allNeighbors.addAll(neighbors);
                }

                // Check thickness consistency
                int commonLength = 0;

                if (count != 0) {
                    final int mean = (int) Math.rint((double) total / count);

                    for (Iterator<LinkedSection> it = allNeighbors.iterator(); it.hasNext();) {
                        final Section link = it.next();

                        if (link.getRunCount() > mean) {
                            logger.debug("Discarding non-consistent {}", link);
                            it.remove(); // Too thick compared with siblings
                        } else {
                            commonLength += contribs.get(link);
                        }
                    }
                }

                // Check quorum
                double ratio = (double) commonLength / fil.getLength(orientation);

                if (ratio >= params.minSideRatio) {
                    // Do enlarge filament on this side
                    for (LinkedSection linked : allNeighbors) {
                        fil.addSection(linked);
                        setProcessed(linked);
                    }

                    grown = true;
                } else {
                    finished.put(reverse, Boolean.TRUE); // We can't go any further on this side
                }
            }
        } while (grown
                 && (Math.rint(fil.getMeanThickness(orientation)) < params.maxStickThickness));
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
                "Should we print out the stop watch for StickFactory?");
    }

    //------------//
    // Parameters //
    //------------//
    private class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final int maxStickThickness;

        public final int minCoreSectionLength;

        public final double minSideRatio;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (int maxStickThickness,
                           int minCoreSectionLength,
                           double minSideRatio)
        {
            this.maxStickThickness = maxStickThickness;
            this.minCoreSectionLength = minCoreSectionLength;
            this.minSideRatio = minSideRatio;
        }
    }
}
