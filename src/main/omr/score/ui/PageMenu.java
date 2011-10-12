//----------------------------------------------------------------------------//
//                                                                            //
//                              P a g e M e n u                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.ui.SymbolMenu;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelPoint;
import omr.score.entity.Measure;
import omr.score.entity.MeasureId;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.SystemPart;
import omr.score.midi.MidiActions;
import omr.score.midi.MidiAgent;

import omr.sheet.SystemInfo;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class {@code PageMenu} defines the popup menu which is linked to the
 * current selection in page editor view.
 * <p>It points to 3 sub-menus: measure, slot, glyph</p>
 *
 * @author Hervé Bitteur
 */
public class PageMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PageMenu.class);

    //~ Instance fields --------------------------------------------------------

    /** The related page */
    private final Page page;

    /** Set of actions to update menu according to current selections */
    private final Collection<DynAction> dynActions = new HashSet<DynAction>();

    //    /** Set of items to update menu according to current selections */
    //    private final Collection<DynItem> dynItems = new HashSet<DynItem>();

    /** Concrete popup menu */
    private final JPopupMenu popup;

    /** Submenus */
    private final SymbolMenu symbolMenu;

    // Context
    private ScoreSystem system;
    private Measure     measure;
    private Slot        slot;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // PageMenu //
    //----------//
    /**
     * Create the page menu
     *
     * @param page the related page
     */
    public PageMenu (Page       page,
                     SymbolMenu symbolMenu)
    {
        this.page = page;
        this.symbolMenu = symbolMenu;

        popup = new JPopupMenu();
        defineLayout();

        // Initialize all dynamic actions
        for (DynAction action : dynActions) {
            action.update();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getMenu //
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
     * @param point the point designated in the page display
     */
    public void updateMenu (PixelPoint point)
    {
        // Analyze the context to retrieve designated system, measure & slot
        SystemInfo systemInfo = page.getSheet()
                                    .getSystemOf(point);
        system = systemInfo.getScoreSystem();

        SystemPart part = system.getPartAt(point);
        measure = part.getMeasureAt(point);

        if (measure != null) {
            slot = measure.getClosestSlot(point);
        }

        // Update all dynamic actions accordingly
        for (DynAction action : dynActions) {
            action.update();
        }

        // Update symbol menu
        symbolMenu.updateMenu();

        //        // Update all dynamic items accordingly
        //        for (DynItem item : dynItems) {
        //            item.update();
        //        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        // Measure
        popup.add(new MeasureMenu().getPopup());

        // Slot
        popup.add(new SlotMenu().getPopup());

        // Symbol
        popup.add(symbolMenu.getMenu());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need to be
     * updated according to the current glyph selection context.
     */
    public abstract class DynAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DynAction ()
        {
            // Record the instance
            dynActions.add(this);

            // Initially update the action items
            ///update();
        }

        //~ Methods ------------------------------------------------------------

        public abstract void update ();
    }

    //    //---------//
    //    // DynItem //
    //    //---------//
    //    /**
    //     * A JMenuItem which can be updated
    //     */
    //    private abstract class DynItem
    //        extends JMenuItem
    //    {
    //        //~ Constructors -------------------------------------------------------
    //
    //        public DynItem ()
    //        {
    //            // Record the instance
    //            dynItems.add(this);
    //
    //            // Initially update the item
    //            update();
    //
    //            setEnabled(false);
    //        }
    //
    //        //~ Methods ------------------------------------------------------------
    //
    //        public abstract void update ();
    //    }
    //
    //    //----------------//
    //    // AdditionalItem //
    //    //----------------//
    //    /**
    //     * Used to host additional JMenuItem, without being directly updated
    //     */
    //    private static class AdditionalItem
    //        extends JMenuItem
    //    {
    //        //~ Constructors -------------------------------------------------------
    //
    //        public AdditionalItem (String text)
    //        {
    //            super(text);
    //            setEnabled(false);
    //        }
    //    }
    //
    //    //-----------//
    //    // ChordItem //
    //    //-----------//
    //    /**
    //     * Used to host information about the first chord of the translation,
    //     * while the subsequent ones are hosted in additional items
    //     */
    //    private class ChordItem
    //        extends DynItem
    //    {
    //        //~ Methods ------------------------------------------------------------
    //
    //        public void update ()
    //        {
    //            // Remove all subsequent Chord additional items
    //            int pos = popup.getComponentIndex(this);
    //
    //            if (pos != -1) {
    //                int index = pos + 1;
    //
    //                while (popup.getComponentCount() > index) {
    //                    Component comp = popup.getComponent(index);
    //
    //                    if (comp instanceof AdditionalItem) {
    //                        popup.remove(index);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //            }
    //
    //            Set<Glyph> glyphs = page.getSheet()
    //                                    .getVerticalLag()
    //                                    .getSelectedGlyphSet();
    //
    //            if ((glyphs != null) && !glyphs.isEmpty()) {
    //                Glyph glyph = glyphs.iterator()
    //                                    .next();
    //
    //                if (glyph.isTranslated()) {
    //                    int itemNb = 0;
    //
    //                    for (Object entity : glyph.getTranslations()) {
    //                        if (entity instanceof Note) {
    //                            Note note = (Note) entity;
    //                            itemNb++;
    //
    //                            if (itemNb == 1) {
    //                                setText(note.getChord().toShortString());
    //                            } else {
    //                                popup.add(
    //                                    new AdditionalItem(
    //                                        note.getChord().toShortString()));
    //                            }
    //                        }
    //                    }
    //                } else {
    //                    setText("");
    //                }
    //            } else {
    //                setText("");
    //            }
    //        }
    //    }

    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
        extends DynAction
    {
        //~ Instance fields ----------------------------------------------------

        /** Concrete popup menu */
        private final JMenu popup;

        //~ Constructors -------------------------------------------------------

        //-------------//
        // MeasureMenu //
        //-------------//
        /**
         * Create the popup menu
         *
         * @param page the related page
         */
        public MeasureMenu ()
        {
            popup = new JMenu("Measure");
            defineLayout();
        }

        //~ Methods ------------------------------------------------------------

        public JMenu getPopup ()
        {
            return popup;
        }

        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        @Override
        public void update ()
        {
            popup.setEnabled(measure != null);

            if (measure != null) {
                popup.setText("Measure #" + measure.getScoreId() + " ...");
            } else {
                popup.setText("no measure");
            }
        }

        private void defineLayout ()
        {
            // Measure
            popup.add(new JMenuItem(new PlayAction()));
            popup.add(new JMenuItem(new DumpAction()));
        }

        //~ Inner Classes ------------------------------------------------------

        //------------//
        // DumpAction //
        //------------//
        /**
         * Dump the current measure
         */
        private class DumpAction
            extends DynAction
        {
            //~ Constructors ---------------------------------------------------

            public DumpAction ()
            {
                putValue(NAME, "Dump voices");
                putValue(
                    SHORT_DESCRIPTION,
                    "Dump the voices of the selected measure");
            }

            //~ Methods --------------------------------------------------------

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

        //------------//
        // PlayAction //
        //------------//
        /**
         * Play the current measure
         */
        private class PlayAction
            extends DynAction
        {
            //~ Constructors ---------------------------------------------------

            public PlayAction ()
            {
                putValue(NAME, "Play");
                putValue(SHORT_DESCRIPTION, "Play the selected measure");
            }

            //~ Methods --------------------------------------------------------

            public void actionPerformed (ActionEvent e)
            {
                try {
                    if (logger.isFineEnabled()) {
                        logger.fine("Play " + measure);
                    }

                    Score score = page.getScore();
                    MidiAgent.getInstance()
                             .reset();
                    new MidiActions.PlayTask(
                        score,
                        new MeasureId.MeasureRange(
                            score,
                            measure.getScoreId(),
                            measure.getScoreId())).execute();
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
    }

    //----------//
    // SlotMenu //
    //----------//
    private class SlotMenu
        extends DynAction
    {
        //~ Instance fields ----------------------------------------------------

        /** Concrete popup menu */
        private final JMenu popup;

        //~ Constructors -------------------------------------------------------

        public SlotMenu ()
        {
            popup = new JMenu("Slot");
            defineLayout();
        }

        //~ Methods ------------------------------------------------------------

        public JMenu getPopup ()
        {
            return popup;
        }

        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        @Override
        public void update ()
        {
            popup.setEnabled(slot != null);

            if (slot != null) {
                popup.setText("Slot #" + slot.getId() + " ...");
            } else {
                popup.setText("no slot");
            }
        }

        //--------------//
        // defineLayout //
        //--------------//
        private void defineLayout ()
        {
            // Slot
            popup.add(new JMenuItem(new DumpChordsAction()));
            popup.add(new JMenuItem(new DumpVoicesAction()));
        }

        //~ Inner Classes ------------------------------------------------------

        //------------------//
        // DumpChordsAction //
        //------------------//
        /**
         * Dump the chords of the current slot
         */
        private class DumpChordsAction
            extends DynAction
        {
            //~ Constructors ---------------------------------------------------

            public DumpChordsAction ()
            {
                putValue(NAME, "Dump chords");
                putValue(
                    SHORT_DESCRIPTION,
                    "Dump the chords of the selected slot");
            }

            //~ Methods --------------------------------------------------------

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

        //------------------//
        // DumpVoicesAction //
        //------------------//
        /**
         * Dump the voices of the current slot
         */
        private class DumpVoicesAction
            extends DynAction
        {
            //~ Constructors ---------------------------------------------------

            public DumpVoicesAction ()
            {
                putValue(NAME, "Dump voices");
                putValue(
                    SHORT_DESCRIPTION,
                    "Dump the voices of the selected slot");
            }

            //~ Methods --------------------------------------------------------

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
    }
}
