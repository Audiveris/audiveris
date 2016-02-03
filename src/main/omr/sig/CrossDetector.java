//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C r o s s D e t e c t o r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
