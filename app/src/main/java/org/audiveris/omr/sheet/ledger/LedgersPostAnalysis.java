//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              L e d g e r s P o s t A n a l y s i s                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet.ledger;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ledger.LedgersStep.Context;
import org.audiveris.omr.sig.inter.LedgerInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class <code>LedgersPostAnalysis</code> runs at sheet level to analyze all raw ledgers
 * created, discard the abnormal ones, and rebuild every staff ledgerMap when needed.
 *
 * @author Hervé Bitteur
 */
public class LedgersPostAnalysis
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LedgersPostAnalysis.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The sheet being processed. */
    private final Sheet sheet;

    /** Map system -> builder. */
    private final Map<SystemInfo, LedgersBuilder> builders;

    /** Population above staff of ordinate deltas with staff line or ledger below. */
    private final Population popDeltaAbove = new Population();

    /** Population below staff of ordinate deltas with staff line or ledger above. */
    private final Population popDeltaBelow = new Population();

    /** Population of heights. */
    private final Population popHeight = new Population();

    /** Population of widths. */
    private final Population popWidth = new Population();

    /** Collected info per ledger. */
    private final Map<LedgerInter, Info> infoMap = new HashMap<>();

    /** Discarded ledgers per system. */
    private final Map<SystemInfo, List<LedgerInter>> discarded = new TreeMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>LedgersPostAnalysis</code>.
     *
     * @param sheet   the sheet being processed
     * @param context processing context
     */
    public LedgersPostAnalysis (Sheet sheet,
                                Context context)
    {
        this.sheet = sheet;
        builders = context.builders;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // collect //
    //---------//
    /**
     * Collect ledger informations for the whole sheet, to measure mean and standard
     * deviation for vertical distance between ledger and previous ledger or staff line.
     */
    private void collect ()
    {
        // Maximum normalized width for an isolated ledger (not merged w/ left or right)
        final double maxWRatio = constants.maxIsolatedLedgerWidth.getValue();

        for (Staff staff : sheet.getStaffManager().getStaves()) {
            final int il = staff.getSpecificInterline();
            logger.debug("   staff#{} il:{} {}", staff.getId(), il, staff.isSmall() ? "SMALL" : "");

            final SortedMap<Integer, List<LedgerInter>> map = staff.getLedgerMap();

            for (Map.Entry<Integer, List<LedgerInter>> entry : map.entrySet()) {
                final int key = entry.getKey();
                final int dir = Integer.signum(key);
                final Integer prevKey = (Math.abs(key) >= 2) ? key - dir : null;
                logger.debug("     {}", key);
                final List<LedgerInter> ledgers = entry.getValue();

                for (LedgerInter ledger : ledgers) {
                    final Line2D median = ledger.getMedian();
                    final Point2D middle = PointUtil.middle(median);

                    final double delta;
                    if (prevKey != null) {
                        final LedgerInter prevLedger = staff.getLedgerAt(prevKey, middle.getX());
                        if (prevLedger == null) {
                            discard(ledger);
                            continue;
                        }

                        delta = Math.abs(
                                middle.getY() - LineUtil.yAtX(
                                        prevLedger.getMedian(),
                                        middle.getX()));
                    } else {
                        delta = staff.doubleDistanceTo(middle);
                    }

                    ///final double delta = staff.doubleDistanceTo(middle) / Math.abs(key);
                    final double dRatio = delta / il;
                    final double height = ledger.getThickness();
                    final double hRatio = height / il;
                    final double width = median.getX2() - median.getX1();
                    final double wRatio = width / il;
                    final Info info = new Info(
                            ledger,
                            staff,
                            key,
                            delta,
                            dRatio,
                            height,
                            hRatio,
                            width,
                            wRatio);
                    infoMap.put(ledger, info);
                    logger.debug("       {}", info);

                    if (key < 0) {
                        popDeltaAbove.includeValue(dRatio);
                    } else {
                        popDeltaBelow.includeValue(dRatio);
                    }
                    popHeight.includeValue(hRatio);

                    // Only isolated ledgers are representative for their width
                    if (wRatio <= maxWRatio) {
                        popWidth.includeValue(wRatio);
                    }
                }
            }
        }

        logger.debug("deltaAboveRatio: {}", popDeltaAbove);
        logger.debug("deltaBelowRatio: {}", popDeltaBelow);
        logger.debug("heightRatio: {}", popHeight);
        logger.debug("widthRatio: {}", popWidth);
    }

    //---------//
    // discard //
    //---------//
    /**
     * Discard the provided ledger candidate.
     *
     * @param LedgerInter ledger
     */
    private void discard (LedgerInter ledger)
    {
        final SystemInfo system = ledger.getStaff().getSystem();
        List<LedgerInter> list = discarded.get(system);

        if (list == null) {
            discarded.put(system, list = new ArrayList<>());
        }

        list.add(ledger);
    }

    //--------//
    // filter //
    //--------//
    /**
     * Filter out the suspicious ledgers.
     * <p>
     * We perform filtering based on:
     * <ul>
     * <li>Delta ordinate (only for ledgers found on lines -1 and +1 with respect to the staff)
     * <li>Ledger height (regardless of line)
     * </ul>
     * Each value is checked to be in typical range [mean - 1*sigma .. mean + 1*sigma]
     * But the sigma coefficients can be customized via their defining Constants.
     */
    private void filter ()
    {
        Double minDeltaAboveRatio = null;
        Double maxDeltaAboveRatio = null;
        Double minDeltaBelowRatio = null;
        Double maxDeltaBelowRatio = null;
        Double minHeightRatio = null;
        Double maxHeightRatio = null;

        if (popDeltaAbove.getCardinality() > 0) {
            minDeltaAboveRatio = popDeltaAbove.getMeanValue() + constants.minDeltaSigmaCoeff
                    .getValue() * popDeltaAbove.getStandardDeviation();
            maxDeltaAboveRatio = popDeltaAbove.getMeanValue() + constants.maxDeltaSigmaCoeff
                    .getValue() * popDeltaAbove.getStandardDeviation();
        }

        if (popDeltaBelow.getCardinality() > 0) {
            minDeltaBelowRatio = popDeltaBelow.getMeanValue() + constants.minDeltaSigmaCoeff
                    .getValue() * popDeltaBelow.getStandardDeviation();
            maxDeltaBelowRatio = popDeltaBelow.getMeanValue() + constants.maxDeltaSigmaCoeff
                    .getValue() * popDeltaBelow.getStandardDeviation();
        }

        if (popHeight.getCardinality() > 0) {
            minHeightRatio = popHeight.getMeanValue() + constants.minHeightSigmaCoeff.getValue()
                    * popHeight.getStandardDeviation();
            maxHeightRatio = popHeight.getMeanValue() + constants.maxHeightSigmaCoeff.getValue()
                    * popHeight.getStandardDeviation();
        }

        logger.debug(
                "{}",
                String.format(
                        "Filter minDeltaAboveRatio:%.2f maxDeltaAboveRatio:%.2f"
                                + " minDeltaBelowRatio:%.2f maxDeltaBelowRatio:%.2f"
                                + " minHeightRatio:%.2f maxHeightRatio:%.2f",
                        minDeltaAboveRatio,
                        maxDeltaAboveRatio,
                        minDeltaBelowRatio,
                        maxDeltaBelowRatio,
                        minHeightRatio,
                        maxHeightRatio));

        // Infos sorted by ledger ID are much more convenient for user review.
        final List<Info> infos = new ArrayList<>(infoMap.values());
        Collections.sort(infos, Info.byId);

        for (Info info : infos) {
            final int interline = info.staff.getSpecificInterline();

            final int minDelta = (int) Math.floor(
                    (info.key < 0 ? minDeltaAboveRatio : minDeltaBelowRatio) * interline);
            final int maxDelta = (int) Math.ceil(
                    (info.key < 0 ? maxDeltaAboveRatio : maxDeltaBelowRatio) * interline);
            final String dD = Math.ceil(info.delta) < minDelta ? "delta"
                    : (Math.floor(info.delta) > maxDelta ? "DELTA" : "     ");

            final int minHeight = (int) Math.floor(minHeightRatio * interline);
            final int maxHeight = (int) Math.ceil(maxHeightRatio * interline);
            final String hH = Math.ceil(info.height) < minHeight ? "height"
                    : (Math.floor(info.height) > maxHeight ? "HEIGHT" : "      ");

            logger.debug(
                    "{} {} {} deltaRange:[{}..{}] heightRange:[{}..{}]",
                    dD,
                    hH,
                    info,
                    minDelta,
                    maxDelta,
                    minHeight,
                    maxHeight);

            if (!dD.isBlank() || !hH.isBlank()) {
                discard(info.ledger);
            }
        }
    }

    //---------//
    // process //
    //---------//
    public void process ()
    {
        // Retrieve mean / sigma on ordinate deltas, heights and widths
        collect();

        // Store filtered out ledgers in discarded map
        filter();

        // Rebuild ledgers for relevant staves in impacted systems
        // Discarded intermediate ledgers will lead to the removal of more external ledgers
        for (Map.Entry<SystemInfo, List<LedgerInter>> entry : discarded.entrySet()) {
            final SystemInfo system = entry.getKey();
            builders.get(system).rebuildLedgers(entry.getValue());
        }

        // Compute ledger lines
        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                staff.buildAllLedgerLines();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction maxIsolatedLedgerWidth = new Scale.Fraction(
                3,
                "Reasonable maximum width for an isolated ledger");

        private final Constant.Double minDeltaSigmaCoeff = new Constant.Double(
                "none",
                -1,
                "Coeff for standard deviation on minDelta");

        private final Constant.Double maxDeltaSigmaCoeff = new Constant.Double(
                "none",
                1,
                "Coeff for standard deviation on maxDelta");

        private final Constant.Double minHeightSigmaCoeff = new Constant.Double(
                "none",
                -1,
                "Coeff for standard deviation on minHeight");

        private final Constant.Double maxHeightSigmaCoeff = new Constant.Double(
                "none",
                1,
                "Coeff for standard deviation on maxHeight");
    }

    //------//
    // Info //
    //------//
    /**
     * Information collected on a ledger.
     */
    private static class Info
    {
        public static Comparator<Info> byId = (Info o1,
                                               Info o2) -> Integer.compare(
                                                       o1.ledger.getId(),
                                                       o2.ledger.getId());

        public final LedgerInter ledger; // The ledger

        public final Staff staff; // Related staff

        public final int key; // Line id with respect to staff

        public final double delta; // Dy

        public final double deltaRatio; // Dy normalized by staff interline

        public final double height; // Thickness

        public final double heightRatio; // Thickness normalized by staff interline

        public final double width; // Width

        public final double widthRatio; // Width normalized by staff interline (not for decision)

        public Info (LedgerInter ledger,
                     Staff staff,
                     int key,
                     double delta,
                     double deltaRatio,
                     double height,
                     double heightRatio,
                     double width,
                     double widthRatio)
        {
            this.ledger = ledger;
            this.staff = staff;
            this.key = key;
            this.delta = delta;
            this.deltaRatio = deltaRatio;
            this.height = height;
            this.heightRatio = heightRatio;
            this.width = width;
            this.widthRatio = widthRatio;
        }

        @Override
        public String toString ()
        {
            return String.format(
                    "#%d s#%2d(%2d) %2d delta:%.2f/%.2f height:%.2f/%.2f width:%.2f/%.2f",
                    ledger.getId(),
                    staff.getId(),
                    staff.getSpecificInterline(),
                    key,
                    delta,
                    deltaRatio,
                    height,
                    heightRatio,
                    width,
                    widthRatio);
        }
    }
}
