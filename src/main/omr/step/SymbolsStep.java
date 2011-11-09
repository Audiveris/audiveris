//----------------------------------------------------------------------------//
//                                                                            //
//                           S y m b o l s S t e p                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.glyph.Grades;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.SelectionService;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code SymbolsStep} recognizes isolated symbols glyphs and aggregates
 * unknown symbols into compound glyphs
 *
 * @author Hervé Bitteur
 */
public class SymbolsStep
    extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolsStep.class);

    //~ Constructors -----------------------------------------------------------

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
            Redoable.REDOABLE,
            DATA_TAB,
            "Recognize Symbols & Compounds");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        sheet.getSymbolsEditor()
             .refresh();

        // Update glyph board if needed (to see OCR'ed data)
        SelectionService service = sheet.getNest()
                                        .getGlyphService();
        GlyphEvent       glyphEvent = (GlyphEvent) service.getLastEvent(
            GlyphEvent.class);

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
        if (Main.getGui() != null) {
            system.getSheet()
                  .getErrorsEditor()
                  .clearSystem(this, system.getId());
        }

        system.inspectGlyphs(Grades.symbolMinGrade);
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet                  sheet)
        throws StepException
    {
        if (Main.getGui() != null) {
            sheet.createSymbolsControllerAndEditor();
        }
    }
}
