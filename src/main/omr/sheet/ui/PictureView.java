//----------------------------------------------------------------------------//
//                                                                            //
//                           P i c t u r e V i e w                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.score.ui.PagePhysicalPainter;
import omr.score.ui.PaintingParameters;

import omr.sheet.Sheet;

import omr.ui.Colors;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Class {@code PictureView} defines the view dedicated to the
 * display of the picture image of a music sheet.
 *
 * @author Hervé Bitteur
 */
public class PictureView
        extends ScrollView
        implements PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            PictureView.class);

    //~ Instance fields --------------------------------------------------------
    /** Link with sheet */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // PictureView //
    //-------------//
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
                .addPropertyChangeListener(
                        new WeakPropertyChangeListener(this));

        // Insert view
        setView(view);
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // propertyChange //
    //----------------//
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        view.repaint();
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private class MyView
            extends RubberPanel
    {
        //~ Methods ------------------------------------------------------------

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            Color oldColor = g.getColor();
            
            // Anti-aliasing ON
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            PaintingParameters painting = PaintingParameters.getInstance();

            // Render the picture image
            if (painting.isInputPainting()) {
                g.drawRenderedImage(sheet.getPicture().getInitialImage(), null);
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
                // Draw sort of system brackets
                //                if (sheet.getTargetBuilder() != null) {
                //                    sheet.getTargetBuilder()
                //                            .renderSystems(g); // TODO: Temporary 
                //                }
                boolean mixed = painting.isInputPainting();
                g.setColor(mixed ? Colors.MUSIC_PICTURE : Colors.MUSIC_ALONE);
                sheet.getPage()
                        .accept(
                                new PagePhysicalPainter(
                                        g,
                                        mixed ? false : painting.isVoicePainting(),
                                        true,
                                        false));
            }

            g.setColor(oldColor);
        }
    }
}
