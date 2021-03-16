//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P i c t u r e V i e w                                      //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.score.ui.SheetPopupMenu;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_OFF;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

/**
 * Class {@code PictureView} defines the view dedicated to the display of the picture
 * image of a music sheet.
 *
 * @author Hervé Bitteur
 */
public class PictureView
        extends ScrollView
        implements PropertyChangeListener
{

    private static final Logger logger = LoggerFactory.getLogger(PictureView.class);

    /** Link with sheet. */
    private final Sheet sheet;

    /** Pop-up page menu. */
    private final SheetPopupMenu pageMenu;

    /** Initial or Binary tab. */
    private final SheetTab sheetTab;

    /**
     * Create a new {@code PictureView} instance, dedicated to a sheet.
     *
     * @param sheet    the related sheet
     * @param sheetTab the desired tab (initial or binary)
     */
    public PictureView (Sheet sheet,
                        SheetTab sheetTab)
    {
        this.sheet = sheet;
        this.sheetTab = sheetTab;

        view = new MyView();
        view.setName("Picture-View");
        view.setPreferredSize(new Dimension(sheet.getWidth(), sheet.getHeight()));

        // Inject dependency of pixel location
        view.setLocationService(sheet.getLocationService());

        // Listen to all view parameters
        ViewParameters.getInstance().addPropertyChangeListener(
                new WeakPropertyChangeListener(this));

        // Insert view
        setView(view);

        pageMenu = new SheetPopupMenu(sheet);
        pageMenu.addMenu(new ExtractionMenu(sheet));
        pageMenu.getPopup().setName("PicturePopupMenu");
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        view.repaint();
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
            extends RubberPanel
    {

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point pt,
                                     MouseMovement movement)
        {
            if (movement == MouseMovement.RELEASING) {
                SelectionService locService = sheet.getLocationService();
                Rectangle rect = (Rectangle) locService.getSelection(LocationEvent.class);

                if (pageMenu.updateMenu(rect)) {
                    JPopupMenu popup = pageMenu.getPopup();
                    popup.show(this, getZoom().scaled(pt.x), getZoom().scaled(pt.y));
                }
            }
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (final Graphics2D g)
        {
            // Check we have all needed data
            // If not, use SwingWorker to spawn a task to retrieve the data and then do the painting
            final ViewParameters viewParams = ViewParameters.getInstance();
            final boolean input = viewParams.isInputPainting();
            final boolean output = viewParams.isOutputPainting();
            final boolean voice = viewParams.isVoicePainting();

            boolean ok = true;

            if (input) {
                final Picture picture = sheet.getPicture();

                if (sheetTab == SheetTab.GRAY_TAB) {
                    BufferedImage gray = picture.getGrayImage();
                    ok = gray != null;
                } else {
                    BufferedImage binary = picture.getImage(Picture.ImageKey.BINARY);
                    ok = binary != null;
                }
            }

            if (ok) {
                RunTable table = sheet.getPicture().getTable(Picture.TableKey.BINARY);
                doRender(g, input, output, voice, table);
            } else {
                // Spawn
                new SwingWorker<RunTable, Void>()
                {
                    @Override
                    protected RunTable doInBackground ()
                            throws Exception
                    {
                        try {
                            LogUtil.start(sheet.getStub());

                            return sheet.getPicture().getTable(Picture.TableKey.BINARY);
                        } finally {
                            LogUtil.stopStub();
                        }
                    }

                    @Override
                    protected void done ()
                    {
                        repaint();
                    }
                }.execute();
            }
        }

        private void doRender (Graphics2D g,
                               boolean input,
                               boolean output,
                               boolean voice,
                               RunTable table)
        {
            final Color oldColor = g.getColor();
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);

            if (input) {
                final Picture picture = sheet.getPicture();

                if (sheetTab == SheetTab.GRAY_TAB) {
                    final BufferedImage gray = picture.getGrayImage();
                    g.drawRenderedImage(gray, null);
                } else if (table != null) {
                    table.render(g, new Point(0, 0));
                }
            }

            // Render the recognized score entities?
            if (output) {
                final boolean coloredVoices = input ? false : voice;
                g.setColor(input ? Colors.MUSIC_PICTURE : Colors.MUSIC_ALONE);
                new SheetResultPainter(sheet, g, coloredVoices, true, false).process();
            }

            g.setColor(oldColor);
        }
    }
}
