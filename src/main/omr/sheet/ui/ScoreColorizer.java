//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e C o l o r i z e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;

import omr.log.Logger;

import omr.score.Score;
import omr.score.entity.Barline;
import omr.score.entity.SystemPart;
import omr.score.visitor.AbstractScoreVisitor;

import java.awt.Color;

/**
 * Class <code>ScoreColorizer</code> can visit the score hierarchy for
 * colorization (assigning colors) of related sections in the Sheet display.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreColorizer
    extends AbstractScoreVisitor
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

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Colorizing " + barline);
        }

        for (Glyph glyph : barline.getGlyphs()) {
            glyph.colorize(lag, viewIndex, color);
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Colorizing score ...");
        }

        score.acceptChildren(this);

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        // Set color for the starting bar line, if any
        Barline startingBarline = part.getStartingBarline();

        if (startingBarline != null) {
            startingBarline.accept(this);
        }

        return true;
    }
}
