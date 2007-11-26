//----------------------------------------------------------------------------//
//                                                                            //
//                          M i d i R e c e i v e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.midi;

import omr.score.Score;
import omr.score.entity.Measure;
import omr.score.entity.Slot;
import omr.score.entity.System;
import omr.score.entity.SystemPart;
import omr.score.ui.ScoreView;

import omr.util.Implement;
import omr.util.Logger;

import java.util.Iterator;

import javax.sound.midi.*;
import javax.swing.*;

/**
 * Class <code>MidiReceiver</code> receives Midi events from the Midi sequencer
 * and uses them to update the current Audiveris score display accordingly.
 * For the time being, the current position in the score is handled in a rather
 * simplistic way, to be improved.
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

    /** The length of the current sequence, in ticks */
    private long sequenceLength = 0;

    /** The last tick value received for the current score */
    private long lastTick = -1;

    /** The current system */
    private System system = null;

    /** The current measure */
    private Measure measure = null;

    /** The current time slot */
    private Slot slot = null;

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
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setScore //
    //----------//
    /**
     * Assign the score whose display is to be kept in sync
     * @param score the (new) current score
     */
    public void setScore (Score score)
    {
        if (this.score != score) {
            reset();
            this.score = score;
        }
    }

    //-------//
    // close //
    //-------//
    /**
     * Not implemented, but needed to be compliant with the Receiver interface
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
     * Method called by the player with the Midi message and time stamp
     *
     * @param message the Midi event
     * @param timeStamp the time stamp (always -1 in fact)
     */
    @Implement(value = Receiver.class)
    public void send (MidiMessage message,
                      long        timeStamp)
    {
        if (score != null) {
            // Make sure we are still playing ...
            if (agent.getStatus() == MidiAgent.Status.PLAYING) {
                long tick = agent.getPositionInTicks();

                if (tick != lastTick) {
                    lastTick = tick;

                    if (logger.isFineEnabled()) {
                        java.lang.System.out.println("t-" + tick);
                    }

                    showPosition(tick);

                    if (sequenceLength == 0) {
                        sequenceLength = agent.getLengthInTicks();
                    }

                    if (tick >= sequenceLength) {
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
     * Reinitialize the cached data of this entity
     */
    void reset ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiReceiver reset");
        }

        lastTick = -1;
        system = null;
        measure = null;
        slot = null;
        showSlot();
        score = null;
    }

    //-------------//
    // getTickTime //
    //-------------//
    /**
     * Compute in Midi ticks the current position (system + measure + slot)
     * @return the computed tick position
     */
    private int getTickTime ()
    {
        if (slot != null) {
            int totalTick = system.getStartTime() + measure.getStartTime() +
                            slot.getStartTime();

            int tick = totalTick / system.getFirstPart()
                                         .getScorePart()
                                         .getDurationDivisor();

            if (logger.isFineEnabled()) {
                logger.fine(
                    "system=" + system.getId() + " measure=" + measure.getId() +
                    " slot=" + slot.getId() + " tick=" + tick);
            }

            return tick;
        } else {
            return -1;
        }
    }

    //----------//
    // nextSlot //
    //----------//
    /**
     * Move to the next measure time Slot, across measures and systems if
     * needed.
     *
     * @return the next slot, or null. Beware, besides 'slot', this may update
     * the 'system' and 'measure' global variables.
     */
    private Slot nextSlot ()
    {
        if (measure == null) {
            // We are just starting
            system = (System) score.getSystems()
                                   .get(0);

            SystemPart part = system.getFirstPart();
            measure = part.getFirstMeasure();
            slot = measure.getSlots()
                          .first();
        } else if (slot == measure.getSlots()
                                  .last()) {
            // We have just finished a measure
            Measure m = (Measure) measure.getNextSibling();

            if (m != null) {
                measure = m;
                slot = measure.getSlots()
                              .first();
            } else {
                // We have just finished a system
                System s = (System) system.getNextSibling();

                if (s == null) {
                    // This is the end...
                    slot = null;
                } else {
                    // Move to next system
                    system = s;

                    SystemPart part = system.getFirstPart();
                    measure = part.getFirstMeasure();
                    slot = measure.getSlots()
                                  .first();
                }
            }
        } else {
            // Move to next slot within the current measure
            for (Iterator<Slot> it = measure.getSlots()
                                            .iterator(); it.hasNext();) {
                Slot s = it.next();

                if (s == slot) {
                    slot = it.next();

                    break;
                }
            }
        }

        return slot;
    }

    //--------------//
    // retrieveSlot //
    //--------------//
    /**
     * Given the position within the sequence, find out the related time
     * slot (as well as the containing measure and system).
     *
     * For the time being, the logic is very simple, we don't care about the
     * tick value, we just move to the next slot!
     *
     * @param tick the position in ticks within the sequence
     */
    private void retrieveSlot (long tick)
    {
        if (score != null) {
            try {
                if (slot == null) {
                    nextSlot();
                }

                Integer tickTime = getTickTime();

                while ((tickTime != -1) && (tickTime < tick)) {
                    nextSlot();
                    tickTime = getTickTime();
                }
            } catch (Exception ex) {
                logger.warning("Cannot retrieve time slot", ex);
            }
        }
    }

    //--------------//
    // showPosition //
    //--------------//
    /**
     * Highlight the corresponding location within the score display of a given
     * position within the Midi sequence
     *
     * @param tick the position in ticks
     */
    private void showPosition (final long tick)
    {
        retrieveSlot(tick);
        showSlot();
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
            final ScoreView.MyPanel view = (ScoreView.MyPanel) score.getView()
                                                                    .getScrollPane()
                                                                    .getView();
            SwingUtilities.invokeLater(
                new Runnable() {
                        public void run ()
                        {
                            view.highLight(measure, slot);
                        }
                    });
        }
    }
}
