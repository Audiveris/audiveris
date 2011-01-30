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

import omr.math.Rational;

import omr.score.Score;
import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.SystemPart;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.step.Steps;

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

    /** The position WRT current score */
    private Current current;

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
            logger.info("playbackAtEnd");
        }

        reset();
        showSlot();
    }

    //--------------//
    // playbackAtMP //
    //--------------//
    @Implement(PlaybackListener.class)
    public void playbackAtMP (MP position)
    {
        if (logger.isFineEnabled()) {
            logger.info("playbackAtMP");
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
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reinitialize the cached data of this entity (mainly the current position
     * within the score sequence)
     */
    final void reset ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("MidiReceiver reset");
        }

        // Flush cached data
        current = new Current();
    }

    //------------------------//
    // retrieveMeasureAndSlot //
    //------------------------//
    /**
     * Assuming we are in the right system, retrieve the measure that
     * corresponds to the provided score-based measure index, and which contains
     * the desired slot.
     * @param measureIndex the score-based index of desired measure
     * @return true if measure & slot found, false otherwise
     */
    private boolean retrieveMeasureAndSlot (int      measureIndex,
                                            Rational targetTick)
    {
        final Score score = agent.getScore();
        final int   pageOffset = score.getMeasureOffset(current.page);
        int         systemMeasureIndex = measureIndex - pageOffset;

        for (TreeNode sn : current.page.getSystems()) {
            ScoreSystem system = (ScoreSystem) sn;

            if (system != current.system) {
                systemMeasureIndex -= system.getFirstPart()
                                            .getMeasures()
                                            .size();
            } else {
                for (TreeNode partNode : current.system.getParts()) {
                    SystemPart part = (SystemPart) partNode;
                    logger.fine("part=" + part.getId());

                    Measure measure = (Measure) part.getMeasures()
                                                    .get(systemMeasureIndex);
                    logger.fine("Slots nb=" + measure.getSlots().size());
                    slotLoop: 
                    for (Slot slot : measure.getSlots()) {
                        Rational slotTick = slot.getStartTime();

                        if (slotTick.equals(targetTick)) {
                            // Let's remember measure & slot
                            current.index = measureIndex;
                            current.measure = measure;
                            current.slot = slot;

                            if (logger.isFineEnabled()) {
                                logger.fine("Slot retrieved " + current.slot);
                            }

                            return true;
                        }
                    }
                }

                return false;
            }
        }

        return false;
    }

    //--------------//
    // retrievePage //
    //--------------//
    /**
     * Assign current.page with the page that contains the provided score-based
     * measure index
     * @param measureIndex the score-based index of desired measure
     * @return true if page found
     */
    private boolean retrievePage (int measureIndex)
    {
        final Score score = agent.getScore();
        int         offset = 0;

        for (TreeNode pn : score.getPages()) {
            current.page = (Page) pn;
            offset += current.page.getMeasureCount();

            if (measureIndex < offset) {
                if (logger.isFineEnabled()) {
                    logger.fine("Page retrieved " + current.page);
                }

                return true;
            }
        }

        return false;
    }

    //--------------//
    // retrieveSlot //
    //--------------//
    /**
     * Retrieve the slot that corresponds to the measure and beat values, while
     * setting the containing system
     * @param measureIndex measure score-based index
     * @param beat beat within the measure
     * @return the retrieved slot, or null if not found
     */
    private boolean retrieveSlot (int      measureIndex,
                                  Fraction beat)
    {
        // Translate beat to tick
        ///(4 * Note.QUARTER_DURATION * beat.getNumerator()) / beat.getDenominator();
        Rational targetTick = new Rational(
            beat.getNumerator(),
            beat.getDenominator());

        if (logger.isFineEnabled()) {
            logger.fine(
                "Measure: " + measureIndex + " beat: " + beat + " tick: " +
                targetTick);
        }

        if ((current.index == null) || (measureIndex != current.index)) {
            current.index = measureIndex;

            if (!retrievePage(measureIndex)) {
                logger.warning(
                    "No page retrieved for measure index " + measureIndex);

                return false;
            }

            if (!retrieveSystem(measureIndex)) {
                logger.warning(
                    "No system retrieved for measure index " + measureIndex);

                return false;
            }
        }

        if (!retrieveMeasureAndSlot(measureIndex, targetTick)) {
            logger.warning(
                "No measure/slot retrieved for measure index " + measureIndex +
                " beat " + beat);

            return false;
        }

        return true;
    }

    //----------------//
    // retrieveSystem //
    //----------------//
    /**
     * Assuming we are in the right page, retrieve the system that contains the
     * provided score-based measure index
     * @param measureIndex the score-based index of desired measure
     * @return true if system found
     */
    private boolean retrieveSystem (int measureIndex)
    {
        final Score score = agent.getScore();
        final int   pageOffset = score.getMeasureOffset(current.page);
        final int   pageMeasureIndex = measureIndex - pageOffset;
        int         pageMeasureCount = 0;

        for (TreeNode sn : current.page.getSystems()) {
            current.system = (ScoreSystem) sn;
            pageMeasureCount += current.system.getLastPart()
                                              .getMeasures()
                                              .size();

            if (pageMeasureIndex < pageMeasureCount) {
                if (logger.isFineEnabled()) {
                    logger.fine("System retrieved " + current.system);
                }

                return true;
            }
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

        // Score-based measure index
        int measureIndex = position.getMeasure();

        // Perhaps we are just playing a range of measures?
        if (agent.getMeasureRange() != null) {
            measureIndex += agent.getMeasureRange()
                                 .getFirstIndex();
        }

        Fraction beat = position.getBeat();

        try {
            if (retrieveSlot(measureIndex, beat)) {
                if (logger.isFineEnabled()) {
                    logger.fine("beat:" + beat + " " + current);
                }

                showSlot(); // Update the score display accordingly
            }
        } catch (Exception ex) {
            logger.warning("Error retrieving slot", ex);
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
            // Special case at the end (current.measure == null)
            // Reset the display of every sheet
            if (current.measure == null) {
                for (TreeNode pn : score.getPages()) {
                    Page  page = (Page) pn;
                    Sheet sheet = page.getSheet();
                    sheet.getSymbolsEditor()
                         .highLight(null, null);
                }
            } else {
                // Retrieve page/sheet display & update slot
                Sheet sheet = current.page.getSheet();

                // Switch page?
                if (current.page != current.prevPage) {
                    sheet.getAssembly()
                         .selectTab(Steps.valueOf(Steps.SYMBOLS));
                    SheetsController.getInstance()
                                    .showAssembly(sheet);
                    current.prevPage = current.page;
                }

                sheet.getSymbolsEditor()
                     .highLight(current.measure, current.slot);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Current //
    //---------//
    /**
     * Remember position data about score being played
     */
    private static class Current
    {
        //~ Instance fields ----------------------------------------------------

        /** The current measure index */
        private Integer index;

        /** To detect page switches */
        private Page prevPage;

        /** The current page */
        private Page page;

        /** The current system */
        private ScoreSystem system;

        /** The current measure */
        private Measure measure;

        /** The current time slot */
        private Slot slot;

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{Current");

            sb.append(" index:")
              .append(index);

            if (prevPage != null) {
                sb.append(" prevPage#")
                  .append(prevPage.getIndex());
            }

            if (page != null) {
                sb.append(" page#")
                  .append(page.getIndex());
            }

            if (system != null) {
                sb.append(" system#")
                  .append(system.getId());
            }

            if (measure != null) {
                sb.append(" ")
                  .append(measure);
            }

            if (slot != null) {
                sb.append(" ")
                  .append(slot);
            }

            sb.append("}");

            return sb.toString();
        }
    }
}
