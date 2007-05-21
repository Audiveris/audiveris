//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C l e a n e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.Glyph;

import omr.score.Measure;
import omr.score.Score;
import omr.score.ScorePart;
import omr.score.System;
import omr.score.SystemPart;

import omr.util.Logger;

/**
 * Class <code>ScoreCleaner</code> can visit the score hierarchy to get rid of
 * all measure items except barlines, ready for a new score translation.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (System system)
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

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart systemPart)
    {
        // Remove slurs and wedges
        systemPart.cleanupNode();

        return true;
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
