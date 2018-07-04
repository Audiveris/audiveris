//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e x t s S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.text;

import ij.process.ByteProcessor;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

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
        new TextBuilder(system).retrieveSystemLines(context.buffer, context.textLines);
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
