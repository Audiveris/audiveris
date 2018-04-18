//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i n k s S t e p                                       //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SigReducer;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.SentenceRoleTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code LinksStep} implements <b>LINKS</b> step, which assigns relations between
 * certain symbols and makes a final reduction.
 *
 * @author Hervé Bitteur
 */
public class LinksStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LinksStep.class);

    /** Classes that may impact texts. */
    private static final Set<Class> forTexts;

    static {
        forTexts = new HashSet<Class>();
        forTexts.add(WordInter.class);
        forTexts.add(SentenceInter.class);
    }

    /** All impacting classes. */
    private static final Set<Class> impactingClasses;

    static {
        impactingClasses = new HashSet<Class>();
        impactingClasses.addAll(forTexts);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code LinksStep} object.
     */
    public LinksStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        StopWatch watch = new StopWatch("LinksStep doSystem #" + system.getId());

        watch.start("SymbolsLinker");
        new SymbolsLinker(system).process();

        // Reduction
        watch.start("reduceLinks");
        new SigReducer(system, false).reduceLinks();

        // Purge deleted lyrics from containing part
        new InterCleaner(system).purgeContainers();

        // Remove all free glyphs?
        if (constants.removeFreeGlyphs.isSet()) {
            system.clearFreeGlyphs();
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //--------//
    // impact //
    //--------//
    @Override
    public void impact (UITaskList seq,
                        OpKind opKind)
    {
        logger.debug("LINKS impact {} {}", opKind, seq);

        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                SystemInfo system = inter.getSig().getSystem();
                Class interClass = (Class) inter.getClass();

                if (isImpactedBy(interClass, forTexts)) {
                    if (inter instanceof LyricItemInter) {
                        LyricItemInter item = (LyricItemInter) inter;

                        if ((opKind != OpKind.UNDO) && task instanceof AdditionTask) {
                            item.mapToChord();
                        }
                    } else if (inter instanceof SentenceInter) {
                        SentenceInter sentence = (SentenceInter) inter;
                        SymbolsLinker linker = new SymbolsLinker(system);

                        if ((opKind != OpKind.UNDO) && task instanceof AdditionTask) {
                            linker.linkOneSentence(sentence);
                        } else if (task instanceof SentenceRoleTask) {
                            SentenceRoleTask roleTask = (SentenceRoleTask) interTask;
                            linker.unlinkOneSentence(
                                    sentence,
                                    (opKind == OpKind.UNDO) ? roleTask.getNewRole()
                                            : roleTask.getOldRole());
                            linker.linkOneSentence(sentence);
                        }
                    }
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

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Void context)
            throws StepException
    {
        // Recheck the ties, now that alterations are available
        for (SystemInfo system : sheet.getSystems()) {
            for (Inter inter : system.getSig().inters(SlurInter.class)) {
                SlurInter slur = (SlurInter) inter;
                slur.recheckTie();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean removeFreeGlyphs = new Constant.Boolean(
                false,
                "Should we remove all free glyphs?");
    }
}
