//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t e m B u i l d e r                                     //
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

import java.awt.Point;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.dynamic.StickFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.glyph.dynamic.SectionCompound;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.*;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker.VLinker;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker.CLinker;
import org.audiveris.omr.sheet.stem.StemItem.GapItem;
import org.audiveris.omr.sheet.stem.StemItem.GlyphItem;
import org.audiveris.omr.sheet.stem.StemItem.HalfLinkerItem;
import org.audiveris.omr.sheet.stem.StemItem.LinkerItem;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class <code>StemBuilder</code> handles the building of one stem, by holding the sequence of
 * {@link StemItem} instances (glyphs, linkers, gaps).
 * <p>
 * It is used from {@link BeamLinker} when linking a beam via stem to heads, and from
 * {@link HeadLinker} when linking a head to stem and perhaps other heads or beams.
 *
 * @author Hervé Bitteur
 */
public class StemBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StemBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing system. */
    private final SystemInfo system;

    /** System-level stem retriever. */
    private final StemsRetriever retriever;

    /** The theoretical line to follow, oriented from refPt to targetPt. */
    private final Line2D theoLine;

    /** Vertical direction from start to stop. */
    private final int yDir;

    /** Initial linker. */
    private final StemHalfLinker startLinker;

    /** Look-up area for stem items. */
    private final Area luArea;

    /** Vertical range for relevant items. (x and width members are irrelevant) */
    private final Rectangle yRange;

    /** Ordinate of last head when going away from a beam. */
    private Double lastHeadY;

    /** Set of all glyphs. */
    private final Set<Glyph> allGlyphs = new LinkedHashSet<>();

    /** Scale-dependent parameters. */
    private final StemsRetriever.Parameters params;

    /** Sequence of detected stem items. */
    private List<StemItem> items = new ArrayList<>();

    /** Reachable lengths, per profile. */
    private final TreeMap<Integer, Integer> lengthMap = new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a <code>StemBuilder</code> object.
     *
     * @param builder        the system-level StemsRetriever
     * @param startLinker    starting linker (cannot be null)
     * @param seeds          seeds available, perhaps containing startGlyph and stopGlyph
     * @param linkers        collection of reachable linkers, perhaps empty
     * @param maxStemProfile maximum possible profile level for stem
     */
    public StemBuilder (StemsRetriever builder,
                        StemHalfLinker startLinker,
                        Collection<Glyph> seeds,
                        List<? extends StemLinker> linkers,
                        int maxStemProfile)
    {
        this.retriever = builder;
        this.startLinker = startLinker;

        system = builder.getSystem();
        theoLine = startLinker.getTheoreticalLine();
        luArea = startLinker.getLookupArea();

        yDir = (theoLine.getY2() > theoLine.getY1()) ? 1 : -1;
        yRange = theoLine.getBounds();

        params = retriever.getParams();

        retrieveAllItems(seeds, linkers, maxStemProfile);
        retrieveLengths(maxStemProfile);

        logger.debug("{} lengths:{}", startLinker, lengthMap);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createStem //
    //------------//
    /**
     * (Try to) create stem interpretation with proper grade.
     *
     * @param glyphs  the collection of glyphs (seeds / chunks) that compose the stem
     * @param profile desired profile level
     * @return the proper stem interpretation or null if failed
     */
    public StemInter createStem (Collection<Glyph> glyphs,
                                 int profile)
    {
        Glyph stemGlyph = (glyphs.size() == 1)
                ? glyphs.iterator().next() : GlyphFactory.buildGlyph(glyphs);
        stemGlyph = system.getSheet().getGlyphIndex().registerOriginal(stemGlyph);

        if (stemGlyph.isVip()) {
            logger.info("VIP createStem? {}", stemGlyph);
        }

        final StemInter stemInter = retriever.getStemInter(stemGlyph);

        if (stemInter != null) {
            return stemInter;
        }

        final GradeImpacts impacts = retriever.getStemChecker().checkStem(stemGlyph, profile);
        final double grade = impacts.getGrade();

        if (grade >= StemInter.getMinGrade()) {
            StemInter stem = new StemInter(stemGlyph, impacts);
            retriever.addStemInter(stem);
            return stem;
        }

        if (profile == Profiles.BEAM_SIDE) {
            // There may be nearly no pixel for the stem, so let's create an artificial one
            StemInter stem = new StemInter(stemGlyph, params.artificialStemGrade);
            retriever.addStemInter(stem);
            return stem;
        }

        return null;
    }

    //-----//
    // get //
    //-----//
    /**
     * Report item at index value.
     *
     * @param index provided index value
     * @return corresponding item
     */
    public StemItem get (int index)
    {
        return items.get(index);
    }

    //-------------//
    // getCLinkers //
    //-------------//
    /**
     * Report the ordered sequence of head corner linkers.
     *
     * @param maxIndex maximum item index or null
     * @return the list of head linkers, perhaps empty
     */
    public List<CLinker> getCLinkers (Integer maxIndex)
    {
        final List<CLinker> found = new ArrayList<>();
        final int iMax = (maxIndex != null) ? maxIndex : items.size() - 1;

        for (int i = 0; i <= iMax; i++) {
            final StemItem ev = items.get(i);

            if (ev instanceof LinkerItem) {
                final StemLinker linker = ((LinkerItem) ev).linker;

                if (linker instanceof CLinker) {
                    found.add((CLinker) linker);
                }
            }
        }

        return found;
    }

    //----------------------//
    // getFirstCLinkerAfter //
    //----------------------//
    /**
     * Report the first occurrence of a CLinker in items after provided index.
     *
     * @param index the provided index
     * @return the first head CLinker found or null
     */
    public CLinker getFirstCLinkerAfter (int index)
    {
        for (int i = index + 1; i < items.size(); i++) {
            final StemItem ev = items.get(i);

            if ((ev instanceof StemItem.LinkerItem)
                        && (((StemItem.LinkerItem) ev).linker instanceof CLinker)) {
                return (CLinker) ((StemItem.LinkerItem) ev).linker;
            }
        }

        return null;
    }

    //-----------------//
    // getSourceLinker //
    //-----------------//
    /**
     * Report the source linker if any.
     *
     * @return the source linker or null
     */
    public StemLinker getSourceLinker ()
    {
        for (int i = 0; i < items.size(); i++) {
            final StemItem ev = items.get(i);

            if ((i <= 1) && (ev instanceof LinkerItem)) {
                return ((LinkerItem) ev).linker;
            }
        }

        return null;
    }

    //------------------//
    // getTargetLinkers //
    //------------------//
    /**
     * Report the ordered sequence of target linkers (all linkers past the source one).
     *
     * @return the list of target linkers, perhaps empty
     */
    public List<StemLinker> getTargetLinkers ()
    {
        final List<StemLinker> targets = new ArrayList<>();
        final StemLinker source = getSourceLinker();

        if (source == null) {
            return targets;
        }

        for (StemItem ev : items) {
            if (ev instanceof LinkerItem) {
                final StemLinker linker = ((LinkerItem) ev).linker;

                if (linker != source) {
                    targets.add(linker);
                }
            }
        }

        return targets;
    }

    //----------------------//
    // headHasConcreteStart //
    //----------------------//
    /**
     * Report whether this CLinker has a concrete start with significant length
     * before first gap if any.
     * <p>
     * This tells if there is really a stem departing directly from the head in this corner.
     *
     * @param profile desired profile level
     * @return true if a concrete start exists
     */
    public boolean headHasConcreteStart (int profile)
    {
        final int lg0 = getLength(profile);
        return lg0 >= params.minLinkerLength;
    }

    //---------------//
    // headHasLength //
    //---------------//
    /**
     * Check if, under the provided profile to filter gaps, there is enough length to
     * try building a stem.
     *
     * @param profile provided profile level
     * @return true if OK
     */
    public boolean headHasLength (int profile)
    {
        return getLength(profile) >= params.minLinkerLength;
    }

    //------------------//
    // getLastGapBefore //
    //------------------//
    /**
     * Report the last gap if any found before the provided index.
     *
     * @param index upper bound
     * @return the gap found or null
     */
    public GapItem getLastGapBefore (int index)
    {
        for (int i = index - 1; i >= 0; i--) {
            final StemItem ev = items.get(i);

            if (ev instanceof GapItem) {
                return (GapItem) ev;
            }
        }

        return null;
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the possible stem length, before hitting a too large gap.
     *
     * @param profile desired profile level for max gap value
     * @return possible stem length
     */
    public int getLength (int profile)
    {
        return lengthMap.get(profile);
    }

    //----------------//
    // getTotalLength //
    //----------------//
    /**
     * Report the total possible stem length, skipping any gap.
     *
     * @return total length of all items
     */
    public int getTotalLength ()
    {
        return lengthMap.get(Profiles.MAX_VALUE);
    }

    //----------------//
    // getGlyphsUntil //
    //----------------//
    /**
     * Report the sequence of glyphs until the provided event index.
     *
     * @param lastIndex index for sequence end
     * @return the sequence of glyphs until lastIndex
     */
    public List<Glyph> getGlyphsUntil (int lastIndex)
    {
        final Set<Glyph> glyphs = new LinkedHashSet<>();

        for (int i = 0; i <= lastIndex; i++) {
            final StemItem ev = items.get(i);

            if (ev.glyph != null) {
                glyphs.add(ev.glyph);
            }
        }

        return new ArrayList<>(glyphs);
    }

    //-------------//
    // getLengthAt //
    //-------------//
    public int getLengthAt (int lastIndex)
    {
        Rectangle rect = null;

        for (int i = 0; i <= lastIndex; i++) {
            final StemItem ev = items.get(i);

            if ((ev != null) && !(ev instanceof GapItem) && (ev.line != null)) {
                if (rect == null) {
                    rect = ev.line.getBounds();
                } else {
                    rect.add(ev.line.getBounds());
                }

                if (ev instanceof LinkerItem
                            && ((LinkerItem) ev).linker instanceof CLinker) {
                    final HeadInter head = (HeadInter) ((LinkerItem) ev).linker.getSource();
                    rect.add(head.getBounds());
                }
            }
        }

        if (rect == null) {
            return 0;
        }

        if (yDir > 0) {
            return rect.y + rect.height - (int) theoLine.getY1();
        } else {
            return (int) theoLine.getY1() - rect.y;
        }
    }

    //-------------//
    // getLinkerOf //
    //-------------//
    /**
     * Report the linker that corresponds to the provided (beam or head) inter.
     *
     * @param inter the inter at hand
     * @return the corresponding linker found in items
     */
    public StemLinker getLinkerOf (Inter inter)
    {
        for (StemItem ev : items) {
            if (ev instanceof LinkerItem) {
                final StemLinker sl = ((LinkerItem) ev).linker;

                if (sl.getSource() == inter) {
                    return sl;
                }
            }
        }

        return null;
    }

    //---------//
    // indexOf //
    //---------//
    /**
     * Report index of provided item in items sequence (or -1 if not found).
     *
     * @param item provided item
     * @return index in items sequence
     */
    public int indexOf (StemItem item)
    {
        return items.indexOf(item);
    }

    //---------//
    // indexOf //
    //---------//
    /**
     * Report index of provided linker in items sequence (or -1 if not found).
     *
     * @param linker provided linker
     * @return index in items sequence
     */
    public int indexOf (StemLinker linker)
    {
        for (int i = 0; i < items.size(); i++) {
            final StemItem ev = items.get(i);

            if ((ev instanceof LinkerItem) && (((LinkerItem) ev).linker == linker)) {
                return i;
            }
        }

        return -1;
    }

    //----------//
    // maxIndex //
    //----------//
    /**
     * Report max index value in items sequence.
     *
     * @return max index
     */
    public int maxIndex ()
    {
        return items.size() - 1;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder("sb{").append(getTotalLength()).append(' ').append(items)
                .append('}').toString();
    }

    //--------//
    // filter //
    //--------//
    /**
     * Include filtered stumps and seeds.
     * <p>
     * Make sure that seeds are compatible (rather aligned with startGlyph).
     * If not, discard seed related linker if any.
     * <p>
     * if two glyphs overlap, keep the one closer abscissa-wise to theoretical line
     *
     * @param startBox      bounds of starting item if any
     * @param seeds         (input/output) collection of seeds found in the lookup area
     * @param targetLinkers (input/output) collection of target linkers found in the lookup area
     * @return the resulting stem items
     */
    private List<StemItem> filter (Rectangle startBox,
                                   Collection<Glyph> seeds,
                                   List<? extends StemLinker> targetLinkers)
    {
        // First, filter non-aligned seeds
        final List<Glyph> removed = filterUnaligned(seeds);

        // Second keep only linkers not related to discarded seeds
        final List<StemItem> list = new ArrayList<>();

        for (Iterator<? extends StemLinker> it = targetLinkers.iterator(); it.hasNext();) {
            final StemLinker linker = it.next();

            if (removed.contains(linker.getStump())) {
                it.remove();
                continue;
            }

            if (linker instanceof StemHalfLinker) {
                final int contrib = (linker.getStump() != null)
                        ? getContrib(linker.getStump().getBounds()) : 0;
                list.add(new HalfLinkerItem((StemHalfLinker) linker, contrib));
            } else {
                list.add(new LinkerItem(linker));
            }
        }

        // Sort linkers
        sortItems(list);
        lastHeadY = getLastHeadY(list);

        if (lastHeadY != null) {
            // Discard seeds located past last head
            for (Glyph seed : seeds) {
                final Point center = GeoUtil.center(seed.getBounds());
                if (yDir * Double.compare(center.getY(), lastHeadY) >= 0) {
                    removed.add(seed);
                }
            }

            seeds.removeAll(removed);
        }

        NextSeed:
        for (Glyph seed : seeds) {
            // Discard seeds that duplicate linkers
            for (StemLinker linker : targetLinkers) {
                if (seed == linker.getStump()) {
                    continue NextSeed;
                }
            }

            final Rectangle seedBox = seed.getBounds();

            // Discard seeds that overlap start glyph
            if ((startBox != null) && GeoUtil.yOverlap(startBox, seedBox) > 0) {
                continue;
            }

            // Keep only seeds that provide actual ordinate contribution
            final int contrib = getContrib(seedBox);

            if (contrib > 0) {
                list.add(new GlyphItem(seed, contrib));
                allGlyphs.add(seed);
            }
        }

        for (StemItem se : list) {
            if (se.glyph != null) {
                allGlyphs.add(se.glyph);
            }
        }

        return list;
    }

    //-----------------//
    // filterUnaligned //
    //-----------------//
    /**
     * Filter out the non-aligned glyphs.
     *
     * @param glyphs the collection to be purged
     * @return the removed glyphs
     */
    private List<Glyph> filterUnaligned (Collection<Glyph> glyphs)
    {
        final List<Glyph> removed = new ArrayList<>();
        final List<Glyph> gList = new ArrayList<>(glyphs);
        Collections.sort(gList, yDir > 0 ? Glyphs.byOrdinate : Glyphs.byReverseBottom);

        // Prominent role assigned to start stump if any
        final Glyph stump = startLinker.getStump();
        if (stump != null) {
            gList.remove(stump);
            gList.add(0, stump);
        }

        for (int i = 0; i < gList.size() - 1; i++) {
            final Glyph s1 = gList.get(i);
            final Glyph s2 = gList.get(i + 1);

            if (!areAligned(s1, s2)) {
                // We remove the shorter glyph
                final Glyph alien = s1.getBounds().height < s2.getBounds().height ? s1 : s2;
                removed.add(alien);
                gList.remove(alien);
                i--;
            }
        }

        glyphs.removeAll(removed);

        return removed;
    }

    //------------//
    // areAligned //
    //------------//
    /**
     * Check whether the two provided glyphs are rather aligned vertically, taking the
     * global sheet slope into account.
     *
     * @param g1 one glyph
     * @param g2 other glyph
     * @return true if OK
     */
    private boolean areAligned (Glyph g1,
                                Glyph g2)
    {
        // Use deskewed centroids
        final Skew skew = system.getSkew();
        final Point2D dsk1 = skew.deskewed(g1.getCentroidDouble());
        final Point2D dsk2 = skew.deskewed(g2.getCentroidDouble());

        // If the two glyphs are too distant in ordinate, the abscissa check is not reliable at all
        final double dy = Math.abs(dsk2.getY() - dsk1.getY());
        if (dy > params.maxStemAlignmentDy) {
            return true;
        }

        final double dx = Math.abs(dsk2.getX() - dsk1.getX());

        return dx <= params.maxStemAlignmentDx;
    }

    //------------//
    // getContrib //
    //------------//
    /**
     * Report the (vertical) contribution of a rectangle to the filling of white
     * space above or below the head.
     *
     * @param box the rectangle to check
     * @return the corresponding height within white space
     */
    private int getContrib (Rectangle box)
    {
        return Math.max(0, GeoUtil.yOverlap(yRange, box));
    }

    //--------------//
    // getLastHeadY //
    //--------------//
    /**
     * When going away from a beam, we don't consider seeds or sections past the last
     * target head reference point.
     *
     * @param items sorted list of items
     * @return last head ordinate or null
     */
    private Double getLastHeadY (List<StemItem> items)
    {
        if (startLinker instanceof VLinker) {
            for (int i = items.size() - 1; i >= 0; i--) {
                final StemItem item = items.get(i);

                if (item instanceof LinkerItem && ((LinkerItem) item).linker instanceof CLinker) {
                    final CLinker lastCl = (CLinker) ((LinkerItem) item).linker;
                    return lastCl.getReferencePoint().getY();
                }
            }
        }

        return null;
    }

    //-----------------//
    // insertGapEvents //
    //-----------------//
    /**
     * Insert vertical gaps between events.
     * <p>
     * A too large gap cuts the event list.
     *
     * @param stemProfile maximum stem profile to be ever envisioned (results in a maximum gap)
     */
    private void insertGapEvents (int stemProfile)
    {
        final Scale scale = system.getSheet().getScale();
        final int maxYGap = scale.toPixels(StemChecker.getMaxYGap(stemProfile));

        // Current ordinate reached
        Point2D lastPt = null;

        for (int i = 0; i < items.size(); i++) {
            final StemItem se = items.get(i);
            final Point2D start = (yDir > 0) ? se.line.getP1() : se.line.getP2();
            final Point2D stop = (yDir > 0) ? se.line.getP2() : se.line.getP1();

            if (lastPt == null) {
                lastPt = stop;
            } else {
                // Check gap if any
                final double gap = yDir * (start.getY() - lastPt.getY());

                if (gap > maxYGap) {
                    // All following items are unreachable
                    items = items.subList(0, i);
                    return;
                }

                if (gap > 0.01) {
                    // Insert a gap within the item sequence
                    items.add(i++, new GapItem((yDir > 0) ? new Line2D.Double(lastPt, start)
                              : new Line2D.Double(start, lastPt)));
                }
            }

            if (yDir * (stop.getY() - lastPt.getY()) > 0.01) {
                lastPt = stop;
            }
        }
    }

    //--------------//
    // lookupChunks //
    //--------------//
    /**
     * Retrieve chunks of stems from additional compatible sections (not part
     * of stumps or seeds found in 'allGlyphs' collection) found in the area.
     * <p>
     * We have to make sure that these chunks are compatible with the existing stumps and seeds.
     *
     * @param seeds the seeds kept so far
     * @return the ordered list of chunks found
     */
    private List<Glyph> lookupChunks (Collection<Glyph> seeds)
    {
        // Look up suitable sections
        final List<Section> vSections = lookupVerticalSections();
        final List<Section> hSections = lookupHorizontalSections();

        final StickFactory factory = new StickFactory(
                Orientation.VERTICAL,
                system,
                system.getSheet().getFilamentIndex(),
                null,
                params.maxStemThickness,
                0, // Because vertical sections are already long enough
                VerticalsBuilder.getMinSideRatio().getValue());

        final List<StraightFilament> fils = factory.retrieveSticks(vSections, hSections);
        final List<Glyph> chunks = new ArrayList<>();
        final GlyphIndex index = system.getSheet().getGlyphIndex();

        for (StraightFilament fil : fils) {
            chunks.add(index.registerOriginal(fil.toGlyph(null)));
        }

        chunks.removeAll(seeds);
        filterUnaligned(chunks);

        final Glyph stump = startLinker.getStump();
        if (stump != null) {
            chunks.remove(stump);
        }

        Collections.sort(chunks, yDir > 0 ? Glyphs.byOrdinate : Glyphs.byReverseBottom);

        return chunks;
    }

    //--------------------------//
    // lookupHorizontalSections //
    //--------------------------//
    /**
     * Retrieve candidate horizontal sections.
     *
     * @return the collection of additional horizontal sections found
     */
    private List<Section> lookupHorizontalSections ()
    {
        final List<Section> sections = new ArrayList<>();

        SectionLoop:
        for (Section section : system.getHorizontalSections()) {
            final Rectangle sectBox = section.getBounds();

            if (section.isVip() && startLinker.getSource().isVip()) {
                logger.info("VIP {}", section);
            }

            // Check intersection with lookup area
            if (!luArea.intersects(sectBox)) {
                continue;
            }

            // Contraint section width
            if (sectBox.width > 1) {
                continue;
            }

            final Point2D center = section.getCentroid2D();

            if (lastHeadY != null) {
                // Within ordinate range?
                if (yDir * Double.compare(center.getY(), lastHeadY) >= 0) {
                    continue;
                }
            }

            sections.add(section);
        }

        Collections.sort(sections, Section.byFullPosition);

        return sections;
    }

    //------------------------//
    // lookupVerticalSections //
    //------------------------//
    /**
     * To complement stem seeds, look up for relevant vertical sections in the lookup area
     * that could be part of a global stem.
     *
     * @return the collection of additional sections found
     */
    private List<Section> lookupVerticalSections ()
    {
        final List<Section> sections = new ArrayList<>();
        final Glyph stump = startLinker.getStump();
        final Rectangle stumpBox = (stump != null) ? stump.getBounds() : null;

        // Consider only vertical sections
        SectionLoop:
        for (Section section : system.getVerticalSections()) {
            final Rectangle sectBox = section.getBounds();

            if (section.isVip()) {
                logger.info("VIP {}", section);
            }

            // Check intersection with lookup area
            if (!luArea.intersects(sectBox)) {
                continue;
            }

            // Contraint section width
            if (sectBox.width > params.maxStemThickness) {
                continue;
            }

            // TODO: do this at chunk level rather than section level?
            if (stumpBox != null && GeoUtil.yOverlap(sectBox, stumpBox) > 0) {
                if (sectBox.height < stumpBox.height) {
                    continue;
                }
            }

            final Point2D center = section.getCentroid2D();

            if (lastHeadY != null) {
                // Within ordinate range?
                if (yDir * Double.compare(center.getY(), lastHeadY) >= 0) {
                    continue;
                }
            }

            // Check section distance to theoretical line
            final double dist = theoLine.ptLineDist(center);

            if (dist > params.maxLineSectionDx) {
                continue;
            }

            sections.add(section);
        }

        Collections.sort(sections, Section.byFullPosition);

        return sections;
    }

    //------------------//
    // retrieveAllItems //
    //------------------//
    /**
     * Retrieve the ordered sequence of candidate items to build a stem.
     * <p>
     * All relevant items along the theoLine are included.
     * Decision about shortening the item sequence will be made later.
     *
     * @param seeds          the collection of seeds, perhaps empty
     * @param targetLinkers  the collection of target linkers, perhaps empty
     * @param maxStemProfile the maximum possible level for stem profile
     */
    private void retrieveAllItems (Collection<Glyph> seeds,
                                   List<? extends StemLinker> targetLinkers,
                                   int maxStemProfile)
    {
        if (startLinker.getSource().isVip()) {
            logger.info("VIP {} retrieveAllItems stemProfile:{}", startLinker, maxStemProfile);
        }

        // Initial linker
        final Glyph startGlyph = startLinker.getStump();
        Rectangle startBox = null;

        if (startGlyph != null) {
            allGlyphs.add(startGlyph);
            startBox = startGlyph.getBounds();
        }

        items.add(new HalfLinkerItem(startLinker,
                                     (startGlyph != null) ? getContrib(startBox) : 0));

        // Include filtered seeds and stumps
        items.addAll(filter(startBox, seeds, targetLinkers));

        // Look for additional chunks built out of vertical sections found.
        final List<Glyph> chunks = lookupChunks(seeds);

        for (Glyph chunk : chunks) {
            allGlyphs.add(chunk); // Just for the sake of completion
            items.add(new GlyphItem(chunk, getContrib(chunk.getBounds())));
        }

        sortItems(items.subList(1, items.size()));

        insertGapEvents(maxStemProfile);

        logger.debug("{}", this);
    }

    //-----------------//
    // retrieveLengths //
    //-----------------//
    /**
     * Retrieve reachable lengths per profile.
     */
    private void retrieveLengths (int maxStemProfile)
    {
        if (startLinker.getSource().isVip()) {
            logger.info("VIP {} retrieveLengths maxStemProfile:{}", startLinker, maxStemProfile);
        }

        final TreeMap<Integer, Integer> gapMap = retriever.getGapMap();
        final int maxGap = gapMap.get(maxStemProfile);
        final int maxIndex = items.size() - 1;

        for (int i = 0; i <= maxIndex; i++) {
            final StemItem ev = items.get(i);

            if (ev instanceof GapItem) {
                for (Entry<Integer, Integer> entry : gapMap.entrySet()) {
                    if (ev.contrib > entry.getValue()) {
                        final int prof = entry.getKey();

                        if (lengthMap.get(prof) == null) {
                            lengthMap.put(prof, getLengthAt(i - 1));
                        }
                    } else {
                        break;
                    }
                }

                if (ev.contrib > maxGap) {
                    if (lengthMap.get(maxStemProfile) == null) {
                        lengthMap.put(maxStemProfile, getLengthAt(i - 1));
                    }

                    return;
                }
            }
        }

        for (Entry<Integer, Integer> entry : gapMap.entrySet()) {
            final int prof = entry.getKey();

            if (lengthMap.get(prof) == null) {
                lengthMap.put(prof, getLengthAt(maxIndex));
            }
        }
    }

    //-----------//
    // sortItems //
    //-----------//
    /**
     * Sort the list items according to yDir.
     *
     * @param list items to sort
     */
    private void sortItems (List<? extends StemItem> list)
    {
        Collections.sort(list, (se1, se2) -> {
                     if (se1.glyph != null && se1.glyph == se2.glyph) {
                         // Linkers based on same glyph cannot be sorted on glyph bounds
                         // but on their refPt ordinate
                         if ((se1 instanceof LinkerItem) && (se2 instanceof LinkerItem)) {
                             return yDir * Double.compare(
                                     ((LinkerItem) se1).linker.getReferencePoint().getY(),
                                     ((LinkerItem) se2).linker.getReferencePoint().getY());
                         }
                     }

                     return (yDir > 0)
                             ? Double.compare(se1.line.getY1(), se2.line.getY1())
                             : Double.compare(se2.line.getY2(), se1.line.getY2());
                 });
    }

    //---------------//
    // trimWideChunk //
    //---------------//
    /**
     * Shrink a too-wide section compound, by incrementally removing side sections most
     * distant from theoretical line.
     *
     * @param wide the compound to shrink
     * @return the shrunk compound, or null if the last section left is still too wide
     */
    private SectionCompound trimWideChunk (SectionCompound wide)
    {
        final List<Section> members = new ArrayList<>(wide.getMembers());

        // Sort by decreasing distance to theoretical line
        Collections.sort(members, (s1, s2) -> Double.compare(
                theoLine.ptLineDistSq(s2.getCentroid2D()),
                theoLine.ptLineDistSq(s1.getCentroid2D())));

        for (Section section : members) {
            wide.removeSection(section);
            final int newWidth = (int) Math.rint(wide.getMeanThickness(VERTICAL));

            if (newWidth <= params.maxStemThickness) {
                return wide;
            }
        }

        return null;
    }
}
