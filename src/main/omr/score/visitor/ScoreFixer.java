//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e F i x e r                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.text.Sentence;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.ScorePoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.Measure;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.Text;
import static omr.score.ui.ScoreConstants.*;

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

    //~ Instance fields --------------------------------------------------------

    /** Flag to assign or not measure ids */
    private final boolean assignMeasureId;

    /** Flag to indicate pass number on the system children */
    private boolean firstSystemPass = true;

    /** Contour of the current system */
    private SystemRectangle systemContour;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // ScoreFixer //
    //------------//
    /**
     * Creates a new ScoreFixer object.
     *
     * @param assignMeasureId Should we assign measure ids?
     */
    public ScoreFixer (boolean assignMeasureId)
    {
        this.assignMeasureId = assignMeasureId;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        // Adjust measure abscissae
        if (!measure.isDummy()) {
            measure.resetAbscissae();
        }

        if (assignMeasureId) {
            // Set measure id, based on a preceding measure, whatever the part
            Measure precedingMeasure = measure.getPreceding();

            // No preceding system?
            if (precedingMeasure == null) {
                ScoreSystem prevSystem = (ScoreSystem) measure.getSystem()
                                                              .getPreviousSibling();

                if (prevSystem != null) { // No preceding part
                    precedingMeasure = prevSystem.getFirstRealPart()
                                                 .getLastMeasure();
                }
            }

            if (precedingMeasure != null) {
                measure.setId(precedingMeasure.getId() + 1);
            } else {
                // Very first measure
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

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        // Initialize system contours with staves contours and margins
        int height = system.getLastPart()
                           .getLastStaff()
                           .getTopLeft().y -
                     system.getFirstPart()
                           .getFirstStaff()
                           .getTopLeft().y;
        systemContour = new SystemRectangle(
            0,
            0,
            system.getDimension().width + (2 * STAFF_MARGIN_WIDTH),
            height + STAFF_HEIGHT + (2 * STAFF_MARGIN_HEIGHT));

        // First pass on contained entities to retrieve contours
        firstSystemPass = true;
        system.acceptChildren(this);

        // Write down the system contour
        if (logger.isFineEnabled()) {
            logger.fine(system + " contour:" + systemContour);
        }

        system.setContour(systemContour);

        // Is there a Previous System ?
        ScoreSystem prevSystem = (ScoreSystem) system.getPreviousSibling();
        ScorePoint  origin = new ScorePoint();

        if (prevSystem == null) {
            // Very first system in the score
            ScorePart scorePart = system.getFirstPart()
                                        .getScorePart();
            origin.x = -systemContour.x + STAFF_MARGIN_WIDTH;
            origin.y = -systemContour.y + STAFF_MARGIN_HEIGHT +
                       ((scorePart != null) ? scorePart.getDisplayOrdinate() : 0);
        } else {
            // Not the first system
            origin.x = (prevSystem.getDisplayOrigin().x +
                       prevSystem.getDimension().width) + INTER_SYSTEM;
            origin.y = (prevSystem.getDisplayOrigin().y +
                       prevSystem.getFirstPart().getFirstStaff().getTopLeft().y) -
                       prevSystem.getFirstRealPart()
                                 .getFirstStaff()
                                 .getTopLeft().y;
        }

        system.setDisplayOrigin(origin);

        // Second pass on contained entities to set display origins
        firstSystemPass = false;
        system.acceptChildren(this);

        return false;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        if (!firstSystemPass) {
            // Display origin for the staff
            ScoreSystem system = staff.getSystem();
            Point       sysorg = system.getDisplayOrigin();
            staff.setDisplayOrigin(
                new ScorePoint(
                    sysorg.x,
                    sysorg.y +
                    (staff.getTopLeft().y - staff.getSystem().getTopLeft().y)));
        }

        return true;
    }

    //------------//
    // visit Text //
    //------------//
    @Override
    public boolean visit (Text text)
    {
        if (firstSystemPass) {
            // Extends system contour if needed
            Sentence sentence = text.getSentence();

            if (sentence != null) {
                systemContour.add(sentence.getSystemContour());

                //                logger.info("adding sentence " + sentence);
                //                logger.info("systemContour=" + systemContour);
            }
        }

        return true;
    }
}
