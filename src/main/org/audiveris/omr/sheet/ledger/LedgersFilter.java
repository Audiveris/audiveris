//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L e d g e r s F i l t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import ij.process.ByteProcessor;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.dynamic.FilamentBoard;
import org.audiveris.omr.lag.BasicLag;
import org.audiveris.omr.lag.JunctionShiftPolicy;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.SectionFactory;
import org.audiveris.omr.lag.SectionService;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.ui.LagController;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.util.IntUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code LedgersFilter} filters the full sheet image for runs suitable for
 * ledgers.
 *
 * @author Hervé Bitteur
 */
public class LedgersFilter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LedgersFilter.class);

    /** Related sheet. */
    private final Sheet sheet;

    // Debug
    final List<Integer> vipSections;

    /**
     * Creates a new LedgersFilter object.
     *
     * @param sheet the related sheet
     */
    public LedgersFilter (Sheet sheet)
    {
        this.sheet = sheet;

        // VIPs
        vipSections = IntUtil.parseInts(constants.ledgerVipSections.getValue());

        if (!vipSections.isEmpty()) {
            logger.info("Ledger VIP sections: {}", vipSections);
        }
    }

    //---------//
    // process //
    //---------//
    /**
     * Start from NO_STAFF image and build the runs and sections that could make ledgers.
     * <p>
     * We build a horizontal RunTable with all runs from NO_STAFF source, except the ones too close
     * to any staff interior, since they could not be part of any ledger.
     * <p>
     * Then runs are packed into rectangular sections (no shift between runs of same section).
     *
     * @return the map: system &rarr; relevant sections for ledgers in system
     */
    public Map<SystemInfo, List<Section>> process ()
    {
        final Scale scale = sheet.getScale();
        final int minDistanceFromStaff = scale.toPixels(constants.minDistanceFromStaff);
        final StaffManager staffManager = sheet.getStaffManager();

        // Filter to keep only the runs which stand outside of staves cores.
        final RunTableFactory.Filter filter = new RunTableFactory.Filter()
        {
            @Override
            public boolean check (int x,
                                  int y,
                                  int length)
            {
                Point center = new Point(x + (length / 2), y);
                Staff staff = staffManager.getClosestStaff(center);

                return staff.distanceTo(center) >= minDistanceFromStaff;
            }
        };

        final RunTableFactory runFactory = new RunTableFactory(HORIZONTAL, filter);
        final ByteProcessor buffer = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        final RunTable ledgerTable = runFactory.createTable(buffer);
        final Lag lag = new BasicLag(Lags.LEDGER_LAG, HORIZONTAL);

        // We accept no shift between runs of the same section!
        final SectionFactory sectionFactory = new SectionFactory(lag, new JunctionShiftPolicy(0));
        sectionFactory.createSections(ledgerTable, null, true);
        setVipSections(lag);

        // Display a view on this lag?
        if ((OMR.gui != null) && constants.displayLedgers.isSet()) {
            lag.setEntityService(new SectionService(lag, sheet.getLocationService()));
            new LagController(sheet, lag, SheetTab.LEDGER_TAB).refresh();

            // Filament board
            sheet.getStub().getAssembly().addBoard(
                    SheetTab.LEDGER_TAB,
                    new FilamentBoard(sheet.getFilamentIndex().getEntityService(), true));
        }

        return dispatchLedgerSections(lag.getEntities());
    }

    //------------------------//
    // dispatchLedgerSections //
    //------------------------//
    /**
     * Dispatch the various horizontal ledger sections among systems.
     */
    private Map<SystemInfo, List<Section>> dispatchLedgerSections (Collection<Section> sections)
    {
        Map<SystemInfo, List<Section>> sectionMap = new TreeMap<>();
        List<SystemInfo> relevants = new ArrayList<>();
        SystemManager systemManager = sheet.getSystemManager();

        for (Section section : sections) {
            Point center = section.getCentroid();
            systemManager.getSystemsOf(center, relevants);

            for (SystemInfo system : relevants) {
                // Check section is within system abscissa boundaries
                if ((center.x >= system.getLeft()) && (center.x <= system.getRight())) {
                    List<Section> list = sectionMap.get(system);

                    if (list == null) {
                        sectionMap.put(system, list = new ArrayList<>());
                    }

                    list.add(section);
                }
            }
        }

        return sectionMap;
    }

    //----------------//
    // setVipSections //
    //----------------//
    private void setVipSections (Lag lag)
    {
        // Debug sections VIPs
        for (int id : vipSections) {
            Section sect = lag.getEntity(id);

            if (sect != null) {
                sect.setVip(true);
                logger.info("Ledger vip section: {}", sect);
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean displayLedgers = new Constant.Boolean(
                false,
                "Should we display the view on ledgers?");

        private final Scale.Fraction minDistanceFromStaff = new Scale.Fraction(
                0.25,
                "Minimum vertical distance from nearest staff");

        private final Constant.String ledgerVipSections = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP ledger sections IDs");
    }
}
