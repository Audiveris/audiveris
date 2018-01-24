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
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
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

    private static final Logger logger = LoggerFactory.getLogger(RhythmsStep.class);

    /** Classes that impact just a measure stack. */
    private static final Set<Class> forStack;

    static {
        forStack = new HashSet<Class>();
        forStack.add(AugmentationDotInter.class);
        forStack.add(BeamHookInter.class);
        forStack.add(BeamInter.class);
        forStack.add(FlagInter.class);
        forStack.add(HeadChordInter.class);
        forStack.add(HeadInter.class);
        forStack.add(RestChordInter.class);
        forStack.add(RestInter.class);
        forStack.add(StemInter.class);
        forStack.add(TupletInter.class);
    }

    /** Classes that impact a whole page. */
    private static final Set<Class> forPage;

    static {
        forPage = new HashSet<Class>();
        forPage.add(SlurInter.class); // Because of possibility of ties
        forPage.add(TimeNumberInter.class);
        forPage.add(TimePairInter.class);
        forPage.add(TimeWholeInter.class);
    }

    /** All impacting classes. */
    private static final Set<Class> impactingClasses;

    static {
        impactingClasses = new HashSet<Class>();
        impactingClasses.addAll(forStack);
        impactingClasses.addAll(forPage);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RhythmsStep} object.
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
    public void impact (UITaskList seq,
                        OpKind opKind)
    {
        logger.debug("RHYTHMS impact {} {}", opKind, seq);

        // First, determine what will be impacted
        Map<Page, Impact> map = new LinkedHashMap<Page, Impact>();

        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                SystemInfo system = inter.getSig().getSystem();
                Class<? extends AbstractInter> interClass = (Class<? extends AbstractInter>) inter.getClass();

                if (isImpactedBy(interClass, forPage)) {
                    // Reprocess the whole page
                    Page page = inter.getSig().getSystem().getPage();
                    Impact impact = map.get(page);

                    if (impact == null) {
                        map.put(page, impact = new Impact());
                    }

                    impact.onPage = true;
                } else if (isImpactedBy(interClass, forStack)) {
                    // Or reprocess just the stack
                    Point center = inter.getCenter();

                    if (center != null) {
                        MeasureStack stack = system.getMeasureStackAt(center);
                        Page page = system.getPage();
                        Impact impact = map.get(page);

                        if (impact == null) {
                            map.put(page, impact = new Impact());
                        }

                        impact.onStacks.add(stack);
                    }
                }
            }
        }

        // Second, handle each rhythm impact
        for (Entry<Page, Impact> entry : map.entrySet()) {
            Page page = entry.getKey();
            Impact impact = entry.getValue();

            if (impact.onPage) {
                new PageRhythm(page).process();
            } else {
                for (MeasureStack stack : impact.onStacks) {
                    new PageRhythm(page).reprocessStack(stack);
                }
            }
        }
    }

    //--------------//
    // isImpactedBy //
    //--------------//
    @Override
    public boolean isImpactedBy (Class classe)
    {
        return isImpactedBy(classe, impactingClasses);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Impact //
    //--------//
    private static class Impact
    {
        //~ Instance fields ------------------------------------------------------------------------

        boolean onPage = false;

        Set<MeasureStack> onStacks = new LinkedHashSet<MeasureStack>();

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("RhythmsImpact{");
            sb.append("page:").append(onPage);
            sb.append(" stacks:").append(onStacks);
            sb.append("}");

            return sb.toString();
        }
    }
}
