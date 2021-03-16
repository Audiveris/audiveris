//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L a g M a n a g e r                                      //
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
package org.audiveris.omr.lag;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.*;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.IntUtil;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Class {@code LagManager} keeps a catalog of Lag instances for a given sheet.
 *
 * @author Hervé Bitteur
 */
public class LagManager
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LagManager.class);

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Map of all public lags. */
    private final Map<String, Lag> lagMap = new TreeMap<>();

    /** Id of last long horizontal section. */
    private int lastLongHSectionId;

    /** (Debug)Predefined IDs for VIP sections. */
    private final EnumMap<Orientation, List<Integer>> vipMap;

    /**
     * Creates a new {@code LagManager} object.
     *
     * @param sheet the related sheet
     */
    public LagManager (Sheet sheet)
    {
        this.sheet = sheet;

        vipMap = getVipSections();
    }

    //--------------------//
    // buildHorizontalLag //
    //--------------------//
    /**
     * Build the underlying horizontal lag from the provided runs table.
     *
     * @param horiTable the provided table of all (horizontal) runs
     * @param hLag      the horizontal lag to populate, existing or created if null
     * @return the created hLag
     */
    public Lag buildHorizontalLag (RunTable horiTable,
                                   Lag hLag)
    {
        if (horiTable == null) {
            return null;
        }

        Lag lag = (hLag != null) ? hLag : new BasicLag(Lags.HLAG, HORIZONTAL);
        SectionFactory factory = new SectionFactory(lag, JunctionRatioPolicy.DEFAULT);
        factory.createSections(horiTable, null, true);
        setLag(Lags.HLAG, lag);
        setVipSections(HORIZONTAL);

        return lag;
    }

    //------------------//
    // buildVerticalLag //
    //------------------//
    /**
     * Build the underlying vertical lag, from the provided runs table.
     * This method must be called before building info.
     *
     * @param vertTable the provided table of (long) vertical runs
     * @return the created vLag
     */
    public Lag buildVerticalLag (RunTable vertTable)
    {
        final Scale scale = sheet.getScale();

        if (scale == null) {
            return null;
        }

        final Lag vLag = new BasicLag(Lags.VLAG, VERTICAL);
        final int maxVerticalRunShift = scale.toPixels(constants.maxVerticalRunShift);
        SectionFactory factory = new SectionFactory(
                vLag,
                new JunctionShiftPolicy(maxVerticalRunShift));
        factory.createSections(vertTable, null, true);
        setLag(Lags.VLAG, vLag);
        setVipSections(VERTICAL);

        return vLag;
    }

    //------------//
    // filterRuns //
    //------------//
    /**
     * Filter the source table into vertical table and horizontal table.
     *
     * @param sourceTable the source table (BINARY or NO_STAF)
     * @param vertTable   (output) populated by long vertical runs, can be null
     * @return the horizontal table built from no-long vertical runs
     */
    public RunTable filterRuns (RunTable sourceTable,
                                RunTable vertTable)
    {
        if (sheet.getScale() == null) {
            return null;
        }

        final int minVerticalRunLength = 1 + (int) Math.rint(
                sheet.getScale().getMaxFore() * constants.ledgerThickness.getValue());

        // Remove runs whose height is larger than line thickness
        RunTable shortVertTable = sourceTable.copy().purge(new Predicate<Run>()
        {
            @Override
            public final boolean test (Run run)
            {
                return run.getLength() >= minVerticalRunLength;
            }
        }, vertTable);
        RunTableFactory runFactory = new RunTableFactory(HORIZONTAL);
        RunTable horiTable = runFactory.createTable(shortVertTable.getBuffer());

        return horiTable;
    }

    //------------//
    // getAllLags //
    //------------//
    /**
     * Report all currently registered lags at this sheet instance.
     *
     * @return the collection of all registered lags, some of which may be null
     */
    public Collection<Lag> getAllLags ()
    {
        return lagMap.values();
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the desired lag.
     *
     * @param key the lag name
     * @return the lag if already registered, null otherwise
     */
    public Lag getLag (String key)
    {
        Lag lag = lagMap.get(key);

        if (lag == null) {
            switch (key) {
            case Lags.HLAG:
                rebuildBothLags();

                return lagMap.get(key);

            case Lags.VLAG:
                rebuildBothLags();

                return lagMap.get(key);

            default:
                break;
            }
        }

        return lag;
    }

    //---------------------//
    // getLongSectionMaxId //
    //---------------------//
    /**
     * Report the id of the last long horizontal section
     *
     * @return the id of the last long horizontal section
     */
    public int getLongSectionMaxId ()
    {
        return lastLongHSectionId;
    }

    //---------------------//
    // setLongSectionMaxId //
    //---------------------//
    /**
     * Remember the id of the last long horizontal section
     *
     * @param id the id of the last long horizontal section
     */
    public void setLongSectionMaxId (int id)
    {
        lastLongHSectionId = id;
    }

    //-------------//
    // rebuildHLag //
    //-------------//
    /**
     * Rebuild hLag from NO_STAFF table.
     */
    public void rebuildHLag ()
    {
        // Build tables
        RunTable sourceTable = sheet.getPicture().buildNoStaffTable();

        if (sourceTable == null) {
            return;
        }

        RunTable horiTable = filterRuns(sourceTable, null);

        // Repopulate hLag
        Lag hLag = lagMap.get(Lags.HLAG);
        hLag.reset();
        buildHorizontalLag(horiTable, hLag);
    }

    //--------//
    // setLag //
    //--------//
    /**
     * Register the provided lag.
     *
     * @param key the registered key for the lag
     * @param lag the lag to register, perhaps null
     */
    public void setLag (String key,
                        Lag lag)
    {
        lagMap.put(key, lag);

        if ((lag != null) && (OMR.gui != null) && (lag.getEntityService() == null)) {
            lag.setEntityService(new SectionService(lag, sheet.getLocationService()));
        }
    }

    //----------------//
    // setVipSections //
    //----------------//
    /**
     * Assign VIP flag to sections based on map of sections IDs.
     *
     * @param orientation section orientation to be processed
     */
    public void setVipSections (Orientation orientation)
    {
        List<Integer> ids = vipMap.get(orientation);
        Lag lag = lagMap.get(orientation.isVertical() ? Lags.VLAG : Lags.HLAG);

        // Debug sections VIPs
        for (int id : ids) {
            Section sect = lag.getEntity(id);

            if (sect != null) {
                sect.setVip(true);
                logger.info("{} vip section: {}", orientation, sect);
            }
        }
    }

    //----------------//
    // getVipSections //
    //----------------//
    private EnumMap<Orientation, List<Integer>> getVipSections ()
    {
        EnumMap<Orientation, List<Integer>> map = new EnumMap<>(Orientation.class);

        for (Orientation orientation : Orientation.values()) {
            String vipStr = orientation.isVertical() ? constants.verticalVipSections.getValue()
                    : constants.horizontalVipSections.getValue();
            List<Integer> ids = IntUtil.parseInts(vipStr);

            if (!ids.isEmpty()) {
                logger.info("{} VIP sections: {}", orientation, ids);
            }

            map.put(orientation, ids);
        }

        return map;
    }

    //-----------------//
    // rebuildBothLags //
    //-----------------//
    /**
     * Rebuild both hLag and vLag directly from NO_STAFF table.
     */
    private void rebuildBothLags ()
    {
        // Build tables
        RunTable sourceTable = sheet.getPicture().buildNoStaffTable();

        if (sourceTable == null) {
            return;
        }

        RunTable vertTable = new RunTable(VERTICAL, sheet.getWidth(), sheet.getHeight());
        RunTable horiTable = filterRuns(sourceTable, vertTable);

        // Build lags
        buildHorizontalLag(horiTable, null);
        buildVerticalLag(vertTable);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxVerticalRunShift = new Scale.Fraction(
                0.05,
                "Max shift between two runs of vertical sections");

        // Constants specified WRT *maximum* line thickness (scale.getmaxFore())
        // ----------------------------------------------
        // Should be 1.0, unless ledgers are thicker than staff lines
        private final Constant.Ratio ledgerThickness = new Constant.Ratio(
                1.2,
                "Ratio of ledger thickness vs staff line MAXIMUM thickness");

        // Constants for debugging
        // -----------------------
        private final Constant.String horizontalVipSections = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP horizontal sections IDs");

        private final Constant.String verticalVipSections = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP vertical sections IDs");
    }
}
