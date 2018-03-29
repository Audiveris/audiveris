//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e F a c t o r y                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.image;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code TemplateFactory} builds needed instances of {@link Template} class
 * and keeps a catalog per desired point size and shape.
 *
 * @author Hervé Bitteur
 */
public class TemplateFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TemplateFactory.class);

    /** Singleton. */
    private static final TemplateFactory INSTANCE = new TemplateFactory();

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Catalog of all templates already allocated, mapped by point size. */
    private final Map<Integer, Catalog> allSizes;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * (Private) Creates the singleton object.
     */
    private TemplateFactory ()
    {
        allSizes = new HashMap<Integer, Catalog>();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    public static TemplateFactory getInstance ()
    {
        return INSTANCE;
    }

    //------------//
    // getCatalog //
    //------------//
    /**
     * Report the template catalog dedicated to the provided interline.
     *
     * @param pointSize provided point size
     * @return the catalog of all templates for the point size value
     */
    public Catalog getCatalog (int pointSize)
    {
        Catalog catalog = allSizes.get(pointSize);

        if (catalog == null) {
            synchronized (allSizes) {
                catalog = allSizes.get(pointSize);

                if (catalog == null) {
                    allSizes.put(pointSize, catalog = new Catalog(pointSize));
                }
            }
        }

        return catalog;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Catalog //
    //---------//
    /**
     * Handles all templates for a given interline value.
     */
    public static class Catalog
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Point size value for this catalog. */
        final int pointSize;

        /** Map of all descriptors for this catalog. */
        final Map<Shape, ShapeDescriptor> descriptors = new EnumMap<Shape, ShapeDescriptor>(
                Shape.class);

        //~ Constructors ---------------------------------------------------------------------------
        public Catalog (int pointSize)
        {
            this.pointSize = pointSize;
            buildAllTemplates();
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------------//
        // getDescriptor //
        //---------------//
        public ShapeDescriptor getDescriptor (Shape shape)
        {
            return descriptors.get(shape);
        }

        //-------------//
        // getTemplate //
        //-------------//
        public Template getTemplate (Shape shape)
        {
            ShapeDescriptor descriptor = descriptors.get(shape);

            return descriptor.getTemplate();
        }

        //-------------------//
        // buildAllTemplates //
        //-------------------//
        private void buildAllTemplates ()
        {
            for (Shape shape : ShapeSet.TemplateNotes) {
                descriptors.put(shape, new ShapeDescriptor(shape, pointSize));
            }
        }
    }
}
