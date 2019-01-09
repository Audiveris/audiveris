//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I m a g e H o l d e r                                     //
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
package org.audiveris.omr.sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class {@code ImageHolder} is a placeholder for (initial) image, backed up on disk.
 * <p>
 * It holds the reference of the initial image, at least the path to its copy on disk, and (on
 * demand) the image itself read from disk.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
public class ImageHolder
        extends DataHolder<BufferedImage>
{

    private static final Logger logger = LoggerFactory.getLogger(ImageHolder.class);

    /** Name of image format on disk: {@value}. */
    public static final String IMAGE_FORMAT = "png";

    /**
     * Creates a new {@code ImageHolder} object.
     *
     * @param pathString (sheet-relative) path to the image file
     */
    public ImageHolder (String pathString)
    {
        super(pathString);
    }

    /** No-arg constructor needed for JAXB. */
    private ImageHolder ()
    {
        super();
    }

    //------//
    // load //
    //------//
    @Override
    protected BufferedImage load (InputStream is)
            throws Exception
    {
        return ImageIO.read(is);
    }

    //-------//
    // store //
    //-------//
    @Override
    protected void store (OutputStream os)
            throws Exception
    {
        ImageIO.write(data, IMAGE_FORMAT, os);
    }
}
