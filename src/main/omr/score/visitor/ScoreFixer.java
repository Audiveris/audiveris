//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e F i x e r                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
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
import omr.score.entity.Text;
import static omr.score.ui.ScoreConstants.*;

/**
 * Class <code>ScoreFixer</code> visits the score hierarchy to fix
 * internal data.
 * <ul>
 * <li>Run computations so that all display data, such as origins and widths
 * are available for display use.</li>
 * <li>Reset Measure abscissae</li>
 * <li>Assign Measure ids</li>
 * <li>Compute System contours</li>
 * <li>Compute System display origins</li>
 * </ul>
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

    /** Flag to indicate first pass */
    private boolean firstPass = true;

    /** Contour of the current system */
    private SystemRectangle systemContour;

    /** Retrieve max offset above first part and use it to align first staves */
    private int highestTop = 0;

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
        if (!measure.isDummy()) {
            measure.resetAbscissae();
        }

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

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        firstPass = true;
        score.acceptChildren(this);

        if (logger.isFineEnabled()) {
            logger.fine("highestTop=" + highestTop);
        }

        firstPass = false;
        score.acceptChildren(this);

        return false;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        if (firstPass) {
            // First pass on contained entities to retrieve contours
            // Initialize system contours with staves contours
            systemContour = new SystemRectangle(
                0,
                0,
                system.getDimension().width,
                system.getDimension().height + STAFF_HEIGHT);

            // Perhaps extend this contour with text items
            system.acceptChildren(this);

            // Now add margins
            systemContour.width += (2 * STAFF_MARGIN_WIDTH);
            systemContour.height += (2 * STAFF_MARGIN_HEIGHT);

            // Write down the system contour
            if (logger.isFineEnabled()) {
                logger.fine(
                    "First pass " + system + " contour:" + systemContour);
            }

            system.setContour(systemContour);

            int top = system.getContour().y + system.getDummyOffset();

            if (top < highestTop) {
                highestTop = top;
            }
        } else {
            // Second pass to properly set system display origin
            SystemRectangle contour = system.getContour();

            // Is there a Previous System ?
            ScoreSystem prevSystem = (ScoreSystem) system.getPreviousSibling();
            ScorePoint  origin = new ScorePoint();
            ScorePart   scorePart = system.getFirstPart()
                                          .getScorePart();

            if (HORIZONTAL_LAYOUT) {
                if (prevSystem == null) {
                    // Very first system in the score
                    origin.x = STAFF_MARGIN_WIDTH - contour.x;
                } else {
                    // Not the first system
                    origin.x = (prevSystem.getDisplayOrigin().x +
                               prevSystem.getDimension().width) + INTER_SYSTEM_WIDTH;
                }

                origin.y = STAFF_MARGIN_HEIGHT - highestTop +
                           system.getDummyOffset() +
                           ((scorePart != null)
                            ? scorePart.getDisplayOrdinate() : 0);
            } else {
                if (prevSystem == null) {
                    // Very first system in the score
                    origin.y = STAFF_MARGIN_HEIGHT - contour.y;
                } else {
                    // Not the first system
                    origin.y = (prevSystem.getDisplayOrigin().y +
                               prevSystem.getDimension().height + STAFF_HEIGHT) +
                               INTER_SYSTEM_HEIGHT;
                }

                origin.x = STAFF_MARGIN_WIDTH - contour.x;
            }

            system.setDisplayOrigin(origin);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Second pass " + system + " origin:" +
                    system.getDisplayOrigin());
            }

            firstPass = false;
            system.acceptChildren(this);
        }

        return false;
    }

    //------------//
    // visit Text //
    //------------//
    @Override
    public boolean visit (Text text)
    {
        if (firstPass) {
            // Extends system contour if needed
            Sentence sentence = text.getSentence();

            if (sentence != null) {
                systemContour.add(sentence.getSystemContour());
            }
        }

        return true;
    }
}
