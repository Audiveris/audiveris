//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R h y t h m s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code RhythmsStep} is a comprehensive step that handles the timing of every
 * relevant item within a page.
 *
 * @author Hervé Bitteur
 */
public class RhythmsStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            RhythmsStep.class);

    /** Inter classes that impact just a measure stack. */
    private static final Set<Class<? extends AbstractInter>> forStack;

    static {
        forStack = new HashSet<Class<? extends AbstractInter>>();
        forStack.add(AugmentationDotInter.class);
        forStack.add(BeamHookInter.class);
        forStack.add(BeamInter.class);
        forStack.add(FlagInter.class);
        forStack.add(HeadChordInter.class);
        forStack.add(HeadInter.class);
        forStack.add(RestChordInter.class);
        forStack.add(RestInter.class);
        forStack.add(TupletInter.class);
    }

    /** Inter classes that impact a whole page. */
    private static final Set<Class<? extends AbstractInter>> forPage;

    static {
        forPage = new HashSet<Class<? extends AbstractInter>>();
        forPage.add(SlurInter.class); // Because of possibility of ties
        forPage.add(TimeNumberInter.class);
        forPage.add(TimePairInter.class);
        forPage.add(TimeWholeInter.class);
    }

    /** All impacting Inter classes. */
    private static final Set<Class<? extends AbstractInter>> impactingInterClasses;

    static {
        impactingInterClasses = new HashSet<Class<? extends AbstractInter>>();
        impactingInterClasses.addAll(forStack);
        impactingInterClasses.addAll(forPage);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimingStep} object.
     */
    public RhythmsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        // Process each page of the sheet
        for (Page page : sheet.getPages()) {
            new PageRhythm(page).process();

            // Complete each measure with its needed data
            for (SystemInfo system : page.getSystems()) {
                new MeasureFiller(system).process();
            }
        }
    }

    //--------//
    // impact //
    //--------//
    @Override
    public void impact (UITaskList seq)
    {
        logger.info("RHYTHMS. impact for {}", seq);

        InterTask interTask = seq.getFirstInterTask();

        if (interTask != null) {
            Inter inter = interTask.getInter();
            SystemInfo system = inter.getSig().getSystem();
            Class<? extends AbstractInter> interClass = (Class<? extends AbstractInter>) inter.getClass();

            if (forPage.contains(interClass)) {
                // Reprocess the whole page
                Page page = inter.getSig().getSystem().getPage();
                //TODO: still to be implemented!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                logger.error("Rhythms step. Impact for page not yet implemented.");
            } else if (forStack.contains(interClass)) {
                // Or reprocess just the stack
                Point center = inter.getCenter();

                if (center != null) {
                    MeasureStack stack = system.getMeasureStackAt(center);
                    Page page = system.getPage();
                    new PageRhythm(page).reprocessStack(stack);
                }
            }
        }
    }

    //-----------------------//
    // impactingInterClasses //
    //-----------------------//
    @Override
    public Set<Class<? extends AbstractInter>> impactingInterClasses ()
    {
        return impactingInterClasses;
    }
}
