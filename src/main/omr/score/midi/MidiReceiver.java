//----------------------------------------------------------------------------//
//                                                                            //
//                          M i d i R e c e i v e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.midi;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.SystemPart;
import omr.score.ui.ScoreView;

import omr.util.Implement;
import omr.util.TreeNode;

import javax.sound.midi.*;
import javax.swing.*;

/**
 * Class <code>MidiReceiver</code> receives Midi events from the Midi sequencer
 * and uses them to update the current Audiveris score display accordingly.
 *
 * <p>We use the Midi midiTick as forwarded by the Midi sequencer, and whenever
 * a new midiTick value is received by the {@link #send} method, we retrieve the
 * corresponding time slot in our score and then move the display to focus on
 * that slot.
 *
 * <p>We forward the related slot information directly to the score display.
 * Perhaps we should keep the sheet display in sync too, in that case, we could
 * use some new "Slot Selection" mechanism. TBD.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MidiReceiver
    implements Receiver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MidiReceiver.class);

    //~ Instance fields --------------------------------------------------------

    /** The Midi agent has information about sequence being played */
    private final MidiAgent agent;

    /** The score being played */
    private Score score;

    /** A specific measure range if any */
    private MeasureRange measureRange;

    /** The length of the current sequence, in Midi ticks */
    private long midiLength;

    /** The maximum measure id, for the current score */
    private int maxMeasureId;

    /** Offset when playback does not start from measure 1 */
    private long tickOffset;

    /** The last midiTick value received for the current score */
    private long currentMidiTick;

    /** The id for last measure of the current system */
    private Integer lastSystemMeasureId;

    /** The current system */
    private ScoreSystem currentSystem;

    /** The current measure id */
    private int currentMeasureId;

    /** The current measure */
    private Measure currentMeasure;

    /** The current time slot */
    private Slot currentSlot;

    /**
     * A trick to correct tick information between Score and Midi.
     * This correction is needed when time errors are left in the score.
     */
    private Double tickRatio;

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

    //----------//
    // setScore //
    //----------//
    /**
     * Assign to this receiver the score whose display is to be kept in sync.
     * This method is needed since the Receiver instance is reused from one
     * score to the other.
     *
     * @param score the (new) current score
     * @param measureRange if non null, specifies a measure range to be played
     */
    public void setScore (Score        score,
                          MeasureRange measureRange)
    {
        if ((this.score != score) || (this.measureRange != measureRange)) {
            reset();
            this.score = score;
            this.measureRange = measureRange;

            if (score != null) {
                // Remember global score information
                maxMeasureId = score.getLastSystem()
                                    .getLastPart()
                                    .getLastMeasure()
                                    .getId();
                currentSystem = score.getFirstSystem();
                lastSystemMeasureId = currentSystem.getLastPart()
                                                   .getLastMeasure()
                                                   .getId();
                currentMeasureId = 1;
            }
        }

        ;
    }

    //-------//
    // close //
    //-------//
    /**
     * This method is not implemented, but is needed to be compliant with the
     * Receiver interface.
     */
    @Implement(value = Receiver.class)
    public void close ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //------//
    // send //
    //------//
    /**
     * Method called by the Midi sequencer with the Midi message and time stamp
     * for each new Midi event.
     *
     * @param message the Midi event (unused)
     * @param timeStamp the time stamp (always -1 in fact, therefore unused),
     * we get the tick information directly by polling the Midi agent
     */
    @Implement(value = Receiver.class)
    public void send (MidiMessage message,
                      long        timeStamp)
    {
        if (score != null) {
            // Make sure we are still playing ...
            if (agent.getStatus() == MidiAgent.Status.PLAYING) {
                // Get the current midi tick value
                long midiTick = agent.getPositionInTicks();

                // Have we moved since last call?
                if (midiTick != currentMidiTick) {
                    currentMidiTick = midiTick;

                    if (logger.isFineEnabled()) {
                        logger.fine("t-" + midiTick);
                    }

                    // First time we get the sequence Midi length in ticks?
                    if (midiLength == -1) {
                        midiLength = agent.getLengthInTicks();
                        tickRatio = getTickRatio();
                    }

                    // Try to retrieve a related time slot in the score
                    long scoreTick = midiTick;

                    if ((tickRatio != null) && (tickRatio != 1)) {
                        scoreTick = (long) Math.rint(midiTick / tickRatio);
                    }

                    boolean slotFound = false;

                    if (retrieveSlot(scoreTick)) {
                        slotFound = true;
                        showSlot(); // Update the score display accordingly
                    }

                    // Are we through?
                    if (!slotFound || (midiTick >= midiLength)) {
                        reset();
                        agent.ending();
                    }
                }
            }
        }
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
        midiLength = -1;
        maxMeasureId = -1;
        tickOffset = 0;
        currentMidiTick = -1;
        lastSystemMeasureId = null;
        currentSystem = null;
        currentMeasureId = -1;
        currentMeasure = null;
        currentSlot = null;
        tickRatio = null;

        // Erase current slot position in the score display
        showSlot();

        score = null;
        measureRange = null;
    }

    //--------------//
    // getTickRatio //
    //--------------//
    private Double getTickRatio ()
    {
        int  divisor = score.getDurationDivisor();
        long midiTicks = agent.getLengthInTicks();
        long scoreTicks;

        if (measureRange == null) {
            measureRange = score.getMeasureRange();
        }

        if (measureRange != null) {
            long startTime = measureRange.getFirstMeasure()
                                         .getStartTime() +
                             measureRange.getFirstSystem()
                                         .getStartTime();
            tickOffset = startTime / divisor;

            if (logger.isFineEnabled()) {
                logger.fine(measureRange + " tickOffset=" + tickOffset);
            }

            scoreTicks = (score.getLastSoundTime(measureRange.getLastId()) -
                         startTime) / divisor;
        } else {
            scoreTicks = score.getLastSoundTime(null) / divisor;
            tickOffset = 0;
        }

        ///logger.info("Score ticks = " + scoreTicks);
        if (midiTicks != scoreTicks) {
            logger.warning(
                "Midi & score ticks don't match (" + midiTicks + "-" +
                scoreTicks + "=" +
                Note.quarterValueOf((int) (midiTicks - scoreTicks) * divisor) +
                ") division=" +
                score.simpleDurationOf(omr.score.entity.Note.QUARTER_DURATION));

            return new Double((double) midiTicks / (double) scoreTicks);
        } else {
            return new Double(1d);
        }
    }

    //-------------//
    // getTickTime //
    //-------------//
    /**
     * Compute in normalized score ticks the current position
     * (system + measure + slot)
     *
     * @return the computed midiTick position
     */
    private int getTickTime (Measure measure,
                             Slot    slot)
    {
        if (slot != null) {
            int rawTick = currentSystem.getStartTime() +
                          measure.getStartTime() + slot.getStartTime();

            int tick = rawTick / score.getDurationDivisor();

            return tick;
        } else {
            return -1;
        }
    }

    //--------------//
    // retrieveSlot //
    //--------------//
    /**
     * Given the midiTick position within the sequence, find out the related
     * time slot (as well as the containing measure and system).
     * We have to cope with measures without any time slots.
     *
     * @param scoreTick the position in ticks within the score
     * @return true if a suitable slot has been found, false otherwise
     */
    private boolean retrieveSlot (long scoreTick)
    {
        scoreTick += tickOffset;

        if (logger.isFineEnabled()) {
            logger.fine("retrieveSlot for score tick " + scoreTick);
        }

        int newTick = -1;

        for (int mid = currentMeasureId; mid <= maxMeasureId; mid++) {
            ///logger.fine("mid=" + mid);

            // Should we move to next system?
            while (mid > lastSystemMeasureId) {
                ///logger.fine("Moving to next system");
                currentSystem = (ScoreSystem) currentSystem.getNextSibling();
                lastSystemMeasureId = currentSystem.getFirstPart()
                                                   .getLastMeasure()
                                                   .getId();
            }

            // Check all measures with the same id, whatever the part
            for (TreeNode partNode : currentSystem.getParts()) {
                SystemPart part = (SystemPart) partNode;

                ///logger.fine("part=" + part.getId());
                Measure measure = (Measure) part.getMeasures()
                                                .get(
                    mid - part.getFirstMeasure().getId());

                ///logger.fine("Slots nb=" + measure.getSlots().size());
                slotLoop: 
                for (Slot slot : measure.getSlots()) {
                    int slotTick = getTickTime(measure, slot);

                    ///logger.fine("Slot#" + slot.getId() + " time=" + slotTick);
                    if ((slotTick >= scoreTick) &&
                        ((newTick == -1) || (slotTick < newTick))) {
                        // Let's remember this slot & midiTick
                        currentMeasure = measure;
                        currentSlot = slot;
                        newTick = slotTick;

                        break slotLoop;
                    }
                }
            }

            // Found a suitable slot?
            if (newTick != -1) {
                currentMeasureId = mid;

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

    //----------//
    // showSlot //
    //----------//
    /**
     * Highlight the corresponding slot within the score display, using the
     * global values of measure and slot.
     */
    private void showSlot ()
    {
        if (score != null) {
            score.getView()
                 .highLight(currentMeasure, currentSlot);
        }
    }
}
