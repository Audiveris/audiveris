//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L e d g e r s S t e p                                     //
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
package org.audiveris.omr.sheet.ledger;

import org.audiveris.omr.lag.Section;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.action.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class <code>LedgersStep</code> implements <b>LEDGERS</b> step, which retrieves all
 * possible ledger interpretations.
 *
 * @author Hervé Bitteur
 */
public class LedgersStep
        extends AbstractSystemStep<LedgersStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(LedgersStep.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LedgersStep object.
     */
    public LedgersStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (OmrStep step,
                           Sheet sheet)
    {
        if (Preferences.Topic.DEBUG.isSet()) {
            // Add ledger checkboard
            new LedgersBuilder(sheet.getSystems().get(0)).addCheckBoard();
        }
    }

    //----------//
    // doEpilog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Analyze the sheet population of ledgers to detect abnormal ones, then discard those,
     * and rebuild the ledger map for each staff.
     *
     * @param sheet   the sheet being processed
     * @param context ledgers context
     * @throws StepException
     */
    @Override
    protected void doEpilog (Sheet sheet,
                             Context context)
        throws StepException
    {
        new LedgersPostAnalysis(sheet, context).process();
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Retrieve horizontal sticks for ledger candidates, mapped by related system(s).
     *
     * @return the map of sections per system
     */
    @Override
    protected Context doProlog (Sheet sheet)
        throws StepException
    {
        return new Context(new LedgersFilter(sheet).process());
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
        throws StepException
    {
        final LedgersBuilder builder = new LedgersBuilder(system);
        context.builders.put(system, builder);
        builder.buildLedgers(context.sectionMap.get(system));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Context //
    //---------//
    /**
     * Context for step processing.
     */
    public static class Context
    {
        /**
         * Ledger candidate sections per system.
         */
        public final Map<SystemInfo, List<Section>> sectionMap;

        /** Map system -> builder. */
        public final Map<SystemInfo, LedgersBuilder> builders = new HashMap<>();

        /**
         * Create a Context.
         *
         * @param sectionMap map of filtered sections per system
         */
        public Context (Map<SystemInfo, List<Section>> sectionMap)
        {
            this.sectionMap = sectionMap;
        }
    }
}
