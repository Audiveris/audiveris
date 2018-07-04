//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C l u m p P r u n e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class {@code ClumpPruner} prunes a clump of competing slurs, and keep the best one
 * by evaluating the connections to embraced chords.
 * <p>
 * It works at sheet level as a companion of {@link SlursBuilder}.
 *
 * @author Hervé Bitteur
 */
public class ClumpPruner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ClumpLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    @Navigable(false)
    private final Sheet sheet;

    /** The engine which selects the best link pair for a given slur. */
    private final SlurLinker slurLinker;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SlursLinker object.
     *
     * @param sheet the underlying sheet
     */
    public ClumpPruner (Sheet sheet)
    {
        this.sheet = sheet;

        slurLinker = new SlurLinker(sheet);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // prune //
    //-------//
    /**
     * Process a clump of slur candidates and select the candidate with best average
     * "distance" to embraced head-based chords on left and right sides.
     *
     * @param clump a bunch of competing slurs
     * @return the best slur, if any
     */
    public SlurInter prune (Set<Inter> clump)
    {
        // Compute lookup areas for each slur in clump
        // If we cannot compute areas for a slur, we simply discard the slur
        Map<Inter, Map<HorizontalSide, Area>> areas = new LinkedHashMap<Inter, Map<HorizontalSide, Area>>();

        for (Iterator<Inter> it = clump.iterator(); it.hasNext();) {
            SlurInter slur = (SlurInter) it.next();

            try {
                areas.put(slur, slurLinker.defineAreaPair(slur));
            } catch (Exception ignored) {
                logger.debug("No lookup area for {} at {}", slur, slur.getBounds());
                it.remove();
            }
        }

        if (clump.isEmpty()) {
            return null;
        }

        // Determine the relevant system(s) and stop at first system with good result.
        Map<HorizontalSide, Rectangle> bounds = getBounds(clump, areas);
        SystemManager mgr = sheet.getSystemManager();
        List<SystemInfo> systems = mgr.getSystemsOf(bounds.get(LEFT), null);
        systems.retainAll(mgr.getSystemsOf(bounds.get(RIGHT), null));

        SystemLoop:
        for (SystemInfo system : systems) {
            final List<Inter> sysChords = system.getSig().inters(HeadChordInter.class);
            final ClumpLinker linker = new ClumpLinker(system, clump, bounds, sysChords);

            // Select the slur with best chord links, if any
            SlurEntry selected = linker.selectSlur(areas);

            if (selected != null) {
                // Either linked head may have a mirror head, so select proper head for tie.
                // NOTA: It is a bit early to check for a tie, since for instance clef changes or
                // head alterations are not yet available, but ties are here set for the sake of
                // tie collision which will trigger chord splitting.
                // Ties will be (re-)checked again at end of LINKS step.
                SlurHeadLink leftLink = selected.links.get(LEFT);
                HeadInter leftHead = (leftLink != null) ? (HeadInter) leftLink.partner : null;

                SlurHeadLink rightLink = selected.links.get(RIGHT);
                HeadInter rightHead = (rightLink != null) ? (HeadInter) rightLink.partner : null;

                if ((leftHead != null) && (rightHead != null)) {
                    final HeadInter leftMirror = (HeadInter) leftHead.getMirror();
                    final HeadInter rightMirror = (HeadInter) rightHead.getMirror();

                    if ((leftHead.getIntegerPitch() == rightHead.getIntegerPitch())
                        && (leftHead.getStaff() == rightHead.getStaff())) {
                        final SlurInter slur = selected.slur;

                        // Check there is no other chords in between
                        if (slur.isSpaceClear(leftHead, rightHead, sysChords)) {
                            slur.setTie(true);
                        } else if (slur.isSpaceClear(leftMirror, rightHead, sysChords)) {
                            slur.setTie(true);
                            switchMirrorHead(selected, LEFT);
                        } else if (slur.isSpaceClear(leftHead, rightMirror, sysChords)) {
                            slur.setTie(true);
                            switchMirrorHead(selected, RIGHT);
                        } else if (slur.isSpaceClear(leftMirror, rightMirror, sysChords)) {
                            slur.setTie(true);
                            switchMirrorHead(selected, LEFT);
                            switchMirrorHead(selected, RIGHT);
                        }
                    }
                }

                // Put everything into sig
                linker.doLink(selected);

                return selected.slur;
            }
        }

        return null; // No acceptable candidate found
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the rectangular bounds of the clump of slurs.
     *
     * @param clump the aggregated slurs
     * @return the global bounds
     */
    private Map<HorizontalSide, Rectangle> getBounds (Set<Inter> clump,
                                                      Map<Inter, Map<HorizontalSide, Area>> areas)
    {
        Map<HorizontalSide, Rectangle> bounds = new EnumMap<HorizontalSide, Rectangle>(
                HorizontalSide.class);

        for (HorizontalSide side : HorizontalSide.values()) {
            // Take union of areas for this side
            Rectangle box = null;

            for (Inter inter : clump) {
                SlurInter slur = (SlurInter) inter;
                Rectangle b = areas.get(slur).get(side).getBounds();

                if (box == null) {
                    box = b;
                } else {
                    box.add(b);
                }
            }

            bounds.put(side, box);
        }

        return bounds;
    }

    /**
     * On the provided slur side, replace the linked head by its mirror head.
     *
     * @param entry the SlurEntry to modify
     * @param side  the side to selectSlur
     */
    private void switchMirrorHead (SlurEntry entry,
                                   HorizontalSide side)
    {
        SlurHeadLink link = entry.links.get(side);
        Inter head = link.partner;
        Inter mirror = head.getMirror();
        Objects.requireNonNull(mirror, "switchMirrorHead needs a non-null mirror"); // Safer
        link.partner = mirror;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // SlurEntry //
    //-----------//
    /**
     * Class {@code SlurEntry} handles link data for a slur.
     */
    private static class SlurEntry
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static Comparator<SlurEntry> byWeightedDist = new Comparator<SlurEntry>()
        {
            @Override
            public int compare (SlurEntry s1,
                                SlurEntry s2)
            {
                return Double.compare(s1.weightedDist(), s2.weightedDist());
            }
        };

        //~ Instance fields ------------------------------------------------------------------------
        /** The slur concerned. */
        public final SlurInter slur;

        /** The two best head links (left and right). */
        public final Map<HorizontalSide, SlurHeadLink> links;

        //~ Constructors ---------------------------------------------------------------------------
        public SlurEntry (SlurInter slur,
                          Map<HorizontalSide, SlurHeadLink> links)
        {
            this.slur = slur;
            this.links = links;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Compute the mean euclidian-distance of this entry
         *
         * @return means euclidian distance
         */
        public double meanEuclidianDist ()
        {
            double dist = 0;
            int n = 0;

            for (HorizontalSide side : HorizontalSide.values()) {
                SlurHeadLink link = links.get(side);

                if (link != null) {
                    dist += ((SlurHeadRelation) link.relation).getEuclidean();
                    n++;
                }
            }

            return dist / n;
        }

        /**
         * Temper raw euclidean distance with the slur length.
         *
         * @return a more valuable measurement of link quality
         */
        public double weightedDist ()
        {
            final double dist = meanEuclidianDist();
            final int nbPoints = slur.getInfo().getPoints().size();

            return dist / nbPoints;
        }
    }

    //-------------//
    // ClumpLinker //
    //-------------//
    /**
     * Handles the links to chords for a whole clump of slurs.
     */
    private class ClumpLinker
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        private final SIGraph sig;

        private final Set<Inter> clump;

        /** All head-chords in the system. */
        private final List<Inter> sysChords;

        /** Head-chords found around clump. */
        private final Map<HorizontalSide, List<Inter>> chords = new EnumMap<HorizontalSide, List<Inter>>(
                HorizontalSide.class);

        //~ Constructors ---------------------------------------------------------------------------
        public ClumpLinker (SystemInfo system,
                            Set<Inter> clump,
                            Map<HorizontalSide, Rectangle> bounds,
                            List<Inter> sysChords)
        {
            this.system = system;
            this.clump = clump;
            this.sysChords = sysChords;
            sig = system.getSig();

            // Pre-select chords candidates according to clump side
            filterChords(bounds);
        }

        //~ Methods --------------------------------------------------------------------------------
        //--------//
        // doLink //
        //--------//
        /**
         * Insert relations between slur and linked heads.
         *
         * @param entry chosen slur entry
         */
        public void doLink (SlurEntry entry)
        {
            final SlurInter slur = entry.slur;

            if (!sig.containsVertex(slur)) {
                sig.addVertex(slur);
            }

            for (SlurHeadLink link : entry.links.values()) {
                if (link != null) {
                    HeadInter head = (HeadInter) link.partner;
                    sig.addEdge(slur, head, link.relation);
                }
            }
        }

        //------------//
        // selectSlur //
        //------------//
        /**
         * Select the best slur in clump (within current system).
         *
         * @param the lookup areas for each candidate
         * @return the entry for best slur and its best links
         */
        public SlurEntry selectSlur (Map<Inter, Map<HorizontalSide, Area>> areas)
        {
            // Determine the pair of best links for every slur candidate
            List<SlurEntry> entries = new ArrayList<SlurEntry>();

            for (Inter inter : clump) {
                SlurInter slur = (SlurInter) inter;

                // Determine the pair of best links (left & right) for this slur candidate
                Map<HorizontalSide, SlurHeadLink> linkPair = slurLinker.lookupLinkPair(
                        slur,
                        areas.get(slur),
                        system,
                        chords);

                if (linkPair != null) {
                    entries.add(new SlurEntry(slur, linkPair));
                }
            }

            // Make a selection among clump slurs
            // Choose (among the longest ones) the slur with best links
            // Accept orphan only if quorum slurs agree
            // Retrieve non-orphans
            List<SlurEntry> nonOrphans = getNonOrphans(entries);
            SlurEntry bestEntry = selectAmong(nonOrphans);

            if (bestEntry != null) {
                return bestEntry;
            }

            entries.removeAll(nonOrphans);

            return selectAmong(entries);
        }

        //--------------//
        // filterChords //
        //--------------//
        /**
         * Filter the chords that could be relevant for clump.
         */
        private void filterChords (Map<HorizontalSide, Rectangle> bounds)
        {
            for (HorizontalSide side : HorizontalSide.values()) {
                Rectangle box = bounds.get(side);

                // Filter via box intersection
                chords.put(side, Inters.intersectedInters(sysChords, null, box));
            }
        }

        //---------------//
        // getNonOrphans //
        //---------------//
        /**
         * Select the slurs that are not orphan.
         *
         * @param map the connection map to browse
         * @return the slurs which have links on both sides
         */
        private List<SlurEntry> getNonOrphans (List<SlurEntry> entries)
        {
            List<SlurEntry> nonOrphans = new ArrayList<SlurEntry>();
            EntryLoop:
            for (SlurEntry entry : entries) {
                for (HorizontalSide side : HorizontalSide.values()) {
                    if (entry.links.get(side) == null) {
                        continue EntryLoop;
                    }
                }

                nonOrphans.add(entry);
            }

            return nonOrphans;
        }

        //-------------//
        // selectAmong //
        //-------------//
        /**
         * Make a selection among the competing slurs.
         * None or all of those slurs are orphans (and on the same side).
         * <p>
         * First, sort the slurs by increasing mean X-distance to their heads.
         * Second, among the first ones that share the same heads, select the one with shortest
         * mean Euclidean-distance.
         * (TODO: this 2nd point is very questionable, we could use slur grade as well).
         *
         * @param entries the slur entries to select from
         * @return the best selected
         */
        private SlurEntry selectAmong (List<SlurEntry> entries)
        {
            if ((entries == null) || entries.isEmpty()) {
                return null;
            } else if (entries.size() == 1) {
                return entries.get(0);
            }

            // Sort by mean euclidian distance
            // TODO: this may be too rigid!
            Collections.sort(entries, SlurEntry.byWeightedDist);

            // Now, select between slurs with same embraced heads, if any
            SlurEntry bestEntry = null;
            double bestDist = Double.MAX_VALUE;
            SlurHeadLink bestLeft = null;
            SlurHeadLink bestRight = null;

            for (SlurEntry entry : entries) {
                if (bestEntry == null) {
                    bestEntry = entry;
                    bestDist = entry.weightedDist();
                    logger.debug("   {} euclide:{}", entry, bestDist);
                } else {
                    // Check whether embraced chords are still the same
                    SlurHeadLink left = entry.links.get(LEFT);

                    if ((bestLeft != null)
                        && (left != null)
                        && (left.getChord() != bestLeft.getChord())) {
                        break;
                    }

                    SlurHeadLink right = entry.links.get(RIGHT);

                    if ((bestRight != null)
                        && (right != null)
                        && (right.getChord() != bestRight.getChord())) {
                        break;
                    }

                    // We do have the same embraced chords as slurs above, so use Euclidian distance
                    double dist = entry.weightedDist();
                    logger.debug("   {} euclide:{}", entry, dist);

                    if (dist < bestDist) {
                        bestDist = dist;
                        bestEntry = entry;
                    }
                }
            }

            return bestEntry;
        }
    }
}
