//----------------------------------------------------------------------------//
//                                                                            //
//                         M e a s u r e s M o d e l                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.log.Logger;

import omr.score.visitor.ScoreFixer;

import omr.step.StepException;

import net.jcip.annotations.NotThreadSafe;

import java.util.*;

/**
 * Class <code>MeasuresModel</code> is in charge of building measures at the
 * sheet level. Most of the processing is delegated to instances of the class
 * {@link MeasuresBuilder} that work at the system level.
 *
 * <p>Ths class is meant to be called by one single thread, thus it is not
 * thread safe</p>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@NotThreadSafe
public class MeasuresModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MeasuresModel.class);

    //~ Instance fields --------------------------------------------------------

    /** The related sheet */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MeasuresModel object.
     *
     * @param sheet the related sheet
     */
    public MeasuresModel (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //------------------------//
    // completeScoreStructure //
    //------------------------//
    /**
     * Run some final processing (such as Measure numbering, System contour)
     * on the whole Score structure, now that all entities down to Measure
     * instances have been allocated
     */
    public void completeScoreStructure ()
        throws StepException
    {
        // Update score internal data
        sheet.getScore()
             .accept(new ScoreFixer(true));

        reportResults();
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder    sb = new StringBuilder();

        List<SystemInfo> systems = sheet.getSystems();
        int              nb = systems.get(systems.size() - 1)
                                     .getScoreSystem()
                                     .getLastPart()
                                     .getLastMeasure()
                                     .getId();

        if (nb > 0) {
            sb.append(nb)
              .append(" measure");

            if (nb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no measure found");
        }

        logger.info(sb.toString());
    }
}
