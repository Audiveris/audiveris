//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    H e a d S e e d T a l l y                                   //
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.util.HorizontalSide;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class {@code HeadSeedTally} records the actual abscissa distance between a head and
 * stem seed, per shape and per head side, for all stem-based heads, usually in a system.
 *
 * @author Hervé Bitteur
 */
public class HeadSeedTally
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeadSeedTally.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Map<HorizontalSide, Map<HeadInter, Double>> data;

    //~ Constructors -------------------------------------------------------------------------------
    public HeadSeedTally ()
    {
        data = new EnumMap<>(HorizontalSide.class);

        for (HorizontalSide hSide : HorizontalSide.values()) {
            data.put(hSide, new LinkedHashMap<>());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // getDx //
    //-------//
    /**
     * Report the head-seed distance, if any, for the provided head and horizontal side.
     *
     * @param head  head to check
     * @param hSide desired horizontal side
     * @return the seed dx value or null
     */
    public Double getDx (HeadInter head,
                         HorizontalSide hSide)
    {
        for (Entry<HeadInter, Double> headEntry : data.get(hSide).entrySet()) {
            if (headEntry.getKey() == head) {
                return headEntry.getValue();
            }
        }

        return null;
    }

    //-------//
    // putDx //
    //-------//
    /**
     * Record the head-seed distance for provided head and side.
     *
     * @param head  head at hand
     * @param hSide horizontal side of seed WRT head
     * @param dx    measured horizontal distance, negative if inside head box, positive if outside
     */
    public void putDx (HeadInter head,
                       HorizontalSide hSide,
                       double dx)
    {
        ///logger.info(String.format("putDx %3d %5s %s", dx, hSide, head));
        data.get(hSide).put(head, dx);
    }

    //-------------------//
    // purgeRemovedHeads //
    //-------------------//
    /**
     * Purge the tally of all removed heads.
     */
    public void purgeRemovedHeads ()
    {
        for (Entry<HorizontalSide, Map<HeadInter, Double>> entry : data.entrySet()) {
            final Set<Entry<HeadInter, Double>> set = entry.getValue().entrySet();

            for (Iterator<Entry<HeadInter, Double>> it = set.iterator(); it.hasNext();) {
                if (it.next().getKey().isRemoved()) {
                    it.remove();
                }
            }
        }
    }

    //---------//
    // analyze //
    //---------//
    /**
     * Perform a global analysis on all tallies in a sheet.
     *
     * @param sheet   the related sheet
     * @param tallies the collection of tallies, one per system
     */
    public static void analyze (Sheet sheet,
                                Collection<HeadSeedTally> tallies)
    {
        final Map<Shape, Map<HorizontalSide, Population>> global = new EnumMap<>(Shape.class);
        int id = 0;

        for (HeadSeedTally tally : tallies) {
            if (constants.dumpTally.isSet()) {
                tally.dump("System#" + ++id);
            }

            for (Entry<HorizontalSide, Map<HeadInter, Double>> sEntry : tally.data.entrySet()) {
                final HorizontalSide hSide = sEntry.getKey();

                for (Entry<HeadInter, Double> headEntry : sEntry.getValue().entrySet()) {
                    final HeadInter head = headEntry.getKey();
                    if (head.isRemoved()) {
                        continue;
                    }

                    final double dx = headEntry.getValue();
                    final Shape shape = head.getShape();

                    Map<HorizontalSide, Population> shapeGlobal = global.get(shape);

                    if (shapeGlobal == null) {
                        global.put(shape, shapeGlobal = new EnumMap<>(HorizontalSide.class));
                        for (HorizontalSide hs : HorizontalSide.values()) {
                            shapeGlobal.put(hs, new Population());
                        }
                    }

                    Population pop = shapeGlobal.get(hSide);
                    pop.includeValue(dx);
                }
            }
        }

        // Now get the sheet results per shape and head side
        final HeadSeedScale hsScale = new HeadSeedScale();

        for (Entry<Shape, Map<HorizontalSide, Population>> shapeEntry : global.entrySet()) {
            final Shape shape = shapeEntry.getKey();

            for (Entry<HorizontalSide, Population> sEntry : shapeEntry.getValue().entrySet()) {
                final HorizontalSide hSide = sEntry.getKey();
                final Population pop = sEntry.getValue();
                final int card = pop.getCardinality();

                if (card > 0) {
                    final double dx = pop.getMeanValue();

                    if (constants.printResults.isSet()) {
                        logger.info(String.format("%-20s %-5s dx:%4.1f count: %d",
                                                  shape, hSide, dx, card));
                    }

                    if (card >= constants.quorum.getValue()) {
                        hsScale.putDx(shape, hSide, dx);
                    }

                }
            }
        }

        logger.info("Scale information: {}", hsScale);
        sheet.getScale().setHeadSeedScale(hsScale);
    }

    //------//
    // dump //
    //------//
    public void dump (String title)
    {
        logger.info("\n{}", title);

        for (Entry<HorizontalSide, Map<HeadInter, Double>> sEntry : data.entrySet()) {
            final HorizontalSide hSide = sEntry.getKey();

            for (Entry<HeadInter, Double> hEntry : sEntry.getValue().entrySet()) {
                logger.info(String.format("%5s %3.1f %s",
                                          hSide, hEntry.getValue(), hEntry.getKey()));
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

        private final Constant.Boolean dumpTally = new Constant.Boolean(
                false,
                "Should we print out a dump of seed-head tally?");

        private final Constant.Boolean printResults = new Constant.Boolean(
                false,
                "Should we print out the tally results?");

        private final Constant.Integer quorum = new Constant.Integer(
                "samples",
                10,
                "Minimum samples per head shape and horizontal side"
        );
    }
}
