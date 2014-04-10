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
package omr.step;

import omr.glyph.ui.SymbolsEditor;

import omr.selection.GlyphEvent;
import omr.selection.SelectionService;

import omr.sheet.Sheet;
import omr.sheet.SymbolsBuilder;
import omr.sheet.SymbolsFilter;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code SymbolsStep} retrieves fixed-shape symbols.
 *
 * @author Hervé Bitteur
 */
public class SymbolsStep
        extends AbstractSystemStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    //-------------//
    // SymbolsStep //
    //-------------//
    /**
     * Creates a new SymbolsStep object.
     */
    public SymbolsStep ()
    {
        super(
                Steps.SYMBOLS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve fixed-shape symbols");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        SymbolsEditor editor = sheet.getSymbolsEditor();

        if (editor != null) {
            editor.refresh();
        }

        // Update glyph board if needed (to see OCR'ed data)
        SelectionService service = sheet.getNest().getGlyphService();
        GlyphEvent glyphEvent = (GlyphEvent) service.getLastEvent(GlyphEvent.class);

        if (glyphEvent != null) {
            service.publish(glyphEvent);
        }
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        new SymbolsBuilder(system).buildSymbols();
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * Prepare image without staff lines and with all (good) inters erased.
     */
    @Override
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        new SymbolsFilter(sheet).process();
    }
}
