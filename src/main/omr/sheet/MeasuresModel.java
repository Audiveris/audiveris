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

import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.visitor.ScoreFixer;

import omr.step.StepException;

import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>MeasuresModel</code> is in charge of building measures at the
 * sheet level. Most of the processing is delegated to instances of the class
 * {@link MeasuresBuilder} that work at the system level.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
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
    // allocateScoreStructure //
    //------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Parts and Staves
     */
    public void allocateScoreStructure ()
        throws StepException
    {
        // Allocate score
        sheet.createScore();

        // Systems
        for (SystemInfo system : sheet.getSystems()) {
            system.allocateScoreStructure();
        }

        // Define score parts
        defineScoreParts();
    }

    //------------------------//
    // completeScoreStructure //
    //------------------------//
    public void completeScoreStructure ()
        throws StepException
    {
        // Update score internal data
        sheet.getScore()
             .accept(new ScoreFixer(true));

        reportResults();
    }

    //-----------------//
    // chooseRefSystem //
    //-----------------//
    /**
     * Look for the first largest system (according to its number of parts)
     * @return the largest system
     * @throws omr.step.StepException
     */
    private SystemInfo chooseRefSystem ()
        throws StepException
    {
        int        NbOfParts = 0;
        SystemInfo refSystem = null;

        for (SystemInfo systemInfo : sheet.getSystems()) {
            int nb = systemInfo.getScoreSystem()
                               .getParts()
                               .size();

            if (nb > NbOfParts) {
                NbOfParts = nb;
                refSystem = systemInfo;
            }
        }

        if (refSystem == null) {
            throw new StepException("No system found");
        }

        return refSystem;
    }

    //------------------//
    // defineScoreParts //
    //------------------//
    /**
     * From system part, define the score parts
     * @throws StepException
     */
    private void defineScoreParts ()
        throws StepException
    {
        // Take the best representative system
        ScoreSystem refSystem = chooseRefSystem()
                                    .getScoreSystem();

        // Build the ScorePart list based on the parts of the ref system
        sheet.getScore()
             .createPartListFrom(refSystem);

        // Now examine each system as compared with the ref system
        // We browse through the parts "bottom up"
        List<ScorePart> partList = sheet.getScore()
                                        .getPartList();
        final int       nbScoreParts = partList.size();

        for (SystemInfo systemInfo : sheet.getSystems()) {
            ScoreSystem    system = systemInfo.getScoreSystem();
            List<TreeNode> systemParts = system.getParts();
            final int      nbp = systemParts.size();

            for (int ip = 0; ip < nbp; ip++) {
                ScorePart  global = partList.get(nbScoreParts - 1 - ip);
                SystemPart sp = (SystemPart) systemParts.get(nbp - 1 - ip);
                sp.setScorePart(global);
                sp.setId(global.getId());
            }
        }
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
