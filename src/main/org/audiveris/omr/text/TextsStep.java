//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e x t s S t e p                                        //
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
package org.audiveris.omr.text;

import ij.process.ByteProcessor;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Classes that may impact texts. */
    private static final Set<Class<?>> forLyrics;

    /** All impacting classes. */
    private static final Set<Class<?>> impactingClasses;

    static {
        forLyrics = new HashSet<>();
        forLyrics.add(LyricLineInter.class);
    }

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.addAll(forLyrics);
    }

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
        new TextBuilder(system).processSystem(context.buffer, context.textLines);
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
            throws StepException
    {
        List<TextLine> lines = new ArrayList<>();

        // Launch OCR on the whole sheet
        SheetScanner scanner = new SheetScanner(sheet);

        if (OcrUtil.getOcr().isAvailable()) {
            lines.addAll(scanner.scanSheet());
            Collections.sort(lines, TextLine.byOrdinate(sheet.getSkew()));

            if (logger.isDebugEnabled()) {
                TextLine.dump("Sheet raw OCRed lines:", lines, false);
            }
        } else {
            logger.warn("TEXTS step: {}", OCR.NO_OCR);
        }

        // Make all this available for system-level processing
        return new Context(scanner.getBuffer(), lines);
    }

    //--------//
    // impact //
    //--------//
    @Override
    public void impact (UITaskList seq,
                        UITask.OpKind opKind)
    {
        logger.debug("TEXTS impact {} {}", opKind, seq);

        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                SystemInfo system = inter.getSig().getSystem();
                Class interClass = inter.getClass();

                if (isImpactedBy(interClass, forLyrics)) {
                    if (inter instanceof LyricLineInter) {
                        // Re-number lyric lines
                        system.numberLyricLines();
                    }
                }
            }
        }
    }

    //--------------//
    // isImpactedBy //
    //--------------//
    @Override
    public boolean isImpactedBy (Class<?> classe)
    {
        return isImpactedBy(classe, impactingClasses);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    /**
     * Context data for this step.
     */
    protected static class Context
    {

        /** The sheet buffer handed to OCR. */
        public final ByteProcessor buffer;

        /** The raw text lines OCR'ed. */
        public final List<TextLine> textLines;

        /**
         * Create a Context object.
         *
         * @param buffer
         * @param textLines
         */
        Context (ByteProcessor buffer,
                 List<TextLine> textLines)
        {
            this.buffer = buffer;
            this.textLines = textLines;
        }
    }
}
