//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E d i t o r M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolMenu;
import omr.glyph.ui.SymbolsController;

import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.math.GeoUtil;

import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.Slot;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.ExtractionMenu;

import omr.sig.ui.GlyphMenu;
import omr.sig.ui.InterMenu;

import omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code EditorMenu} defines the pop-up menu which is linked to the current
 * selection in page editor view.
 * <p>
 * It points to several sub-menus: measure, slot, chords, glyphs
 *
 * @author Hervé Bitteur
 */
public class EditorMenu
        extends PageMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EditorMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Glyph submenu. */
    private final SymbolMenu symbolMenu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create the editor page menu.
     *
     * @param sheet      the related sheet
     * @param symbolMenu already allocated symbol menu
     */
    public EditorMenu (Sheet sheet,
                       SymbolMenu symbolMenu)
    {
        super(sheet);
        this.symbolMenu = symbolMenu;
        defineLayout(symbolMenu.getMenu());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the pop-up menu according to the currently selected items.
     *
     * @param rect the selected rectangle, if any
     * @return true if not empty
     */
    @Override
    public boolean updateMenu (Rectangle rect)
    {
        // Update symbol menu (which is a specific case)
        int symbolNb = symbolMenu.updateMenu(sheet.getNest().getSelectedGlyphSet());
        symbolMenu.getMenu().setVisible(symbolNb > 0);

        return super.updateMenu(rect);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (JMenu symbolMenu)
    {
        addMenu(new InterMenu(sheet));
        addMenu(new GlyphMenu(sheet));
        addMenu(new MeasureMenu());
        addMenu(new SlotMenu());
        addMenu(new ChordMenu());
        addMenu(symbolMenu);
        addMenu(new StaffMenu());
        addMenu(new ExtractionMenu(sheet));
    }

    private Slot getCurrentSlot (Point point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            return sheet.getSymbolsEditor().getSlotAt(point);
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // ChordMenu //
    //-----------//
    /**
     * Dump the chords that translate the selected glyphs.
     */
    private class ChordMenu
            extends LocationDependentMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Selected chords. */
        private final Set<Chord> selectedChords = new HashSet<Chord>();

        //~ Constructors ---------------------------------------------------------------------------
        public ChordMenu ()
        {
            super("Chord");
            add(new JMenuItem(new DumpAction()));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void updateUserLocation (Rectangle rect)
        {
            SymbolsController controller = sheet.getSymbolsController();
            Set<Glyph> glyphs = controller.getNest().getSelectedGlyphSet();
            selectedChords.clear();
            setText("");

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
                    List<Chord> chordList = new ArrayList<Chord>(selectedChords);
                    Collections.sort(chordList, Chord.byAbscissa);

                    StringBuilder sb = new StringBuilder();

                    for (Chord chord : chordList) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }

                        sb.append("Chord #").append(chord.getId());
                    }

                    sb.append(" ...");
                    setText(sb.toString());
                }
            }

            setVisible(!selectedChords.isEmpty());
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Dump the current chord(s)
         */
        private class DumpAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public DumpAction ()
            {
                putValue(NAME, "Dump chord(s)");
                putValue(SHORT_DESCRIPTION, "Dump the selected chord(s)");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                // Dump the selected chords
                for (Chord chord : selectedChords) {
                    logger.info(chord.toString());
                }
            }
        }
    }

    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
            extends LocationDependentMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Selected measure. */
        private Measure measure;

        //~ Constructors ---------------------------------------------------------------------------
        public MeasureMenu ()
        {
            super("Measure");
            add(new JMenuItem(new DumpAction()));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void updateUserLocation (Rectangle rect)
        {
            Slot slot = getCurrentSlot(GeoUtil.centerOf(rect));

            if (slot != null) {
                measure = slot.getMeasure();
            } else {
                measure = null;
            }

            setVisible(measure != null);

            if (measure != null) {
                setText("Measure #" + measure.getScoreId() + " ...");
            }
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Dump the current measure
         */
        private class DumpAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public DumpAction ()
            {
                putValue(NAME, "Dump voices");
                putValue(SHORT_DESCRIPTION, "Dump the voices of the selected measure");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                measure.printVoices(null);
            }
        }
    }

    //----------//
    // SlotMenu //
    //----------//
    private class SlotMenu
            extends LocationDependentMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Selected slot. */
        private Slot slot;

        //~ Constructors ---------------------------------------------------------------------------
        public SlotMenu ()
        {
            super("Slot");
            add(new JMenuItem(new DumpSlotChordsAction()));
            add(new JMenuItem(new DumpVoicesAction()));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void updateUserLocation (Rectangle rect)
        {
            List<SystemInfo> systems = sheet.getSystems();

            if (systems != null) {
                slot = sheet.getSymbolsEditor().getSlotAt(GeoUtil.centerOf(rect));
            } else {
                slot = null;
            }

            setVisible(slot != null);

            if (slot != null) {
                setText("Slot #" + slot.getId() + " ...");
            }
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Dump the chords of the current slot
         */
        private class DumpSlotChordsAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public DumpSlotChordsAction ()
            {
                putValue(NAME, "Dump chords");
                putValue(SHORT_DESCRIPTION, "Dump the chords of the selected slot");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                logger.info(slot.toChordString());
            }
        }

        /**
         * Dump the voices of the current slot
         */
        private class DumpVoicesAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public DumpVoicesAction ()
            {
                putValue(NAME, "Dump voices");
                putValue(SHORT_DESCRIPTION, "Dump the voices of the selected slot");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                logger.info(slot.toVoiceString());
            }
        }
    }

    //-----------//
    // StaffMenu //
    //-----------//
    private class StaffMenu
            extends LocationDependentMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        private StaffInfo staff;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create the staff menu
         */
        public StaffMenu ()
        {
            super("Staff");
            add(new JMenuItem(new PlotAction()));
            add(new JMenuItem(new PlotDmzAction()));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void updateUserLocation (Rectangle rect)
        {
            StaffManager staffManager = sheet.getStaffManager();
            staff = staffManager.getStaffAt(GeoUtil.centerOf(rect));
            setVisible(staff != null);

            if (staff != null) {
                setText("Staff #" + staff.getId() + " ...");
            }
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Plot the x-axis projection of the current staff.
         */
        private class PlotAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public PlotAction ()
            {
                putValue(NAME, "Staff projection");
                putValue(SHORT_DESCRIPTION, "Display staff horizontal projection");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                sheet.getGridBuilder().barsRetriever.plot(staff);
            }
        }
        /**
         * Plot the x-axis projection of the current staff DMZ.
         */
        private class PlotDmzAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public PlotDmzAction ()
            {
                putValue(NAME, "DMZ projection");
                putValue(SHORT_DESCRIPTION, "Display staff DMZ horizontal projection");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                for (SystemInfo system : sheet.getSystems()) {
                    if (system.getStaves().contains(staff)) {
                        system.keysBuilder.plot(staff);
                        return;
                    }
                }
            }
        }
    }
}
