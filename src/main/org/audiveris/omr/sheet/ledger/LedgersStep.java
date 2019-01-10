//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L e d g e r s S t e p                                     //
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
package org.audiveris.omr.sheet.ledger;

import org.audiveris.omr.lag.Section;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.action.AdvancedTopics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Class {@code LedgersStep} implements <b>LEDGERS</b> step, which retrieves all
 * possible ledger interpretations.
 *
 * @author Hervé Bitteur
 */
public class LedgersStep
        extends AbstractSystemStep<LedgersStep.Context>
{

    private static final Logger logger = LoggerFactory.getLogger(LedgersStep.class);

    /**
     * Creates a new LedgersStep object.
     */
    public LedgersStep ()
    {
    }

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        if (AdvancedTopics.Topic.DEBUG.isSet()) {
            // Add ledger checkboard
            new LedgersBuilder(sheet.getSystems().get(0)).addCheckBoard();
        }
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        final List<Section> sections = context.sectionMap.get(system);
        new LedgersBuilder(system).buildLedgers(sections);
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Retrieve horizontal sticks for ledger candidates, mapped by related system(s).
     *
     * @return the context (map of sections per system)
     */
    @Override
    protected Context doProlog (Sheet sheet)
            throws StepException
    {
        return new Context(new LedgersFilter(sheet).process());
    }

    //---------//
    // Context //
    //---------//
    /**
     * Context for step processing.
     */
    protected static class Context
    {

        /**
         * Ledger candidate sections per system.
         */
        public final Map<SystemInfo, List<Section>> sectionMap;

        /**
         * Create a Context.
         *
         * @param sectionMap
         */
        Context (Map<SystemInfo, List<Section>> sectionMap)
        {
            this.sectionMap = sectionMap;
        }
    }
}
