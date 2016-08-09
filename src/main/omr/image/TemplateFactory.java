//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e F a c t o r y                               //
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
package omr.image;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code TemplateFactory} builds needed instances of {@link Template} class
 * and keeps a catalog per desired size and shape.
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
    /** Catalog of all templates already allocated. */
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
     * @param interline provided interline
     * @return the catalog of all templates for the interline value
     */
    public Catalog getCatalog (int interline)
    {
        Catalog catalog = allSizes.get(interline);

        if (catalog == null) {
            synchronized (allSizes) {
                catalog = allSizes.get(interline);

                if (catalog == null) {
                    catalog = new Catalog(interline);
                    allSizes.put(interline, catalog);
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
     * Handles all templates or a given interline value.
     */
    public static class Catalog
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Interline value for this catalog. */
        final int interline;

        /** Map of all descriptors for this catalog. */
        final Map<Shape, ShapeDescriptor> descriptors = new EnumMap<Shape, ShapeDescriptor>(
                Shape.class);

        //~ Constructors ---------------------------------------------------------------------------
        public Catalog (int interline)
        {
            this.interline = interline;
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
                descriptors.put(shape, new ShapeDescriptor(shape, interline));
            }
        }
    }
}
