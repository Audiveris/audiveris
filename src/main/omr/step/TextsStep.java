//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e x t s S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.text.SheetScanner;
import omr.text.TextLine;

import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
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

    private static final Constants constants = new Constants();

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
        context.watch.start("system #" + system.getId());
        system.getTextBuilder().retrieveLines(context.buffer, context.textLines);
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet sheet,
                             Context context)
            throws StepException
    {
        if (constants.printWatch.isSet()) {
            context.watch.print();
        }
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Collection<SystemInfo> systems,
                                Sheet sheet)
            throws StepException
    {
        final StopWatch watch = new StopWatch("TextsStep");
        watch.start("prolog");

        // Launch OCR on the whole sheet
        SheetScanner scanner = new SheetScanner(sheet);
        List<TextLine> lines = scanner.scanSheet();

        // Make all this available for system-level processing
        return new Context(watch, scanner.getBuffer(), lines);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    public static class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final StopWatch watch;

        public final ByteProcessor buffer;

        public final List<TextLine> textLines;

        //~ Constructors ---------------------------------------------------------------------------
        public Context (StopWatch watch,
                        ByteProcessor buffer,
                        List<TextLine> textLines)
        {
            this.watch = watch;
            this.buffer = buffer;
            this.textLines = textLines;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
