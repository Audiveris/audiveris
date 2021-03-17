//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S a m p l e C o n t e x t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import static org.audiveris.omr.ui.selection.MouseMovement.PRESSING;
import static org.audiveris.omr.ui.selection.SelectionHint.LOCATION_INIT;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.view.Rubber;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.ui.view.ZoomAssembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code SampleContext} displays a sample within the context image of its
 * containing sheet.
 *
 * @author Hervé Bitteur
 */
public class SampleContext
        extends ZoomAssembly
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleContext.class);

    private static final Dimension NO_DIM = new Dimension(0, 0);

    private static final Point NO_OFFSET = new Point(0, 0);

    //~ Instance fields ----------------------------------------------------------------------------
    private final SampleRepository repository;

    private final ContextView contextView;

    private final SelectionService locationService = new SelectionService(
            "sampleLocationService",
            new Class<?>[]{LocationEvent.class});

    private EntityService<Sample> sampleService;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleContext} object.
     *
     * @param repository the underlying repository
     */
    public SampleContext (SampleRepository repository)
    {
        this.repository = repository;

        contextView = new ContextView(zoom, rubber);
        contextView.setLocationService(locationService);
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // connect //
    //---------//
    /**
     * Connect to the provided SampleService
     *
     * @param sampleService the SampleService to connect to
     */
    public void connect (EntityService<Sample> sampleService)
    {
        this.sampleService = sampleService;
        sampleService.subscribeStrongly(EntityListEvent.class, contextView);
        locationService.subscribeStrongly(LocationEvent.class, contextView);
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Update the context view with the current sample.
     */
    public void refresh ()
    {
        Sample sample = sampleService.getSelectedEntity();
        contextView.display(sample);
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of this component.
     */
    private void defineLayout ()
    {
        component.add(new ScrollView(contextView).getComponent(), BorderLayout.CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ContextView //
    //-------------//
    private class ContextView
            extends RubberPanel
    {

        /** Current sample, if any. */
        private Sample sample;

        /** RunTable of sheet image, if any. */
        private RunTable sheetTable;

        ContextView (Zoom zoom,
                     Rubber rubber)
        {
            super(zoom, rubber);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof LocationEvent) {
                    handleLocationEvent((LocationEvent) event);
                } else if (event instanceof EntityListEvent) {
                    handleEntityListEvent((EntityListEvent) event);
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }

        /**
         * Interest in EntityList
         *
         * @param listEvent list of inters
         */
        protected void handleEntityListEvent (EntityListEvent<Sample> listEvent)
        {
            // Sample => sample, sheet & location
            display(listEvent.getEntity());
        }

        @Override
        protected void handleLocationEvent (LocationEvent locationEvent)
        {
            // Location => move view focus on this location w/ markers
            showFocusLocation(locationEvent.getData(), true); // Centered: true
        }

        @Override
        protected void render (Graphics2D g)
        {
            if (sheetTable != null) {
                g.setColor(Color.LIGHT_GRAY);
                sheetTable.render(g, new Point(0, 0));
            }
        }

        @Override
        protected void renderItems (Graphics2D g)
        {
            if (sample != null) {
                g.setColor(Color.BLUE);
                sample.getRunTable()
                        .render(g, (sheetTable != null) ? sample.getTopLeft() : NO_OFFSET);
            }
        }

        private void display (Sample newSample)
        {
            sample = newSample;

            Dimension dim = NO_DIM;
            Rectangle rect = null;

            if (sample != null) {
                logger.debug("SampleContext sample:{}", sample);

                SampleSheet sampleSheet = repository.getSampleSheet(sample);

                if (sampleSheet != null) {
                    switch (sampleSheet.getImageStatus(repository)) {
                    case NO_IMAGE:
                        sheetTable = null;

                        break;

                    case ON_DISK:
                        sheetTable = repository.loadImage(sampleSheet);

                        break;

                    case LOADED:
                        sheetTable = sampleSheet.getImage();

                        break;
                    }
                } else {
                    sheetTable = null;
                }

                if (sheetTable != null) {
                    dim = sheetTable.getDimension();
                    rect = sample.getBounds();
                }
            } else {
                sheetTable = null; // To erase background image
            }

            setModelSize(dim);
            locationService.publish(new LocationEvent(this, LOCATION_INIT, PRESSING, rect));
            repaint();
        }
    }
}
