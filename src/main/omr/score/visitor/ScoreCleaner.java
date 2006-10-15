//----------------------------------------------------------------------------//
//                                                                            //
//                       C l e a n i n g V i s i t o r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Barline;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Score;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.System;
import omr.score.TimeSignature;

import omr.util.Logger;

/**
 * Class <code>ScoreCleaner</code> can visit the score hierarchy to get rid of
 * all measure items except barlines.
 * 
 * 
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreCleaner
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(
        ScoreCleaner.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreCleaner object.
     */
    public ScoreCleaner ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    public boolean visit (Barline barline)
    {
        return true;
    }

    public boolean visit (Clef clef)
    {
        return true;
    }

    public boolean visit (KeySignature keySignature)
    {
        return true;
    }

    public boolean visit (Measure measure)
    {
        measure.cleanupNode();

        return false;
    }

    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Cleaning up score ...");
        }

        score.acceptChildren(this);

        return false;
    }

    public boolean visit (Slur slur)
    {
        return true;
    }

    public boolean visit (Staff staff)
    {
        return true;
    }

    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    public boolean visit (System system)
    {
        return true;
    }

    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }
}
