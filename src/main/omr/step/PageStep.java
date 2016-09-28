//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a g e S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.step;

import omr.score.MeasureFixer;
import omr.score.Page;
import omr.score.PageReduction;

import omr.sheet.Part;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Voices;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;
import omr.sig.inter.LyricLineInter;
import omr.sig.inter.SlurInter;

import static omr.util.HorizontalSide.*;

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
            for (SystemInfo system : page.getSystems()) {
                final SIGraph sig = system.getSig();

                connectSystemInitialSlurs(system);

                // Refine syllables across systems
                for (Inter inter : sig.inters(LyricLineInter.class)) {
                    LyricLineInter line = (LyricLineInter) inter;
                    line.refineLyricSyllables();
                }
            }

            // Refine voices IDs (and thus colors) across all systems of the page
            Voices.refinePage(page);

            // Merge / renumber measure stacks within the page
            new MeasureFixer().process(page);
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
}
