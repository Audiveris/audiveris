//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B e a m s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
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
        new BeamsBuilder(system, context.distancemap.get(system)).buildBeams(); // -> Beams
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
        Population distances = new Population();

        for (Population pop : context.distancemap.values()) {
            distances.includePopulation(pop);
        }

        if (distances.getCardinality() > 0) {
            logger.info("{}BeamDistance{{}}", sheet.getLogPrefix(), distances);

            sheet.getScale().setBeamDistance(distances.getMeanValue(), distances.getStandardDeviation());
        }

    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Perform a closing operation on the whole image with a disk shape as the structure
     * element to point out concentrations of foreground pixels (meant for beams).
     *
     * @return the populated context
     */
    @Override
    protected Context doProlog (Collection<SystemInfo> systems,
                                Sheet sheet)
    {
        // Retrieve significant spots for the whole sheet
        new SpotsBuilder(sheet).buildSheetSpots();

        // Allocate map to collect vertical distances
        Map<SystemInfo, Population> distanceMap = new TreeMap<SystemInfo, Population>();

        for (SystemInfo system : sheet.getSystems()) {
            distanceMap.put(system, new Population());
        }

        return new Context(distanceMap);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    protected static class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Beam group vertical distances, per system. */
        public final Map<SystemInfo, Population> distancemap;

        //~ Constructors ---------------------------------------------------------------------------
        public Context (Map<SystemInfo, Population> distanceMap)
        {
            this.distancemap = distanceMap;
        }
    }
}
