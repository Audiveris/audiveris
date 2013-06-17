//----------------------------------------------------------------------------//
//                                                                            //
//                      S y s t e m T r a n s l a t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.Shape;
import static omr.glyph.ShapeSet.*;
import omr.glyph.facets.Glyph;

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
import omr.score.entity.Page;
import omr.score.entity.Pedal;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.TimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Wedge;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.text.TextLine;

import omr.util.HorizontalSide;
import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code SystemTranslator} performs all translation tasks for
 * a given system, translating glyphs to score entities.
 */
public class SystemTranslator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SystemTranslator.class);

    //~ Instance fields --------------------------------------------------------
    /** The physical system. */
    private final SystemInfo systemInfo;

    /** The logical system. */
    private ScoreSystem system;

    /** The current systempart. */
    private SystemPart currentPart;

    /** The current staff. */
    private Staff currentStaff;

    /** The current point in current system. */
    private Point currentCenter;

    /** The current measure. */
    private Measure currentMeasure;

    //~ Constructors -----------------------------------------------------------
    //
    //------------------//
    // SystemTranslator //
    //------------------//
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
    //
    //----------------//
    // translateFinal //
    //----------------//
    /**
     * Final actions to be done, starting on this first impacted system,
     * until the very last system in the page.
     */
    public void translateFinal ()
    {
        system = systemInfo.getScoreSystem();
        logger.debug("buildFinal starting from {}", system);

        final Page page = system.getPage();
        final Sheet sheet = page.getSheet();

        // Connect parts across systems
        new PageReduction(page).reduce();

        // Get the (sub) list of all systems for final processing
        List<SystemInfo> systems = sheet.getSystems()
                .subList(
                system.getId() - 1,
                sheet.getSystems().size());

        // All actions for completed systems
        for (SystemInfo info : systems) {
            ScoreSystem syst = info.getScoreSystem();
            syst.fillMissingParts();
            syst.connectSystemInitialSlurs();
            syst.connectTiedVoices();
            syst.refineLyricSyllables();
        }
    }

    //-----------------//
    // translateSystem //
    //-----------------//
    /**
     * This is where glyph information is translated to score entity
     * information.
     */
    public void translateSystem ()
    {
        // Translations in proper order

        // Whole score impact
        //-------------------

        // Brace
        translate(new BraceTranslator());
        // Clef
        translate(new ClefTranslator());
        // Time signature
        translate(new TimeTranslator());
        // Key
        translate(new KeyTranslator());

        // Measure impact
        //---------------

        // Chord, Note
        translate(new ChordTranslator());
        // Beam (-> chord), BeamItem, BeamGroup
        translate(new BeamTranslator());
        // Flag (-> chord)
        translate(new FlagTranslator());
        // Dots (-> chord) as staccato / augmentation / repeat
        translate(new DotTranslator());
        // Tuplets (-> chords)
        translate(new TupletTranslator());
        // Accidental (-> note)
        translate(new AccidentalTranslator());
        // Slur (-> chord or note)
        translate(new SlurTranslator());
        // Finalize measure slots, ties, voices & durations, barlines
        translate(new MeasureTranslator());

        // Local impact
        //-------------

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
     * handled by the provided translator.
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
     * Class {@code Translator} is an abstract class that defines the
     * pattern for every translation engine.
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

            logger.debug("Creating {}", this);
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Check if provided glyph is relevant.
         *
         * @param glyph the glyph at hand
         * @return true if the glyph at hand is relevant for the translator
         */
        public abstract boolean isRelevant (Glyph glyph);

        /**
         * Specific browsing of a given measure.
         *
         * @param measure the given measure
         */
        public void browse (Measure measure)
        {
        }

        /**
         * Hook for final processing at end of the system.
         */
        public void completeSystem ()
        {
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                for (TreeNode mn : part.getMeasures()) {
                    Measure measure = (Measure) mn;

                    try {
                        browse(measure);
                    } catch (Exception ex) {
                        logger.warn(
                                measure.getContextString()
                                + " Exception in measure browsing",
                                ex);
                    }
                }
            }
        }

        /**
         * Compute the location system environment of the provided glyph.
         * Results are written in global variables currentXXX.
         *
         * @param glyph the glyph to locate
         */
        public void computeLocation (Glyph glyph)
        {
            currentCenter = glyph.getLocation();
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
         *
         * @param glyph the glyph at hand
         */
        public abstract void translate (Glyph glyph);

        /**
         * Browsing on every glyph within the system.
         */
        protected void translateGlyphs ()
        {
            for (Glyph glyph : system.getInfo()
                    .getGlyphs()) {
                Shape shape = glyph.getShape();

                if (glyph.isWellKnown()
                    && (shape != Shape.CLUTTER)
                    && !glyph.isTranslated()) {
                    // Check for glyph relevance
                    if (isRelevant(glyph)) {
                        // Determine part/staff/measure containment
                        computeLocation(glyph);

                        try {
                            // Perform the translation on this glyph
                            translate(glyph);
                        } catch (Exception ex) {
                            logger.warn(
                                    "Error translating glyph #" + glyph.getId()
                                    + " by " + this,
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return Accidentals.contains(glyph.getShape());
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return glyph.getShape() == Shape.ARPEGGIATO;
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            // ARPEGGIATO is processed by ArpeggiateTranslator.
            // DOT_set-shape staccato is processed by DotTranslation,
            // while STACCATO-shape staccato is processed here
            return Articulations.contains(shape)
                   && (shape != Shape.ARPEGGIATO);
        }

        @Override
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
        @Override
        public void browse (Measure measure)
        {
            // Link beams to chords, and populate beam groups
            BeamGroup.populate(measure);
        }

        @Override
        public void computeLocation (Glyph glyph)
        {
            // Staff, measure and staff point need specific processing
            // We use the attached stem(s) to determine proper containment
            Glyph stem = glyph.getFirstStem();

            if (stem != null) {
                super.computeLocation(stem);
            } else {
                currentMeasure.addError(
                        glyph,
                        "Beam glyph with no attached stem");
                super.computeLocation(glyph);
            }
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return Beams.contains(glyph.getShape());
        }

        @Override
        public void translate (Glyph glyph)
        {
            BeamItem.populate(glyph, currentMeasure);
        }
    }

    //-----------------//
    // BraceTranslator //
    //-----------------//
    private class BraceTranslator
            extends Translator
    {
        //~ Constructors -------------------------------------------------------

        public BraceTranslator ()
        {
            super("Brace");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return (shape == Shape.BRACE) || (shape == Shape.BRACKET);
        }

        @Override
        public void translate (Glyph glyph)
        {
            currentPart.setBrace(glyph);
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Rests.contains(shape) || NoteHeads.contains(shape)
                   || Notes.contains(shape);
        }

        @Override
        public void translate (Glyph glyph)
        {
            Chord.populate(glyph, currentMeasure);
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
        @Override
        public void browse (Measure measure)
        {
            // Sort the clefs according to containing staff
            Collections.sort(measure.getClefs(), MeasureNode.staffComparator);
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return Clefs.contains(glyph.getShape());
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return shape == Shape.CODA;
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return Dots.contains(glyph.getShape());
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Dynamics.contains(shape) && (shape != Shape.CRESCENDO)
                   && (shape != Shape.DECRESCENDO);
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return (glyph.getShape() == Shape.FERMATA)
                   || (glyph.getShape() == Shape.FERMATA_BELOW);
        }

        @Override
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
        @Override
        public void browse (Measure measure)
        {
            if (logger.isDebugEnabled()) {
                // Print flag/beam value of each chord
                logger.debug("Flag/Beams for {}", measure.getContextString());

                for (TreeNode node : measure.getChords()) {
                    Chord chord = (Chord) node;
                    logger.debug(chord.toString());

                    if (!chord.getBeams()
                            .isEmpty()) {
                        logger.debug("   Beams:{}", chord.getBeams().size());
                    }

                    if (chord.getFlagsNumber() > 0) {
                        logger.debug("   Flags:{}", chord.getFlagsNumber());
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
            Glyph stem = glyph.getStem(HorizontalSide.LEFT);

            if (stem != null) {
                super.computeLocation(stem);
            } else {
                system.addError(
                        glyph,
                        "Flag glyph " + glyph.getId() + " with no attached stem");
                super.computeLocation(glyph);
            }
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return Flags.contains(shape);
        }

        @Override
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
        @Override
        public void completeSystem ()
        {
            try {
                new KeySignatureVerifier(system).verifyKeys();
            } catch (Exception ex) {
                logger.warn("Error verifying keys for " + system, ex);
            }
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return (glyph.getShape().isSharpBased())
                   || (glyph.getShape().isFlatBased());
        }

        @Override
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
        //~ Instance fields ----------------------------------------------------

        SlotBuilder slotBuilder = new SlotBuilder(system);

        //~ Constructors -------------------------------------------------------
        public MeasureTranslator ()
        {
            super("Measure");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void browse (Measure measure)
        {
            // Check that a chord is not tied to different chords
            measure.checkTiedChords();

            // Build slots and voices
            slotBuilder.buildSlots(measure);

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
                Barline barline = part.getStartingBarline();

                if (barline != null) {
                    barline.translateGlyphs();
                }
            }
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return false;
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            final Shape shape = glyph.getShape();

            return (shape == Shape.TR) || (shape == Shape.TURN)
                   || (shape == Shape.MORDENT)
                   || (shape == Shape.INVERTED_MORDENT);
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return (shape == Shape.PEDAL_MARK)
                   || (shape == Shape.PEDAL_UP_MARK);
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return shape == Shape.SEGNO;
        }

        @Override
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
        @Override
        public void computeLocation (Glyph glyph)
        {
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return glyph.getShape() == Shape.SLUR;
        }

        @Override
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

        Rectangle systemBox;

        //~ Constructors -------------------------------------------------------
        public TextTranslator ()
        {
            super("Text");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void completeSystem ()
        {
            for (TextLine sentence : system.getInfo()
                    .getSentences()) {
                Text.populate(sentence, sentence.getLocation());
            }

            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;
                part.populateLyricsLines();
                part.mapSyllables();
            }
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return false; // Text is not handled at glyph but system level
        }

        @Override
        public void translate (Glyph glyph)
        {
            // Void
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return Times.contains(glyph.getShape());
        }

        @Override
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
        @Override
        public boolean isRelevant (Glyph glyph)
        {
            return Tuplets.contains(glyph.getShape());
        }

        @Override
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
        @Override
        public void computeLocation (Glyph glyph)
        {
            // Take the left edge for glyph center
            Rectangle box = glyph.getBounds();
            currentCenter = new Point(box.x, box.y + (box.height / 2));
            currentStaff = system.getStaffAt(currentCenter);
            currentPart = currentStaff.getPart();
            currentMeasure = currentPart.getMeasureAt(currentCenter);
        }

        @Override
        public boolean isRelevant (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return (shape == Shape.CRESCENDO) || (shape == Shape.DECRESCENDO);
        }

        @Override
        public void translate (Glyph glyph)
        {
            Wedge.populate(glyph, currentMeasure, currentCenter);
        }
    }
}
