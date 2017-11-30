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
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BraceInter;
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
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    private static final List<Class<? extends AbstractInter>> impactingInterClasses = Arrays.asList(
            // AlterInter.class,
            AugmentationDotInter.class,
            // ArpeggiatoInter.class,
            // ArticulationInter.class,
            // BarConnectorInter.class,
            BarlineInter.class,
            BeamHookInter.class,
            BeamInter.class,
            BraceInter.class,
            // BracketConnectorInter.class,
            // BracketInter.class,
            // BreathMarkInter.class,
            // CaesuraInter.class,
            // ChordNameInter.class,
            // ClefInter.class,
            // DynamicsInter.class,
            // EndingInter.class,
            // FermataDotInter.class,
            // FermataArcInter.class,
            // FermataInter.class,
            // FingeringInter.class,
            FlagInter.class,
            // FretInter.class,
            HeadChordInter.class,
            HeadInter.class,
            // KeyAlterInter.class,
            // KeyInter.class,
            // LedgerInter.class,
            // LyricItemInter.class,
            // LyricLineInter.class,
            // MarkerInter.class,
            // OrnamentInter.class,
            // PedalInter.class,
            // PluckingInter.class,
            // RepeatDotInter.class,
            RestChordInter.class,
            RestInter.class,
            // SegmentInter.class,
            // SentenceInter.class,
            SlurInter.class,
            // SmallBeamInter.class,
            // SmallChordInter.class,
            // SmallFlagInter.class,
            StemInter.class,
            TimeNumberInter.class,
            TimePairInter.class,
            TimeWholeInter.class,
            TupletInter.class // WedgeInter.class,
    // WordInter.class,
    );

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimingStep} object.
     */
    public RhythmsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    @Override
    public void impact (UITaskList seq)
    {
        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                SystemInfo system = inter.getSig().getSystem();
                Point center = inter.getCenter();

                if (center != null) {
                    MeasureStack stack = system.getMeasureStackAt(center);
                    Page page = system.getPage();
                    new PageRhythm(page).reprocessStack(stack);

                    break;
                }
            }
        }
    }

    @Override
    public Collection<Class<? extends AbstractInter>> impactingInterClasses ()
    {
        return impactingInterClasses;
    }
}
