//----------------------------------------------------------------------------//
//                                                                            //
//                           S y m b o l s S t e p                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.ui.SymbolsEditor;

import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.selection.GlyphEvent;
import omr.selection.SelectionService;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Class {@code SymbolsStep} builds symbols glyphs and
 * performs specific patterns at symbol level
 * (clefs, sharps, naturals, stems, slurs, etc).
 *
 * @author Hervé Bitteur
 */
public class SymbolsStep
        extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SymbolsStep.class);

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
                DATA_TAB,
                "Apply specific glyph patterns");
    }

    //~ Methods ----------------------------------------------------------------
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
        SelectionService service = sheet.getNest()
                .getGlyphService();
        GlyphEvent glyphEvent = (GlyphEvent) service.getLastEvent(
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
        //        // Cleanup system sentences
        //        system.getSentences().clear();

        // Cleanup system dummy parts
        ScoreSystem scoreSystem = system.getScoreSystem();

        for (Iterator<TreeNode> it = scoreSystem.getParts()
                .iterator(); it.hasNext();) {
            SystemPart part = (SystemPart) it.next();

            if (part.isDummy()) {
                it.remove();
            }
        }

        // Iterate
        for (int iter = 1; iter <= constants.MaxPatternsIterations.getValue();
                iter++) {
            logger.debug("System#{} patterns iter #{}", system.getId(), iter);
            clearSystemErrors(system);

            if (!system.runPatterns()) {
                return; // No more progress made
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        private final Constant.Integer MaxPatternsIterations = new Constant.Integer(
                "count",
                1,
                "Maximum number of iterations for PATTERNS task");

    }
}
