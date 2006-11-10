//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C l e a n e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
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

    //---------------//
    // visit Barline //
    //---------------//
    public boolean visit (Barline barline)
    {
        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    public boolean visit (Beam beam)
    {
        return true;
    }

    //------------//
    // visit Chord //
    //------------//
    public boolean visit (Chord chord)
    {
        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    public boolean visit (Clef clef)
    {
        return true;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    public boolean visit (KeySignature keySignature)
    {
        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    public boolean visit (Measure measure)
    {
        measure.cleanupNode();

        return false;
    }

    //-----------------//
    // visit MusicNode //
    //-----------------//
    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Cleaning up score ...");
        }

        score.acceptChildren(this);

        return false;
    }

    //------------//
    // visit Slur //
    //------------//
    public boolean visit (Slur slur)
    {
        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    public boolean visit (Staff staff)
    {
        return true;
    }

    //-----------------//
    // visit StaffNode //
    //-----------------//
    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    public boolean visit (System system)
    {
        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }
}
