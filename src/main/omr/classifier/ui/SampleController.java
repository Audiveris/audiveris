//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S a m p l e C o n t r o l l e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.classifier.ui;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.SampleSheet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.ui.GlyphsController;

import omr.ui.OmrGui;
import omr.ui.selection.SelectionService;

import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final SampleRepository repository;

    private final ApplicationAction removeAction;

    private final AssignAction assignAction;

    //~ Constructors -------------------------------------------------------------------------------
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
        ///assignAction = (ApplicationAction) actionMap.get("assignSample");
        assignAction = new AssignAction();
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void assignSample (Sample sample,
                              Shape newShape)
    {
        final SampleModel sampleModel = (SampleModel) model;
        final SampleSheet sampleSheet = repository.getSampleSheet(sample);

        // Add new sample
        Sample newSample = new Sample(
                sample.getLeft(),
                sample.getTop(),
                sample.getRunTable(),
                sample.getInterline(),
                sample.getId(),
                newShape,
                sample.getPitch());
        sampleModel.addSample(newSample, sampleSheet);

        // Remove old sample
        removeSample(sample);
    }

    @Override
    public Task<Void, Void> asyncAssignGlyphs (Collection<Glyph> glyphs,
                                               Shape shape,
                                               boolean compound)
    {
        for (Glyph glyph : glyphs) {
            Sample sample = (Sample) glyph;
            assignSample(sample, shape);
        }

        return null;
    }

    public AssignAction getAssignAction ()
    {
        return assignAction;
    }

    @Override
    public SelectionService getLocationService ()
    {
        return null;
    }

    public ApplicationAction getRemoveAction ()
    {
        return removeAction;
    }

    public void removeSample (Sample sample)
    {
        final SampleModel sampleModel = (SampleModel) model;
        sampleModel.removeSample(sample);
    }

    //--------------//
    // RemoveSample //
    //--------------//
    @Action
    public void removeSample (ActionEvent e)
    {
        final SampleModel sampleModel = (SampleModel) model;
        final Sample sample = (Sample) sampleModel.getGlyphService().getSelectedEntity();
        SampleController.this.removeSample(sample);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // AssignAction //
    //--------------//
    public class AssignAction
            extends AbstractAction
    {
        //~ Instance fields ------------------------------------------------------------------------

        public JMenu menu = new JMenu("Assign to...");

        public JPopupMenu popup = new JPopupMenu();

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

        //~ Constructors ---------------------------------------------------------------------------
        public AssignAction ()
        {
            super("Assign to...");
            this.putValue(javax.swing.Action.SHORT_DESCRIPTION, "Assign a new shape");

            ShapeSet.addAllShapes(menu, actionListener);
            ShapeSet.addAllShapes(popup, actionListener);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Object source = e.getSource();

            if (source instanceof JButton) {
                JButton button = (JButton) source;
                popup.show(button, button.getWidth(), 20);
            }
        }
    }
}
