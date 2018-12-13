//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I m a g e V i e w                                       //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.ui.view.RubberPanel;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;

/**
 * Class {@code ImageView} displays a view on an image.
 * Typically, subclasses would have to only override the {@link #renderItems} method.
 *
 * @author Hervé Bitteur
 */
public class ImageView
        extends RubberPanel
{

    private final RenderedImage image;

    /**
     * Creates a new ImageView object.
     *
     * @param image the image to display
     */
    public ImageView (RenderedImage image)
    {
        this.image = image;

        setName("Image-View");

        ////setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        setModelSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    @Override
    public void render (Graphics2D g)
    {
        g.drawRenderedImage(image, null);
    }
}
