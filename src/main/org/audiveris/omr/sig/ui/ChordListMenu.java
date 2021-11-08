//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C h o r d L i s t M e n u                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.ui;

import java.awt.Color;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SameTimeRelation;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.SameVoiceRelation;
import org.audiveris.omr.sig.relation.SeparateTimeRelation;
import org.audiveris.omr.sig.relation.SeparateVoiceRelation;
import org.audiveris.omr.sig.ui.InterController.SourceTargetRelation;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.view.LocationDependentMenu;
import org.audiveris.omr.util.ClassUtil;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 * Class <code>ChordListMenu</code> displays a collection of chords.
 *
 * @author Hervé Bitteur
 */
public class ChordListMenu
        extends LocationDependentMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ChordListMenu.class);

    private static final String NO_VOICE_ID = "None";

    //~ Instance fields ----------------------------------------------------------------------------
    private final Sheet sheet;

    private final ChordListener chordListener = new ChordListener();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>ChordListMenu</code> object.
     *
     * @param sheet the related sheet
     */
    public ChordListMenu (Sheet sheet)
    {
        super("Chords");
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // updateUserLocation //
    //--------------------//
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        updateMenu(sheet.getInterIndex().getEntityService().getSelectedEntityList());

        super.updateUserLocation(rect);
    }

    //----------------//
    // buildMergeItem //
    //----------------//
    /**
     * Try to build a merge action item.
     * <p>
     * We check that the provided chords:
     * <ul>
     * <li>Are all head-chords.
     * <li>Belong to the same measure.
     * <li>Are consistent, i.e. all stem-less or all stem-based head chords of standard size,
     * no cue heads.
     * <li>Are rather well aligned vertically.
     * </ul>
     *
     * @param chords   the (head?) chords selected, 2 or more
     * @param listener the selection listener to use
     */
    private void buildMergeItem (final List<AbstractChordInter> chords,
                                 final SelectionListener listener)
    {
        final List<HeadChordInter> headChords = new ArrayList<>();

        if (!checkHeadChords(chords, headChords)) {
            return;
        }

        if (!checkSingleMeasure(headChords)) {
            logger.debug("Chords of different measures: {}", headChords);

            return;
        }

        // Check chords consistency (all with or all without stem)
        final Boolean withStem = checkStemsForMerge(headChords);

        if (withStem == null) {
            logger.debug("Chords non consistent for a merge: {}", headChords);

            return;
        }

        // Check chords are rather aligned vertically
        if (!checkAlignedForMerge(headChords, withStem)) {
            logger.debug("Chords not vertically aligned for a merge: {}", headChords);

            return;
        }

        addItem(new JMenuItem(new MergeAction(headChords, withStem)), listener);
    }

    //----------------//
    // buildVoiceMenu //
    //----------------//
    /**
     * Build an assign voice action item.
     *
     * @param chord    the provided head chord
     * @param listener the selection listener to use
     */
    private void buildVoiceMenu (final AbstractChordInter chord,
                                 final SelectionListener listener)
    {
        addItem(new VoiceMenu(chord), listener);
    }

    //----------------//
    // buildSplitItem //
    //----------------//
    /**
     * Try to build a split action item.
     * <p>
     * We check that the provided chord is indeed a head-chord and that there are at least 2 heads
     * in it.
     * <p>
     * The strategy is to split the heads where inter-head vertical distance is maximum.
     *
     * @param chord    the provided head chord
     * @param listener the selection listener to use
     */
    private void buildSplitItem (final AbstractChordInter chord,
                                 final SelectionListener listener)
    {
        if (!(chord instanceof HeadChordInter)) {
            return;
        }

        final HeadChordInter headChord = (HeadChordInter) chord;
        final List<? extends Inter> notes = headChord.getNotes();

        if (notes.size() < 2) {
            logger.debug("Chord with too few heads for a split: {}", notes.size());

            return;
        }

        addItem(new JMenuItem(new SplitAction(headChord)), listener);
    }

    //------------------//
    // buildSystemTitle //
    //------------------//
    /**
     * Build the title for system chords.
     *
     * @param system    the provided system
     * @param sysChords the selected chords in system
     * @param listener  the selection listener to use
     */
    private void buildSystemTitle (final SystemInfo system,
                                   final List<AbstractChordInter> sysChords,
                                   final SelectionListener listener)
    {
        String plural = (sysChords.size() > 1) ? "s" : "";
        String text = sysChords.size() + " chord" + plural + " for System #" + system.getId() + ":";
        JMenuItem title = new JMenuItem(text);
        title.setEnabled(false);

        addItem(title, listener);
    }

    //----------------//
    // buildTimeItems //
    //----------------//
    /**
     * Try to build time item(s) with the 2+ provided chords.
     * <p>
     * we check that the 2+ provided chords:
     * <ul>
     * <li>Belong to the same measure.
     * <li>Are rather close abscissa-wise.
     * <li>Don't belong to the same beam group (because their time relations are imposed)
     * </ul>
     *
     * @param chords   the 2+ provided chords
     * @param listener the selection listener to use
     */
    private void buildTimeItems (final List<AbstractChordInter> chords,
                                 final SelectionListener listener)
    {
        if (!checkSingleMeasure(chords)) {
            logger.debug("Chords of different measures: {}", chords);

            return;
        }

        // Check abscissa gap between the chords
        int xGap = 0;

        for (int i1 = 0, ilBreak = chords.size() - 1; i1 < ilBreak; i1++) {
            final AbstractChordInter ch1 = chords.get(i1);
            final Rectangle b1 = ch1.getBounds();

            for (AbstractChordInter ch2 : chords.subList(i1 + 1, chords.size())) {
                final Rectangle b2 = ch2.getBounds();
                xGap = Math.max(xGap, GeoUtil.xGap(b1, b2));
            }
        }

        final int max = sheet.getScale().toPixels(constants.maxAbscissaGapForTimeItems);

        if (xGap > max) {
            logger.debug("Chords with a too wide gap for time: {}", chords);

            return;
        }

        // No two candidates can belong to the same beam group
        for (int i1 = 0, ilBreak = chords.size() - 1; i1 < ilBreak; i1++) {
            final AbstractChordInter ch1 = chords.get(i1);
            final BeamGroupInter bg1 = ch1.getBeamGroup();

            if (bg1 != null) {
                for (AbstractChordInter ch2 : chords.subList(i1 + 1, chords.size())) {
                    final BeamGroupInter bg2 = ch2.getBeamGroup();

                    if (bg1 == bg2) {
                        logger.debug("Chords belong to the same beam group: {}", chords);

                        return;
                    }
                }
            }
        }

        final SIGraph sig = chords.get(0).getSig();

        if (chords.size() >= 3) {
            addItem(
                    new MultipleRelationAdditionItem(
                            "Same Time Slot for all",
                            "Make all chords share the same time slot",
                            chords,
                            SameTimeRelation.class),
                    listener);
        } else {
            final AbstractChordInter src = chords.get(0);
            final AbstractChordInter tgt = chords.get(1);
            Relation same = null;
            Relation separate = null;

            for (Relation rel : sig.getAllEdges(src, tgt)) {
                if (rel instanceof SameTimeRelation) {
                    same = rel;
                } else if (rel instanceof SeparateTimeRelation) {
                    separate = rel;
                }
            }

            if (same == null) {
                if (separate == null) {
                    addItem(
                            new RelationAdditionItem(
                                    "Same Time Slot",
                                    "Make the two chords share the same time slot",
                                    src,
                                    tgt,
                                    new SameTimeRelation()),
                            listener);
                }
            } else {
                addItem(
                        new RelationRemovalItem(
                                "cancel Same Time Slot",
                                "Cancel use of same time slot",
                                sig,
                                same),
                        listener);
            }

            if (separate == null) {
                if (same == null) {
                    addItem(
                            new RelationAdditionItem(
                                    "Separate Time Slots",
                                    "Make the two chords use separate time slots",
                                    src,
                                    tgt,
                                    new SeparateTimeRelation()),
                            listener);
                }
            } else {
                addItem(
                        new RelationRemovalItem(
                                "cancel Separate Time Slots",
                                "Cancel use of separate time slots",
                                sig,
                                separate),
                        listener);
            }
        }
    }

    //-----------------//
    // buildVoiceItems //
    //-----------------//
    /**
     * Try to build voice item(s) with the two provided chords.
     * <p>
     * The two chords cannot be too close abscissa-wise.
     *
     * @param chords   the two provided chords
     * @param listener the selection listener to use
     */
    private void buildVoiceItems (final List<AbstractChordInter> chords,
                                  final SelectionListener listener)
    {
        final AbstractChordInter src = chords.get(0);
        final AbstractChordInter tgt = chords.get(1);
        final SIGraph sig = src.getSig();

        final Rectangle srcBox = src.getBounds();
        final Rectangle tgtBox = tgt.getBounds();
        final int xOverlap = GeoUtil.xOverlap(srcBox, tgtBox);
        final int max = sheet.getScale().toPixels(constants.maxAbscissaOverlapForVoiceItems);

        if (xOverlap > max) {
            logger.debug("Chords with a too big overlap for voice: {}", chords);

            return;
        }

        final Point srcCenter = GeoUtil.center(srcBox);
        final Point tgtCenter = GeoUtil.center(tgtBox);
        final AbstractChordInter left = srcCenter.x <= tgtCenter.x ? src : tgt;
        final AbstractChordInter right = srcCenter.x > tgtCenter.x ? src : tgt;

        Relation same = null;
        Relation nextInVoice = null;
        Relation separate = null;

        final LinkedHashSet<Relation> rels = new LinkedHashSet<>();
        rels.addAll(sig.getAllEdges(src, tgt));
        rels.addAll(sig.getAllEdges(tgt, src));

        for (Relation rel : rels) {
            if (rel instanceof SameVoiceRelation) {
                same = rel;
            } else if (rel instanceof NextInVoiceRelation) {
                nextInVoice = rel;
            } else if (rel instanceof SeparateVoiceRelation) {
                separate = rel;
            }
        }

        if (nextInVoice == null) {
            if (separate == null) {
                addItem(
                        new RelationAdditionItem(
                                "Next in Voice",
                                "The two chords are in sequence within the same voice",
                                left,
                                right,
                                new NextInVoiceRelation()),
                        listener);
            }
        } else {
            addItem(
                    new RelationRemovalItem(
                            "cancel Next in Voice",
                            "Cancel use of next in voice",
                            sig,
                            nextInVoice),
                    listener);
        }

        if (same == null) {
            if (separate == null) {
                addItem(
                        new RelationAdditionItem(
                                "Same Voice",
                                "The two chords share the same voice",
                                src,
                                tgt,
                                new SameVoiceRelation()),
                        listener);
            }
        } else {
            addItem(
                    new RelationRemovalItem(
                            "cancel Same Voice",
                            "Cancel use of same voice",
                            sig,
                            same),
                    listener);
        }

        if (separate == null) {
            if ((same == null) && (nextInVoice == null)) {
                addItem(
                        new RelationAdditionItem(
                                "Separate Voices",
                                "Make the two chords use separate voices",
                                src,
                                tgt,
                                new SeparateVoiceRelation()),
                        listener);
            }
        } else {
            addItem(
                    new RelationRemovalItem(
                            "cancel Separate Voices",
                            "Cancel use of separate voices",
                            sig,
                            separate),
                    listener);
        }
    }

    //----------------------//
    // checkAlignedForMerge //
    //----------------------//
    /**
     * Check whether the provided head chords are vertically aligned for a merge.
     *
     * @param chords   the provided (head) chords
     * @param withStem true for stem-based chords, so that alignment check is performed on stems
     * @return true if OK
     */
    private boolean checkAlignedForMerge (List<HeadChordInter> chords,
                                          boolean withStem)
    {
        if (withStem) {
            // Focus on stems
            final int maxStemDx = sheet.getScale().toPixels(constants.maxStemDxForMerge);
            Point prevCenter = null;

            for (HeadChordInter chord : chords) {
                Point center = chord.getStem().getCenter();

                if (prevCenter != null) {
                    int dx = Math.abs(center.x - prevCenter.x);

                    if (dx > maxStemDx) {
                        return false;
                    }
                }

                prevCenter = center;
            }
        } else {
            // Focus on chords
            final int maxChordDx = sheet.getScale().toPixels(constants.maxChordDxForMerge);
            Point prevCenter = null;

            for (HeadChordInter chord : chords) {
                Point center = chord.getCenter();

                if (prevCenter != null) {
                    int dx = Math.abs(center.x - prevCenter.x);

                    if (dx > maxChordDx) {
                        return false;
                    }
                }

                prevCenter = center;
            }
        }

        return true;
    }

    //--------------------//
    // checkSingleMeasure //
    //--------------------//
    /**
     * Make sure the provided chords belong all to a single measure.
     *
     * @return true if OK
     */
    private boolean checkSingleMeasure (List<? extends AbstractChordInter> chords)
    {
        Measure measure = null;

        for (AbstractChordInter ch : chords) {
            if (measure == null) {
                measure = ch.getMeasure();
            } else if (measure != ch.getMeasure()) {
                return false;
            }
        }

        return true;
    }

    //--------------------//
    // checkStemsForMerge //
    //--------------------//
    /**
     * Check all the provided head chords consistently have a stem (or all have no stem)
     * for a merge.
     *
     * @param chords the collection of head chords to check
     * @return true if all chords have a stem, false if all chords have no stem, null in case any
     *         inconsistency is detected regarding stem or presence of small (cue) chords.
     */
    private Boolean checkStemsForMerge (List<HeadChordInter> chords)
    {
        // Check input
        Boolean withStem = null;

        for (HeadChordInter ch : chords) {
            StemInter stem = ch.getStem();

            if (stem != null) {
                if (withStem == null) {
                    withStem = true;
                } else if (!withStem) {
                    return null;
                }
            } else {
                if (withStem == null) {
                    withStem = false;
                } else if (withStem) {
                    return null;
                }
            }
        }

        return withStem;
    }

    //-------------------//
    // getChordsBySystem //
    //-------------------//
    /**
     * Retrieve the chords related to selected inters and gather them by system.
     *
     * @param inters the selected inters
     * @return the related chords per system, sorted by ID
     */
    private Map<SystemInfo, List<AbstractChordInter>> getChordsBySystem (Collection<Inter> inters)
    {
        // Sort the chords, first by containing system, then by ID
        final Map<SystemInfo, List<AbstractChordInter>> chordMap = new TreeMap<>();

        if (inters != null) {
            for (Inter inter : inters) {
                SIGraph sig = inter.getSig();

                if (sig != null) {
                    SystemInfo system = sig.getSystem();

                    if (system != null) {
                        // Is there a relevant chord related to this inter?
                        AbstractChordInter chord = relatedChord(inter);

                        if (chord != null) {
                            List<AbstractChordInter> list = chordMap.get(system);

                            if (list == null) {
                                chordMap.put(system, list = new ArrayList<>());
                            }

                            if (!list.contains(chord)) {
                                list.add(chord);
                            }
                        }
                    }
                }
            }

            for (List<AbstractChordInter> list : chordMap.values()) {
                Collections.sort(list, Entities.byId);
            }
        }

        return chordMap;
    }

    //--------------//
    // relatedChord //
    //--------------//
    /**
     * Report the head or rest chord, if any, related to the provided inter.
     * <p>
     * NOTA: Subclass SmallChordInter (cue chord) is not accepted.
     *
     * @param inter the provided inter
     * @return the related head chord or null
     */
    private AbstractChordInter relatedChord (Inter inter)
    {
        if (inter instanceof AbstractChordInter && !(inter instanceof SmallChordInter)) {
            return (AbstractChordInter) inter;
        }

        if (inter instanceof AbstractNoteInter) {
            AbstractNoteInter note = (AbstractNoteInter) inter;
            AbstractChordInter chord = note.getChord();

            if ((chord != null) && !(chord instanceof SmallChordInter)) {
                return chord;
            }
        }

        return null;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Organize the chord menu based on which relevant inters have been selected.
     *
     * @param inters the selected inters
     */
    private void updateMenu (Collection<Inter> inters)
    {
        // Gather the extracted chords (both heads and rests) by containing system
        final Map<SystemInfo, List<AbstractChordInter>> chordMap = getChordsBySystem(inters);

        try {
            // We rebuild the menu items on each update, since the set of chords is brand new.
            removeAll();

            if (!chordMap.isEmpty()) {
                for (Entry<SystemInfo, List<AbstractChordInter>> entry : chordMap.entrySet()) {
                    final SystemInfo system = entry.getKey();
                    final List<AbstractChordInter> sysChords = entry.getValue();
                    final SelectionListener listener = new SelectionListener(sysChords);
                    final int systemStartCount = getItemCount();

                    if (systemStartCount > 0) {
                        addSeparator();
                    }

                    // System title
                    buildSystemTitle(system, sysChords, listener);

                    // List of selected chords
                    for (AbstractChordInter chord : sysChords) {
                        final JMenuItem item = new JMenuItem(new InterAction(chord, "-  " + chord));
                        item.setEnabled(false);
                        item.addMouseListener(chordListener);
                        add(item);
                    }

                    // Actions according to the number of selected chords
                    final int actionsStartCount = getItemCount();

                    switch (sysChords.size()) {
                    case 0:
                        return; // Cannot occur actually

                    case 1:
                        buildSplitItem(sysChords.get(0), listener);
                        buildVoiceMenu(sysChords.get(0), listener);

                        break;

                    case 2:
                        buildMergeItem(sysChords, listener);
                        buildVoiceItems(sysChords, listener);
                        buildTimeItems(sysChords, listener);

                        break;

                    default:
                        // 3 and above
                        buildMergeItem(sysChords, listener);
                        buildTimeItems(sysChords, listener);
                    }

                    if (getItemCount() == actionsStartCount) {
                        // No user action for this system, remove any related menu item
                        while (getItemCount() > systemStartCount) {
                            remove(getItemCount() - 1);
                        }
                    }
                }
            }

            setVisible(getItemCount() > 0);
        } catch (Exception ex) {
            logger.warn("Error updating menu " + ex, ex);
        }
    }

    //---------//
    // addItem //
    //---------//
    /**
     * Connect the selection listener to the menu item and add it to the top menu.
     *
     * @param item     the menu item
     * @param listener the listener to connect
     */
    private void addItem (JMenuItem item,
                          SelectionListener listener)
    {
        item.addMouseListener(listener);
        add(item);
    }

    //-----------------//
    // checkHeadChords //
    //-----------------//
    /**
     * Check if the provided chords are all head chords.
     *
     * @param chords     (input) provided list of chords (heads and rests)
     * @param headChords (output) the list of head chords
     */
    private boolean checkHeadChords (List<AbstractChordInter> chords,
                                     List<HeadChordInter> headChords)
    {
        for (AbstractChordInter ch : chords) {
            if (!(ch instanceof HeadChordInter)) {
                return false;
            }

            headChords.add((HeadChordInter) ch);
        }

        return true;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // ChordListener //
    //---------------//
    /**
     * Publish the single related chord when menu item is entered by the mouse.
     */
    private static class ChordListener
            extends AbstractMouseListener
    {

        @Override
        public void mouseEntered (MouseEvent e)
        {
            JMenuItem item = (JMenuItem) e.getSource();
            InterAction action = (InterAction) item.getAction();
            action.publish(SelectionHint.ENTITY_INIT);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxStemDxForMerge = new Scale.Fraction(
                0.3,
                "Maximum abscissa shift between chords stems for a merge");

        private final Scale.Fraction maxChordDxForMerge = new Scale.Fraction(
                1.6,
                "Maximum abscissa shift between chords centers for a merge");

        private final Scale.Fraction maxAbscissaGapForTimeItems = new Scale.Fraction(
                2.0,
                "Maximum abscissa gap between chords bounds for a time menu item");

        private final Scale.Fraction maxAbscissaOverlapForVoiceItems = new Scale.Fraction(
                0.2,
                "Maximum abscissa overlap between chords bounds for a voice menu item");
    }

    //-------------//
    // MergeAction //
    //-------------//
    private class MergeAction
            extends AbstractAction
    {

        private final List<HeadChordInter> chords;

        private final boolean withStem;

        MergeAction (List<HeadChordInter> chords,
                     boolean withStem)
        {
            super("Merge");
            putValue(Action.SHORT_DESCRIPTION, "Merge the provided chords into a single one");

            this.chords = chords;
            this.withStem = withStem;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.debug("Merging {}", chords);
            sheet.getInterController().mergeChords(chords, withStem);
        }
    }

    //------------------------------//
    // MultipleRelationAdditionItem //
    //-----------------------------//
    /**
     * Menu item which consists in adding a relation between every couple.
     */
    private class MultipleRelationAdditionItem
            extends JMenuItem
    {

        public MultipleRelationAdditionItem (String label,
                                             String tip,
                                             final List<AbstractChordInter> chords,
                                             final Class<? extends Relation> relationClass)
        {
            setAction(new AbstractAction()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    final SIGraph sig = chords.get(0).getSig();

                    try {
                        final List<SourceTargetRelation> strs = new ArrayList<>();

                        for (int i = 0; i < (chords.size() - 1); i++) {
                            final AbstractChordInter src = chords.get(i);

                            for (AbstractChordInter tgt : chords.subList(
                                    i + 1,
                                    chords.size())) {
                                Relation rel = relationClass.newInstance();
                                strs.add(new SourceTargetRelation(src, tgt, rel));
                            }
                        }

                        sheet.getInterController().linkMultiple(sig, strs);
                    } catch (InstantiationException |
                             IllegalAccessException ex) {
                        logger.error("Could not instantiate class {}", relationClass, ex);
                    }
                }
            });

            setText(label);
            setToolTipText(tip);
        }
    }

    //----------------------//
    // RelationAdditionItem //
    //----------------------//
    /**
     * Menu item which consists in adding a relation.
     */
    private class RelationAdditionItem
            extends JMenuItem
    {

        public RelationAdditionItem (String label,
                                     String tip,
                                     final AbstractChordInter source,
                                     final AbstractChordInter target,
                                     final Relation relation)
        {
            Objects.requireNonNull(relation, "null relation");
            Objects.requireNonNull(source, "null source inter");
            Objects.requireNonNull(target, "null target inter");

            setAction(new AbstractAction()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    final SIGraph sig = source.getSig();
                    sheet.getInterController().link(sig, source, target, relation);
                }
            });

            setText(label);
            setToolTipText(tip);
        }
    }

    //---------------------//
    // RelationRemovalItem //
    //---------------------//
    /**
     * Menu item which consists in removing a relation.
     */
    private class RelationRemovalItem
            extends JMenuItem
    {

        public RelationRemovalItem (String label,
                                    String tip,
                                    final SIGraph sig,
                                    final Relation relation)
        {
            Objects.requireNonNull(relation, "null relation");

            setAction(new AbstractAction()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    sheet.getInterController().unlink(sig, relation);
                }
            });

            setText(label);
            setToolTipText(tip);
        }
    }

    //-------------------//
    // SelectionListener //
    //-------------------//
    /**
     * Publish selection of all chords when menu item is entered by the mouse.
     */
    private class SelectionListener
            extends AbstractMouseListener
    {

        private final List<AbstractChordInter> chords;

        public SelectionListener (List<AbstractChordInter> chords)
        {
            this.chords = chords;
        }

        @Override
        public void mouseEntered (MouseEvent e)
        {
            // Display the provided chord(s)
            sheet.getInterIndex().getEntityService().publish(
                    new EntityListEvent<>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            chords));
        }

        @Override
        public String toString ()
        {
            return ClassUtil.nameOf(this);
        }
    }

    //-----------//
    // VoiceMenu //
    //-----------//
    /**
     * Allows to set a preferred voice ID.
     */
    private class VoiceMenu
            extends JMenu
            implements ActionListener
    {

        private final AbstractChordInter chord; // Chord involved

        private final Integer chordPrefId; // Chord preferred voice id, if any

        public VoiceMenu (AbstractChordInter chord)
        {
            this.chord = chord;
            chordPrefId = chord.getPreferredVoiceId();

            final int prefId = (chordPrefId != null) ? chordPrefId : 0;
            setText("Preferred voice [Experimental]");
            setToolTipText("Assign specific voice ID to first chord in system voice");

            // None item
            final JMenuItem noneItem = new JRadioButtonMenuItem(NO_VOICE_ID);
            noneItem.setSelected(0 == prefId);
            noneItem.addActionListener(this);
            add(noneItem);

            // ID items
            for (int id : chord.getMeasure().inferVoiceFamily(chord).ids()) {
                final JMenuItem item = new JRadioButtonMenuItem("" + id);
                item.setOpaque(true);
                item.setBackground(new Color(Voices.colorOf(id).getRGB()));
                item.setSelected(id == prefId);
                item.addActionListener(this);
                add(item);
            }
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final String command = e.getActionCommand();

            if (command.equals(NO_VOICE_ID)) {
                if (chordPrefId != null) {
                    logger.info("No more preferred voice for {}", chord);
                    sheet.getInterController().changeVoiceId(chord, null);
                }
            } else {
                final int prefId = Integer.decode(e.getActionCommand());

                if ((chordPrefId == null) || (chordPrefId != prefId)) {
                    logger.info("Preferred voice {} for {}", prefId, chord);
                    sheet.getInterController().changeVoiceId(chord, prefId);
                }
            }
        }
    }

    //-------------//
    // SplitAction //
    //-------------//
    private class SplitAction
            extends AbstractAction
    {

        private final HeadChordInter chord;

        SplitAction (HeadChordInter chord)
        {
            super("Split");
            putValue(Action.SHORT_DESCRIPTION, "Split the provided chord into two chords");

            this.chord = chord;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.debug("Splitting {}", chord);
            sheet.getInterController().splitChord(chord);
        }
    }
}
