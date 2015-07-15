//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.beam;

import omr.math.Population;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code BeamsStep} implements <b>BEAMS</b> step, which uses the spots produced
 * by an image closing operation to retrieve all possible beam interpretations.
 *
 * @author Hervé Bitteur
 */
public class BeamsStep
        extends AbstractSystemStep<BeamsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BeamsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamsStep object.
     */
    public BeamsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        new BeamsBuilder(system, context.gapMap.get(system)).buildBeams(); // -> Beams
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet sheet,
                             Context context)
            throws StepException
    {
        // Cumulate system results
        Population vGaps = new Population();

        for (Population pop : context.gapMap.values()) {
            vGaps.includePopulation(pop);
        }

        if (vGaps.getCardinality() > 0) {
            logger.info("{}InterBeam{{}}", sheet.getLogPrefix(), vGaps);
        }

        sheet.setBeamGaps(vGaps);
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Perform a closing operation on the whole image with a disk shape as the structure
     * element to point out concentrations of foreground pixels (for beams essentially).
     *
     * @return the populated context
     */
    @Override
    protected Context doProlog (Collection<SystemInfo> systems,
                                Sheet sheet)
    {
        // Retrieve significant spots for the whole sheet
        new SpotsBuilder(sheet).buildSheetSpots();

        // Allocate map to collect vertical gaps
        Map<SystemInfo, Population> gapMap = new TreeMap<SystemInfo, Population>();

        for (SystemInfo system : sheet.getSystems()) {
            gapMap.put(system, new Population());
        }

        return new Context(gapMap);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public static class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Beam group vertical gaps, per system. */
        public final Map<SystemInfo, Population> gapMap;

        //~ Constructors ---------------------------------------------------------------------------
        public Context (Map<SystemInfo, Population> gapMap)
        {
            this.gapMap = gapMap;
        }
    }
}
