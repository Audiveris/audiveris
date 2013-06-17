//----------------------------------------------------------------------------//
//                                                                            //
//                              P a g e M e n u                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolMenu;
import omr.glyph.ui.SymbolsController;

import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.BoundaryEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class {@code PageMenu} defines the popup menu which is linked to the
 * current selection in page editor view.
 * <p>It points to several sub-menus: measure, slot, chords, glyphs, boundaries
 * </p>
 *
 * @author Hervé Bitteur
 */
public class PageMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(PageMenu.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The related page. */
    private final Page page;

    /** Actions to update according to current selections. */
    private final Collection<DynAction> dynActions = new HashSet<>();

    /** Concrete popup menu. */
    private final JPopupMenu popup;

    /** Measure submenu. */
    private final MeasureMenu measureMenu = new MeasureMenu();

    /** Slot submenu. */
    private final SlotMenu slotMenu = new SlotMenu();

    /** Chord submenu. */
    private final ChordMenu chordMenu = new ChordMenu();

    /** Glyph submenu. */
    private final SymbolMenu symbolMenu;

    /** Boundary submenu. */
    private final BoundaryEditor boundaryEditor;

    // Context
    //
    /** Selected system. */
    private ScoreSystem system;

    /** Selected measure. */
    private Measure measure;

    /** Selected slot. */
    private Slot slot;

    /** Selected chords. */
    private Set<Chord> selectedChords;

    /** Number of chords referred to. */
    private int chordNb = 0;

    /** Number of glyphs in the selection. */
    private int glyphNb = 0;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // PageMenu //
    //----------//
    /**
     * Create the page menu.
     *
     * @param page the related page
     */
    public PageMenu (Page page,
                     SymbolMenu symbolMenu)
    {
        this.page = page;
        this.symbolMenu = symbolMenu;

        boundaryEditor = page.getSheet().getBoundaryEditor();

        popup = new JPopupMenu();
        defineLayout();

        // Initialize all dynamic actions
        for (DynAction action : dynActions) {
            action.update();
        }
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // getPopup //
    //----------//
    /**
     * Report the concrete popup menu.
     */
    public JPopupMenu getPopup ()
    {
        return popup;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the popup menu according to the currently selected glyphs.
     *
     * @param point the point designated in the page display
     */
    public void updateMenu (Point point)
    {
        // Analyze the context to retrieve designated system, measure & slot
        Sheet sheet = page.getSheet();
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            SystemInfo systemInfo = sheet.getSystemOf(point);
            if (systemInfo != null) {
                system = systemInfo.getScoreSystem();
            }

            slot = sheet.getSymbolsEditor().getSlotAt(point);
            if (slot != null) {
                measure = slot.getMeasure();
            } else {
                measure = null;
            }

            // Update all dynamic actions accordingly
            for (DynAction action : dynActions) {
                action.update();
            }
        }

        // Update symbol menu
        glyphNb = symbolMenu.updateMenu();

        // Update chord menu
        chordNb = chordMenu.updateMenu();

        // Update boundary menu
        boundaryEditor.updateMenu();

        defineLayout();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        popup.removeAll();

        // Measure
        if (measure != null) {
            popup.add(measureMenu.getMenu());
        }

        // Slot
        if (slot != null) {
            popup.add(slotMenu.getMenu());
        }

        // Related chords?
        if (chordNb > 0) {
            popup.add(chordMenu.getMenu());
        }

        // Symbol
        if (glyphNb > 0) {
            popup.add(symbolMenu.getMenu());
        }

        // Boundary
        popup.add(boundaryEditor.getMenu());
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // DynAction //
    //-----------//
    /**
     * Base implementation, to register the dynamic actions that need
     * to be updated according to the current glyph selection context.
     */
    public abstract class DynAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DynAction ()
        {
            // Record the instance
            dynActions.add(this);
        }

        //~ Methods ------------------------------------------------------------
        public abstract void update ();
    }

    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
            extends DynAction
    {
        //~ Instance fields ----------------------------------------------------

        /** Concrete menu */
        private final JMenu menu;

        //~ Constructors -------------------------------------------------------
        /**
         * Create the popup menu
         *
         * @param page the related page
         */
        public MeasureMenu ()
        {
            menu = new JMenu("Measure");
            defineLayout();
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        public JMenu getMenu ()
        {
            return menu;
        }

        @Override
        public void update ()
        {
            menu.setEnabled(measure != null);

            if (measure != null) {
                menu.setText("Measure #" + measure.getScoreId() + " ...");
            } else {
                menu.setText("no measure");
            }
        }

        private void defineLayout ()
        {
            // Measure
            ///menu.add(new JMenuItem(new PlayAction()));
            menu.add(new JMenuItem(new DumpAction()));
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
                putValue(SHORT_DESCRIPTION,
                        "Dump the voices of the selected measure");
            }

            //~ Methods --------------------------------------------------------
            @Override
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
    }

    //-----------//
    // ChordMenu //
    //-----------//
    /**
     * Dump the chords that translate the selected glyphs.
     */
    private class ChordMenu
            extends DynAction
    {
        //~ Instance fields ----------------------------------------------------

        /** Concrete menu */
        private final JMenu menu;

        //~ Constructors -------------------------------------------------------
        public ChordMenu ()
        {
            menu = new JMenu("Chord");
            defineLayout();
        }

        public JMenuItem getMenu ()
        {
            return menu;
        }

        @Override
        public void update ()
        {
            if (chordNb > 0) {
                menu.setText("Chords");
            } else {
                menu.setText("no chords");
            }
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Void
        }

        public int updateMenu ()
        {
            SymbolsController controller = page.getSheet().getSymbolsController();
            Set<Glyph> glyphs = controller.getNest().getSelectedGlyphSet();
            selectedChords = new HashSet<>();

            if (glyphs != null) {
                for (Glyph glyph : glyphs) {
                    for (Object obj : glyph.getTranslations()) {
                        if (obj instanceof Note) {
                            Note note = (Note) obj;
                            Chord chord = note.getChord();
                            if (chord != null) {
                                selectedChords.add(chord);
                            }
                        } else if (obj instanceof Chord) {
                            selectedChords.add((Chord) obj);
                        }
                    }
                }

                if (!selectedChords.isEmpty()) {
                    List<Chord> chordList = new ArrayList<>(selectedChords);
                    Collections.sort(chordList, Chord.byAbscissa);

                    StringBuilder sb = new StringBuilder();
                    for (Chord chord : chordList) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append("Chord #")
                                .append(chord.getId());
                    }
                    sb.append(" ...");

                    menu.setText(sb.toString());
                }
            }

            return selectedChords.size();
        }

        private void defineLayout ()
        {
            menu.add(new JMenuItem(new DumpAction()));
        }

        //~ Inner Classes ------------------------------------------------------
        //------------//
        // DumpAction //
        //------------//
        /**
         * Dump the current chord(s)
         */
        private class DumpAction
                extends DynAction
        {
            //~ Constructors ---------------------------------------------------

            public DumpAction ()
            {
                putValue(NAME, "Dump chord(s)");
                putValue(SHORT_DESCRIPTION,
                        "Dump the selected chord(s)");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                // Dump the selected chords
                for (Chord chord : selectedChords) {
                    logger.info(chord.toString());
                }
            }

            @Override
            public void update ()
            {
                setEnabled(chordNb > 0);
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

        /** Concrete menu */
        private final JMenu menu;

        //~ Constructors -------------------------------------------------------
        public SlotMenu ()
        {
            menu = new JMenu("Slot");
            defineLayout();
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Default action is to open the menu
            assert false;
        }

        public JMenu getMenu ()
        {
            return menu;
        }

        @Override
        public void update ()
        {
            menu.setEnabled(slot != null);

            if (slot != null) {
                menu.setText("Slot #" + slot.getId() + " ...");
            } else {
                menu.setText("no slot");
            }
        }

        //--------------//
        // defineLayout //
        //--------------//
        private void defineLayout ()
        {
            // Slot
            menu.add(new JMenuItem(new DumpSlotChordsAction()));
            menu.add(new JMenuItem(new DumpVoicesAction()));
        }

        //~ Inner Classes ------------------------------------------------------
        //
        //----------------------//
        // DumpSlotChordsAction //
        //----------------------//
        /**
         * Dump the chords of the current slot
         */
        private class DumpSlotChordsAction
                extends DynAction
        {
            //~ Constructors ---------------------------------------------------

            public DumpSlotChordsAction ()
            {
                putValue(NAME, "Dump chords");
                putValue(SHORT_DESCRIPTION,
                        "Dump the chords of the selected slot");
            }

            //~ Methods --------------------------------------------------------
            @Override
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
                putValue(SHORT_DESCRIPTION,
                        "Dump the voices of the selected slot");
            }

            //~ Methods --------------------------------------------------------
            @Override
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
