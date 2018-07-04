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
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
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
import org.audiveris.omr.sig.ui.UITaskList.Option;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.action.AdvancedTopics;
import org.audiveris.omr.ui.view.LocationDependentMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
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
        extends SheetPopupMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EditorMenu.class);

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
    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        addMenu(new InterListMenu(sheet));
        addMenu(new GlyphListMenu(sheet));

        if (AdvancedTopics.Topic.SAMPLES.isSet() && SampleRepository.USE_TRIBES) {
            addMenu(new TribesMenu(sheet));
        }

        addMenu(new MeasureMenu());
        addMenu(new SlotMenu());

        if (AdvancedTopics.Topic.PLOTS.isSet()) {
            addMenu(new StaffMenu());
        }

        addMenu(new ExtractionMenu(sheet));
    }

    //-------------------//
    // getCurrentMeasure //
    //-------------------//
    private Measure getCurrentMeasure (Point point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            return sheet.getSymbolsEditor().getStrictMeasureAt(point);
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
            return sheet.getSymbolsEditor().getStrictSlotAt(point);
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
            extends LocationDependentMenu
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Selected measure. */
        private MeasureStack stack;

        private final RhythmAction rhythmAction = new RhythmAction();

        private final MergeAction mergeAction = new MergeAction();

        //~ Constructors ---------------------------------------------------------------------------
        public MeasureMenu ()
        {
            super("Measure");
            add(new JMenuItem(new DumpAction()));
            add(new JMenuItem(rhythmAction));
            add(new JMenuItem(mergeAction));
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
                setText("Measure #" + stack.getPageId() + " ...");
            }

            rhythmAction.update();
            mergeAction.update();
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * Dump the current measure.
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

        /**
         * Merge current measure with the following one in current system.
         */
        private class MergeAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public MergeAction ()
            {
                putValue(NAME, "Merge on right");
                putValue(SHORT_DESCRIPTION, "Merge this measure stack with next one on right");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                logger.info("MergeAction for {}", stack);

                final SystemInfo system = stack.getSystem();
                final List<Part> parts = system.getParts();
                final List<Measure> measures = stack.getMeasures();

                // All the StaffBarline pieces
                List<Inter> toRemove = new ArrayList<Inter>();

                for (int ip = 0; ip < parts.size(); ip++) {
                    Measure measure = measures.get(ip);
                    PartBarline pb = measure.getRightPartBarline();
                    toRemove.addAll(pb.getStaffBarlines());
                }

                sheet.getInterController()
                        .removeInters(toRemove, Option.VALIDATED, Option.UPDATE_MEASURES);
            }

            private void update ()
            {
                setEnabled(
                        (stack != null) && (stack != stack.getSystem().getLastStack())
                        && (sheet.getStub().getLatestStep().compareTo(Step.MEASURES) >= 0));
            }
        }

        /**
         * Reprocess rhythm of the current measure.
         */
        private class RhythmAction
                extends AbstractAction
        {
            //~ Constructors -----------------------------------------------------------------------

            public RhythmAction ()
            {
                putValue(NAME, "Reprocess rhythm");
                putValue(SHORT_DESCRIPTION, "Reprocess rhythm on the selected measure");
            }

            //~ Methods ----------------------------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                sheet.getInterController().reprocessRhythm(stack);
            }

            private void update ()
            {
                if (stack == null) {
                    setEnabled(false);
                } else {
                    // Action enabled only if step >= RHYTHMS
                    setEnabled(sheet.getStub().getLatestStep().compareTo(Step.RHYTHMS) >= 0);
                }
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
                slot = sheet.getSymbolsEditor().getStrictSlotAt(GeoUtil.centerOf(rect));
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
            staff = staffManager.getStrictStaffAt(GeoUtil.centerOf(rect));
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
