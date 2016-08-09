//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C r o s s D e t e c t o r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;

import omr.sig.inter.Inter;
import omr.sig.inter.SentenceInter;

import omr.util.Predicate;
import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;

/**
 * Class {@code CrossDetector} browses the horizontal gutters between systems for inters
 * which overlap across systems.
 *
 * @author Hervé Bitteur
 */
public class CrossDetector
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CrossDetector.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CrossDetector} object.
     *
     * @param sheet the underlying sheet
     */
    public CrossDetector (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process ()
    {
        final SystemManager manager = sheet.getSystemManager();

        for (SystemInfo above : sheet.getSystems()) {
            for (SystemInfo below : manager.verticalNeighbors(above, VerticalSide.BOTTOM)) {
                detect(above, below);
            }
        }
    }

    //--------//
    // detect //
    //--------//
    private void detect (SystemInfo aboveSystem,
                         SystemInfo belowSystem)
    {
        // Gutter area
        final Area gutter = new Area(aboveSystem.getArea());
        gutter.intersect(belowSystem.getArea());

        final Rectangle gutterBounds = gutter.getBounds();

        // Build lists of candidates for above and for below
        Predicate<Inter> predicate = new Predicate<Inter>()
        {
            @Override
            public boolean check (Inter inter)
            {
                if (inter instanceof SentenceInter) {
                    return false;
                }

                final Point center = inter.getCenter();

                return gutterBounds.contains(center) && gutter.contains(center);
            }
        };

        List<Inter> aboveInters = aboveSystem.getSig().inters(predicate);

        if (aboveInters.isEmpty()) {
            return;
        }

        List<Inter> belowInters = belowSystem.getSig().inters(predicate);

        if (belowInters.isEmpty()) {
            return;
        }

        // Cross exclusions
        logger.info(
                "Cross detection between {}:{} and {}:{}",
                aboveSystem,
                aboveInters.size(),
                belowSystem,
                belowInters.size());
        SigReducer.detectCrossOverlaps(aboveInters, belowInters);
    }
}
