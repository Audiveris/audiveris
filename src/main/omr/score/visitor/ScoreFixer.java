//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e F i x e r                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Measure;
import omr.score.Score;
import static omr.score.ScoreConstants.*;
import omr.score.ScorePoint;
import omr.score.Staff;
import omr.score.System;

import omr.util.Dumper;
import omr.util.Logger;

import java.awt.Point;

/**
 * Class <code>ScoreFixer</code> visits the score hierarchy to fix
 * internal data.
 * Run computations on the tree of score, systems, etc, so that all display
 * data, such as origins and widths are available for display use.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreFixer
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreFixer.class);

    //~ Constructors -----------------------------------------------------------

    //------------//
    // ScoreFixer //
    //------------//
    /**
     * Creates a new ScoreFixer object.
     */
    public ScoreFixer ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        // Adjust measure abscissae
        measure.resetAbscissae();

        // Set measure id
        Measure prevMeasure = (Measure) measure.getPreviousSibling();

        if (prevMeasure != null) {
            measure.setId(prevMeasure.getId() + 1);
        } else {
            // Look for a previous system
            System system = measure.getSystem();
            System prevSystem = (System) system.getPreviousSibling();

            if (prevSystem != null) {
                measure.setId(
                    prevSystem.getFirstPart().getLastMeasure().getId() + 1);
            } else {
                measure.setId(measure.isImplicit() ? 0 : 1);
            }
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        // Display origin for the staff
        System system = staff.getSystem();
        Point  sysorg = system.getDisplayOrigin();
        staff.setDisplayOrigin(
            new ScorePoint(
                sysorg.x,
                sysorg.y +
                (staff.getTopLeft().y - staff.getSystem().getTopLeft().y)));

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (System system)
    {
        // Is there a Previous System ?
        System     prevSystem = (System) system.getPreviousSibling();
        ScorePoint origin = new ScorePoint();

        if (prevSystem == null) {
            // Very first system in the score
            origin.move(SCORE_INIT_X, SCORE_INIT_Y);
        } else {
            // Not the first system
            origin.setLocation(prevSystem.getDisplayOrigin());
            origin.translate(
                prevSystem.getDimension().width - 1 + INTER_SYSTEM,
                0);
        }

        system.setDisplayOrigin(origin);

        if (logger.isFineEnabled()) {
            Dumper.dump(system, "Computed");
        }

        return true;
    }
}
