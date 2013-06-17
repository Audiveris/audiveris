//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e C o l o r i z e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.glyph.facets.Glyph;

import omr.score.Score;
import omr.score.entity.Barline;
import omr.score.entity.SystemPart;
import omr.score.visitor.AbstractScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

/**
 * Class {@code ScoreColorizer} can visit the score hierarchy for
 * colorization (assigning colors) of related sections in the Sheet display.
 *
 * @author Hervé Bitteur
 */
public class ScoreColorizer
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScoreColorizer.class);

    //~ Instance fields --------------------------------------------------------
    /** The color to use */
    private final Color color;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new ScoreColorizer object.
     *
     * @param color the color to use
     */
    public ScoreColorizer (Color color)
    {
        this.color = color;
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        try {
            logger.debug("Colorizing {}", barline);

            for (Glyph glyph : barline.getGlyphs()) {
                glyph.colorize(color);
            }
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + barline,
                    ex);
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        try {
            logger.debug("Colorizing score ...");
            score.acceptChildren(this);
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + score,
                    ex);
        }

        return false;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        try {
            // Set color for the starting bar line, if any
            Barline startingBarline = part.getStartingBarline();

            if (startingBarline != null) {
                startingBarline.accept(this);
            }
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + part,
                    ex);
        }

        return true;
    }
}
