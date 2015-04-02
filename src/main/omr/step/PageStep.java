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
import omr.score.PageReduction;
import omr.score.entity.Page;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.MeasureFiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code PageStep} handles connections between systems in a page.
 * <ul>
 * <li>Physical system Part instances are abstracted into logical ScorePart instances.
 * For systems where a given ScorePart has no representative Part, a corresponding dummy Part is
 * inserted (with its proper staves and minimal measures).</li>
 * <li>Slurs are connected across systems.</li>
 * <li>Tied voices.</li>
 * <li>Refined lyric syllables (?) </li>
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
                //                system.connectSystemInitialSlurs();
                //                system.connectTiedVoices();
                //                system.refineLyricSyllables();
            }

            // Refine voices ids (and thus colors) across all systems of the page
            ///new PageVoiceFixer(page).refine();
            // Merge / renumber measure stacks within the page
            new MeasureFixer().process(page);

            // Populate each measure with its data
            for (SystemInfo system : page.getSystems()) {
                new MeasureFiller(system).process();
            }
        }
    }
}
