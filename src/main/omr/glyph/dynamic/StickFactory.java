//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t i c k F a c t o r y                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.LinkedSection;
import omr.lag.Section;
import static omr.run.Orientation.VERTICAL;
import omr.run.Run;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code StickFactory} builds straight filaments to be considered as stem seeds.
 * <p>
 * As opposed to wavy filaments meant for staff lines, a stem seed is rather short, straight and
 * vertical.
 * <p>
 * We aim at stem seeds rather than full stems, hence we can focus on sticks made of connected
 * sections, with no vertical gap. Seed merging will take place only later during STEMS step.
 * <p>
 * We know the most frequent value of stem width in the sheet, and also a maximum value.
 * For any given stem, the width must be rather constant all along the stem, hence we can add side
 * sections to a seed only if they are limited and consistent in width and rather significant
 * in their cumulated height along the seed.
 * <p>
 * We need an efficient way to access the connected sections on one side (left or right) of any
 * vertical section. This is not needed for horizontal sections, since they are used only during the
 * ending sticker phase.
 * <p>
 * Selection and aggregation are done with vertical sections only.
 * <p>
 * At the end, isolated stickers can be integrated into the seed, provided they are slim (1 pixel)
 * and not connected to any external section. Stickers can be vertical or horizontal sections,
 * provided their horizontal width is limited (1 pixel)
 *
 * @author Hervé Bitteur
 */
public class StickFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StickFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related system. */
    private final SystemInfo system;

    /** Related sheet. */
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Where filaments are to be stored. */
    private final FilamentIndex index;

    /** Map (abscissa -> horizontal sections (1-pixel wide)) of potential stickers. */
    private Map<Integer, List<Section>> hMap;

    private List<LinkedSection> allVerticals;

    private final int maxStickWidth;

    /** Scale-dependent constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StickFactory} object.
     *
     * @param system        the system to process
     * @param index         the index to host created filaments
     * @param maxStickWidth maximum expected width for a stem
     */
    public StickFactory (SystemInfo system,
                         FilamentIndex index,
                         int maxStickWidth)
    {
        this.system = system;
        this.index = index;
        this.maxStickWidth = maxStickWidth;

        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * At system level, retrieve all sticks that could be considered as stem seeds.
     *
     * @param vSections the (vertical) sections to process
     * @param hSections the horizontal sections as possible stickers
     * @return the collection of seeds built.
     */
    public List<StraightFilament> retrieveSticks (List<Section> vSections,
                                                  List<Section> hSections)
    {
        // Store potential horizontal stickers, by abscissa
        hMap = getHorizontalSections(hSections);

        // We need all vertical sections with adjacency links between them
        allVerticals = buildLinkedSections(vSections);

        // Pick up good candidates (slim and long enough)
        List<LinkedSection> goods = filterCandidates();

        // Build sticks from the good candidates
        return buildSticks(goods);
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
        final Set<Section> stickers = new HashSet<Section>();

        for (HorizontalSide side : HorizontalSide.values()) {
            for (Section s : members) {
                LinkedSection ls = (LinkedSection) s;

                // Verticals
                for (LinkedSection linked : ls.getLinkedOn(side)) {
                    // Must be external, thin & isolated
                    if (!members.contains(linked)
                        && (linked.getRunCount() == 1)
                        && linked.getLinkedOn(side).isEmpty()) {
                        stickers.add(linked);
                    }
                }

                // Horizontals
                Run endRun = (side == LEFT) ? ls.getFirstRun() : ls.getLastRun();
                int x = (side == LEFT) ? (ls.getFirstPos() - 1) : (ls.getLastPos() + 1);
                Rectangle luArea = new Rectangle(x, endRun.getStart(), 1, endRun.getLength());
                List<Section> col = hMap.get(x);

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

        for (Section sticker : stickers) {
            fil.addSection(sticker, false);
            sticker.setProcessed(true);
        }
    }

    //---------------------//
    // buildLinkedSections //
    //---------------------//
    /**
     * Encapsulate each vertical section in a LinkedSection with links to adjacent
     * vertical sources and targets.
     *
     * @param sections the (vertical) sections to process
     */
    private List<LinkedSection> buildLinkedSections (List<Section> sections)
    {
        List<LinkedSection> list = new ArrayList<LinkedSection>();

        for (Section section : sections) {
            section.setProcessed(false);
            list.add(new LinkedSection(section));
        }

        Collections.sort(
                list,
                new Comparator<LinkedSection>()
        {
            @Override
            public int compare (LinkedSection ls1,
                                LinkedSection ls2)
            {
                return Section.byFullAbscissa.compare(ls1, ls2);
            }
        });

        // Register sections by their starting pos
        // 'starts' is a vector parallel to sheet abscissae (+1 additional cell)
        // Vector at index x gives the index in 'list' of the first section starting at 'x' (or -1).
        // And similarly at index 'x+1', either -1 (for no section) or index for column 'x+1'.
        // Hence, all sections starting  at 'x' are in [starts[x]..starts[x+1][ sublist
        int[] starts = new int[sheet.getWidth() + 1];
        Arrays.fill(starts, 0, starts.length, -1);

        int currentPos = -1;

        for (int i = 0, iBreak = list.size(); i < iBreak; i++) {
            final LinkedSection left = list.get(i);
            final int pos = left.getFirstPos();

            if (pos > currentPos) {
                starts[pos] = i;
                currentPos = pos;
            }
        }

        starts[starts.length - 1] = list.size(); // End mark

        // Detect and record connections
        for (int i = 0, iBreak = list.size(); i < iBreak; i++) {
            final LinkedSection left = list.get(i);
            final int nextPos = left.getFirstPos() + left.getRunCount();

            final int iStart = starts[nextPos];

            if (iStart == -1) {
                continue; // No section at all starting at this pos value
            }

            int iNextStart = iBreak;

            for (int j = nextPos + 1; j < starts.length; j++) {
                iNextStart = starts[j];

                if (iNextStart != -1) {
                    break;
                }
            }

            final Run lastRun = left.getLastRun();

            for (LinkedSection right : list.subList(iStart, iNextStart)) {
                Run rightRun = right.getFirstRun();

                if (lastRun.getCommonLength(rightRun) > 0) {
                    // Record connection, both ways
                    left.addTarget(right);
                    right.addSource(left);
                }
            }
        }

        return list;
    }

    //-------------//
    // buildSticks //
    //-------------//
    /**
     * Build sticks out of good sections.
     *
     * @param goods collection of good sections
     * @return the list of sticks built
     */
    private List<StraightFilament> buildSticks (List<LinkedSection> goods)
    {
        final int interline = scale.getInterline();
        final List<StraightFilament> fils = new ArrayList<StraightFilament>();

        for (LinkedSection good : goods) {
            if (good.isProcessed()) {
                continue;
            }

            StraightFilament fil = new StraightFilament(interline);
            fil.addSection(good, true);
            good.setProcessed(true);
            fils.add(fil);
            index.register(fil);

            // Grow this filament as much as possible
            growFilament(fil);

            // Finally, aggregate isolated stickers (from vertical and from horizontal sections)
            addStickers(fil);
        }

        return fils;
    }

    //------------------//
    // filterCandidates //
    //------------------//
    /**
     * Filter all the vertical sections to come out with only good candidates for seeds.
     *
     * @return the good sections, sorted by decreasing height
     */
    private List<LinkedSection> filterCandidates ()
    {
        List<LinkedSection> candidates = new ArrayList<LinkedSection>();

        // Discard too wide or too short sections
        for (LinkedSection ls : allVerticals) {
            if ((ls.getRunCount() <= maxStickWidth)
                && (ls.getLength(VERTICAL) >= params.minCoreSectionLength)) {
                candidates.add(ls);
            }
        }

        // Sort candidates by decreasing vertical length
        Collections.sort(
                candidates,
                new Comparator<LinkedSection>()
        {
            @Override
            public int compare (LinkedSection ls1,
                                LinkedSection ls2)
            {
                return Integer.compare(ls2.getLength(VERTICAL), ls1.getLength(VERTICAL));
            }
        });

        return candidates;
    }

    //----------------//
    // getendSections //
    //----------------//
    /**
     * Report the sections that currently end the filament on the provided side
     *
     * @param fil  filament to process
     * @param side provide side
     * @return the set of side sections
     */
    private Set<LinkedSection> getEndSections (Filament fil,
                                               HorizontalSide side)
    {
        Set<LinkedSection> sideSections = new LinkedHashSet<LinkedSection>();
        Set<Section> members = fil.getMembers();

        // Look for members with no included section yet on desired side
        MemberLoop:
        for (Section s : members) {
            LinkedSection ls = (LinkedSection) s;
            Set<LinkedSection> links = ls.getLinkedOn(side);

            for (LinkedSection linked : links) {
                if (members.contains(linked)) {
                    continue MemberLoop; // Not on border!
                }
            }

            sideSections.add(ls);
        }

        return sideSections;
    }

    //-----------------------//
    // getHorizontalSections //
    //-----------------------//
    /**
     * Organize the provided horizontal sections (they are 1 pixel wide) per their
     * related abscissa.
     *
     * @param hSections the collection of slim horizontal sections
     * @return the map abscissa -> sections for this abscissa
     */
    private Map<Integer, List<Section>> getHorizontalSections (List<Section> hSections)
    {
        Map<Integer, List<Section>> map = new TreeMap<Integer, List<Section>>();
        Collections.sort(hSections, Section.byFullAbscissa);

        int iStart = -1;
        int xStart = -1;

        for (int i = 0, iBreak = hSections.size(); i < iBreak; i++) {
            final Section section = hSections.get(i);
            final int x = section.getBounds().x;

            if (x > xStart) {
                // Finish previous column
                if (xStart != -1) {
                    map.put(xStart, hSections.subList(iStart, i));
                }

                // Start new column
                iStart = i;
                xStart = x;
            }
        }

        return map;
    }

    //--------------//
    // growFilament //
    //--------------//
    /**
     * Incrementally grow the provided filament, initially composed of just one section,
     * with suitable vertical sections on both sides.
     *
     * @param fil the filament to grow
     */
    private void growFilament (Filament fil)
    {
        boolean grown;
        EnumMap<HorizontalSide, Boolean> finished = new EnumMap<HorizontalSide, Boolean>(
                HorizontalSide.class);

        if (fil.isVip()) {
            index.publish(fil);
            logger.info("VIP growFilament with {}", fil);
        }

        do {
            grown = false;

            for (HorizontalSide side : HorizontalSide.values()) {
                if (Boolean.TRUE.equals(finished.get(side))) {
                    continue;
                }

                final int filMeanWidth = (int) Math.rint(
                        fil.getMeanThickness(VERTICAL));

                // Determine the section(s) on this side of filament
                Set<LinkedSection> endSections = getEndSections(fil, side);
                Map<LinkedSection, Integer> contribs = new HashMap<LinkedSection, Integer>();

                // Look for possible extensions on this side
                final Set<LinkedSection> links = new LinkedHashSet<LinkedSection>();
                int total = 0;
                int count = 0;

                for (LinkedSection sideSection : endSections) {
                    Run sideRun = (side == LEFT) ? sideSection.getFirstRun()
                            : sideSection.getLastRun();
                    Set<LinkedSection> sideLinks = sideSection.getLinkedOn(side);

                    for (Iterator<LinkedSection> it = sideLinks.iterator(); it.hasNext();) {
                        final LinkedSection linked = it.next();
                        linked.setProcessed(true);

                        final int width = linked.getRunCount();

                        if ((width + filMeanWidth) > maxStickWidth) {
                            it.remove();
                        } else {
                            int height = linked.getLength(VERTICAL);
                            count += height;
                            total += (width * height);

                            Integer contrib = contribs.get(linked);
                            int common = sideRun.getCommonLength(
                                    (side == LEFT) ? linked.getLastRun() : linked.getFirstRun());
                            contribs.put(linked, (contrib != null) ? (contrib + common) : common);
                        }
                    }

                    links.addAll(sideLinks);
                }

                // Check consistency in thickness
                int commonHeight = 0;

                if (count != 0) {
                    final int mean = (int) Math.rint((double) total / count);

                    for (Iterator<LinkedSection> it = links.iterator(); it.hasNext();) {
                        final LinkedSection link = it.next();

                        if (link.getRunCount() > mean) {
                            logger.debug("Discarding non-consistent {}", link);
                            it.remove(); // Too thick compared with siblings
                        } else {
                            commonHeight += contribs.get(link);
                        }
                    }
                }

                // Check quorum
                double ratio = (double) commonHeight / fil.getLength(VERTICAL);

                if (ratio >= params.minSideRatio) {
                    // Do expand filament on this side
                    for (LinkedSection linked : links) {
                        fil.addSection(linked, false);
                        linked.setProcessed(true);
                    }

                    grown = true;
                } else {
                    finished.put(side, Boolean.TRUE); // We can't go any further on this side
                }
            }
        } while (grown && (Math.rint(fil.getMeanThickness(VERTICAL)) < maxStickWidth));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio minSideRatio = new Constant.Ratio(
                0.4,
                "Minimum ratio of filament length to be actually extended horizontally");

        private final Scale.Fraction minCoreSectionLength = new Scale.Fraction(
                1.5,
                "Minimum length for core sections");
    }

    //------------//
    // Parameters //
    //------------//
    private class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final int minCoreSectionLength;

        public final double minSideRatio;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            minSideRatio = constants.minSideRatio.getValue();
            minCoreSectionLength = scale.toPixels(constants.minCoreSectionLength);
        }
    }
}
