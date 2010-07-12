//----------------------------------------------------------------------------//
//                                                                            //
//                          M i d i R e c e i v e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.midi;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.SystemPart;

import omr.util.Implement;
import omr.util.TreeNode;

import com.xenoage.util.math.Fraction;
import com.xenoage.zong.core.music.MP;
import com.xenoage.zong.io.midi.out.PlaybackListener;

/**
 * Class <code>MidiReceiver</code> receives events from the Zong player
 * and uses them to update the current Audiveris score display accordingly.
 *
 * @author Hervé Bitteur
 */
public class MidiReceiver
    implements PlaybackListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiReceiver.class);

    //~ Instance fields --------------------------------------------------------

    /** The Midi agent has information about sequence being played */
    private final MidiAgent agent;

    /** The current system */
    private ScoreSystem currentSystem;

    /** The current measure id */
    private int currentMeasureId;

    /** The current measure */
    private Measure currentMeasure;

    /** The current time slot */
    private Slot currentSlot;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // MidiReceiver //
    //--------------//
    /**
     * Create an instance of receiver (meant to be created only once by the
     * single instance of MidiAgent, since the actual score to be updated will
     * be provided later)
     *
     * @param agent the midi agent
     */
    public MidiReceiver (MidiAgent agent)
    {
        this.agent = agent;
        reset();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // playbackAtEnd //
    //---------------//
    @Implement(PlaybackListener.class)
    public void playbackAtEnd ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("playbackAtEnd");
        }
    }

    //--------------//
    // playbackAtMP //
    //--------------//
    @Implement(PlaybackListener.class)
    public void playbackAtMP (MP position)
    {
        if (logger.isFineEnabled()) {
            logger.fine("playbackAtMP");
        }

        showPosition(position);
    }

    //-----------------//
    // playbackStopped //
    //-----------------//
    @Implement(PlaybackListener.class)
    public void playbackStopped (MP position)
    {
        if (logger.isFineEnabled()) {
            logger.info("playbackStopped");
        }

        showPosition(position);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reinitialize the cached data of this entity (mainly the current position
     * within the score sequence)
     */
    void reset ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiReceiver reset");
        }

        // Set to default values
        currentSystem = null;
        currentMeasureId = -1;
        currentMeasure = null;
        currentSlot = null;

        // Erase current slot position in the score display
        showSlot();
    }

    //--------------//
    // retrieveSlot //
    //--------------//
    /**
     * Retrieve the slot that corresponds to the measure and beat values, while
     * setting the containing system
     * @param measureId measure id
     * @param beat beat within the measure
     * @return the retrieved slot, or null if not found
     */
    private boolean retrieveSlot (int      measureId,
                                  Fraction beat)
    {
        // Translate beat to tick
        int targetTick = (4 * Note.QUARTER_DURATION * beat.getNumerator()) / beat.getDenominator();

        if (logger.isFineEnabled()) {
            logger.fine(
                "Measure: " + measureId + " beat: " + beat + " tick: " +
                targetTick);
        }

        // Make sure that current system is the right one
        if (measureId != currentMeasureId) {
            if (currentSystem == null) {
                currentSystem = agent.getScore()
                                     .getFirstSystem();
            }

            // Should we move to another system?
            while (measureId < currentSystem.getFirstPart()
                                            .getFirstMeasure()
                                            .getId()) {
                ///logger.fine("Moving to previous system");
                currentSystem = (ScoreSystem) currentSystem.getPreviousSibling();
            }

            while (measureId > currentSystem.getFirstPart()
                                            .getLastMeasure()
                                            .getId()) {
                ///logger.fine("Moving to next system");
                currentSystem = (ScoreSystem) currentSystem.getNextSibling();
            }
        }

        int newTick = -1;

        // Check all measures with the required id, whatever the part
        for (TreeNode partNode : currentSystem.getParts()) {
            SystemPart part = (SystemPart) partNode;

            ///logger.fine("part=" + part.getId());
            Measure measure = (Measure) part.getMeasures()
                                            .get(
                measureId - part.getFirstMeasure().getId());

            ///logger.fine("Slots nb=" + measure.getSlots().size());
            slotLoop: 
            for (Slot slot : measure.getSlots()) {
                int slotTick = slot.getStartTime();

                if (slotTick == targetTick) {
                    // Let's remember this slot & midiTick
                    currentMeasure = measure;
                    currentSlot = slot;
                    newTick = slotTick;

                    break slotLoop;
                }
            }

            // Found a suitable slot?
            if (newTick != -1) {
                currentMeasureId = measureId;

                if (logger.isFineEnabled()) {
                    logger.fine("Slot retrieved " + currentSlot);
                }

                return true;
            }
        }

        // Not found
        if (logger.isFineEnabled()) {
            logger.fine("No slot retrieved");
        }

        return false;
    }

    //--------------//
    // showPosition //
    //--------------//
    /**
     * Translate position (measure index & beat) to proper slot
     * @param position score position, as forwarded by Zong player
     */
    private void showPosition (MP position)
    {
        Score score = agent.getScore();

        if (score == null) {
            logger.warning("Score is null");

            return;
        }

        int          measureId = 1 + position.getMeasure();
        MeasureRange range = agent.getMeasureRange();

        if (range != null) {
            measureId += (range.getFirstId() - 1);
        }

        Fraction beat = position.getBeat();

        if (retrieveSlot(measureId, beat)) {
            showSlot(); // Update the score display accordingly
        }
    }

    //----------//
    // showSlot //
    //----------//
    /**
     * Highlight the corresponding slot within the score display, using the
     * global values of measure and slot.
     */
    private void showSlot ()
    {
        Score score = agent.getScore();

        if (score != null) {
            score.getFirstView()
                 .highLight(currentMeasure, currentSlot);
        }
    }
}
