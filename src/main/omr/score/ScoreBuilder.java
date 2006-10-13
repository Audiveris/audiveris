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

import omr.score.visitor.CleaningVisitor;

import omr.sheet.PixelPoint;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    private static final Logger            logger = Logger.getLogger(
        ScoreBuilder.class);

    /** A comparator to order glyph according to their translation order */
    private static final Comparator<Glyph> glyphComparator = new Comparator<Glyph>() {
        public int compare (Glyph g1,
                            Glyph g2)
        {
            // First, order on translation interest
            if (g1.getShape() != g2.getShape()) {
                return getPriority(g1) - getPriority(g2);
            } else {
                // Second, order on abscissa
                return g1.getCentroid().x - g2.getCentroid().x;
            }
        }
    };

    /** (Partial) array of ranges ordered  by priority order */
    private static final Range[] orderedRanges = new Range[] {
                                                     //
    NoteHeads, //
    Notes, //
    Stems, //
    Bars, //
    Clefs, //
    Times, //
    Accidentals, // Must be after alterable notes
    Octaves, //
    Rests, //
    Flags, //
    Articulations, //
    Dynamics, //
    Ornaments, //
    Pedals, //
    Barlines, //
    Logicals, //
    Physicals, //
    Garbage
                                                 };

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
     * Build the score information, system after system, glyph after glyph.
     * Nota: Only local tests can be performed here, global ones are performed
     * via the {@link CheckingVisitor}.
     */
    public void buildInfo ()
    {
        // First, cleanup the score
        scoreCleanup();

        // Browse each system info of the sheet
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();

            if (logger.isFineEnabled()) {
                logger.fine("System " + systemInfo);
            }

            translateSystem(systemInfo);
        }

        // Update score view
        score.getView()
             .getScrollPane()
             .getComponent()
             .repaint();
    }

    //-------------//
    // getPriority //
    //-------------//
    /**
     * Report the 'priority index' of this glyph for its translating into score
     * entity. NOTA: low value means high priority.
     *
     * @param glyph the glyph to be ordered based on its shape
     * @return the priority index
     */
    private static int getPriority (Glyph glyph)
    {
        Shape shape = glyph.getShape();
        int   i = 0;

        for (; i < orderedRanges.length; i++) {
            if (orderedRanges[i].contains(shape)) {
                return i;
            }
        }

        // Last ones, if not part of the ordered ranges
        return i;
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
        score.accept(new CleaningVisitor());
    }

    //-----------------//
    // translateSystem //
    //-----------------//
    private void translateSystem (SystemInfo systemInfo)
    {
        // Sort the glyph collection, to ease its processing
        List<Glyph> orderedGlyphs = new ArrayList<Glyph>(
            systemInfo.getGlyphs());
        Collections.sort(orderedGlyphs, glyphComparator);

        // Translate each glyph in turn
        for (Glyph glyph : orderedGlyphs) {
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
                staff = systemInfo.getScoreSystem()
                                  .getStaffAt(p);
                staffPoint = staff.toStaffPoint(p);
                measure = staff.getMeasureAt(staffPoint);

                boolean success = true;

                // Processing is based on glyph shape
                if (Shape.Clefs.contains(shape)) {
                    // Clef
                    success = Clef.populate(glyph, measure, staffPoint);
                } else if (Shape.Times.contains(shape)) {
                    // Time
                    success = TimeSignature.populate(glyph, measure, scale);
                } else if ((shape == SHARP) || (shape == FLAT)) {
                    // Key signature or just accidental ?
                } else if (shape == NATURAL) {
                    // Accidental
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
}
