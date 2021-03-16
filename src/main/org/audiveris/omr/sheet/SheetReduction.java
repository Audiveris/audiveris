//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t R e d u c t i o n                                  //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import static org.audiveris.omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code SheetReduction} works at sheet level to reduce the duplicated inters
 * located in inter-system areas (gutters).
 * <p>
 * Since there is no reliable way to decide upfront if a glyph located between systems belongs to
 * the upper or to the lower system, both systems try to find out glyph interpretation(s) with
 * respect to the system at hand.
 * <p>
 * This can lead to duplications that can be solved only when the whole sheet processing has be
 * done, that is at PAGE step.
 *
 * @author Hervé Bitteur
 */
public class SheetReduction
{

    private static final Logger logger = LoggerFactory.getLogger(SheetReduction.class);

    /** Sheet to process. */
    private final Sheet sheet;

    /**
     * Create a new {@code SheetReduction} object.
     *
     * @param sheet the sheet to reduce
     */
    public SheetReduction (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //---------//
    // process //
    //---------//
    /**
     * Check all inter-systems gutters to resolve 'duplicated' inters.
     */
    public void process ()
    {
        final SystemManager systemMgr = sheet.getSystemManager();

        for (SystemInfo systemAbove : systemMgr.getSystems()) {
            List<SystemInfo> neighbors = systemMgr.verticalNeighbors(systemAbove, BOTTOM);

            for (SystemInfo systemBelow : neighbors) {
                checkGutter(systemAbove, systemBelow);
            }
        }
    }

    //-------------//
    // checkGutter //
    //-------------//
    /**
     * Check the gutter between the two provided systems.
     * <p>
     * This concerns the area that is below the last line of last staff in system above
     * and above the first line of first staff in system below.
     * <p>
     * Using vertical distance to staff does not work correctly for lyrics lines.
     *
     * @param system1 system above
     * @param system2 system below
     */
    private void checkGutter (SystemInfo system1,
                              SystemInfo system2)
    {
        logger.debug("--- checkGutter {}/{}", system1, system2);
        final Area gutter = new Area(system1.getArea());
        gutter.intersect(system2.getArea());

        checkInters(system1, system2, gutter);
    }

    //-------------//
    // checkInters // non-ensembles in fact
    //-------------//
    /**
     * Process remaining inters, now that lyrics/sentences have been processed.
     *
     * @param system1 upper system
     * @param system2 lower system
     * @param gutter  inter-system area
     */
    private void checkInters (SystemInfo system1,
                              SystemInfo system2,
                              Area gutter)
    {
        logger.debug("checkInters");
        final Staff staff1 = system1.getLastStaff();
        final Staff staff2 = system2.getFirstStaff();

        final List<Inter> inters1 = getGutterInters(system1, gutter);
        final List<Inter> inters2 = getGutterInters(system2, gutter);

        Loop1:
        for (Inter inter1 : inters1) {
            final Rectangle box1 = inter1.getBounds();
            final Glyph g1 = inter1.getGlyph();
            final double d1 = Math.abs(staff1.distanceTo(inter1.getCenter()));

            for (Inter inter2 : inters2) {
                final Rectangle box2 = inter2.getBounds();

                if (box1.intersects(box2)) {
                    logger.debug("{} vs {}", inter1, inter2);
                    final Glyph g2 = inter2.getGlyph();

                    if ((g1 != null) && (g1 == g2)) {
                        final double d2 = Math.abs(staff2.distanceTo(inter2.getCenter()));

                        if (d1 <= d2) {
                            logger.debug("Removing lower {}", inter2);
                            inter2.remove();
                        } else {
                            logger.debug("Removing upper {}", inter1);
                            inter1.remove();
                            continue Loop1;
                        }
                    } else {
                        logger.info("Gutter. Different glyphs {}/{} {} vs {}",
                                    system1.getId(), system2.getId(), inter1, inter2);
                    }
                }
            }
        }

    }

    //-----------------//
    // getGutterInters //
    //-----------------//
    /**
     * Report all inters of provided system whose center lies within provided area.
     *
     * @param system containing system
     * @param area   provided area
     * @return list of found inters, perhaps empty
     */
    private List<Inter> getGutterInters (SystemInfo system,
                                         Area area)
    {
        List<Inter> found = new ArrayList<>();

        for (Inter inter : system.getSig().vertexSet()) {
            if ((inter != null)
                        && !inter.isRemoved()
                        && !(inter instanceof InterEnsemble)
                        && area.contains(inter.getCenter())) {
                found.add(inter);
            }
        }

        Collections.sort(found, Inters.byCenterAbscissa);

        return found;
    }
}
