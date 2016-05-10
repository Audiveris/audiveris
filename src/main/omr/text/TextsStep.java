//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e x t s S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code TextsStep} discovers text items in a system area.
 *
 * @author Hervé Bitteur
 */
public class TextsStep
        extends AbstractSystemStep<TextsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TextsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a TextsStep instance.
     */
    public TextsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        // Process texts at system level
        new TextBuilder(system).retrieveLines(context.buffer, context.textLines);
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
            throws StepException
    {
        // Launch OCR on the whole sheet
        SheetScanner scanner = new SheetScanner(sheet);
        List<TextLine> lines = scanner.scanSheet();

        // Make all this available for system-level processing
        return new Context(scanner.getBuffer(), lines);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    protected static class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final ByteProcessor buffer;

        public final List<TextLine> textLines;

        //~ Constructors ---------------------------------------------------------------------------
        public Context (ByteProcessor buffer,
                        List<TextLine> textLines)
        {
            this.buffer = buffer;
            this.textLines = textLines;
        }
    }
}
