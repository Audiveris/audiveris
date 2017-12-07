//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a g e S t e p                                        //
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

import org.audiveris.omr.score.MeasureFixer;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageReduction;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sig.inter.AbstractInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITaskList;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class {@code PageStep} handles connections between systems in a page.
 * <ul>
 * <li>Physical system Part instances are abstracted into LogicalPart instances.
 * For systems where a given LogicalPart has no representative Part, a corresponding dummy Part is
 * inserted (with its proper staves and minimal measures).</li>
 * <li>Slurs are connected across systems.</li>
 * <li>Tied voices.</li>
 * <li>Refined lyric syllables.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PageStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            PageStep.class);

    /** Inter classes that may impact voices. */
    private static final Set<Class<? extends AbstractInter>> forVoices;

    static {
        forVoices = new HashSet<Class<? extends AbstractInter>>();
        forVoices.add(AugmentationDotInter.class);
        forVoices.add(BeamHookInter.class);
        forVoices.add(BeamInter.class);
        forVoices.add(FlagInter.class);
        forVoices.add(HeadChordInter.class);
        forVoices.add(HeadInter.class);
        forVoices.add(RestChordInter.class);
        forVoices.add(RestInter.class);
        forVoices.add(SlurInter.class);
        forVoices.add(TimeNumberInter.class);
        forVoices.add(TimePairInter.class);
        forVoices.add(TimeWholeInter.class);
        forVoices.add(TupletInter.class);
    }

    /** Inter classes that may impact lyrics. */
    private static final Set<Class<? extends AbstractInter>> forLyrics;

    static {
        forLyrics = new HashSet<Class<? extends AbstractInter>>();
        forLyrics.add(LyricItemInter.class);
        forLyrics.add(LyricLineInter.class);
    }

    /** Inter classes that may impact slurs. */
    private static final Set<Class<? extends AbstractInter>> forSlurs;

    static {
        forSlurs = new HashSet<Class<? extends AbstractInter>>();
        forSlurs.add(SlurInter.class);
    }

    /** All impacting Inter classes. */
    private static final Set<Class<? extends AbstractInter>> impactingInterClasses;

    static {
        impactingInterClasses = new HashSet<Class<? extends AbstractInter>>();
        impactingInterClasses.addAll(forVoices);
        impactingInterClasses.addAll(forLyrics);
        impactingInterClasses.addAll(forSlurs);
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageStep} object.
     */
    public PageStep ()
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
        for (Page page : sheet.getPages()) {
            // Connect parts across systems in the page
            new PageReduction(page).reduce();

            // Inter-system connections
            connectSlurs(page);

            // Lyrics
            refineLyrics(page);

            // Refine voices IDs (and thus colors) across all systems of the page
            Voices.refinePage(page);

            // Merge / renumber measure stacks within the page
            new MeasureFixer().process(page);
        }
    }

    //--------//
    // impact //
    //--------//
    @Override
    public void impact (UITaskList seq)
    {
        logger.info("PAGE. impact for {}", seq);

        InterTask interTask = seq.getFirstInterTask();

        if (interTask != null) {
            Inter inter = interTask.getInter();
            Page page = inter.getSig().getSystem().getPage();
            Class<? extends AbstractInter> interClass = (Class<? extends AbstractInter>) inter.getClass();

            if (forSlurs.contains(interClass)) {
                connectSlurs(page);
            }

            if (forLyrics.contains(interClass)) {
                refineLyrics(page);
            }

            if (forVoices.contains(interClass)) {
                Voices.refinePage(page);
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

    //--------------//
    // connectSlurs //
    //--------------//
    /**
     * Connect slurs across systems in page
     *
     * @param page provided page
     */
    private void connectSlurs (Page page)
    {
        for (SystemInfo system : page.getSystems()) {
            connectSystemInitialSlurs(system);
        }
    }

    //---------------------------//
    // connectSystemInitialSlurs //
    //---------------------------//
    /**
     * Within the current page only, retrieve the connections between the (orphan) slurs
     * at the beginning of the provided system and the (orphan) slurs at the end of the
     * previous system if any (within the same page).
     */
    private void connectSystemInitialSlurs (SystemInfo system)
    {
        SystemInfo prevSystem = system.getPrecedingInPage();

        if (prevSystem != null) {
            // Examine every part in sequence
            for (Part part : system.getParts()) {
                // Connect to ending orphans in preceding system/part (if such part exists)
                Part precedingPart = part.getPrecedingInPage();

                if (precedingPart != null) {
                    // Links: Slur -> prevSlur
                    Map<SlurInter, SlurInter> links = part.connectSlursWith(precedingPart);

                    for (Entry<SlurInter, SlurInter> entry : links.entrySet()) {
                        entry.getKey().setExtension(LEFT, entry.getValue());
                        entry.getValue().setExtension(RIGHT, entry.getKey());
                    }
                }
            }
        }
    }

    //--------------//
    // refineLyrics //
    //--------------//
    /**
     * Refine syllables across systems in page
     *
     * @param page provided page
     */
    private void refineLyrics (Page page)
    {
        for (SystemInfo system : page.getSystems()) {
            for (Inter inter : system.getSig().inters(LyricLineInter.class)) {
                LyricLineInter line = (LyricLineInter) inter;
                line.refineLyricSyllables();
            }
        }
    }
}
