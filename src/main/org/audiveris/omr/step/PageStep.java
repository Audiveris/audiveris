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
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITaskList;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

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

    private static final Logger logger = LoggerFactory.getLogger(PageStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageStep} object.
     */
    public PageStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                Page page = inter.getSig().getSystem().getPage();

                inter.accept(new Impactor(page));

                break;
            }
        }
    }

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

    //~ Inner Classes ------------------------------------------------------------------------------
    private class Impactor
            extends AbstractInterVisitor
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Page page;

        //~ Constructors ---------------------------------------------------------------------------
        public Impactor (Page page)
        {
            this.page = page;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void visit (AugmentationDotInter inter)
        {
            Voices.refinePage(page);
        }

        @Override
        public void visit (BeamHookInter inter)
        {
            Voices.refinePage(page);
        }

        @Override
        public void visit (BeamInter inter)
        {
            Voices.refinePage(page);
        }

        @Override
        public void visit (FlagInter inter)
        {
            Voices.refinePage(page);
        }

        @Override
        public void visit (HeadInter inter)
        {
            Voices.refinePage(page);
        }

        @Override
        public void visit (LyricItemInter inter)
        {
            refineLyrics(page);
        }

        @Override
        public void visit (LyricLineInter inter)
        {
            refineLyrics(page);
        }

        @Override
        public void visit (RestInter inter)
        {
            Voices.refinePage(page);
        }

        @Override
        public void visit (SlurInter inter)
        {
            connectSlurs(page);
        }

        @Override
        public void visit (TupletInter inter)
        {
            Voices.refinePage(page);
        }
    }
}
