//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C l e a n e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.visitor.AbstractScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ScoreCleaner} can visit the score hierarchy to get rid
 * of all measure items except barlines, ready for a new score
 * translation.
 *
 * @author Hervé Bitteur
 */
public class ScoreCleaner
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScoreCleaner.class);

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // ScoreCleaner //
    //--------------//
    /**
     * Creates a new ScoreCleaner object.
     */
    public ScoreCleaner ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        try {
            logger.debug("Cleaning up {}", system);

            // Remove recorded translations for all system glyphs
            for (Glyph glyph : system.getInfo()
                    .getGlyphs()) {
                if (glyph.getShape() != Shape.LEDGER) {
                    glyph.clearTranslations();
                }
            }

            system.acceptChildren(this);
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + system,
                    ex);
        }

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart systemPart)
    {
        try {
            if (systemPart.isDummy()) {
                systemPart.getParent()
                        .getChildren()
                        .remove(systemPart);

                return false;
            } else {
                // Remove slurs and wedges
                systemPart.cleanupNode();

                return true;
            }
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + systemPart,
                    ex);
        }

        return false;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        try {
            measure.cleanupNode();
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + measure,
                    ex);
        }

        return false;
    }
}
