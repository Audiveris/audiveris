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

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.Collections;
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

    //~ Instance fields --------------------------------------------------------

    /** The score we are populating */
    private Score score;

    /** The related sheet */
    private Sheet sheet;

    /** The current system */
    private System currentSystem;

    /** The current systempart */
    private SystemPart currentPart;

    /** The current staff */
    private Staff currentStaff;

    /** The current point in current system */
    private SystemPoint currentCenter;

    /** The current measure */
    private Measure currentMeasure;

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

        // Update score view if any
        if (score.getView() != null) {
            score.getView()
                 .getScrollPane()
                 .getComponent()
                 .repaint();
        }
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    private void deassignGlyph (Glyph glyph)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Deassigning " + glyph);
        }

        // AIE AIE AIE TBD, should not depend on a UI element !!!
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
            currentSystem = systemInfo.getScoreSystem();
            translateSystem(translator);
        }

        // Final score processing if any
        translator.completeScore();
    }

    //-----------------//
    // translateSystem //
    //-----------------//
    private void translateSystem (Translator translator)
    {
        for (Glyph glyph : currentSystem.getInfo()
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
         * Results are written in global variables currentXXX.
         *
         * @param glyph the glyph to locate
         */
        public void computeLocation (Glyph glyph)
        {
            currentCenter = currentSystem.toSystemPoint(glyph.getCenter());
            currentStaff = currentSystem.getStaffAt(currentCenter);
            currentPart = currentStaff.getPart();
            currentMeasure = currentPart.getMeasureAt(currentCenter);
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
                super.computeLocation(glyph); // Backup alternative...
            }
        }

        public boolean isrelevant (Glyph glyph)
        {
            return (glyph.getShape() == BEAM) ||
                   (glyph.getShape() == BEAM_HOOK);
        }

        public void translate (Glyph glyph)
        {
            Beam.populate(glyph, currentMeasure);
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
            for (TreeNode node : currentSystem.getParts()) {
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
            Chord.populate(glyph, currentMeasure, currentCenter);
        }

        private void dumpSystemSlots ()
        {
            // Dump all measure slots
            logger.fine(currentSystem.toString());

            for (TreeNode node : currentSystem.getParts()) {
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
        @Override
        public void completeSystem ()
        {
            // Sort the clefs in any measure, according to staff number
            for (TreeNode node : currentSystem.getParts()) {
                SystemPart part = (SystemPart) node;

                for (TreeNode mn : part.getMeasures()) {
                    Measure measure = (Measure) mn;
                    Collections.sort(
                        measure.getClefs(),
                        MeasureNode.staffComparator);
                }
            }
        }

        public boolean isrelevant (Glyph glyph)
        {
            return Shape.Clefs.contains(glyph.getShape());
        }

        public void translate (Glyph glyph)
        {
            Clef.populate(glyph, currentMeasure, currentStaff, currentCenter);
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
            KeySignature.verifySystemKeys(currentSystem);

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
            KeySignature.populate(
                glyph,
                currentMeasure,
                currentStaff,
                currentCenter);
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
            TimeSignature.populate(
                glyph,
                currentMeasure,
                currentStaff,
                currentCenter);
        }
    }
}
