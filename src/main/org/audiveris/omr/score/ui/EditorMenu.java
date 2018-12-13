//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E d i t o r M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

    private static final Logger logger = LoggerFactory.getLogger(EditorMenu.class);

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

    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
            extends LocationDependentMenu
    {

        /** Selected measure. */
        private MeasureStack stack;

        private final RhythmAction rhythmAction = new RhythmAction();

        private final MergeAction mergeAction = new MergeAction();

        MeasureMenu ()
        {
            super("Measure");
            add(new JMenuItem(new DumpAction()));
            add(new JMenuItem(rhythmAction));
            add(new JMenuItem(mergeAction));
        }

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

        /**
         * Dump the current measure.
         */
        private class DumpAction
                extends AbstractAction
        {

            DumpAction ()
            {
                putValue(NAME, "Dump voices");
                putValue(SHORT_DESCRIPTION, "Dump the voices of the selected measure");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                stack.printVoices("\n");
            }

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
            }
        }

        /**
         * Merge current measure with the following one in current system.
         */
        private class MergeAction
                extends AbstractAction
        {

            MergeAction ()
            {
                putValue(NAME, "Merge on right");
                putValue(SHORT_DESCRIPTION, "Merge this measure stack with next one on right");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                logger.info("MergeAction for {}", stack);

                final SystemInfo system = stack.getSystem();
                final List<Part> parts = system.getParts();
                final List<Measure> measures = stack.getMeasures();

                // All the StaffBarline pieces
                List<Inter> toRemove = new ArrayList<>();

                for (int ip = 0; ip < parts.size(); ip++) {
                    Measure measure = measures.get(ip);
                    PartBarline pb = measure.getRightPartBarline();
                    toRemove.addAll(pb.getStaffBarlines());
                }

                sheet.getInterController().removeInters(
                        toRemove,
                        Option.VALIDATED,
                        Option.UPDATE_MEASURES);
            }

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
            }

            private void update ()
            {
                setEnabled(
                        (stack != null) && (stack != stack.getSystem().getLastStack()) && (sheet
                        .getStub().getLatestStep().compareTo(Step.MEASURES) >= 0));
            }
        }

        /**
         * Reprocess rhythm of the current measure.
         */
        private class RhythmAction
                extends AbstractAction
        {

            RhythmAction ()
            {
                putValue(NAME, "Reprocess rhythm");
                putValue(SHORT_DESCRIPTION, "Reprocess rhythm on the selected measure");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                sheet.getInterController().reprocessRhythm(stack);
            }

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
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

        /** Selected slot. */
        private Slot slot;

        SlotMenu ()
        {
            super("Slot");
            add(new JMenuItem(new DumpSlotChordsAction()));
            add(new JMenuItem(new DumpVoicesAction()));
        }

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

        /**
         * Dump the chords of the current slot
         */
        private class DumpSlotChordsAction
                extends AbstractAction
        {

            DumpSlotChordsAction ()
            {
                putValue(NAME, "Dump chords");
                putValue(SHORT_DESCRIPTION, "Dump the chords of the selected slot");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                logger.info(slot.toChordString());
            }

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
            }
        }

        /**
         * Dump the voices of the current slot
         */
        private class DumpVoicesAction
                extends AbstractAction
        {

            DumpVoicesAction ()
            {
                putValue(NAME, "Dump voices");
                putValue(SHORT_DESCRIPTION, "Dump the voices of the selected slot");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                logger.info(slot.toVoiceString());
            }

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
            }
        }
    }

    //-----------//
    // StaffMenu //
    //-----------//
    private class StaffMenu
            extends LocationDependentMenu
    {

        private Staff staff;

        /**
         * Create the staff menu
         */
        StaffMenu ()
        {
            super("Staff");
            add(new JMenuItem(new PlotAction()));
            add(new JMenuItem(new PlotHeaderAction()));
        }

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

        /**
         * Plot the x-axis projection of the current staff.
         */
        private class PlotAction
                extends AbstractAction
        {

            PlotAction ()
            {
                putValue(NAME, "Staff projection");
                putValue(SHORT_DESCRIPTION, "Display staff horizontal projection");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                try {
                    new StaffProjector(sheet, staff, null).plot();
                } catch (Throwable ex) {
                    logger.warn("StaffProjector error " + ex, ex);
                }
            }

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
            }
        }

        /**
         * Plot the x-axis projection of the current staff header.
         */
        private class PlotHeaderAction
                extends AbstractAction
        {

            PlotHeaderAction ()
            {
                putValue(NAME, "Header projection");
                putValue(SHORT_DESCRIPTION, "Display staff header horizontal projection");
            }

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

            @Override
            public Object clone ()
                    throws CloneNotSupportedException
            {
                return super.clone(); //To change body of generated methods, choose Tools | Templates.
            }
        }
    }
}
