//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              L e d g e r s P o s t A n a l y s i s                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

    /** Population of ordinate deltas with preceding staff line or ledger. */
    private final Population popDelta = new Population();

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
    // process //
    //---------//
    public void process ()
    {
        // Retrieve mean / sigma on ordinate deltas and heights
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

    //---------//
    // collect //
    //---------//
    /**
     * Collect ledger informations for the whole sheet, to measure mean and standard
     * deviation for vertical distance between ledger and previous ledger or staff line.
     */
    private void collect ()
    {
        final int maxWidth = sheet.getScale().toPixels(constants.maxIsolatedLedgerWidth);

        for (Staff staff : sheet.getStaffManager().getStaves()) {
            final int il = staff.getSpecificInterline();
            logger.debug("   staff#{} il:{} {}",
                         staff.getId(),
                         il,
                         staff.isSmall() ? "SMALL" : "");

            final SortedMap<Integer, List<LedgerInter>> map = staff.getLedgerMap();

            for (Map.Entry<Integer, List<LedgerInter>> entry : map.entrySet()) {
                final int key = entry.getKey();
                logger.debug("     {}", key);
                final List<LedgerInter> ledgers = entry.getValue();

                for (LedgerInter ledger : ledgers) {
                    final Line2D median = ledger.getMedian();
                    final Point2D middle = PointUtil.middle(median);
                    final double dist = staff.doubleDistanceTo(middle);
                    final double dRatio = dist / (Math.abs(key) * il);
                    final double hRatio = ledger.getThickness() / il;
                    final double wRatio = (median.getX2() - median.getX1()) / il;
                    final Info info = new Info(ledger, staff, key, dRatio, hRatio, wRatio);
                    infoMap.put(ledger, info);
                    logger.debug("{}", info);

                    popDelta.includeValue(dRatio);
                    popHeight.includeValue(hRatio);

                    if (wRatio <= maxWidth) {
                        popWidth.includeValue(wRatio);
                    }
                }
            }
        }

        logger.debug("deltaRatio: {}", popDelta);
        logger.debug("heightRatio: {}", popHeight);
        logger.debug("widthRatio: {}", popWidth);
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
     * Each value is checked to be in range [mean - 1.5*sigma .. mean + 2*sigma]
     * The sigma coefficients can be customized via their defining Constants.
     */
    private void filter ()
    {
        final double minDelta = popDelta.getMeanValue()
                                        + constants.minDeltaSigmaCoeff.getValue()
                                                  * popDelta.getStandardDeviation();
        final double maxDelta = popDelta.getMeanValue()
                                        + constants.maxDeltaSigmaCoeff.getValue()
                                                  * popDelta.getStandardDeviation();

        final double minHeight = popHeight.getMeanValue()
                                         + constants.minHeightSigmaCoeff.getValue()
                                                   * popHeight.getStandardDeviation();
        final double maxHeight = popHeight.getMeanValue()
                                         + constants.maxHeightSigmaCoeff.getValue()
                                                   * popHeight.getStandardDeviation();

        logger.debug("{}", String.format(
                     "Filter minDelta:%.2f maxDelta:%.2f minHeight:%.2f maxHeight:%.2f",
                     minDelta, maxDelta, minHeight, maxHeight));

        // Infos sorted by ledger ID are much more convenient for user review.
        final List<Info> infos = new ArrayList<>(infoMap.values());
        Collections.sort(infos, Info.byId);

        for (Info info : infos) {
            final String dD = (Math.abs(info.key) == 1)
                    ? (info.deltaRatio < minDelta ? "delta"
                            : (info.deltaRatio > maxDelta ? "DELTA" : "     "))
                    : "     ";
            final String hH = info.heightRatio < minHeight ? "height"
                    : (info.heightRatio > maxHeight ? "HEIGHT" : "      ");
            logger.debug("{} {} {}", dD, hH, info);

            if (!dD.isBlank() || !hH.isBlank()) {
                final SystemInfo system = info.staff.getSystem();
                List<LedgerInter> list = discarded.get(system);

                if (list == null) {
                    discarded.put(system, list = new ArrayList<>());
                }

                list.add(info.ledger);
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
                -1.5,
                "Coeff for standard deviation on minDelta");

        private final Constant.Double maxDeltaSigmaCoeff = new Constant.Double(
                "none",
                2.0,
                "Coeff for standard deviation on maxDelta");

        private final Constant.Double minHeightSigmaCoeff = new Constant.Double(
                "none",
                -1.5,
                "Coeff for standard deviation on minHeight");

        private final Constant.Double maxHeightSigmaCoeff = new Constant.Double(
                "none",
                2.0,
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

        public static Comparator<Info> byId
                = (Info o1, Info o2) -> Integer.compare(o1.ledger.getId(), o2.ledger.getId());

        public final LedgerInter ledger; // The ledger

        public final Staff staff; // Related staff

        public final int key; // Line id with respect to staff

        public final double deltaRatio; // Dy normalized by staff interline

        public final double heightRatio; // Thickness normalized by staff interline

        public final double widthRatio; // Width normalized by staff interline (not for decision)

        public Info (LedgerInter ledger,
                     Staff staff,
                     int key,
                     double deltaRatio,
                     double heightRatio,
                     double widthRatio)
        {
            this.ledger = ledger;
            this.staff = staff;
            this.key = key;
            this.deltaRatio = deltaRatio;
            this.heightRatio = heightRatio;
            this.widthRatio = widthRatio;
        }

        @Override
        public String toString ()
        {
            return String.format("#%d s#%2d(%2d) %2d delta:%.2f height:%.2f width:%.2f",
                                 ledger.getId(), staff.getId(), staff.getSpecificInterline(),
                                 key, deltaRatio, heightRatio, widthRatio);
        }
    }
}
