//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P i c t u r e V i e w                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.log.LogUtil;

import omr.run.RunTable;

import omr.score.ui.PageMenu;
import omr.score.ui.PaintingParameters;

import omr.sheet.Picture;
import omr.sheet.Sheet;

import omr.ui.Colors;
import omr.ui.selection.MouseMovement;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PictureView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Link with sheet. */
    private final Sheet sheet;

    /** Pop-up page menu. */
    private final PageMenu pageMenu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new {@code PictureView} instance, dedicated to a sheet.
     *
     * @param sheet the related sheet
     */
    public PictureView (Sheet sheet)
    {
        this.sheet = sheet;

        view = new MyView();
        view.setName("Picture-View");
        view.setPreferredSize(new Dimension(sheet.getWidth(), sheet.getHeight()));

        // Inject dependency of pixel location
        view.setLocationService(sheet.getLocationService());

        // Listen to all painting parameters
        PaintingParameters.getInstance()
                .addPropertyChangeListener(new WeakPropertyChangeListener(this));

        // Insert view
        setView(view);

        pageMenu = new PageMenu(sheet);
        pageMenu.addMenu(new ExtractionMenu(sheet));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // propertyChange //
    //----------------//
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        view.repaint();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private class MyView
            extends RubberPanel
    {
        //~ Methods --------------------------------------------------------------------------------

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point pt,
                                     MouseMovement movement)
        {
            if (movement == MouseMovement.RELEASING) {
                if (pageMenu.updateMenu(getRubberRectangle())) {
                    JPopupMenu popup = pageMenu.getPopup();
                    popup.show(this, getZoom().scaled(pt.x) + 20, getZoom().scaled(pt.y) + 30);
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
            final PaintingParameters painting = PaintingParameters.getInstance();
            final boolean input = painting.isInputPainting();
            final boolean output = painting.isOutputPainting();
            final boolean voice = painting.isVoicePainting();

            boolean ok = true;

            if (input) {
                Picture picture = sheet.getPicture();
                BufferedImage initial = picture.getInitialImage();

                if ((initial == null) && !picture.hasTableReady(Picture.TableKey.BINARY)) {
                    ok = false;
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
                            LogUtil.start(sheet);

                            return sheet.getPicture().getTable(Picture.TableKey.BINARY);
                        } finally {
                            LogUtil.stopBook();
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

            // Render the picture image (either initial or binary)
            if (input) {
                Picture picture = sheet.getPicture();
                BufferedImage initial = picture.getInitialImage();

                if (initial != null) {
                    g.drawRenderedImage(initial, null);
                } else if (table != null) {
                    table.render(g, new Point(0, 0));
                }
            }

            // Render the recognized score entities?
            if (output) {
                final boolean mixed = input;
                final boolean coloredVoices = mixed ? false : voice;
                g.setColor(mixed ? Colors.MUSIC_PICTURE : Colors.MUSIC_ALONE);
                new SheetResultPainter(sheet, g, coloredVoices, true, false).process();
            }

            g.setColor(oldColor);
        }
    }
}
