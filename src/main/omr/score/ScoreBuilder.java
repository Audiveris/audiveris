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

import omr.constant.ConstantSet;
import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.sheet.PixelPoint;
import omr.sheet.Scale;
import omr.sheet.Sheet;
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
    private static final Constants constants = new Constants();


    //~ Instance variables ------------------------------------------------

    private Score score;
    private Sheet sheet;
    private Scale scale;

    private Staff staff;
    private StaffPoint staffPoint;
    private Measure measure;

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
        scale = sheet.getScale();
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo()
    {
        // First, cleanup the score
        scoreCleanup();

        // Browse each system info of the sheet
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();
            logger.fine("System " + systemInfo);
            for (Glyph glyph : systemInfo.getGlyphs()) {
                Shape shape = glyph.getShape();
                if (glyph.isWellKnown() && shape != CLUTTER) {
                    if (logger.isFineEnabled()) {
                        logger.fine(glyph.toString());
                    }

                    Rectangle box = glyph.getContourBox();
                    PixelPoint pp = new PixelPoint(box.x + box.width/2,
                                                   box.y + box.height/2);
                    PagePoint p = scale.toPagePoint(pp);
                    staff = system.getStaffAt(p);
                    staffPoint = staff.toStaffPoint(p);
                    measure = staff.getMeasureAt(staffPoint);

                    boolean success = true;

                    // Processing based on shape
                    if (Shape.Clefs.contains(shape)) {
                        success = processClef(shape);
                    } else if (Shape.Times.contains(shape)) {
                        success = processTime(shape, glyph);
                    } else {
                        // Basic processing
                        switch (shape) {
                        default:
                        }
                    }

                    if (!success) {
                        deassignGlyph(glyph);
                    }
                }
            }
        }

        score.getView().getScrollPane().getComponent().repaint();
    }

    //-------------//
    // processClef //
    //-------------//
    private boolean processClef (Shape shape)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_OTTAVA_ALTA :
        case G_CLEF_OTTAVA_BASSA:
            new Clef(measure, staff, shape, staffPoint, 2);
            return true;

        case F_CLEF:
        case F_CLEF_OTTAVA_ALTA:
        case F_CLEF_OTTAVA_BASSA:
            new Clef(measure, staff, shape, staffPoint, -2);
            return true;

        default:
            logger.warning("No implementation yet for " + shape);
            return false;
        }
    }

    //-------------//
    // processTime //
    //-------------//
    private boolean processTime (Shape shape,
                                 Glyph glyph)
    {
        // First, some basic tests
        // Horizontal distance since beginning of measure
        StaffPoint center = staff.computeGlyphCenter(glyph, scale);
        int unitDx = center.x - measure.getLeftX();
        if (unitDx < scale.toUnits(constants.minTimeOffset)) {
            if (logger.isFineEnabled()) {
                logger.fine("Too small offset for time signature" +
                        " (glyph#" + glyph.getId() + ")");
            }
            return false;
        }

        // Then, processing depends on single/multi time signature
        if (SingleTimes.contains(shape)) {
            return processSingleTime(shape, glyph);
        } else {
            return processMultiTime(shape, glyph);
        }
    }

    //-------------------//
    // processSingleTime //
    //-------------------//
    private boolean processSingleTime (Shape shape,
                                       Glyph glyph)
    {
        TimeSignature ts = measure.getTimeSignature();
        if (ts != null) {
            // Check we are not too far from this first time signature part
            StaffPoint center = staff.computeGlyphCenter
                (glyph, scale);
            double unitDist = center.distance(ts.getCenter());
            double unitMax = scale.toUnitsDouble
                (constants.maxTimeDistance);
            if (unitDist <= unitMax) {
                ts.addGlyph(glyph);
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine("Time signature part" +
                                " (glyph#" + glyph.getId() + ")" +
                                " too far from previous one");
                }
                return false;
            }
        } else {
            ts = new TimeSignature
                (measure, staff, scale);
            ts.addGlyph(glyph);
            measure.setTimeSignature(ts);
        }

        return true;
    }

    //------------------//
    // processMultiTime //
    //------------------//
    private boolean processMultiTime (Shape shape,
                                      Glyph glyph)
    {
        TimeSignature ts = measure.getTimeSignature();
        if (ts == null) {
            ts = new TimeSignature
                (measure, staff, scale);
            ts.addGlyph(glyph);
            measure.setTimeSignature(ts);
        } else {
            if (logger.isFineEnabled()) {
                logger.fine("Second whole time signature" +
                            " (glyph#" + glyph.getId() + ")" +
                            " in the same measure");
            }
            return false;
        }

        return true;
    }

    //--------------//
    // scoreCleanup //
    //--------------//
    private void scoreCleanup()
    {
        // Keep only the systems, slurs, staves, measures, barlines
        score.cleanupChildren();
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    private void deassignGlyph (Glyph glyph)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Deassigning " + glyph);
        }

        sheet.getSymbolsBuilder().assignGlyphShape(glyph, null);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Scale.Fraction maxTimeDistance = new Scale.Fraction
            (4d,
             "Maximum distance between two parts of a time signature");

        Scale.Fraction minTimeOffset = new Scale.Fraction
            (3d,
             "Minimum offset for a time signature since start of measure");

        Constants ()
        {
            initialize();
        }
    }
}
