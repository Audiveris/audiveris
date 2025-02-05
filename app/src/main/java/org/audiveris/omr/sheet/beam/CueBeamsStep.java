//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C u e B e a m s S t e p                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sheet.ProcessingSwitch;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>CueBeamsStep</code> implements <b>CUE_BEAMS</b> step, which attempts to
 * retrieve small beams for cue notes.
 * <p>
 * This step is effective only if both following conditions are met:
 * <ol>
 * <li>The switch {@link ProcessingSwitch#smallHeads} is set.
 * <li>No small beam height has been set (either detected by SCALE step or manually set by end-user)
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class CueBeamsStep
        extends AbstractSystemStep<CueBeamsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(CueBeamsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new CueBeamsStep object.
     */
    public CueBeamsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
    {
        // Check whether we have to effectively run this step
        if (!sheet.getStub().getProcessingSwitches().getValue(ProcessingSwitch.smallHeads)) {
            logger.info("Step CUE_BEAMS is skipped because small heads switch is off");
            return null;
        }

        if (sheet.getScale().getSmallBeamScale() != null) {
            logger.info("Step CUE_BEAMS is skipped because small beams have been processed");
            return null;
        }

        final List<Glyph> spots = new ArrayList<>();
        final Lag spotLag = new BasicLag(Lags.SPOT_LAG, SpotsBuilder.SPOT_ORIENTATION);

        if ((OMR.gui != null) && constants.displayCueBeamSpots.isSet()) {
            // Display on cue spot glyphs
            final SpotsController spotController = new SpotsController(sheet, spots, spotLag);
            spotController.refresh();
        }

        return new Context(spots, spotLag);
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        if (context != null) {
            new BeamsBuilder(system, context.spotLag).buildCueBeams(context.spots); // -> Cue beams
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
        public final List<Glyph> spots;

        Context (List<Glyph> spots,
                 Lag spotLag)
        {
            this.spots = spots;
            this.spotLag = spotLag;
        }
    }
}
