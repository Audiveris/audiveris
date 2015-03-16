//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P i c t u r e V i e w                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.run.RunTable;

import omr.score.ui.PageMenu;
import omr.score.ui.PaintingParameters;

import omr.selection.MouseMovement;

import omr.sheet.Picture;
import omr.sheet.Sheet;

import omr.ui.Colors;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPopupMenu;

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
        view.setPreferredSize(sheet.getDimension());

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
        public void render (Graphics2D g)
        {
            Color oldColor = g.getColor();

            // Anti-aliasing OFF
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            PaintingParameters painting = PaintingParameters.getInstance();

            // Render the picture image
            if (painting.isInputPainting()) {
                Picture picture = sheet.getPicture();
                BufferedImage initial = picture.getInitialImage();

                if (initial != null) {
                    g.drawRenderedImage(initial, null);
                } else {
                    RunTable table = picture.getTable(Picture.TableKey.BINARY);

                    if (table != null) {
                        table.render(g);
                    }
                }
            } else {
                // Use a white background
                Rectangle rect = g.getClipBounds();

                if (rect != null) {
                    g.setColor(Color.WHITE);
                    g.fill(rect);
                }
            }

            // Render the recognized score entities?
            if (painting.isOutputPainting()) {
                final boolean mixed = painting.isInputPainting();
                g.setColor(mixed ? Colors.MUSIC_PICTURE : Colors.MUSIC_ALONE);

                //                final PagePhysicalPainter painter = new PagePhysicalPainter(
                //                        g,
                //                        mixed ? false : painting.isVoicePainting(),
                //                        true,
                //                        false);
                //
                //                for (Page page : sheet.getPages()) {
                //                    page.accept(painter);
                //                }
                final boolean coloredVoices = mixed ? false : painting.isVoicePainting();
                new SheetResultPainter(sheet, g, coloredVoices, true, false).process();
            }

            g.setColor(oldColor);
        }
    }
}
