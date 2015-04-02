//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y m b o l s S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolsEditor;

import omr.selection.GlyphEvent;
import omr.selection.SelectionService;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.note.ChordsBuilder;

import omr.step.AbstractSystemStep;
import omr.step.Step;
import omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code SymbolsStep} retrieves fixed-shape symbols.
 * <p>
 * This accounts for rests, flags, dots, tuplets, alterations, ...
 *
 * @author Hervé Bitteur
 */
public class SymbolsStep
        extends AbstractSystemStep<SymbolsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

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
        SymbolsEditor editor = sheet.getSymbolsEditor();

        if (editor != null) {
            editor.refresh();
        }

        // Update glyph board if needed (to see OCR'ed data)
        SelectionService service = sheet.getGlyphNest().getGlyphService();
        GlyphEvent glyphEvent = (GlyphEvent) service.getLastEvent(GlyphEvent.class);

        if (glyphEvent != null) {
            service.publish(glyphEvent);
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
        SymbolFactory factory = new SymbolFactory(system);

        // Retrieve symbols inters
        new SymbolsBuilder(system, factory).buildSymbols(context.optionalsMap);

        // Retrieve relations between symbols inters
        factory.linkSymbols();

        // Allocate rest-based chords
        new ChordsBuilder(system).buildRestChords();
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Collection<SystemInfo> systems,
                                Sheet sheet)
            throws StepException
    {
        /**
         * Prepare image without staff lines, with all good inters erased and with all
         * weak inters saved as optional symbol parts.
         */
        Context context = new Context();
        new SymbolsFilter(sheet).process(context.optionalsMap);

        return context;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    public static class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Map of optional (weak) glyphs per system. */
        public final Map<SystemInfo, List<Glyph>> optionalsMap = new TreeMap<SystemInfo, List<Glyph>>();
    }
}
