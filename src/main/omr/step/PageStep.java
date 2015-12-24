//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a g e S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.MeasureFixer;
import omr.score.Page;
import omr.score.PageReduction;

import omr.sheet.Part;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;
import omr.sig.inter.LyricLineInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

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
    public void doit (Collection<SystemInfo> systems,
                      Sheet sheet)
            throws StepException
    {
        for (Page page : sheet.getPages()) {
            // Connect parts across systems in the page
            new PageReduction(page).reduce();

            // Inter-system connections
            for (SystemInfo system : page.getSystems()) {
                final SIGraph sig = system.getSig();

                connectSystemInitialSlurs(system);

                //                system.connectTiedVoices();
                //
                // Refine syllables across systems
                for (Inter inter : sig.inters(LyricLineInter.class)) {
                    LyricLineInter line = (LyricLineInter) inter;
                    line.refineLyricSyllables();
                }
            }

            // Refine voices ids (and thus colors) across all systems of the page
            ///new PageVoiceFixer(page).refine();
            //
            // Merge / renumber measure stacks within the page
            new MeasureFixer().process(page);
        }
    }

    //---------------------------//
    // connectSystemInitialSlurs //
    //---------------------------//
    /**
     * Retrieve the connections between the (orphan) slurs at the beginning of the
     * provided system and the (orphan) slurs at the end of the previous system if any.
     */
    private void connectSystemInitialSlurs (SystemInfo system)
    {
        SystemInfo prevSystem = system.getPrecedingInPage();

        if (prevSystem != null) {
            // Examine every part in sequence
            for (Part part : system.getParts()) {
                // Ending orphans in preceding system/part (if such part exists)
                part.connectSlursWith(part.getPrecedingInPage());
            }
        }
    }
}
