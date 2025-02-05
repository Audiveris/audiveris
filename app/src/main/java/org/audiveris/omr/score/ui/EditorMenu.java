//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E d i t o r M e n u                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.ui.TribesMenu;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.ScoreReduction;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.StaffProjector;
import org.audiveris.omr.sheet.header.HeaderBuilder;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Slot;
import org.audiveris.omr.sheet.ui.ExtractionMenu;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.ChordListMenu;
import org.audiveris.omr.sig.ui.GlyphListMenu;
import org.audiveris.omr.sig.ui.InterListMenu;
import org.audiveris.omr.sig.ui.UITaskList.Option;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.action.Preferences;
import org.audiveris.omr.ui.view.LocationDependentMenu;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.VoidTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 * Class <code>EditorMenu</code> defines the pop-up menu which is linked to the current
 * selection in page editor view.
 * <p>
 * It points to several sub-menus:
 * chords, inters, glyphs, slot, measure, staff, part, system, page, score, extraction
 *
 * @author Hervé Bitteur
 */
public class EditorMenu
        extends SheetPopupMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

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
        popup.setName("EditorMenu");
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        addMenu(new ChordListMenu(sheet));
        addMenu(new InterListMenu(sheet));
        addMenu(new GlyphListMenu(sheet));

        if (Preferences.Topic.SAMPLES.isSet() && SampleRepository.USE_TRIBES) {
            addMenu(new TribesMenu(sheet));
        }

        addMenu(new SlotMenu());
        addMenu(new MeasureMenu());
        addMenu(new StaffMenu());
        addMenu(new PartMenu());
        addMenu(new SystemMenu());
        addMenu(new PageMenu());
        addMenu(new ScoreMenu());
        addMenu(new ExtractionMenu(sheet));
    }

    //-------------------//
    // getCurrentMeasure //
    //-------------------//
    private Measure getCurrentMeasure (Point2D point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            return sheet.getSheetEditor().getStrictMeasureAt(point);
        }

        return null;
    }

    //----------------//
    // getCurrentPage //
    //----------------//
    private Page getCurrentPage (Point2D point)
    {
        if (sheet.getPages().size() == 1) {
            return sheet.getPages().get(0);
        }

        List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(point);

        Page p = null;

        for (SystemInfo system : systems) {
            if (p == null) {
                p = system.getPage();
            } else if (p != system.getPage()) {
                // Several pages were selected
                return null;
            }
        }

        return p;
    }

    //----------------//
    // getCurrentSlot //
    //----------------//
    private Slot getCurrentSlot (Point2D point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            return sheet.getSheetEditor().getStrictSlotAt(point);
        }

        return null;
    }

    //------------------//
    // getCurrentSystem //
    //------------------//
    /**
     * Report the single current system that contains the provided point.
     * <p>
     * If the provided point is located between 2 systems, no system is reported.
     *
     * @param point provided point
     * @return the selected system or null
     */
    private SystemInfo getCurrentSystem (Point2D point)
    {
        List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(point);

        if (systems.size() == 1) {
            return systems.get(0);
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction maxEndShift = new Scale.Fraction(
                1.0,
                "Maximum deskewed abscissa difference between measure end");

        private final Scale.Fraction maxStaffDx = new Scale.Fraction(
                1.0,
                "Abscissa margin around staff sides");
    }

    //-------------//
    // MeasureMenu //
    //-------------//
    private class MeasureMenu
            extends LocationDependentMenu
    {
        /** Selected stack. */
        private MeasureStack stack;

        /** Selected measure. */
        private Measure measure;

        private final DumpStackAction stackAction = new DumpStackAction();

        private final RhythmAction rhythmAction = new RhythmAction();

        private final MergeAction mergeAction = new MergeAction();

        MeasureMenu ()
        {
            super("Measure");
            add(new JMenuItem(stackAction));
            add(new JMenuItem(new DumpMeasureAction()));
            add(new JMenuItem(rhythmAction));
            add(new JMenuItem(mergeAction));
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            measure = getCurrentMeasure(GeoUtil.center2D(rect));

            if (measure != null) {
                stack = measure.getStack();
            } else {
                stack = null;
            }

            setVisible(stack != null);

            if (stack != null) {
                setText("Measure #" + stack.getPageId());
            }

            stackAction.update();
            rhythmAction.update();
            mergeAction.update();
        }

        /**
         * Dump the current measure.
         */
        private class DumpMeasureAction
                extends AbstractAction
        {
            DumpMeasureAction ()
            {
                putValue(NAME, "Dump measure voices");
                putValue(SHORT_DESCRIPTION, "Dump the voices of the selected measure");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                stack.printVoices("\n", measure);
            }
        }

        /**
         * Dump the current measure stack.
         */
        private class DumpStackAction
                extends AbstractAction
        {
            DumpStackAction ()
            {
                putValue(NAME, "Dump stack voices");
                putValue(SHORT_DESCRIPTION, "Dump the voices of the selected measure stack");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                stack.printVoices("\n", null);
            }

            private void update ()
            {
                setEnabled((stack != null) && (stack.getMeasures().size() > 1));
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

                sheet.getInterController().removeInters(toRemove, Option.VALIDATED);
            }

            private void update ()
            {
                setEnabled(
                        (stack != null) && (stack != stack.getSystem().getLastStack()) && (sheet
                                .getStub().getLatestStep().compareTo(OmrStep.MEASURES) >= 0));
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
                sheet.getInterController().reprocessStackRhythm(stack);
            }

            private void update ()
            {
                if (stack == null) {
                    setEnabled(false);
                } else {
                    // Action enabled only if step >= RHYTHMS
                    setEnabled(sheet.getStub().getLatestStep().compareTo(OmrStep.RHYTHMS) >= 0);
                }
            }
        }
    }

    //----------//
    // PageMenu //
    //----------//
    private class PageMenu
            extends LocationDependentMenu
    {
        /** Selected page. */
        private Page page;

        private final RhythmAction rhythmAction = new RhythmAction();

        PageMenu ()
        {
            super("Page");
            add(new JMenuItem(rhythmAction));
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            Page newPage = getCurrentPage(GeoUtil.center2D(rect));

            if (newPage != null) {
                page = newPage;
            } else {
                page = null;
            }

            setVisible(page != null);

            if (page != null) {
                setText("Page #" + page.getId());
            }

            rhythmAction.update();
        }

        /**
         * Reprocess rhythm of the current page.
         */
        private class RhythmAction
                extends AbstractAction
        {
            RhythmAction ()
            {
                putValue(NAME, "Reprocess rhythm");
                putValue(SHORT_DESCRIPTION, "Redo rhythm and systems connection for current page");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                sheet.getInterController().reprocessPageRhythm(page);
            }

            private void update ()
            {
                if (page == null) {
                    setEnabled(false);
                } else {
                    // Action enabled only if step >= RHYTHMS
                    setEnabled(sheet.getStub().getLatestStep().compareTo(OmrStep.RHYTHMS) >= 0);
                }
            }
        }
    }

    //----------//
    // PartMenu //
    //----------//
    private class PartMenu
            extends LocationDependentMenu
    {
        /** Selected part. */
        private Part part;

        PartMenu ()
        {
            super("Part");
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            setVisible(false);

            // Make sure we point to a precise part
            final Point center = GeoUtil.center(rect);
            final SystemInfo system = getCurrentSystem(center);

            if (system == null) {
                return;
            }

            final List<Staff> staves = system.getStavesAround(center); // 1 or 2 staves
            part = staves.get(0).getPart();

            if ((staves.size() > 1) && (staves.get(1).getPart() != part)) {
                return;
            }

            if (system.getPage().getScore() == null) {
                return;
            }

            if (system.getPage().getScore().getLogicalParts() == null) {
                return;
            }

            setText("Part #" + (1 + system.getParts().indexOf(part)));
            removeAll();
            add(new LogicalMenu(part));
            setVisible(true);
        }

        /**
         * Allows to manually set the logical part that relates to this (physical) part.
         */
        private class LogicalMenu
                extends JMenu
                implements ActionListener
        {
            private final Part part; // Part involved

            private final LogicalPart currentLogical; // Part related logical, if any

            public LogicalMenu (Part part)
            {
                this.part = part;
                currentLogical = part.getLogicalPart();

                setText("Assign logical");
                setToolTipText("Assign the related logical part");

                final List<LogicalPart> logicals = part.getSystem().getPage().getScore()
                        .getLogicalParts();
                for (LogicalPart logical : logicals) {
                    final JMenuItem item = new JRadioButtonMenuItem(logical.getFullName());
                    item.setSelected(logical == currentLogical);
                    item.addActionListener(this);
                    add(item);
                }
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                final String prefName = e.getActionCommand();
                final LogicalPart prefLogical = LogicalPart.valueOf(
                        prefName,
                        part.getSystem().getPage().getScore().getLogicalParts());

                if (prefLogical != null) {
                    logger.info("Mapped {} to {}", part, prefLogical);
                    part.setLogicalPart(prefLogical, true);

                    sheet.getSheetEditor().refresh();
                    sheet.getStub().setModified(true);
                }
            }
        }
    }

    //-----------//
    // ScoreMenu //
    //-----------//
    private class ScoreMenu
            extends LocationDependentMenu
    {
        /** Selected score. */
        private Score score;

        private final PartsCollation partsCollation = new PartsCollation();

        private final LogicalsEditing logicalsEditing = new LogicalsEditing();

        ScoreMenu ()
        {
            super("Score");
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            final Page page = getCurrentPage(GeoUtil.center2D(rect));

            if (page == null) {
                return;
            }

            score = page.getScore();

            setVisible(score != null);

            if (score == null) {
                return;
            }

            setText("Score #" + score.getId());

            removeAll();

            if (score.isLogicalsLocked()) {
                // Use a banner to signal that logicals are locked
                final JMenuItem item = new JMenuItem("Logicals are locked");
                item.setBackground(new Color(200, 255, 0));
                item.setEnabled(false);
                add(item);
                addSeparator();
            }

            add(new JMenuItem(partsCollation));
            add(new JMenuItem(logicalsEditing));

            logicalsEditing.update();
            partsCollation.update();
        }

        /**
         * Edits and locks/unlocks the list of logical parts defined in score.
         */
        private class LogicalsEditing
                extends AbstractAction
        {
            LogicalsEditing ()
            {
                putValue(NAME, "Edit logical parts");
                putValue(SHORT_DESCRIPTION, "Review the definition of all logical parts in score");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                LogicalPartsEditor editor = score.getLogicalsEditor();
                if (editor == null) {
                    score.setLogicalsEditor(editor = new LogicalPartsEditor(score));
                }

                OmrGui.getApplication().show(editor.getComponent());
            }

            private void update ()
            {
                setEnabled(false);

                // Action enabled only if score exists with logical parts
                if (score != null) {
                    final List<LogicalPart> logicals = score.getLogicalParts();
                    setEnabled(logicals != null && !logicals.isEmpty());
                }
            }
        }

        /**
         * Collects and combines all (physical) parts in score into logical parts.
         */
        private class PartsCollation
                extends AbstractAction
        {
            PartsCollation ()
            {
                putValue(NAME, "Collate parts");
                putValue(SHORT_DESCRIPTION, "Collate all parts in score into logical parts");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                final List<SheetStub> theStubs = score.getBook().getValidSelectedStubs();
                // Done asynchronously, since some sheets may need to get loaded
                new VoidTask()
                {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        new ScoreReduction(score).reduce(theStubs);
                        return null;
                    }

                    @Override
                    protected void succeeded (Void result)
                    {
                        final SheetStub currentStub = StubsController.getCurrentStub();
                        currentStub.getSheet().getSheetEditor().refresh();
                        currentStub.getBook().setModified(true);
                    }

                }.execute();
            }

            private void update ()
            {
                setEnabled(score != null);
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
                slot = sheet.getSheetEditor().getStrictSlotAt(GeoUtil.center2D(rect));
            } else {
                slot = null;
            }

            setVisible(slot != null);

            if (slot != null) {
                setText("Slot #" + slot.getId() + " @" + slot.getTimeOffset());
            }
        }

        /**
         * Dump the chords of the current slot.
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
        }

        /**
         * Dump the voices of the current slot.
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
        }
    }

    //-----------//
    // StaffMenu //
    //-----------//
    private class StaffMenu
            extends LocationDependentMenu
    {
        private Staff staff;

        private StaffEditingAction editing = new StaffEditingAction();

        private LineEditingAction lineEditing = new LineEditingAction();

        private Point2D center; // Current user location

        /**
         * Create the staff menu
         */
        StaffMenu ()
        {
            super("Staff");

            add(new JMenuItem(editing));
            add(new JMenuItem(lineEditing));

            if (Preferences.Topic.PLOTS.isSet()) {
                addSeparator();
                add(new JMenuItem(new PlotAction()));
                add(new JMenuItem(new PlotHeaderAction()));
            }
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            final StaffManager staffManager = sheet.getStaffManager();
            center = GeoUtil.center2D(rect);
            staff = staffManager.getStrictStaffAt(center);
            setVisible(staff != null);

            if (staff != null) {
                setText("Staff #" + staff.getId());

                final int margin = sheet.getScale().toPixels(constants.maxStaffDx);
                final double x = center.getX();
                final int left = staff.getAbscissa(HorizontalSide.LEFT);
                final int right = staff.getAbscissa(HorizontalSide.RIGHT);

                editing.setEnabled(true);
                lineEditing.setEnabled((x >= (left - margin)) && (x <= (right + margin)));
            }
        }

        /**
         * Launch user editing on current staff line.
         */
        private class LineEditingAction
                extends AbstractAction
        {
            LineEditingAction ()
            {
                putValue(NAME, "Edit lines");
                putValue(SHORT_DESCRIPTION, "Edit the staff lines individually");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                sheet.getSheetEditor().openEditMode(staff, false);
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
                if (sheet.getStub().getLatestStep().compareTo(OmrStep.HEADERS) < 0) {
                    logger.info("HEADERS not yet available");
                    return;
                }

                for (SystemInfo system : sheet.getSystems()) {
                    if (system.getStaves().contains(staff) && !staff.isTablature()) {
                        // Allocate a HeaderBuilder instance on demand, and ask for plot
                        new HeaderBuilder(system).plot(staff);

                        return;
                    }
                }
            }
        }

        /**
         * Launch user editing on current staff.
         */
        private class StaffEditingAction
                extends AbstractAction
        {
            StaffEditingAction ()
            {
                putValue(NAME, "Edit staff");
                putValue(SHORT_DESCRIPTION, "Edit the staff as a whole");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                sheet.getSheetEditor().openEditMode(staff, true);
            }
        }
    }

    //------------//
    // SystemMenu //
    //------------//
    private class SystemMenu
            extends LocationDependentMenu
    {
        /** Selected system. */
        private SystemInfo system;

        private final MergeAction mergeAction = new MergeAction();

        SystemMenu ()
        {
            super("System");
            add(new JMenuItem(mergeAction));
        }

        @Override
        public void updateUserLocation (Rectangle rect)
        {
            SystemInfo newSystem = getCurrentSystem(GeoUtil.center2D(rect));

            if (newSystem != null) {
                system = newSystem;
            } else {
                system = null;
            }

            setVisible(system != null);

            if (system != null) {
                setText("System #" + system.getId());
            }

            mergeAction.update();
        }

        /**
         * Merge current system with the one below.
         */
        private class MergeAction
                extends AbstractAction
        {
            MergeAction ()
            {
                putValue(NAME, "Merge with system below");
                putValue(SHORT_DESCRIPTION, "Merge this system with the one located below");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                final Scale scale = system.getSheet().getScale();
                final int maxEndShift = scale.toPixels(constants.maxEndShift);
                final List<SystemInfo> systems = sheet.getSystems();
                final SystemInfo system2 = systems.get(1 + systems.indexOf(system));

                // Check that the stacks are compatible between the two systems
                // Same number of stacks
                final List<MeasureStack> stacks = system.getStacks();
                final List<MeasureStack> stacks2 = system2.getStacks();

                if (stacks.size() != stacks2.size()) {
                    OMR.gui.displayWarning(
                            String.format(
                                    "Different measure counts %d vs %d",
                                    stacks.size(),
                                    stacks2.size()),
                            "Incompatible " + system + " & " + system2);

                    return;
                }

                // Check rough alignment of each right limit
                final Skew skew = system.getSheet().getSkew();
                final Staff staff = system.getLastStaff();
                final Staff staff2 = system2.getFirstStaff();

                for (int i = 0; i < stacks.size(); i++) {
                    MeasureStack stack = stacks.get(i);
                    Measure measure = stack.getMeasureAt(staff);
                    Point2D right = measure.getSidePoint(HorizontalSide.RIGHT, staff);
                    Point2D dsk = skew.deskewed(right);

                    MeasureStack stack2 = stacks2.get(i);
                    Measure measure2 = stack2.getMeasureAt(staff2);
                    Point2D right2 = measure2.getSidePoint(HorizontalSide.RIGHT, staff2);
                    Point2D dsk2 = skew.deskewed(right2);

                    double dx = Math.abs(dsk.getX() - dsk2.getX());

                    if (dx > maxEndShift) {
                        OMR.gui.displayWarning(
                                "Misaligned left ends between " + measure + " and " + measure2,
                                "Incompatible " + system + " & " + system2);

                        return;
                    }
                }

                if (OMR.gui.displayConfirmation(
                        "Merge " + system.toLongString() + " with " + system2.toLongString()
                                + "?")) {
                    sheet.getInterController().mergeSystem(system);
                }
            }

            private void update ()
            {
                if (system == null) {
                    setEnabled(false);
                } else {
                    // Action enabled only if system is not the last in sheet and step >= GRID
                    final List<SystemInfo> systems = sheet.getSystemManager().getSystems();
                    boolean isLast = system == systems.get(systems.size() - 1);
                    boolean gridDone = sheet.getStub().getLatestStep().compareTo(OmrStep.GRID) >= 0;
                    setEnabled(!isLast && gridDone);
                }
            }
        }
    }
}
