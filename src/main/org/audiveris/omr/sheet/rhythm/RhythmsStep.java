//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R h y t h m s S t e p                                     //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.RelationTask;
import org.audiveris.omr.sig.ui.RemovalTask;
import org.audiveris.omr.sig.ui.StackTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    private static final Logger logger = LoggerFactory.getLogger(RhythmsStep.class);

    /** Classes that impact just a measure stack. */
    private static final Set<Class> forStack;

    /** Classes that impact a whole page. */
    private static final Set<Class> forPage;

    /** All impacting classes. */
    private static final Set<Class> impactingClasses;

    static {
        forStack = new HashSet<>();
        forStack.add(AugmentationDotInter.class);
        forStack.add(BarlineInter.class);
        forStack.add(BeamHookInter.class);
        forStack.add(BeamInter.class);
        forStack.add(FlagInter.class);
        forStack.add(HeadChordInter.class);
        forStack.add(HeadInter.class);
        forStack.add(RestChordInter.class);
        forStack.add(RestInter.class);
        forStack.add(SmallBeamInter.class);
        forStack.add(SmallChordInter.class);
        forStack.add(SmallFlagInter.class);
        forStack.add(StaffBarlineInter.class);
        forStack.add(StemInter.class);
        forStack.add(TupletInter.class);
        forStack.add(MeasureStack.class);
        // Relations
        forStack.add(AugmentationRelation.class);
        forStack.add(BeamStemRelation.class);
        forStack.add(DoubleDotRelation.class);
        forStack.add(HeadStemRelation.class);
    }

    static {
        forPage = new HashSet<>();
        forPage.add(SlurInter.class); // Because of possibility of ties
        forPage.add(TimeNumberInter.class);
        forPage.add(TimePairInter.class);
        forPage.add(TimeWholeInter.class);
    }

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.addAll(forStack);
        impactingClasses.addAll(forPage);
    }

    /**
     * Creates a new {@code RhythmsStep} object.
     */
    public RhythmsStep ()
    {
    }

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

        final SIGraph sig = seq.getSig();
        final SystemInfo system = sig.getSystem();
        final Page page = system.getPage();

        // First, determine what will be impacted
        final Impact impact = new Impact();

        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                Class classe = inter.getClass();

                if (isImpactedBy(classe, forPage)) {
                    // Reprocess the whole page
                    impact.onPage = true;
                } else if (isImpactedBy(classe, forStack)) {
                    // Reprocess just the stack
                    Point center = inter.getCenter();

                    if (center != null) {
                        MeasureStack stack = system.getStackAt(center);
                        impact.onStacks.add(stack);

                        if (inter instanceof BarlineInter || inter instanceof StaffBarlineInter) {
                            if ((task instanceof RemovalTask && (opKind == OpKind.UNDO))
                                        || (task instanceof AdditionTask && (opKind != OpKind.UNDO))) {
                                // Add next stack as well
                                impact.onStacks.add(stack.getNextSibling());
                            }
                        }
                    }
                }
            } else if (task instanceof StackTask) {
                // Reprocess the stack
                MeasureStack stack = ((StackTask) task).getStack();
                Class classe = stack.getClass();

                if (isImpactedBy(classe, forStack)) {
                    impact.onStacks.add(stack);
                }
            } else if (task instanceof RelationTask) {
                RelationTask relationTask = (RelationTask) task;
                Relation relation = relationTask.getRelation();
                Class classe = relation.getClass();

                if (isImpactedBy(classe, forStack)) {
                    Inter source = relationTask.getSource();
                    MeasureStack stack = system.getStackAt(source.getCenter());
                    impact.onStacks.add(stack);
                }
            }
        }

        // Second, handle each rhythm impact
        if (impact.onPage) {
            new PageRhythm(page).process();
        } else {
            for (MeasureStack stack : impact.onStacks) {
                new PageRhythm(page).reprocessStack(stack);
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

    //--------//
    // Impact //
    //--------//
    private static class Impact
    {

        boolean onPage = false;

        Set<MeasureStack> onStacks = new LinkedHashSet<>();

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
