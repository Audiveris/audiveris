//----------------------------------------------------------------------------//
//                                                                            //
//                     C o l o r i z i n g V i s i t o r                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.GlyphLag;

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

import omr.stick.Stick;

import omr.util.Logger;

import java.awt.Color;

/**
 * Class <code>ScoreColorizer</code> can visit the score hierarchy for
 * colorization (assigning colors) of related sections in the Sheet display.
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreColorizer
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreColorizer.class);

    //~ Instance fields --------------------------------------------------------

    /** The lag to be colorized */
    private final GlyphLag lag;

    /** The provided lag view index */
    private final int viewIndex;

    /** The color to use */
    private final Color color;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreColorizer object.
     *
     * @param lag       the lag to be colorized
     * @param viewIndex the provided lag view index
     * @param color     the color to use
     */
    public ScoreColorizer (GlyphLag lag,
                           int      viewIndex,
                           Color    color)
    {
        this.lag = lag;
        this.viewIndex = viewIndex;
        this.color = color;
    }

    //~ Methods ----------------------------------------------------------------

    public boolean visit (Barline barline)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Colorizing " + barline);
        }

        for (Stick stick : barline.getSticks()) {
            stick.colorize(lag, viewIndex, color);
        }

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
        return true;
    }

    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Colorizing score ...");
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
        // Set color for the starting bar line, if any
        Barline startingBarline = staff.getStartingBarline();

        if (startingBarline != null) {
            startingBarline.accept(this);
        }

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
