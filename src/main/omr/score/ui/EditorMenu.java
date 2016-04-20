//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E d i t o r M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.ui.SymbolMenu;

import omr.math.GeoUtil;

import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.grid.StaffProjector;
import omr.sheet.header.HeaderBuilder;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Slot;
import omr.sheet.ui.ExtractionMenu;

import omr.sig.ui.GlyphMenu;
import omr.sig.ui.InterMenu;

import omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.List;

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
    /** Glyph sub-menu. */
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
        int symbolNb = symbolMenu.updateMenu(sheet.getGlyphIndex().getSelectedGlyphList());
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
        ///addMenu(new ChordMenu());
        addMenu(symbolMenu);
        addMenu(new StaffMenu());
        addMenu(new ExtractionMenu(sheet));
    }

    //-------------------//
    // getCurrentMeasure //
    //-------------------//
    private Measure getCurrentMeasure (Point point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            return sheet.getSymbolsEditor().getMeasureAt(point);
        }

        return null;
    }

    //----------------//
    // getCurrentSlot //
    //----------------//
    private Slot getCurrentSlot (Point point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            return sheet.getSymbolsEditor().getSlotAt(point);
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //    //-----------//
    //    // ChordMenu //
    //    //-----------//
    //    /**
    //     * Dump the chords that translate the selected glyphs.
    //     */
    //    private class ChordMenu
    //            extends LocationDependentMenu
    //    {
    //        //~ Instance fields ------------------------------------------------------------------------
    //
    //        /** Selected chords. */
    //        private final Set<ChordInter> selectedChords = new HashSet<ChordInter>();
    //
    //        //~ Constructors ---------------------------------------------------------------------------
    //        public ChordMenu ()
    //        {
    //            super("Chord");
    //            add(new JMenuItem(new DumpAction()));
    //        }
    //
    //        //~ Methods --------------------------------------------------------------------------------
    //        @Override
    //        public void updateUserLocation (Rectangle rect)
    //        {
    //            SymbolsController controller = sheet.getSymbolsController();
    //            Set<Glyph> glyphs = controller.getNest().getSelectedGlyphSet();
    //            selectedChords.clear();
    //            setText("");
    //
    //            if (glyphs != null) {
    //                for (Glyph glyph : glyphs) {
    //                    for (Object obj : glyph.getTranslations()) {
    //                        if (obj instanceof AbstractNoteInter) {
    //                            AbstractNoteInter note = (AbstractNoteInter) obj;
    //                            ChordInter chord = note.getChord();
    //
    //                            if (chord != null) {
    //                                selectedChords.add(chord);
    //                            }
    //                        } else if (obj instanceof ChordInter) {
    //                            selectedChords.add((ChordInter) obj);
    //                        }
    //                    }
    //                }
    //
    //                if (!selectedChords.isEmpty()) {
    //                    List<ChordInter> chordList = new ArrayList<ChordInter>(selectedChords);
    //                    Collections.sort(chordList, ChordInter.byAbscissa);
    //
    //                    StringBuilder sb = new StringBuilder();
    //
    //                    for (ChordInter chord : chordList) {
    //                        if (sb.length() > 0) {
    //                            sb.append(", ");
    //                        }
    //
    //                        sb.append("Chord #").append(chord.getId());
    //                    }
    //
    //                    sb.append(" ...");
    //                    setText(sb.toString());
    //                }
    //            }
    //
    //            setVisible(!selectedChords.isEmpty());
    //        }
    //
    //        //~ Inner Classes --------------------------------------------------------------------------
    //        /**
    //         * Dump the current chord(s)
    //         */
    //        private class DumpAction
    //                extends AbstractAction
    //        {
    //            //~ Constructors -----------------------------------------------------------------------
    //
    //            public DumpAction ()
    //            {
    //                putValue(NAME, "Dump chord(s)");
    //                putValue(SHORT_DESCRIPTION, "Dump the selected chord(s)");
    //            }
    //
    //            //~ Methods ----------------------------------------------------------------------------
    //            @Override
    //            public void actionPerformed (ActionEvent e)
    //            {
    //                // Dump the selected chords
    //                for (ChordInter chord : selectedChords) {
    //                    logger.info(chord.toString());
    //                }
    //            }
    //        }
    //    }
    //
    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
            extends LocationDependentMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Selected measure. */
        private MeasureStack stack;

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
            Measure measure = getCurrentMeasure(GeoUtil.centerOf(rect));

            if (measure != null) {
                stack = measure.getStack();
            } else {
                stack = null;
            }

            setVisible(stack != null);

            if (stack != null) {
                String id = stack.getPageId() + (stack.isCautionary() ? "C" : "");
                setText("Measure #" + id + " ...");
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
                stack.printVoices("\n");
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

        private Staff staff;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create the staff menu
         */
        public StaffMenu ()
        {
            super("Staff");
            add(new JMenuItem(new PlotAction()));
            add(new JMenuItem(new PlotHeaderAction()));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void updateUserLocation (Rectangle rect)
        {
            StaffManager staffManager = sheet.getStaffManager();
            staff = staffManager.getClosestStaff(GeoUtil.centerOf(rect));
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
                try {
                    new StaffProjector(sheet, staff, null).plot();
                } catch (Throwable ex) {
                    logger.warn("StaffProjector error " + ex, ex);
                }
            }
        }

        /**
         * Plot the x-axis projection of the current staff header.
         */
        private class PlotHeaderAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public PlotHeaderAction ()
            {
                putValue(NAME, "Header projection");
                putValue(SHORT_DESCRIPTION, "Display staff header horizontal projection");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                for (SystemInfo system : sheet.getSystems()) {
                    if (system.getStaves().contains(staff)) {
                        // Allocate a HeaderBuilder instance on demand, and ask for plot
                        new HeaderBuilder(system).plot(staff);

                        return;
                    }
                }
            }
        }
    }
}
