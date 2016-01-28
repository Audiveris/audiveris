//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    L e d g e r s F i l t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ledger;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.dynamic.FilamentBoard;

import omr.lag.BasicLag;
import omr.lag.JunctionShiftPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionFactory;
import omr.lag.SectionService;

import static omr.run.Orientation.HORIZONTAL;

import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.ui.LagController;
import omr.sheet.ui.SheetTab;

import omr.util.StringUtil;

import ij.process.ByteProcessor;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LedgersFilter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Related sheet. */
    private final Sheet sheet;

    // Debug
    final List<String> vipSections;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LedgersFilter object.
     *
     * @param sheet the related sheet
     */
    public LedgersFilter (Sheet sheet)
    {
        this.sheet = sheet;

        // VIPs
        vipSections = StringUtil.parseStrings(constants.ledgerVipSections.getValue());

        if (!vipSections.isEmpty()) {
            logger.info("Ledger VIP sections: {}", vipSections);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * @return the map: system -> relevant sections for ledgers in system
     */
    public Map<SystemInfo, List<Section>> process ()
    {
        final Scale scale = sheet.getScale();
        final int minDistanceFromStaff = scale.toPixels(
                constants.minDistanceFromStaff);
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
        final SectionFactory sectionsBuilder = new SectionFactory(lag, new JunctionShiftPolicy(0));
        sectionsBuilder.createSections(ledgerTable, null, true);
        setVipSections(lag);

        // Display a view on this lag?
        if ((OMR.getGui() != null) && constants.displayLedgers.isSet()) {
            lag.setEntityService(new SectionService(lag, sheet.getLocationService()));
            new LagController(sheet, lag, SheetTab.LEDGER_TAB).refresh();

            // Filament board
            sheet.getAssembly().addBoard(
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
        Map<SystemInfo, List<Section>> sectionMap = new TreeMap<SystemInfo, List<Section>>();
        List<SystemInfo> relevants = new ArrayList<SystemInfo>();
        SystemManager systemManager = sheet.getSystemManager();

        for (Section section : sections) {
            Point center = section.getCentroid();
            systemManager.getSystemsOf(center, relevants);

            for (SystemInfo system : relevants) {
                // Check section is within system abscissa boundaries
                if ((center.x >= system.getLeft()) && (center.x <= system.getRight())) {
                    List<Section> list = sectionMap.get(system);

                    if (list == null) {
                        sectionMap.put(system, list = new ArrayList<Section>());
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
        for (String id : vipSections) {
            Section sect = lag.getEntity(id);

            if (sect != null) {
                sect.setVip(true);
                logger.info("Ledger vip section: {}", sect);
            }
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
