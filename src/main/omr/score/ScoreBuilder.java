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

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.sheet.PixelPoint;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;
import omr.util.Logger;

import static omr.glyph.Shape.*;

import java.awt.Rectangle;

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
        //logger.setLevel (Level.FINE);

        // Browse each system info of the sheet
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();
            logger.fine("System " + systemInfo);
            for (Glyph glyph : systemInfo.getGlyphs()) {
                Shape shape = glyph.getShape();
                if (glyph.isWellKnown() && shape != CLUTTER) {
                    logger.fine(glyph.toString());

                    Rectangle box = glyph.getContourBox();
                    PixelPoint pp = new PixelPoint(box.x + box.width/2,
                            box.y + box.height/2);
                    PagePoint p = sheet.getScale().pixelsToUnits(pp);
                    Staff staff = system.getStaffAt(p);

                    StaffPoint staffPoint = staff.toStaffPoint(p);

                    Measure measure = staff.getMeasureAt(staffPoint);

                    if (Shape.Clefs.contains(shape)) {
                        Clef clef = new Clef
                                (measure, staff, shape, staffPoint, 2);
                        measure.getClefs().add(clef);
                    }
                }
            }
        }

        score.getView().getScrollPane().getComponent().repaint();
    }
}
