//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Abstract class {@code AbstractStep} provides a convenient basis for any {@link Step}
 * implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStep
{

    private static final Logger logger = LoggerFactory.getLogger(AbstractStep.class);

    /**
     * List of all non-abstract Inter classes sorted alphabetically.
     * Kept here as a convenient list to define a step impactingInterClasses.
     * Then to be removed.
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

    /**
     * Creates a new {@code AbstractStep} object.
     */
    public AbstractStep ()
    {
    }

    //-------------//
    // clearErrors //
    //-------------//
    /**
     * Clear the errors window in editor (No longer used).
     *
     * @param step  step concerned
     * @param sheet related sheet
     */
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
    /**
     * Update UI at step completion.
     *
     * @param step  step just completed
     * @param sheet related sheet
     */
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
     * @param seq    the sequence of UI tasks
     * @param opKind which operation is done on seq
     */
    public void impact (UITaskList seq,
                        OpKind opKind)
    {
        // No-op by default
    }

    /**
     * Report whether the provided class impacts this step.
     *
     * @param classe the class to check
     * @return true if step is impacted
     */
    public boolean isImpactedBy (Class<?> classe)
    {
        return false; // By default
    }

    /**
     * Report whether the collection of classes contains the provided class, or a super
     * type of the provided class.
     *
     * @param classe  the class to check
     * @param classes the collection of classes
     * @return true if compatible class found
     */
    protected boolean isImpactedBy (Class<?> classe,
                                    Collection<Class<?>> classes)
    {
        for (Class<?> cl : classes) {
            if (cl.isAssignableFrom(classe)) {
                return true;
            }
        }

        return false;
    }
}
