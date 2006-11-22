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
import omr.glyph.Shape.Range;

import omr.score.visitor.ScoreCleaner;

import omr.sheet.PixelPoint;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.Rectangle;
import java.util.Comparator;

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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreBuilder.class);

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

    /** The current system */
    private System system;

    /** The current systempart */
    private SystemPart systemPart;

    /** The current staff */
    private Staff staff;

    /** The current point in current system */
    private SystemPoint center;

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
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Build the score information, system after system, glyph after glyph.
     * Nota: Only local tests can be performed here, global ones are performed
     * via the {@link omr.score.visitor.ScoreChecker}.
     */
    public void buildInfo ()
    {
        // First, cleanup the score
        scoreCleanup();

        // Perhaps, order the glyphs within each system ?
        // TBD

        // Ordered translations
        translate(new ClefTranslator());
        translate(new TimeTranslator());
        translate(new KeyTranslator());
        translate(new BeamTranslator());
        translate(new ChordTranslator());

        ///translate(new ChordTranslator());

        // Update score view if any
        if (score.getView() != null) {
            score.getView()
                 .getScrollPane()
                 .getComponent()
                 .repaint();
        }
    }

    //-------------//
    // getPriority //
    //-------------//
    /**
     * Report the 'priority index' of this glyph for its translating into score
     * entity. <b>NOTA</b>: low value means high priority.
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
        score.accept(new ScoreCleaner());
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Perform translation on all glyphs, with a certain translation engine
     *
     * @param translator the actual translation engine
     */
    private void translate (Translator translator)
    {
        for (SystemInfo systemInfo : sheet.getSystems()) {
            system = systemInfo.getScoreSystem();
            translateSystem(translator);
        }

        // Final processing if any
        translator.completeScore();
    }

    //-----------------//
    // translateSystem //
    //-----------------//
    private void translateSystem (Translator translator)
    {
        for (Glyph glyph : system.getInfo()
                                 .getGlyphs()) {
            if (glyph.isWellKnown() && (glyph.getShape() != CLUTTER)) {
                // Check for glyph relevance
                if (translator.isrelevant(glyph)) {
                    // Determine part/staff containment
                    translator.computeLocation(glyph);
                    // Perform the translation on this glyph
                    translator.translate(glyph);
                }
            }
        }

        // Processing at end of system if any
        translator.completeSystem();
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // Translator //
    //------------//
    /**
     * Class <code>Translator</code> is an abstract class that defines the
     * pattern for every translation engine
     */
    private abstract class Translator
    {
        /**
         * Hook for final processing at end of the score
         */
        public void completeScore ()
        {
        }

        /**
         * Hook for final processing at end of each system
         */
        public void completeSystem ()
        {
        }

        /**
         * Compute the location system environment of the provided glyph.
         * Results are written in global variables 'staff', 'staffPoint' and
         * 'measure'.
         *
         * @param glyph the glyph to locate
         */
        public void computeLocation (Glyph glyph)
        {
            center = system.toSystemPoint(glyph.getCenter());
            staff = system.getStaffAt(center);
            systemPart = staff.getPart();
            measure = systemPart.getMeasureAt(center);
        }

        /**
         * Check if provided glyph is relevant
         *
         * @param glyph the glyph at hand
         */
        public abstract boolean isrelevant (Glyph glyph);

        /**
         * Perform the desired translation
         *
         * @param glyph the glyph at hand
         */
        public abstract void translate (Glyph glyph);
    }

    //----------------//
    // BeamTranslator //
    //----------------//
    private class BeamTranslator
        extends Translator
    {
        @Override
        public void computeLocation (Glyph glyph)
        {
            // Staff, measure and staff point need specific processing
            // We use the attached stem(s) to determine proper containment
            if (glyph.getLeftStem() != null) {
                super.computeLocation(glyph.getLeftStem());
            } else if (glyph.getRightStem() != null) {
                super.computeLocation(glyph.getRightStem());
            } else {
                logger.warning(
                    "Beam glyph " + glyph.getId() + " with no attached stem");
            }
        }

        public boolean isrelevant (Glyph glyph)
        {
            return (glyph.getShape() == BEAM) ||
                   (glyph.getShape() == BEAM_HOOK);
        }

        public void translate (Glyph glyph)
        {
            Beam.populate(glyph, measure);
        }
    }

    //-----------------//
    // ChordTranslator //
    //-----------------//
    private class ChordTranslator
        extends Translator
    {
        @Override
        public void completeSystem ()
        {
            if (logger.isFineEnabled()) {
                dumpSystemSlots();
            }

            // Allocate proper chords in every slot
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                for (TreeNode mn : part.getMeasures()) {
                    Measure measure = (Measure) mn;

                    for (Slot slot : measure.getSlots()) {
                        slot.allocateChords();
                    }
                }
            }
        }

        public boolean isrelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Shape.Rests.contains(shape) ||
                   Shape.NoteHeads.contains(shape) ||
                   Shape.Notes.contains(shape);
        }

        public void translate (Glyph glyph)
        {
            Chord.populate(glyph, measure, center);
        }

        private void dumpSystemSlots ()
        {
            // Dump all measure slots
            logger.fine(system.toString());

            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                logger.fine(part.toString());

                for (TreeNode mn : part.getMeasures()) {
                    Measure measure = (Measure) mn;

                    logger.fine(measure.toString());

                    for (Slot slot : measure.getSlots()) {
                        logger.fine(slot.toString());
                    }
                }
            }
        }
    }

    //----------------//
    // ClefTranslator //
    //----------------//
    private class ClefTranslator
        extends Translator
    {
        public boolean isrelevant (Glyph glyph)
        {
            return Shape.Clefs.contains(glyph.getShape());
        }

        public void translate (Glyph glyph)
        {
            Clef.populate(glyph, measure, staff, center);
        }
    }

    //---------------//
    // KeyTranslator //
    //---------------//
    private class KeyTranslator
        extends Translator
    {
        @Override
        public void completeSystem ()
        {
            KeySignature.verifySystemKeys(system);

            //            // Dummy
            //            if (false) {
            //                int is = 0;
            //
            //                for (TreeNode node : systemInfo.getScoreSystem()
            //                                               .getStaves()) {
            //                    Staff staff = (Staff) node;
            //                    int   im = 0;
            //
            //                    for (TreeNode n : staff.getMeasures()) {
            //                        Measure measure = (Measure) n;
            //
            //                        if (im > 0) {
            //                            int k = (is * 3) + im;
            //
            //                            if (Math.abs(k) <= 7) {
            //                                KeySignature sig = new KeySignature(
            //                                    measure,
            //                                    -k);
            //                            }
            //                        }
            //
            //                        im++;
            //                    }
            //
            //                    is++;
            //                }
            //            }
        }

        public boolean isrelevant (Glyph glyph)
        {
            return (glyph.getShape() == SHARP) || (glyph.getShape() == FLAT);
        }

        public void translate (Glyph glyph)
        {
            // Key signature or just accidental ?
            KeySignature.populate(glyph, measure, staff, center);
        }
    }

    //----------------//
    // TimeTranslator //
    //----------------//
    private class TimeTranslator
        extends Translator
    {
        public boolean isrelevant (Glyph glyph)
        {
            return Shape.Times.contains(glyph.getShape());
        }

        public void translate (Glyph glyph)
        {
            TimeSignature.populate(glyph, measure, staff, center);
        }
    }
}
