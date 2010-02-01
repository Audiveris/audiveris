//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C l e a n e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.Glyph;

import omr.log.Logger;

import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.visitor.AbstractScoreVisitor;

/**
 * Class <code>ScoreCleaner</code> can visit the score hierarchy to get rid of
 * all measure items except barlines, ready for a new score translation.
 *
 * @author Hervé Bitteur
 */
public class ScoreCleaner
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreCleaner.class);

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

    //    //-------------//
    //    // visit Score //
    //    //-------------//
    //    @Override
    //    public boolean visit (Score score)
    //    {
    //        score.cleanupNode();
    //
    //        return true;
    //    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Cleaning up " + system);
        }

        // Remove recorded translations for all system glyphs
        for (Glyph glyph : system.getInfo()
                                 .getGlyphs()) {
            glyph.clearTranslations();
        }

        system.acceptChildren(this);
        system.cleanupNode();

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart systemPart)
    {
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
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        measure.cleanupNode();

        return false;
    }
}
