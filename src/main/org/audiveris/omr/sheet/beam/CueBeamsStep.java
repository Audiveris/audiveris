//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C u e B e a m s S t e p                                    //
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.lag.BasicLag;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code CueBeamsStep} implements <b>CUE_BEAMS</b> step, which attempts to
 * retrieve beams for cue notes.
 *
 * @author Hervé Bitteur
 */
public class CueBeamsStep
        extends AbstractSystemStep<CueBeamsStep.Context>
{

    private static final Constants constants = new Constants();

    /**
     * Creates a new CueBeamsStep object.
     */
    public CueBeamsStep ()
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
        new BeamsBuilder(system, context.spotLag).buildCueBeams(context.spots); // -> Cue beams
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
    {
        List<Glyph> spots = new ArrayList<>();
        Lag spotLag = new BasicLag(Lags.SPOT_LAG, SpotsBuilder.SPOT_ORIENTATION);

        // Display on cue spot glyphs?
        if ((OMR.gui != null) && constants.displayCueBeamSpots.isSet()) {
            SpotsController spotController = new SpotsController(sheet, spots, spotLag);
            spotController.refresh();
        }

        return new Context(spots, spotLag);
    }

    //---------//
    // Context //
    //---------//
    /**
     * Context for step processing.
     */
    protected static class Context
    {

        /** Lag of spot sections. */
        public final Lag spotLag;

        /** Spot glyphs. */
        private final List<Glyph> spots;

        /**
         * Create Context.
         *
         * @param spots
         * @param spotLag
         */
        Context (List<Glyph> spots,
                 Lag spotLag)
        {
            this.spots = spots;
            this.spotLag = spotLag;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean displayCueBeamSpots = new Constant.Boolean(
                false,
                "Should we display the cue beam Spots view?");
    }
}
