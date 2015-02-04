//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        D e l t a V i e w                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.run.RunTable;

import omr.score.ui.PaintingParameters;

import omr.sheet.Picture;
import omr.sheet.Sheet;

import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Class {@code DeltaView} handles the view dedicated to the
 * differences between input and output data of a sheet.
 *
 * @author Hervé Bitteur
 */
public class DeltaView
        extends ScrollView
        implements PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DeltaView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying sheet. */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // DeltaView //
    //-----------//
    /**
     * Create a new {@code DeltaView} instance, dedicated to a sheet.
     *
     * @param sheet the related sheet
     */
    public DeltaView (Sheet sheet)
    {
        this.sheet = sheet;

        view = new MyView();
        view.setName("Delta-View");
        view.setPreferredSize(sheet.getDimension());

        // Inject dependency of pixel location
        view.setLocationService(sheet.getLocationService());

        // Listen to all painting parameters
        PaintingParameters.getInstance()
                .addPropertyChangeListener(new WeakPropertyChangeListener(this));

        // Insert view
        setView(view);
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
        //~ Constructors ---------------------------------------------------------------------------

        public MyView ()
        {
            setModelSize(sheet.getDimension());
        }

        //~ Methods --------------------------------------------------------------------------------
        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            BufferedImage img = new BufferedImage(
                    sheet.getWidth(),
                    sheet.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D gbi = img.createGraphics();

            // Anti-aliasing OFF
            gbi.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle clip = g.getClipBounds();

            if (clip != null) {
                gbi.setClip(clip.x, clip.y, clip.width, clip.height);
            }

            Color veryLight = new Color(222, 222, 200);
            RunTable input = sheet.getPicture().getTable(Picture.TableKey.BINARY);
            gbi.setColor(Color.BLACK);
            gbi.setComposite(AlphaComposite.SrcOver);

            PaintingParameters painting = PaintingParameters.getInstance();
            logger.debug("PaintingLayer:{}", painting.getPaintingLayer());

            switch (painting.getPaintingLayer()) {
            case INPUT:
                /** Display NEGATIVES. */
                input.render(gbi);
                gbi.setComposite(AlphaComposite.DstOut);
                renderOutput(gbi);

                break;

            case INPUT_OUTPUT:
                /** Display POSITIVES. */
                gbi.setColor(veryLight); // Could be totally white...
                input.render(gbi);
                gbi.setComposite(AlphaComposite.SrcIn);
                gbi.setColor(Color.BLACK);
                renderOutput(gbi);

                break;

            case OUTPUT:
                /** Display FALSE_POSITIVES. */
                renderOutput(gbi);
                gbi.setComposite(AlphaComposite.DstOut);
                input.render(gbi);

                break;

            default:
                assert false : "Unhandled PaintingLayer";
            }

            gbi.dispose();

            // Draws the buffered image.
            g.drawImage(img, null, 0, 0);
        }

        private void renderOutput (Graphics2D g)
        {
            ///////////////////////////////////////////////////sheet.getPage().accept(new PagePhysicalPainter(g, false, true, false));
        }
    }
}
