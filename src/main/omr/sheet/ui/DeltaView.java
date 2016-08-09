//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        D e l t a V i e w                                       //
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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
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
        view.setPreferredSize(new Dimension(sheet.getWidth(), sheet.getHeight()));

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
            setModelSize(new Dimension(sheet.getWidth(), sheet.getHeight()));
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
                input.render(gbi, new Point(0, 0));
                gbi.setComposite(AlphaComposite.DstOut);
                renderOutput(gbi);

                break;

            case INPUT_OUTPUT:
                /** Display POSITIVES. */
                gbi.setColor(veryLight); // Could be totally white...
                input.render(gbi, new Point(0, 0));
                gbi.setComposite(AlphaComposite.SrcIn);
                gbi.setColor(Color.BLACK);
                renderOutput(gbi);

                break;

            case OUTPUT:
                /** Display FALSE_POSITIVES. */
                renderOutput(gbi);
                gbi.setComposite(AlphaComposite.DstOut);
                input.render(gbi, new Point(0, 0));

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
