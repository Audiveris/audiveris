//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e B u i l d e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.sheet.PixelPoint;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;

import java.awt.Rectangle;

/**
 * Class <code>ScoreBuilder</code> is in charge of translating each relevant
 * glyph found in the sheet into its score counterpart.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(ScoreBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** The score we are populating */
    private Score score;

    /** The related sheet */
    private Sheet sheet;

    /** The sheet mean scale */
    private Scale scale;

    /** The current staff */
    private Staff staff;

    /** The current point in current staff */
    private StaffPoint staffPoint;

    /** The current measure */
    private Measure measure;

    //~ Constructors -----------------------------------------------------------

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

        scale = sheet.getScale();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Build the score information
     */
    public void buildInfo ()
    {
        // First, cleanup the score
        scoreCleanup();

        // Browse each system info of the sheet
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();
            logger.fine("System " + systemInfo);

            for (Glyph glyph : systemInfo.getGlyphs()) {
                Shape shape = glyph.getShape();

                if (glyph.isWellKnown() && (shape != CLUTTER)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Translating " + glyph.toString());
                    }

                    // Retrieve related score items
                    Rectangle  box = glyph.getContourBox();
                    PixelPoint pp = new PixelPoint(
                        box.x + (box.width / 2),
                        box.y + (box.height / 2));
                    PagePoint  p = scale.toPagePoint(pp);
                    staff = system.getStaffAt(p);
                    staffPoint = staff.toStaffPoint(p);
                    measure = staff.getMeasureAt(staffPoint);

                    boolean success = true;

                    // Processing is based on shape
                    if (Shape.Clefs.contains(shape)) {
                        // Clef
                        success = Clef.populate(
                            shape,
                            measure,
                            staff,
                            staffPoint);
                    } else if (Shape.Times.contains(shape)) {
                        // Time
                        success = TimeSignature.populate(
                            shape,
                            measure,
                            staff,
                            scale,
                            glyph);
                    } else {
                        // Basic processing
                        switch (shape) {
                        default :
                        }
                    }

                    if (!success) {
                        deassignGlyph(glyph);
                    }
                }
            }
        }

        // Update score view
        score.getView()
             .getScrollPane()
             .getComponent()
             .repaint();
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    private void deassignGlyph (Glyph glyph)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Deassigning " + glyph);
        }

        sheet.getSymbolsEditor()
             .deassignGlyphShape(glyph);
    }

    //--------------//
    // scoreCleanup //
    //--------------//
    private void scoreCleanup ()
    {
        // Keep only the systems, slurs, staves, measures, barlines
        score.cleanupChildren();
    }
}
