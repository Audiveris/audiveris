//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.lag.BasicLag;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code BeamsStep} implements <b>BEAMS</b> step, which uses the spots produced
 * by an image closing operation to retrieve all possible beam interpretations.
 *
 * @author Hervé Bitteur
 */
public class BeamsStep
        extends AbstractSystemStep<BeamsStep.Context>
{

    private static final Logger logger = LoggerFactory.getLogger(BeamsStep.class);

    /**
     * Creates a new BeamsStep object.
     */
    public BeamsStep ()
    {
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        new BeamsBuilder(system, context.distancemap.get(system), context.spotLag).buildBeams();
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Context context)
            throws StepException
    {
        // Cumulate system results
        Population distances = new Population();

        for (Population pop : context.distancemap.values()) {
            distances.includePopulation(pop);
        }

        if (distances.getCardinality() > 0) {
            logger.info("BeamDistance{{}}", distances);

            sheet.getScale().setBeamDistance(distances.getMeanValue(), distances
                                             .getStandardDeviation());
        }

        // Dispose of BEAM_SPOT glyphs, a glyph may be split into several beams
        // (NOTA: the weak references may survive as long as a related SpotsController exists)
        for (SystemInfo system : sheet.getSystems()) {
            system.removeGroupedGlyphs(GlyphGroup.BEAM_SPOT);
        }
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Perform a closing operation on the whole image with a disk shape as the structure
     * element to point out concentrations of foreground pixels (meant for beams).
     *
     * @return the populated context
     */
    @Override
    protected Context doProlog (Sheet sheet)
    {
        Lag spotLag = new BasicLag(Lags.SPOT_LAG, SpotsBuilder.SPOT_ORIENTATION);

        // Retrieve significant spots for the whole sheet
        new SpotsBuilder(sheet).buildSheetSpots(spotLag);

        // Allocate map to collect vertical distances between beams of the same group
        Map<SystemInfo, Population> distanceMap = new TreeMap<SystemInfo, Population>();

        for (SystemInfo system : sheet.getSystems()) {
            distanceMap.put(system, new Population());
        }

        return new Context(distanceMap, spotLag);
    }

    //---------//
    // Context //
    //---------//
    protected static class Context
    {

        /** Beam group vertical distances, per system. */
        public final Map<SystemInfo, Population> distancemap;

        /** Lag of spot sections. */
        public final Lag spotLag;

        public Context (Map<SystemInfo, Population> distanceMap,
                        Lag spotLag)
        {
            this.distancemap = distanceMap;
            this.spotLag = spotLag;
        }
    }
}
