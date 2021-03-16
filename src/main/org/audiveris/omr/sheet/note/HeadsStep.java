//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d s S t e p                                       //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code HeadsStep} implements <b>HEADS</b> step, which uses distance matching
 * technique to retrieve all possible interpretations of note heads (black, void or
 * whole) but no note rests.
 *
 * @author Hervé Bitteur
 */
public class HeadsStep
        extends AbstractSystemStep<HeadsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadsStep} object.
     */
    public HeadsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        new NoteHeadsBuilder(system,
                             context.distanceTable,
                             context.sheetSpots.get(system),
                             context.tallies.get(system))
                .buildHeads();
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
            throws StepException
    {
        // Build proper distance table and make it available for system-level processing
        DistanceTable distances = new DistancesBuilder(sheet).buildDistances();

        // Retrieve spots for (black) heads
        Map<SystemInfo, List<Glyph>> sheetSpots = new HeadSpotsBuilder(sheet).getSpots();

        // Allocate the collectors for seed-head data
        Map<SystemInfo, HeadSeedTally> tallies = new TreeMap<>();

        for (SystemInfo system : sheet.getSystems()) {
            tallies.put(system, new HeadSeedTally());
        }

        return new Context(distances, sheetSpots, tallies);
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Context context)
            throws StepException
    {
        // Remove HEAD_SPOTS image from disk
        sheet.getPicture().discardImage(Picture.ImageKey.HEAD_SPOTS);

        // Analyze seed-head data
        HeadSeedTally.analyze(sheet, context.tallies.values());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    /**
     * Context for step processing.
     */
    protected static class Context
    {

        /** Table of distances. */
        public final DistanceTable distanceTable;

        /** Spots per system. */
        public final Map<SystemInfo, List<Glyph>> sheetSpots;

        /** Seed-head tally per system. */
        public final Map<SystemInfo, HeadSeedTally> tallies;

        /**
         * Create a Context.
         *
         * @param distanceTable
         * @param sheetSpots
         * @param tallies
         */
        Context (DistanceTable distanceTable,
                 Map<SystemInfo, List<Glyph>> sheetSpots,
                 Map<SystemInfo, HeadSeedTally> tallies)
        {
            this.distanceTable = distanceTable;
            this.sheetSpots = sheetSpots;
            this.tallies = tallies;
        }
    }
}
