//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y m b o l s S t e p                                      //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.note.ChordsBuilder;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code SymbolsStep} retrieves fixed-shape symbols in a system.
 * <p>
 * This accounts for rests, flags, dots, tuplets, alterations, ...
 *
 * @author Hervé Bitteur
 */
public class SymbolsStep
        extends AbstractSystemStep<SymbolsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsStep object.
     */
    public SymbolsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        sheet.getSymbolsEditor().refresh();

        // Update glyph board if needed (to see OCR'ed data)
        final SelectionService service = sheet.getGlyphIndex().getEntityService();

        @SuppressWarnings("unchecked")
        final EntityListEvent<Glyph> listEvent = (EntityListEvent<Glyph>) service.getLastEvent(
                EntityListEvent.class);

        if (listEvent != null) {
            service.publish(listEvent);
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
        StopWatch watch = new StopWatch("SymbolsStep doSystem #" + system.getId());
        watch.start("factory");

        final InterFactory factory = new InterFactory(system);

        // Retrieve symbols inters
        watch.start("buildSymbols");
        new SymbolsBuilder(system, factory).buildSymbols(context.optionalsMap);

        // Allocate rest-based chords
        watch.start("buildRestChords");
        new ChordsBuilder(system).buildRestChords();

        // Some checks that need presence of other symbols
        watch.start("lateChecks");
        factory.lateChecks();

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
            throws StepException
    {
        /**
         * Prepare image without staff lines, with all good and weak inters erased and
         * with all weak inters saved as optional symbol parts.
         */
        final Context context = new Context();
        new SymbolsFilter(sheet).process(context.optionalsMap);

        return context;
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

        /** Map of optional (weak) glyphs per system. */
        public final Map<SystemInfo, List<Glyph>> optionalsMap = new TreeMap<>();
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
