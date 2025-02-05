//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m s S t e p                                       //
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

import org.audiveris.omr.glyph.GlyphGroup;
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

/**
 * Class <code>BeamsStep</code> implements <b>BEAMS</b> step, which uses the spots
 * produced by an image closing operation to retrieve all possible beam interpretations.
 * <p>
 * Typical beam height should have been detected by SCALE step.
 * If not, the end-user can still force a beam height via menu Sheet | Set scaling data.
 * <p>
 * A secondary (small) height may have been detected by SCALE step, or forced by end-user.
 * If the switch {@link ProcessingSwitch#smallBeams} is set, and if no secondary height is known,
 * a default value is used (about 2/3 of typical height).
 *
 * @author Hervé Bitteur
 */
public class BeamsStep
        extends AbstractSystemStep<BeamsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BeamsStep.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BeamsStep object.
     */
    public BeamsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // doEpilog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * For beams, dispose of BEAM_SPOT glyphs, a glyph may be split into several stuck beams.
     * <p>
     * (NOTA: Weak references to glyphs may survive as long as a related SpotsController exists)
     */
    @Override
    protected void doEpilog (Sheet sheet,
                             Context context)
        throws StepException
    {
        sheet.getSystems().forEach(system -> system.removeGroupedGlyphs(GlyphGroup.BEAM_SPOT));
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * For beams, perform a closing operation on the whole image with a disk shape as the
     * structure element to point out concentrations of foreground pixels (meant for beams).
     *
     * @return the populated context
     */
    @Override
    protected Context doProlog (Sheet sheet)
    {
        final Lag spotLag = new BasicLag(Lags.SPOT_LAG, SpotsBuilder.SPOT_ORIENTATION);

        // Retrieve significant spots for the whole sheet
        new SpotsBuilder(sheet).buildSheetSpots(spotLag);

        return new Context(spotLag);
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
        throws StepException
    {
        new BeamsBuilder(system, context.spotLag).buildBeams();

        // Detection of multiple-measure rests, since they look like long horizontal beams
        new MultipleRestsBuilder(system).process();
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
        /** Lag of spot sections. */
        public final Lag spotLag;

        /**
         * Create Context.
         *
         * @param spotLag
         */
        Context (Lag spotLag)
        {
            this.spotLag = spotLag;
        }
    }
}
