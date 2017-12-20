//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E d i t o r M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.score.ui;

import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.ui.TribesMenu;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.StaffProjector;
import org.audiveris.omr.sheet.header.HeaderBuilder;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Slot;
import org.audiveris.omr.sheet.ui.ExtractionMenu;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.GlyphListMenu;
import org.audiveris.omr.sig.ui.InterListMenu;
import org.audiveris.omr.ui.selection.LocationEvent;
import static org.audiveris.omr.ui.selection.MouseMovement.PRESSING;
import static org.audiveris.omr.ui.selection.SelectionHint.LOCATION_INIT;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.util.AbstractMouseListener;
import org.audiveris.omr.ui.view.LocationDependent;
import org.audiveris.omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class {@code EditorMenu} defines the pop-up menu which is linked to the current
 * selection in page editor view.
 * <p>
 * It points to several sub-menus: measure, slot, chords, glyphs
 *
 * @author Hervé Bitteur
 */
public class EditorMenu
        extends SheetPopupMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EditorMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private JMenuItem focusItem;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create the editor page menu.
     *
     * @param sheet the related sheet
     */
    public EditorMenu (Sheet sheet)
    {
        super(sheet);
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public JPopupMenu getPopup ()
    {
        // Update focus
        final Inter interFocus = sheet.getInterController().getInterFocus();
        final boolean focused = interFocus != null;

        if (focused) {
            focusItem.setText("Unfocus " + interFocus);
        }

        focusItem.setVisible(focused);

        // Return the popup menu
        return super.getPopup();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        popup.add(focusItem = new FocusItem());

        addMenu(new InterListMenu(sheet));
        addMenu(new GlyphListMenu(sheet));

        if (SampleRepository.USE_TRIBES) {
            addMenu(new TribesMenu(sheet));
        }

        addMenu(new MeasureMenu());
        addMenu(new SlotMenu());
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
    //-----------//
    // FocusItem //
    //-----------//
    private final class FocusItem
            extends JMenuItem
            implements LocationDependent, ActionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        private Rectangle userLocation;

        //~ Constructors ---------------------------------------------------------------------------
        public FocusItem ()
        {
            super("Unset focus");

            setToolTipText("Unset user focus");
            addMouseListener(new Listener());
            addActionListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            sheet.getInterController().setInterFocus(null);
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            userLocation = rect;
        }

        //~ Inner Classes --------------------------------------------------------------------------
        private class Listener
                extends AbstractMouseListener
        {
            //~ Methods ----------------------------------------------------------------------------

            @Override
            public void mouseEntered (MouseEvent e)
            {
                // Move location to focused inter
                final Inter interFocus = sheet.getInterController().getInterFocus();
                interFocus.getSig().publish(interFocus);
            }

            @Override
            public void mouseExited (MouseEvent e)
            {
                // Restore user location
                SelectionService service = sheet.getLocationService();
                service.publish(new LocationEvent(this, LOCATION_INIT, PRESSING, userLocation));
            }
        }
    }

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
    //        private final Set<ChordInter> selectedChords = new LinkedHashSet<ChordInter>();
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
    //            SymbolsController controller = sheet.getGlyphsController();
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
                setText("Staff #" + staff.getId());
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
