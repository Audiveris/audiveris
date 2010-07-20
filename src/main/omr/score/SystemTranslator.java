//----------------------------------------------------------------------------//
//                                                                            //
//                      S y s t e m T r a n s l a t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.Main;

import omr.glyph.Shape;
import static omr.glyph.ShapeRange.*;
import omr.glyph.facets.Glyph;
import omr.glyph.text.Sentence;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
import omr.score.entity.Barline;
import omr.score.entity.BeamGroup;
import omr.score.entity.BeamItem;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.DotTranslation;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.Measure;
import omr.score.entity.MeasureNode;
import omr.score.entity.Note;
import omr.score.entity.Ornament;
import omr.score.entity.Pedal;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slot;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.TimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Wedge;
import omr.score.midi.MidiAgent;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>SystemTranslator</code> performs all translation tasks for one
 * system only.
 */
public class SystemTranslator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SystemTranslator.class);

    //~ Instance fields --------------------------------------------------------

    /** The physical system */
    private final SystemInfo systemInfo;

    /** The logical system */
    private ScoreSystem system;

    /** The current systempart */
    private SystemPart currentPart;

    /** The current staff */
    private Staff currentStaff;

    /** The current point in current system */
    private SystemPoint currentCenter;

    /** The current measure */
    private Measure currentMeasure;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SystemTranslator object.
     *
     * @param systemInfo the dedicated system
     */
    public SystemTranslator (SystemInfo systemInfo)
    {
        this.systemInfo = systemInfo;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // translateFinal //
    //----------------//
    /**
     * Final actions to be done, starting on this first impacted system,
     * until the very last system in the score
     */
    public void translateFinal ()
    {
        system = systemInfo.getScoreSystem();

        if (logger.isFineEnabled()) {
            logger.fine("buildFinal starting from " + system);
        }

        final Score      score = system.getScore();
        final Sheet      sheet = score.getSheet();

        // Get the (sub) list of all systems for final processing
        List<SystemInfo> systems = sheet.getSystems()
                                        .subList(
            system.getId() - 1,
            sheet.getSystems().size());

        for (SystemInfo info : systems) {
            ScoreSystem syst = info.getScoreSystem();

            syst.fillMissingParts();
            syst.retrieveSlurConnections();
            syst.refineLyricSyllables();
        }

        score.accept(new ScoreTimeFixer());
        score.accept(new TimeSignatureFixer());
        score.accept(new ScoreFixer());

        // Invalidate score data within MidiAgent, if needed
        if (Main.getGui() != null) {
            try {
                if (MidiAgent.getInstance()
                             .getScore() == score) {
                    MidiAgent.getInstance()
                             .reset();
                }
            } catch (Exception ex) {
                logger.warning("Cannot access Midi agent", ex);
            }

            // Update score views if any
            score.updateViews();
        }
    }

    //-----------------//
    // translateSystem //
    //-----------------//
    /**
     * This is where glyph information is translated to score entity information
     */
    public void translateSystem ()
    {
        // Translations in proper order

        // Whole score impact
        //-------------------
        // Clef
        translate(new ClefTranslator());
        // Time signature
        translate(new TimeTranslator());
        // Key
        translate(new KeyTranslator());

        // Measure impact
        //---------------
        // Slot, Chord, Note
        translate(new ChordTranslator());
        // Slur
        translate(new SlurTranslator());
        // Beam (-> chord), BeamGroup
        translate(new BeamTranslator());
        // Flag (-> chord)
        translate(new FlagTranslator());
        // Dots (-> chord) as staccato / augmentation / repeat
        translate(new DotTranslator());
        // Tuplets
        translate(new TupletTranslator());
        // Finalize measure ties, voices & durations, barlines
        translate(new MeasureTranslator());

        // Local impact
        //-------------
        // Accidental (-> note)
        translate(new AccidentalTranslator());
        // Fermata
        translate(new FermataTranslator());
        // Arpeggiate
        translate(new ArpeggiateTranslator());
        // Articulation
        translate(new ArticulationTranslator());
        // Crescendo / decrescendo
        translate(new WedgeTranslator());
        // Pedal on / off
        translate(new PedalTranslator());
        // Segno
        translate(new SegnoTranslator());
        // Coda
        translate(new CodaTranslator());
        // Ornaments
        translate(new OrnamentTranslator());
        // Dynamics
        translate(new DynamicsTranslator());
        // Text (-> Lyrics, Directions, etc...)
        translate(new TextTranslator());
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
        system = systemInfo.getScoreSystem();

        // Browse the system collection of glyphs
        translator.translateGlyphs();

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
        //~ Instance fields ----------------------------------------------------

        /** Name of this translator (for debugging) */
        protected final String name;

        //~ Constructors -------------------------------------------------------

        public Translator (String name)
        {
            super();
            this.name = name;

            if (logger.isFineEnabled()) {
                logger.fine("Creating " + this);
            }
        }

        //~ Methods ------------------------------------------------------------

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
            currentCenter = system.toSystemPoint(glyph.getLocation());
            currentStaff = system.getStaffAt(currentCenter);
            currentPart = currentStaff.getPart();
            currentMeasure = currentPart.getMeasureAt(currentCenter);
        }

        @Override
        public String toString ()
        {
            return "{Translator " + name + "}";
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

        /**
         * Browsing on every glyph within the system
         */
        protected void translateGlyphs ()
        {
            for (Glyph glyph : system.getInfo()
                                     .getGlyphs()) {
                Shape shape = glyph.getShape();

                if (glyph.isWellKnown() &&
                    (shape != Shape.CLUTTER) &&
                    (!glyph.isTranslated() || HeadAndFlags.contains(shape))) {
                    // Check for glyph relevance
                    if (isRelevant(glyph)) {
                        // Determine part/staff/measure containment
                        computeLocation(glyph);

                        try {
                            // Perform the translation on this glyph
                            translate(glyph);
                        } catch (Exception ex) {
                            logger.warning(
                                "Error translating glyph #" + glyph.getId() +
                                " by " + this,
                                ex);
                        }
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
        //~ Constructors -------------------------------------------------------

        public AccidentalTranslator ()
        {
            super("Accidental");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return Accidentals.contains(glyph.getShape());
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
        //~ Constructors -------------------------------------------------------

        public ArpeggiateTranslator ()
        {
            super("Arpeggiate");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return glyph.getShape() == Shape.ARPEGGIATO;
        }

        public void translate (Glyph glyph)
        {
            Arpeggiate.populate(glyph, currentMeasure, currentCenter);
        }
    }

    //------------------------//
    // ArticulationTranslator //
    //------------------------//
    private class ArticulationTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public ArticulationTranslator ()
        {
            super("Articulation");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            // ARPEGGIATO is processed by ArpeggiateTranslator.
            // DOT-shape staccato is processed by DotTranslation,
            // while STACCATO-shape staccato is processed here
            return Articulations.contains(shape) &&
                   (shape != Shape.ARPEGGIATO);
        }

        public void translate (Glyph glyph)
        {
            Articulation.populate(glyph, currentMeasure, currentCenter);
        }
    }

    //----------------//
    // BeamTranslator //
    //----------------//
    private class BeamTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public BeamTranslator ()
        {
            super("Beam");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Beams.contains(shape) && (shape != Shape.COMBINING_STEM);
        }

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
                super.computeLocation(glyph);
            }
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
        //~ Constructors -------------------------------------------------------

        public ChordTranslator ()
        {
            super("Chord");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Rests.contains(shape) || NoteHeads.contains(shape) ||
                   Notes.contains(shape) || HeadAndFlags.contains(shape);
        }

        @Override
        public void browse (Measure measure)
        {
            // Allocate proper chords in every slot
            int id = 0;

            for (Slot slot : measure.getSlots()) {
                slot.setId(++id);
                slot.allocateChordsAndNotes();
            }

            // Check that slots are not too close to each other
            Scale scale = system.getScale();
            Slot  prevSlot = null;
            int   minSlotSpacing = scale.toUnits(Score.getMinSlotSpacing());
            int   minSpacing = Integer.MAX_VALUE;
            Slot  minSlot = null;

            for (Slot slot : measure.getSlots()) {
                if (prevSlot != null) {
                    int spacing = slot.getX() - prevSlot.getX();

                    if (minSpacing > spacing) {
                        minSpacing = spacing;
                        minSlot = slot;
                    }
                }

                prevSlot = slot;
            }

            if (minSpacing < minSlotSpacing) {
                measure.addError(
                    minSlot.getLocationGlyph(),
                    "Suspicious narrow spacing of slots: " +
                    scale.unitsToFrac(minSpacing));
            }
        }

        @Override
        public void completeSystem ()
        {
            super.completeSystem();

            if (logger.isFineEnabled()) {
                Slot.dumpSystemSlots(system);
            }
        }

        public void translate (Glyph glyph)
        {
            Score score = system.getScore();
            Slot.populate(
                glyph,
                currentMeasure,
                score.hasSlotPolicy() ? score.getSlotPolicy()
                                : Score.getDefaultSlotPolicy());
        }
    }

    //----------------//
    // ClefTranslator //
    //----------------//
    private class ClefTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public ClefTranslator ()
        {
            super("Clef");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return Clefs.contains(glyph.getShape());
        }

        @Override
        public void browse (Measure measure)
        {
            // Sort the clefs according to containing staff
            Collections.sort(measure.getClefs(), MeasureNode.staffComparator);
        }

        public void translate (Glyph glyph)
        {
            Clef.populate(glyph, currentMeasure, currentStaff, currentCenter);
        }
    }

    //----------------//
    // CodaTranslator //
    //----------------//
    private class CodaTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public CodaTranslator ()
        {
            super("Coda");
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public DotTranslator ()
        {
            super("Dot");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return Dots.contains(glyph.getShape());
        }

        public void translate (Glyph glyph)
        {
            DotTranslation.populateDot(glyph, currentMeasure, currentCenter);
        }
    }

    //--------------------//
    // DynamicsTranslator //
    //--------------------//
    private class DynamicsTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public DynamicsTranslator ()
        {
            super("Dynamics");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Dynamics.contains(shape) && (shape != Shape.CRESCENDO) &&
                   (shape != Shape.DECRESCENDO);
        }

        public void translate (Glyph glyph)
        {
            omr.score.entity.Dynamics.populate(
                glyph,
                currentMeasure,
                currentCenter);
        }
    }

    //-------------------//
    // FermataTranslator //
    //-------------------//
    private class FermataTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public FermataTranslator ()
        {
            super("Fermata");
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public FlagTranslator ()
        {
            super("Flag");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Flags.contains(shape) || HeadAndFlags.contains(shape);
        }

        @Override
        public void browse (Measure measure)
        {
            if (logger.isFineEnabled()) {
                // Print flag/beam value of each chord
                logger.fine("Flag/Beams for " + measure.getContextString());

                for (TreeNode node : measure.getChords()) {
                    Chord chord = (Chord) node;
                    logger.fine(chord.toString());

                    if (!chord.getBeams()
                              .isEmpty()) {
                        logger.fine("   Beams:" + chord.getBeams().size());
                    }

                    if (chord.getFlagsNumber() > 0) {
                        logger.fine("   Flags:" + chord.getFlagsNumber());
                    }

                    // Just to be sure
                    if ((chord.getBeams()
                              .size() * chord.getFlagsNumber()) != 0) {
                        chord.addError("Inconsistent Flag/Beam configuration");
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
                    "Flag glyph " + glyph.getId() + " with no attached stem");
                super.computeLocation(glyph);
            }
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
        //~ Constructors -------------------------------------------------------

        public KeyTranslator ()
        {
            super("Key");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return (glyph.getShape() == Shape.SHARP) ||
                   (glyph.getShape() == Shape.FLAT);
        }

        @Override
        public void completeSystem ()
        {
            try {
                KeySignature.verifySystemKeys(system);
            } catch (Exception ex) {
                logger.warning("Error verifying keys for " + system, ex);
            }
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
        //~ Constructors -------------------------------------------------------

        public MeasureTranslator ()
        {
            super("Measure");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return false;
        }

        @Override
        public void browse (Measure measure)
        {
            // Check that a chord is not tied to different slots
            measure.checkTiedChords();
            // Determine the voices within this measure
            measure.buildVoices();
            // Check duration sanity in this measure
            measure.checkDuration();

            // Make sure all barline glyphs point to it
            Barline barline = measure.getBarline();

            if (barline != null) {
                barline.translateGlyphs();
            }
        }

        @Override
        public void completeSystem ()
        {
            super.completeSystem();

            // Make sure all starting barline glyphs point to it
            for (TreeNode pn : system.getParts()) {
                SystemPart part = (SystemPart) pn;
                Barline    barline = part.getStartingBarline();

                if (barline != null) {
                    barline.translateGlyphs();
                }
            }
        }

        public void translate (Glyph glyph)
        {
        }

        @Override
        protected void translateGlyphs ()
        {
        }
    }

    //--------------------//
    // OrnamentTranslator //
    //--------------------//
    private class OrnamentTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public OrnamentTranslator ()
        {
            super("Ornament");
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public PedalTranslator ()
        {
            super("Pedal");
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public SegnoTranslator ()
        {
            super("Segno");
        }

        //~ Methods ------------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

        public SlurTranslator ()
        {
            super("Slur");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return glyph.getShape() == Shape.SLUR;
        }

        @Override
        public void computeLocation (Glyph glyph)
        {
        }

        public void translate (Glyph glyph)
        {
            Slur.populate(glyph, system);
        }
    }

    //----------------//
    // TextTranslator //
    //----------------//
    private class TextTranslator
        extends Translator
    {
        //~ Instance fields ----------------------------------------------------

        SystemRectangle systemBox;

        //~ Constructors -------------------------------------------------------

        public TextTranslator ()
        {
            super("Text");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return glyph.getShape()
                        .isText();
        }

        @Override
        public void completeSystem ()
        {
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;
                part.populateLyricsLines();
                part.mapSyllables();
            }
        }

        @Override
        public void computeLocation (Glyph glyph)
        {
            systemBox = system.toSystemRectangle(glyph.getContourBox());
            currentCenter = new SystemPoint(
                systemBox.x + (systemBox.width / 2),
                systemBox.y + systemBox.height);
            currentStaff = system.getTextStaff(
                glyph.getTextInfo().getTextRole(),
                currentCenter);
            currentPart = currentStaff.getPart();
        }

        public void translate (Glyph glyph)
        {
            Sentence sentence = glyph.getTextInfo()
                                     .getSentence();

            // Translate the sentence here
            // Using the left edge for x and the baseline for y
            if (sentence != null) {
                Text.populate(sentence, sentence.getLocation());
            } else {
                logger.warning(
                    "No sentence for glyph #" + glyph.getId() +
                    ". Cannot translate to score entity.");
            }
        }
    }

    //----------------//
    // TimeTranslator //
    //----------------//
    private class TimeTranslator
        extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public TimeTranslator ()
        {
            super("Time");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return Times.contains(glyph.getShape());
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
        //~ Constructors -------------------------------------------------------

        public TupletTranslator ()
        {
            super("Tuplet");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            return Tuplets.contains(glyph.getShape());
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
        //~ Constructors -------------------------------------------------------

        public WedgeTranslator ()
        {
            super("Wedge");
        }

        //~ Methods ------------------------------------------------------------

        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return (shape == Shape.CRESCENDO) || (shape == Shape.DECRESCENDO);
        }

        @Override
        public void computeLocation (Glyph glyph)
        {
            // Take the left edge for glyph center
            PixelRectangle box = glyph.getContourBox();
            currentCenter = system.toSystemPoint(
                new PixelPoint(box.x, box.y + (box.height / 2)));
            currentStaff = system.getStaffAt(currentCenter);
            // Bof!
            currentPart = currentStaff.getPart();
            currentMeasure = currentPart.getMeasureAt(currentCenter);
        }

        public void translate (Glyph glyph)
        {
            Wedge.populate(glyph, currentMeasure, currentCenter);
        }
    }
}
