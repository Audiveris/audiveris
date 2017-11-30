//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S t e p                                     //
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
package org.audiveris.omr.step;

import org.audiveris.omr.OMR;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.inter.AbstractInter;
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.BreathMarkInter;
import org.audiveris.omr.sig.inter.CaesuraInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SegmentInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITaskList;
import static org.audiveris.omr.step.Step.RHYTHMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class {@code AbstractStep} provides a convenient basis for any {@link Step}
 * implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractStep.class);

    private static final Impactor IMPACTOR = new Impactor();

    /**
     * List of all non-abstract Inter classes sorted alphabetically.
     */
    private static final List<Class<? extends AbstractInter>> concreteInterClasses = Arrays.asList(
            AlterInter.class,
            AugmentationDotInter.class,
            ArpeggiatoInter.class,
            ArticulationInter.class,
            BarConnectorInter.class,
            BarlineInter.class,
            BeamHookInter.class,
            BeamInter.class,
            BraceInter.class,
            BracketConnectorInter.class,
            BracketInter.class,
            BreathMarkInter.class,
            CaesuraInter.class,
            ChordNameInter.class,
            ClefInter.class,
            DynamicsInter.class,
            EndingInter.class,
            FermataDotInter.class,
            FermataArcInter.class,
            FermataInter.class,
            FingeringInter.class,
            FlagInter.class,
            FretInter.class,
            HeadChordInter.class,
            HeadInter.class,
            KeyAlterInter.class,
            KeyInter.class,
            LedgerInter.class,
            LyricItemInter.class,
            LyricLineInter.class,
            MarkerInter.class,
            OrnamentInter.class,
            PedalInter.class,
            PluckingInter.class,
            RepeatDotInter.class,
            RestChordInter.class,
            RestInter.class,
            SegmentInter.class,
            SentenceInter.class,
            SlurInter.class,
            SmallBeamInter.class,
            SmallChordInter.class,
            SmallFlagInter.class,
            StemInter.class,
            TimeNumberInter.class,
            TimePairInter.class,
            TimeWholeInter.class,
            TupletInter.class,
            WedgeInter.class,
            WordInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractStep} object.
     */
    public AbstractStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // clearErrors //
    //-------------//
    public void clearErrors (Step step,
                             Sheet sheet)
    {
        if (OMR.gui != null) {
            sheet.getErrorsEditor().clearStep(step);
        }
    }

    //-----------//
    // displayUI //
    //-----------//
    public void displayUI (Step step,
                           Sheet sheet)
    {
        // Void by default
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method must be defined for any concrete Step.
     *
     * @param sheet the related sheet
     * @throws StepException raised if processing failed
     */
    public abstract void doit (Sheet sheet)
            throws StepException;

    /**
     * First step that is impacted by addition or removal of the provided inter.
     *
     * @param inter the provided inter (added or removed)
     * @return first impacted step
     */
    public static Step firstImpactedStep (Inter inter)
    {
        return IMPACTOR.firstImpactedStep(inter);
    }

    /**
     * First step that is impacted by the provided UI tasks sequence.
     *
     * @param seq the provided UI tasks sequence
     * @return first impacted step
     */
    public static Step firstImpactedStep (UITaskList seq)
    {
        return IMPACTOR.firstImpactedStep(seq);
    }

    //-------------//
    // getSheetTab //
    //-------------//
    /**
     * Report the related assembly view tab, selected when step completes
     *
     * @return the related tab
     */
    public SheetTab getSheetTab ()
    {
        return SheetTab.DATA_TAB;
    }

    /**
     * Apply the provided UI sequence to this step.
     *
     * @param seq the sequence of UI tasks
     */
    public void impact (UITaskList seq)
    {
        // No-op by default
    }

    /**
     * Report the set of Inter classes that impact this step.
     *
     * @return the set of impacting classes, perhaps empty but not null
     */
    public Collection<Class<? extends AbstractInter>> impactingInterClasses ()
    {
        return Collections.EMPTY_SET;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    private static class Impactor
            extends AbstractInterVisitor
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** First impacted step. */
        private Step step;

        //~ Methods --------------------------------------------------------------------------------
        public Step firstImpactedStep (Inter inter)
        {
            step = null;

            inter.accept(this);

            return step;
        }

        public Step firstImpactedStep (UITaskList seq)
        {
            Step seqFirst = null;

            for (UITask task : seq.getTasks()) {
                Step s = null;

                if (task instanceof InterTask) {
                    InterTask interTask = (InterTask) task;
                    s = firstImpactedStep(interTask.getInter());
                }

                if ((seqFirst == null) || ((s != null) && (s.compareTo(seqFirst) < 0))) {
                    seqFirst = s;
                }
            }

            return seqFirst;
        }

        //    @Override
        //    public void visit (AbstractBeamInter inter)
        //    {
        //    }
        //
        //    @Override
        //    public void visit (AbstractChordInter inter)
        //    {
        //    }
        //
        //    @Override
        //    public void visit (AbstractFlagInter inter)
        //    {
        //    }
        //
        @Override
        public void visit (AlterInter inter)
        {
        }

        @Override
        public void visit (ArpeggiatoInter inter)
        {
        }

        @Override
        public void visit (ArticulationInter inter)
        {
        }

        @Override
        public void visit (AugmentationDotInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (BarConnectorInter inter)
        {
        }

        @Override
        public void visit (BarlineInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (BeamHookInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (BeamInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (BraceInter inter)
        {
        }

        @Override
        public void visit (BracketConnectorInter inter)
        {
        }

        @Override
        public void visit (BracketInter inter)
        {
        }

        @Override
        public void visit (BreathMarkInter inter)
        {
        }

        @Override
        public void visit (CaesuraInter inter)
        {
        }

        @Override
        public void visit (ChordNameInter inter)
        {
        }

        @Override
        public void visit (ClefInter inter)
        {
        }

        @Override
        public void visit (DynamicsInter inter)
        {
        }

        @Override
        public void visit (EndingInter inter)
        {
        }

        @Override
        public void visit (FermataArcInter inter)
        {
        }

        @Override
        public void visit (FermataDotInter inter)
        {
        }

        @Override
        public void visit (FermataInter inter)
        {
        }

        @Override
        public void visit (FingeringInter inter)
        {
        }

        @Override
        public void visit (FlagInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (FretInter inter)
        {
        }

        @Override
        public void visit (HeadChordInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (HeadInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (Inter inter)
        {
        }

        @Override
        public void visit (KeyAlterInter inter)
        {
        }

        @Override
        public void visit (KeyInter inter)
        {
        }

        @Override
        public void visit (LedgerInter inter)
        {
        }

        @Override
        public void visit (LyricItemInter inter)
        {
        }

        @Override
        public void visit (LyricLineInter inter)
        {
        }

        @Override
        public void visit (MarkerInter inter)
        {
        }

        @Override
        public void visit (OrnamentInter inter)
        {
        }

        @Override
        public void visit (PedalInter inter)
        {
        }

        @Override
        public void visit (RepeatDotInter inter)
        {
        }

        @Override
        public void visit (PluckingInter inter)
        {
        }

        @Override
        public void visit (RestChordInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (RestInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (SegmentInter inter)
        {
        }

        @Override
        public void visit (SentenceInter inter)
        {
        }

        @Override
        public void visit (SlurInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (SmallBeamInter inter)
        {
        }

        @Override
        public void visit (SmallChordInter inter)
        {
        }

        @Override
        public void visit (SmallFlagInter inter)
        {
        }

        @Override
        public void visit (StemInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (TimeNumberInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (TimePairInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (TimeWholeInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (TupletInter inter)
        {
            step = RHYTHMS;
        }

        @Override
        public void visit (WedgeInter inter)
        {
        }

        @Override
        public void visit (WordInter inter)
        {
        }
    }
}
