//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H e a d C o n t e x t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omrdataset.api;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code HeadContext} defines a context specifically meant for the processing of
 * potential head symbols.
 *
 * @author Hervé Bitteur
 */
public class HeadContext
        extends Context<HeadShape>
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(HeadContext.class);

    /** Height for symbol context, in pixels: {@value}. */
    private static final int CONTEXT_HEIGHT = 21;

    /** Width for symbol context, in pixels: {@value}. */
    private static final int CONTEXT_WIDTH = 21;

    /** Number of pixels in a patch: {@value}. */
    private static final int NUM_PIXELS = CONTEXT_HEIGHT * CONTEXT_WIDTH;

    /** Number of classes handled: {@value}. */
    private static final int NUM_CLASSES = HeadShape.values().length;

    /** Average <b>Compressed</b> size of a patch (in its .csv.zip file). */
    private static final int MEAN_PATCH_COMPRESSED_SIZE = 100;

    private static final HeadShape[] LABELS = HeadShape.values();

    // Singleton
    public static final HeadContext INSTANCE = new HeadContext();

    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    private HeadContext ()
    {

    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int getContextHeight ()
    {
        return CONTEXT_HEIGHT;
    }

    @Override
    public int getContextWidth ()
    {
        return CONTEXT_WIDTH;
    }

    @Override
    public Class<HeadShape> getLabelClass ()
    {
        return HeadShape.class;
    }

    @Override
    public HeadShape getLabel (OmrShape omrShape)
    {
        return HeadShape.toHeadShape(omrShape);
    }

    @Override
    public HeadShape[] getLabels ()
    {
        return LABELS;
    }

    @Override
    public List<String> getLabelList ()
    {
        final List<String> list = new ArrayList<>(LABELS.length);

        for (HeadShape shape : LABELS) {
            list.add(shape.toString());
        }

        return list;
    }

    @Override
    public HeadShape getLabel (int ordinal)
    {
        return LABELS[ordinal];
    }

    @Override
    public int getMeanPatchCompressedSize ()
    {
        return MEAN_PATCH_COMPRESSED_SIZE;
    }

    @Override
    public int getNumClasses ()
    {
        return NUM_CLASSES;
    }

    @Override
    public int getNumPixels ()
    {
        return NUM_PIXELS;
    }

    @Override
    public HeadShape getNone ()
    {
        return HeadShape.none;
    }

    @Override
    public String toString ()
    {
        return "HEAD";
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
