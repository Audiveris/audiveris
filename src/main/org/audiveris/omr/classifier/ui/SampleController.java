//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S a m p l e C o n t r o l l e r                                //
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SampleSheet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.ui.GlyphsController;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.selection.SelectionService;

import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class {@code SampleController} is a very basic sample controller, with no location
 * service.
 *
 * @author Hervé Bitteur
 */
public class SampleController
        extends GlyphsController
{

    private static final Logger logger = LoggerFactory.getLogger(SampleController.class);

    private final SampleRepository repository;

    private final ApplicationAction removeAction;

    private final AssignAction assignAction;

    /**
     * Creates a new {@code SampleController} object.
     *
     * @param sampleModel the underlying sample model
     */
    public SampleController (SampleModel sampleModel)
    {
        super(sampleModel);
        repository = sampleModel.getRepository();

        ApplicationActionMap actionMap = OmrGui.getApplication().getContext().getActionMap(this);
        removeAction = (ApplicationAction) actionMap.get("removeSample");
        assignAction = new AssignAction();
    }

    /**
     * Assign a new shape to a sample.
     *
     * @param sample   sample at hand
     * @param newShape new sample shape
     */
    public void assignSample (Sample sample,
                              Shape newShape)
    {
        final SampleModel sampleModel = (SampleModel) model;
        final SampleSheet sampleSheet = repository.getSampleSheet(sample);

        // Add new sample
        Sample newSample = new Sample(sample.getLeft(), sample.getTop(), sample.getRunTable(),
                                      sample.getInterline(), sample.getId(), newShape, sample
                                      .getPitch());
        sampleModel.addSample(newSample, sampleSheet);

        // Remove old sample
        removeSample(sample);
    }

    @Override
    public Task<Void, Void> asyncAssignGlyph (Glyph glyph,
                                              Shape shape)
    {
        Sample sample = (Sample) glyph;
        assignSample(sample, shape);

        return null;
    }

    /**
     * Report the Assign action.
     *
     * @return the Assign action
     */
    public AssignAction getAssignAction ()
    {
        return assignAction;
    }

    @Override
    public SelectionService getLocationService ()
    {
        return null;
    }

    /**
     * Report the Remove action
     *
     * @return the Remove action
     */
    public ApplicationAction getRemoveAction ()
    {
        return removeAction;
    }

    /**
     * Remove the provided sample
     *
     * @param sample sample to remove
     */
    public void removeSample (Sample sample)
    {
        final SampleModel sampleModel = (SampleModel) model;
        sampleModel.removeSample(sample);
    }

    //--------------//
    // RemoveSample //
    //--------------//
    /**
     * Action to remove current sample
     *
     * @param e triggering event
     */
    @Action
    public void removeSample (ActionEvent e)
    {
        final SampleModel sampleModel = (SampleModel) model;
        final Sample sample = (Sample) sampleModel.getGlyphService().getSelectedEntity();
        SampleController.this.removeSample(sample);
    }

    //--------------//
    // AssignAction //
    //--------------//
    /**
     * Action to assign a new shape for a given sample.
     * <p>
     * This action can be accessed by different ways:<ul>
     * <li>By pressing the assignButton in {@link SampleBoard},
     * <li>By selecting the assign item in the private class SamplePopup menu, triggered by a
     * right-click in {@link SampleListing}.
     * </ul>
     */
    public class AssignAction
            extends AbstractAction
    {

        /** Popup used by assign button in SampleBoard. */
        public JPopupMenu popup = new JPopupMenu();

        /** Menu used by context popup in SampleListing. */
        private final JMenu menu;

        ActionListener actionListener = new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                final JMenuItem source = (JMenuItem) e.getSource();
                final Shape shape = Shape.valueOf(source.getText());
                final SampleModel sampleModel = (SampleModel) model;
                final Sample sample = (Sample) sampleModel.getGlyphService().getSelectedEntity();
                SampleController.this.assignSample(sample, shape);
            }
        };

        /**
         * Create an {@code AssignAction} object.
         */
        public AssignAction ()
        {
            super("Assign to...");
            putValue(javax.swing.Action.SHORT_DESCRIPTION, "Assign a new shape");
            ShapeSet.addAllShapes(popup, actionListener);

            // Build menu for SamplePopup
            menu = new JMenu("Assign to");
            ShapeSet.addAllShapes(menu, actionListener);
            menu.setToolTipText("Assign a new shape");
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            Object source = e.getSource();

            if (source instanceof JButton) {
                JButton button = (JButton) source;
                popup.show(button, button.getWidth(), 20);
            }
        }

        /**
         * Context popup menu.
         *
         * @return related menu
         */
        public JMenu getMenu ()
        {
            return menu;
        }

        @Override
        public void setEnabled (boolean newValue)
        {
            super.setEnabled(newValue);

            // Forward new value to containing menu in popup
            menu.setEnabled(newValue);
        }
    }
}
