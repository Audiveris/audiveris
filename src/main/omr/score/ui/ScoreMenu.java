//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e M e n u                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.glyph.Glyph;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.common.PagePoint;
import omr.score.common.ScorePoint;
import omr.score.common.SystemPoint;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.Slot;
import omr.score.entity.System;
import omr.score.entity.SystemPart;
import omr.score.midi.MidiActions;
import omr.score.midi.MidiActions.PlayAction;
import omr.score.midi.MidiAgent;

import omr.selection.Selection;
import omr.selection.SelectionTag;

import omr.ui.ActionManager;

import omr.util.Logger;

import java.awt.Component;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import static javax.swing.Action.*;

/**
 * Class <code>ScoreMenu</code> defines the popup menu which is linked to the
 * current selection in score view
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreMenu.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /** Set of actions to update menu according to current selections */
    private final Collection<DynAction> dynActions = new HashSet<DynAction>();

    /** Set of items to update menu according to current selections */
    private final Collection<DynItem> dynItems = new HashSet<DynItem>();

    /** Selection of glyphs, which is read on demand (no automatic update) */
    private final Selection glyphSetSelection;

    /** Concrete popup menu */
    private final JPopupMenu popup;

    // Context
    private System  system;
    private Measure measure;
    private Slot    slot;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScoreMenu //
    //-----------//
    /**
     * Create the popup menu
     *
     * @param score the related score
     */
    public ScoreMenu (Score score)
    {
        this.score = score;
        glyphSetSelection = score.getSheet()
                                 .getSelection(SelectionTag.GLYPH_SET);

        popup = new JPopupMenu();
        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getPopup //
    //----------//
    /**
     * Report the concrete popup menu
     *
     * @return the popup menu
     */
    public JPopupMenu getPopup ()
    {
        return popup;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the popup menu according to the currently selected glyphs
     *
     * @param scrPt the point designated in the score display
     */
    public void updateMenu (ScorePoint scrPt)
    {
        // Analyze the context
        system = score.scoreLocateSystem(scrPt);

        PagePoint   pagPt = system.toPagePoint(scrPt);
        SystemPoint sysPt = system.toSystemPoint(pagPt);
        SystemPart  part = system.getPartAt(sysPt);
        measure = part.getMeasureAt(sysPt);
        slot = measure.getClosestSlot(sysPt);

        // Update all dynamic actions accordingly
        for (DynAction action : dynActions) {
            action.update();
        }

        // Update all dynamic items accordingly
        for (DynItem item : dynItems) {
            item.update();
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        // Measure 
        popup.add(new MeasureItem());
        popup.add(new JMenuItem(new PlayMeasureAction()));
        popup.add(new JMenuItem(new DumpMeasureAction()));

        popup.addSeparator();

        // Slot 
        popup.add(new SlotItem());
        popup.add(new JMenuItem(new DumpSlotChordsAction()));
        popup.add(new JMenuItem(new DumpSlotVoicesAction()));

        popup.addSeparator();

        // Chord 
        popup.add(new ChordItem());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need to be
     * updated according to the current glyph selection context.
     */
    private abstract class DynAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DynAction ()
        {
            // Record the instance
            dynActions.add(this);

            // Initially update the action items
            update();
        }

        //~ Methods ------------------------------------------------------------

        public abstract void update ();
    }

    //-------------------//
    // DumpMeasureAction //
    //-------------------//
    /**
     * Dump the current measure
     */
    private class DumpMeasureAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpMeasureAction ()
        {
            putValue(NAME, "Dump measure voices");
            putValue(
                SHORT_DESCRIPTION,
                "Dump the voices of the selected measure");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            measure.printVoices(null);
        }

        @Override
        public void update ()
        {
            setEnabled(measure != null);
        }
    }

    //---------------------//
    // DumpSlotChordsAction //
    //---------------------//
    /**
     * Dump the chords of the current slot
     */
    private class DumpSlotChordsAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpSlotChordsAction ()
        {
            putValue(NAME, "Dump slot chords");
            putValue(SHORT_DESCRIPTION, "Dump the chords of the selected slot");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            logger.info(slot.toChordString());
        }

        @Override
        public void update ()
        {
            setEnabled(slot != null);
        }
    }

    //---------------------//
    // DumpSlotVoicesAction //
    //---------------------//
    /**
     * Dump the voices of the current slot
     */
    private class DumpSlotVoicesAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public DumpSlotVoicesAction ()
        {
            putValue(NAME, "Dump slot voices");
            putValue(SHORT_DESCRIPTION, "Dump the voices of the selected slot");
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            logger.info(slot.toVoiceString());
        }

        @Override
        public void update ()
        {
            setEnabled(slot != null);
        }
    }

    //---------//
    // DynItem //
    //---------//
    /**
     * A JMenuItem which can be updated
     */
    private abstract class DynItem
        extends JMenuItem
    {
        //~ Constructors -------------------------------------------------------

        public DynItem ()
        {
            // Record the instance
            dynItems.add(this);

            // Initially update the item
            update();

            setEnabled(false);
        }

        //~ Methods ------------------------------------------------------------

        public abstract void update ();
    }

    //----------------//
    // AdditionalItem //
    //----------------//
    /**
     * Used to host additional JMenuItem, without being directly updated
     */
    private static class AdditionalItem
        extends JMenuItem
    {
        //~ Constructors -------------------------------------------------------

        public AdditionalItem (String text)
        {
            super(text);
            setEnabled(false);
        }
    }

    //-----------//
    // ChordItem //
    //-----------//
    /**
     * Used to host information about the first chord of the translation,
     * while the subsequent ones are hosted in additional items
     */
    private class ChordItem
        extends DynItem
    {
        //~ Methods ------------------------------------------------------------

        public void update ()
        {
            // Remove all subsequent Chord additional items
            int pos = popup.getComponentIndex(this);

            if (pos != -1) {
                int index = pos + 1;

                while (popup.getComponentCount() > index) {
                    Component comp = popup.getComponent(index);

                    if (comp instanceof AdditionalItem) {
                        popup.remove(index);
                    } else {
                        break;
                    }
                }
            }

            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning

            if ((glyphs != null) && !glyphs.isEmpty()) {
                Glyph glyph = glyphs.get(0);

                if (glyph.isTranslated()) {
                    int itemNb = 0;

                    for (Object entity : glyph.getTranslations()) {
                        if (entity instanceof Note) {
                            Note note = (Note) entity;
                            itemNb++;

                            if (itemNb == 1) {
                                setText(note.getChord().toShortString());
                            } else {
                                popup.add(
                                    new AdditionalItem(
                                        note.getChord().toShortString()));
                            }
                        }
                    }
                } else {
                    setText("");
                }
            } else {
                setText("");
            }
        }
    }

    //-------------//
    // MeasureItem //
    //-------------//
    /**
     * Used to host information about the selected measure
     */
    private class MeasureItem
        extends DynItem
    {
        //~ Methods ------------------------------------------------------------

        public void update ()
        {
            setText(
                (measure == null) ? "[no measure]"
                                : ("[Measure #" + measure.getId() + "]"));
        }
    }

    //-------------------//
    // PlayMeasureAction //
    //-------------------//
    /**
     * Play the current measure
     */
    private class PlayMeasureAction
        extends DynAction
    {
        //~ Constructors -------------------------------------------------------

        public PlayMeasureAction ()
        {
            putValue(NAME, "Play measure");
            putValue(SHORT_DESCRIPTION, "Play the selected measure");

            // Retrieve icon
            Action playAction = ActionManager.getInstance()
                                             .getActionInstance(
                PlayAction.class.getName());

            if (playAction != null) {
                putValue(
                    SMALL_ICON,
                    (Icon) playAction.getValue(Action.SMALL_ICON));
            }
        }

        //~ Methods ------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            try {
                if (logger.isFineEnabled()) {
                    logger.fine("Play " + measure);
                }

                MidiAgent.getInstance()
                         .reset();
                new MidiActions.PlayTask(
                    score,
                    new MeasureRange(score, measure.getId(), measure.getId())).execute();
            } catch (Exception ex) {
                logger.warning("Cannot play measure", ex);
            }
        }

        @Override
        public void update ()
        {
            setEnabled(measure != null);
        }
    }

    //----------//
    // SlotItem //
    //----------//
    /**
     * Used to host information about the current slot
     */
    private class SlotItem
        extends DynItem
    {
        //~ Methods ------------------------------------------------------------

        public void update ()
        {
            setText(
                (slot == null) ? "[no slot]"
                                : ("[Slot #" + slot.getId() + " start:" +
                                Note.quarterValueOf(slot.getStartTime()) + "]"));
        }
    }
}
