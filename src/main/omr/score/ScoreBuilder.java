//-----------------------------------------------------------------------//
//                                                                       //
//                        S c o r e B u i l d e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import java.util.logging.Level;
import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.sheet.StaveInfo;
import omr.sheet.SystemInfo;
import omr.util.Logger;

/**
 * Class <code>ScoreBuilder</code> is in charge of translating each
 * relevant glyph found in the sheet into its score counterpart.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreBuilder
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(ScoreBuilder.class);

    //~ Instance variables ------------------------------------------------

    private Score score;
    private Sheet sheet;

    //~ Methods -----------------------------------------------------------

    //--------------//
    // ScoreBuilder //
    //--------------//
    /**
     * Creates a new instance of ScoreBuilder
     * @param score the score entity to be filled
     * @param sheet the sheet entity to be browsed
     */
    public ScoreBuilder (Score score,
                         Sheet sheet)
    {
        this.score = score;
        this.sheet = sheet;
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo()
    {
        logger.setLevel (Level.FINE);

        // Browse each system info of the sheet
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();
            logger.fine("System " + systemInfo);
            for (Glyph glyph : systemInfo.getGlyphs ()) {
                if (glyph.isWellKnown () &&
                        glyph.getShape () != Shape.CLUTTER) {
                    logger.fine(glyph.toString ());
                    int y = glyph.getCentroid ().y;
                    int si = sheet.getStaveIndexAtY (y);
                    StaveInfo staveInfo = sheet.getStaves ().get(si);
                    int uY = staveInfo.getScale ().pixelsToUnits(y);
                    ///Stave stave = system.getStaveAtY(uY);

                }
            }
        }
    }
}
