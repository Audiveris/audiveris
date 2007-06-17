//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e B u i l d e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.OmrExecutors;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.visitor.ScoreCleaner;
import omr.score.visitor.ScoreFixer;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.SignallingRunnable;
import omr.util.TreeNode;

import java.util.*;
import java.util.concurrent.*;

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

    //-----------------//
    // translateSystem //
    //-----------------//
    /**
     * Build the score information, system after system, glyph after glyph.
     * Nota: Only local tests can be performed here, global ones are performed
     * via the {@link omr.score.visitor.ScoreChecker}.
     */
    public void buildInfo ()
    {
        //        final long startTime = java.lang.System.currentTimeMillis();
        sheet.getErrorsEditor()
             .clear();

        // Should we process systems in parallel or sequentially?
        if (OmrExecutors.useParallelism() &&
            (OmrExecutors.getNumberOfCpus() > 1)) {
            buildParallelInfo();
        } else {
            buildSequentialInfo();
        }

        // Score processing once all systems are completed
        checkSlurConnections();
        checkImplicitMeasures();
        score.accept(new ScoreFixer());

        //        final long stopTime = java.lang.System.currentTimeMillis();
        //        logger.info("Score translated in " + (stopTime - startTime) + " ms");

        // Update score view if any
        if (score.getView() != null) {
            score.getView()
                 .repaint();
        }
    }

    //-------------------//
    // buildParallelInfo //
    //-------------------//
    /**
     * Systems are built in parallel
     */
    private void buildParallelInfo ()
    {
        Executor       executor = OmrExecutors.getHighExecutor();
        CountDownLatch doneSignal = new CountDownLatch(
            sheet.getSystems().size());

        // First, cleanup the score, keeping only the systems, staves,
        // measures, barlines
        for (SystemInfo systemInfo : sheet.getSystems()) {
            final System       system = systemInfo.getScoreSystem();
            SignallingRunnable work = new SignallingRunnable(
                doneSignal,
                new Runnable() {
                        public void run ()
                        {
                            new SystemBuilder(system).translateSystem();
                        }
                    });
            executor.execute(work);
        }

        // Wait for end of work
        try {
            doneSignal.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    //---------------------//
    // buildSequentialInfo //
    //---------------------//
    /**
     * Systems are built in sequence
     */
    private void buildSequentialInfo ()
    {
        // First, cleanup the score, keeping only the systems, staves,
        // measures, barlines
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();
            new SystemBuilder(system).translateSystem();
        }
    }

    //-----------------------//
    // checkImplicitMeasures //
    //-----------------------//
    /**
     * Check for an implicit measure at the beginning of the score.  This
     * version looks only at the very first measure of the score, which is
     * too restrictive. TBD.
     */
    private void checkImplicitMeasures ()
    {
        // On the very first system, all parts have their very first measure
        // ending too short with the same value (or filled by whole rest)
        System  system = score.getFirstSystem();
        Integer finalDuration = null;

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;
            Measure    measure = part.getFirstMeasure();

            for (int voice = 0; voice < measure.getVoicesNumber(); voice++) {
                Integer voiceFinal = measure.getFinalDuration(voice);

                if (voiceFinal != null) {
                    if (finalDuration == null) {
                        finalDuration = voiceFinal;
                    } else if (!voiceFinal.equals(finalDuration)) {
                        logger.fine("No introduction measure");

                        return;
                    }
                }
            }
        }

        if ((finalDuration != null) && (finalDuration < 0)) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Found an introduction measure for " + finalDuration);
            }

            // Flag these measures as implicit, and get rid of their final
            // forward marks if any
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;
                part.getFirstMeasure()
                    .setImplicit();
            }
        }
    }

    //----------------------//
    // checkSlurConnections //
    //----------------------//
    /**
     * Make attempts to connect slurs between systems
     */
    private void checkSlurConnections ()
    {
        for (SystemInfo systemInfo : sheet.getSystems()) {
            System system = systemInfo.getScoreSystem();
            Slur.retrieveSlurConnections(system);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------------//
    // RebuildException //
    //------------------//
    /**
     * Exception used to trigger a rebuild
     */
    public static class RebuildException
        extends RuntimeException
    {
        /**
         * Annotate the exception with a message
         * @param message a meaningful explanation
         */
        public RebuildException (String message)
        {
            super(message);
        }
    }

    //---------------//
    // SystemBuilder //
    //---------------//
    /**
     * Class <code>SystemBuilder</code> does all translation tasks for one
     * system only. Additional work that needs availability of several
     * systems is performed at higher level by the translateSystem() method.
     */
    private class SystemBuilder
    {
        /** The current system */
        private System system;

        /** The current systempart */
        private SystemPart currentPart;

        /** The current staff */
        private Staff currentStaff;

        /** The current point in current system */
        private SystemPoint currentCenter;

        /** The current measure */
        private Measure currentMeasure;

        //---------------//
        // SystemBuilder //
        //---------------//
        SystemBuilder (System system)
        {
            this.system = system;
        }

        //-----------------//
        // translateSystem //
        //-----------------//
        public void translateSystem ()
        {
            // First, cleanup the system, staves, measures, barlines, ...
            system.accept(new ScoreCleaner());

            // Translations in proper order

            // Whole score impact
            //-------------------

            // Clef
            logger.fine("Starting ClefTranslator...");
            translate(new ClefTranslator());

            // Time signature
            logger.fine("Starting TimeTranslator...");
            translate(new TimeTranslator());

            // Key
            logger.fine("Starting KeyTranslator...");
            translate(new KeyTranslator());

            // Measure impact
            //---------------

            // Slot, Chord, Note
            logger.fine("Starting ChordTranslator...");
            translate(new ChordTranslator());

            // Slur
            logger.fine("Starting SlurTranslator...");
            translate(new SlurTranslator());

            // Beam (-> chord), BeamGroup
            logger.fine("Starting BeamTranslator...");
            translate(new BeamTranslator());

            // Flag (-> chord)
            logger.fine("Starting FlagTranslator...");
            translate(new FlagTranslator());

            // Augmentation dots (-> chord)
            logger.fine("Starting DotTranslator...");
            translate(new DotTranslator());

            // Tuplets
            logger.fine("Starting TupletTranslator...");
            translate(new TupletTranslator());

            // Finalize measure voices & durations
            logger.fine("Starting MeasureTranslator...");
            translate(new MeasureTranslator());

            // Local impact
            //-------------

            // Accidental (-> note)
            logger.fine("Starting AccidentalTranslator...");
            translate(new AccidentalTranslator());

            // Fermata
            logger.fine("Starting FermataTranslator...");
            translate(new FermataTranslator());

            // Arpeggiate
            logger.fine("Starting ArpeggiateTranslator...");
            translate(new ArpeggiateTranslator());

            // Crescendo / decrescendo
            logger.fine("Starting WedgeTranslator...");
            translate(new WedgeTranslator());

            // Pedal on / off
            logger.fine("Starting PedalTranslator...");
            translate(new PedalTranslator());

            // Segno
            logger.fine("Starting SegnoTranslator...");
            translate(new SegnoTranslator());

            // Coda
            logger.fine("Starting CodaTranslator...");
            translate(new CodaTranslator());

            // Ornaments
            logger.fine("Starting OrnamentTranslator...");
            translate(new OrnamentTranslator());

            // Dynamics
            logger.fine("Starting DynamicsTranslator...");
            translate(new DynamicsTranslator());
        }

        //-----------//
        // translate //
        //-----------//
        /**
         * Drive the translation at system level of certain glyphs as
         * handled by the provided translator
         *
         * @param translator the specific translator for this task
         */
        private void translate (Translator translator)
        {
            // Browse the system collection of glyphs
            for (Glyph glyph : system.getInfo()
                                     .getGlyphs()) {
                Shape shape = glyph.getShape();

                if (glyph.isWellKnown() &&
                    (shape != Shape.CLUTTER) &&
                    (!glyph.isTranslated() ||
                    // HeadAndFlags are translated twice (for head, for flag)
                Shape.HeadAndFlags.contains(shape))) {
                    // Check for glyph relevance
                    if (translator.isRelevant(glyph)) {
                        // Determine part/staff/measure containment
                        translator.computeLocation(glyph);
                        // Perform the translation on this glyph
                        translator.translate(glyph);
                    }
                }
            }

            // Processing at end of system if any
            translator.completeSystem();
        }

        //------------//
        // Translator //
        //------------//
        /**
         * Class <code>Translator</code> is an abstract class that defines the
         * pattern for every translation engine
         */
        private abstract class Translator
        {
            public Translator ()
            {
                if (logger.isFineEnabled()) {
                    logger.fine("Creating translator " + this);
                }
            }

            /**
             * Check if provided glyph is relevant
             * @param glyph the glyph at hand
             * @return true if the glyph at hand is relevant for the translator
             */
            public abstract boolean isRelevant (Glyph glyph);

            /**
             * Specific browsing of a given measure
             * @param measure the given measure
             */
            public void browse (Measure measure)
            {
            }

            /**
             * Hook for final processing at end of the system
             */
            public void completeSystem ()
            {
                browseSystemMeasures();
            }

            /**
             * Compute the location system environment of the provided glyph.
             * Results are written in global variables currentXXX.
             * @param glyph the glyph to locate
             */
            public void computeLocation (Glyph glyph)
            {
                currentCenter = system.toSystemPoint(glyph.getCenter());
                currentStaff = system.getStaffAt(currentCenter);
                currentPart = currentStaff.getPart();
                currentMeasure = currentPart.getMeasureAt(currentCenter);
            }

            /**
             * Perform the desired translation
             * @param glyph the glyph at hand
             */
            public abstract void translate (Glyph glyph);

            /**
             * Pattern to browse through all measures in the current system
             */
            public void browseSystemMeasures ()
            {
                for (TreeNode node : system.getParts()) {
                    SystemPart part = (SystemPart) node;

                    for (TreeNode mn : part.getMeasures()) {
                        Measure measure = (Measure) mn;

                        try {
                            browse(measure);
                        } catch (Exception ex) {
                            logger.warning(
                                measure.getContextString() +
                                " Exception in measure browsing",
                                ex);
                        }
                    }
                }
            }
        }

        //----------------------//
        // AccidentalTranslator //
        //----------------------//
        private class AccidentalTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                return Shape.Accidentals.contains(glyph.getShape());
            }

            public void translate (Glyph glyph)
            {
                Note.populateAccidental(glyph, currentMeasure, currentCenter);
            }
        }

        //----------------------//
        // ArpeggiateTranslator //
        //----------------------//
        private class ArpeggiateTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                return glyph.getShape() == Shape.ARPEGGIATO;
            }

            public void translate (Glyph glyph)
            {
                Arpeggiate.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //----------------//
        // BeamTranslator //
        //----------------//
        private class BeamTranslator
            extends Translator
        {
            @Override
            public void browse (Measure measure)
            {
                // Allocate beams to chords, and populate beam groups
                BeamGroup.populate(measure);
            }

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
                    currentMeasure.addError(
                        glyph,
                        "Beam glyph with no attached stem");
                    super.computeLocation(glyph); // Backup alternative...
                }
            }

            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return Shape.Beams.contains(shape) && (shape != Shape.SLUR);
            }

            public void translate (Glyph glyph)
            {
                BeamItem.populate(glyph, currentMeasure);
            }
        }

        //-----------------//
        // ChordTranslator //
        //-----------------//
        private class ChordTranslator
            extends Translator
        {
            @Override
            public void browse (Measure measure)
            {
                // Allocate proper chords in every slot
                int id = 0;

                for (Slot slot : measure.getSlots()) {
                    slot.setId(++id);
                    slot.allocateChordsAndNotes();
                }
            }

            @Override
            public void completeSystem ()
            {
                super.completeSystem();

                if (logger.isFineEnabled()) {
                    dumpSystemSlots();
                }
            }

            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return Shape.Rests.contains(shape) ||
                       Shape.NoteHeads.contains(shape) ||
                       Shape.Notes.contains(shape) ||
                       Shape.HeadAndFlags.contains(shape);
            }

            public void translate (Glyph glyph)
            {
                Slot.populate(glyph, currentMeasure, currentCenter);
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
            @Override
            public void browse (Measure measure)
            {
                // Sort the clefs according to containing staff
                Collections.sort(
                    measure.getClefs(),
                    MeasureNode.staffComparator);
            }

            public boolean isRelevant (Glyph glyph)
            {
                return Shape.Clefs.contains(glyph.getShape());
            }

            public void translate (Glyph glyph)
            {
                Clef.populate(
                    glyph,
                    currentMeasure,
                    currentStaff,
                    currentCenter);
            }
        }

        //----------------//
        // CodaTranslator //
        //----------------//
        private class CodaTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return shape == Shape.CODA;
            }

            public void translate (Glyph glyph)
            {
                Coda.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //---------------//
        // DotTranslator //
        //---------------//
        private class DotTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                return glyph.getShape() == Shape.DOT;
            }

            public void translate (Glyph glyph)
            {
                Chord.populateDot(glyph, currentMeasure, currentCenter);
            }
        }

        //--------------------//
        // DynamicsTranslator //
        //--------------------//
        private class DynamicsTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return Shape.Dynamics.contains(shape) &&
                       (shape != Shape.CRESCENDO) &&
                       (shape != Shape.DECRESCENDO);
            }

            public void translate (Glyph glyph)
            {
                Dynamics.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //-------------------//
        // FermataTranslator //
        //-------------------//
        private class FermataTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                return (glyph.getShape() == Shape.FERMATA) ||
                       (glyph.getShape() == Shape.FERMATA_BELOW);
            }

            public void translate (Glyph glyph)
            {
                Fermata.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //----------------//
        // FlagTranslator //
        //----------------//
        private class FlagTranslator
            extends Translator
        {
            @Override
            public void browse (Measure measure)
            {
                if (logger.isFineEnabled()) {
                    // Print flag/beam value of each chord
                    logger.fine("Flag/Beams for " + measure.getContextString());

                    for (TreeNode node : measure.getChords()) {
                        Chord chord = (Chord) node;
                        logger.fine(chord.toString());

                        if (chord.getBeams()
                                 .size() > 0) {
                            logger.fine("   Beams:" + chord.getBeams().size());
                        }

                        if (chord.getFlagsNumber() > 0) {
                            logger.fine("   Flags:" + chord.getFlagsNumber());
                        }

                        // Just to be sure
                        if ((chord.getBeams()
                                  .size() * chord.getFlagsNumber()) != 0) {
                            chord.addError(
                                "Inconsistent Flag/Beam configuration");
                        }
                    }
                }
            }

            @Override
            public void computeLocation (Glyph glyph)
            {
                // We use the attached stem(s) to determine proper containment
                if (glyph.getLeftStem() != null) {
                    super.computeLocation(glyph.getLeftStem());
                } else if (glyph.getRightStem() != null) {
                    super.computeLocation(glyph.getRightStem());
                } else {
                    system.addError(
                        glyph,
                        "Flag glyph " + glyph.getId() +
                        " with no attached stem");
                    super.computeLocation(glyph); // Backup alternative...
                }
            }

            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return Shape.Flags.contains(shape) ||
                       Shape.HeadAndFlags.contains(shape);
            }

            public void translate (Glyph glyph)
            {
                Chord.populateFlag(glyph, currentMeasure);
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
            }

            public boolean isRelevant (Glyph glyph)
            {
                return (glyph.getShape() == Shape.SHARP) ||
                       (glyph.getShape() == Shape.FLAT);
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

        //-------------------//
        // MeasureTranslator //
        //-------------------//
        private class MeasureTranslator
            extends Translator
        {
            @Override
            public void browse (Measure measure)
            {
                // Determine the voices within this measure
                Slot.buildVoices(measure);

                // Check duration sanity in this measure
                measure.checkDuration();
            }

            public boolean isRelevant (Glyph glyph)
            {
                return false;
            }

            public void translate (Glyph glyph)
            {
                // Not called
            }
        }

        //--------------------//
        // OrnamentTranslator //
        //--------------------//
        private class OrnamentTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                final Shape shape = glyph.getShape();

                return (shape == Shape.TR) || (shape == Shape.TURN) ||
                       (shape == Shape.MORDENT) ||
                       (shape == Shape.INVERTED_MORDENT);
            }

            public void translate (Glyph glyph)
            {
                Ornament.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //-----------------//
        // PedalTranslator //
        //-----------------//
        private class PedalTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return (shape == Shape.PEDAL_MARK) ||
                       (shape == Shape.PEDAL_UP_MARK);
            }

            public void translate (Glyph glyph)
            {
                Pedal.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //-----------------//
        // SegnoTranslator //
        //-----------------//
        private class SegnoTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return shape == Shape.SEGNO;
            }

            public void translate (Glyph glyph)
            {
                Segno.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //----------------//
        // SlurTranslator //
        //----------------//
        private class SlurTranslator
            extends Translator
        {
            @Override
            public void computeLocation (Glyph glyph)
            {
                // We do not compute location here
            }

            public boolean isRelevant (Glyph glyph)
            {
                return (glyph.getShape() == Shape.SLUR);
            }

            public void translate (Glyph glyph)
            {
                Slur.populate(glyph, system);
            }
        }

        //----------------//
        // TimeTranslator //
        //----------------//
        private class TimeTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
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

        //-------------------//
        // TupletTranslator //
        //-------------------//
        private class TupletTranslator
            extends Translator
        {
            public boolean isRelevant (Glyph glyph)
            {
                return Shape.Tuplets.contains(glyph.getShape());
            }

            public void translate (Glyph glyph)
            {
                Tuplet.populate(glyph, currentMeasure, currentCenter);
            }
        }

        //-----------------//
        // WedgeTranslator //
        //-----------------//
        private class WedgeTranslator
            extends Translator
        {
            @Override
            public void computeLocation (Glyph glyph)
            {
                // Take the left edge for glyph center
                PixelRectangle box = glyph.getContourBox();
                currentCenter = system.toSystemPoint(
                    new PixelPoint(box.x, box.y + (box.height / 2)));
                currentStaff = system.getStaffAt(currentCenter); // Bof!
                currentPart = currentStaff.getPart();
                currentMeasure = currentPart.getMeasureAt(currentCenter);
            }

            public boolean isRelevant (Glyph glyph)
            {
                Shape shape = glyph.getShape();

                return (shape == Shape.CRESCENDO) ||
                       (shape == Shape.DECRESCENDO);
            }

            public void translate (Glyph glyph)
            {
                Wedge.populate(glyph, currentMeasure, currentCenter);
            }
        }
    }
}
