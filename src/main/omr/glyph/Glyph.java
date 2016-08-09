//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           G l y p h                                            //
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
package omr.glyph;

import omr.image.Table;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import omr.run.RunTable;

import omr.sig.inter.Inter;

import ij.process.ByteProcessor;

import java.awt.Point;
import java.awt.Shape;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Glyph} is a symbol made of a fixed set of pixels.
 * <p>
 * A glyph is un-mutable, meaning one cannot add or remove pixels to/from an existing instance,
 * although one can always create another glyph instance with the proper collection of pixels.
 * Instead of Glyph, see {@link omr.glyph.dynamic.SectionCompound} class to deal with growing
 * compounds of sections.
 * <p>
 * A glyph is implemented and persisted as a run table located at a given absolute origin.
 * <p>
 * A glyph has no intrinsic orientation, hence some methods such as {@link #getLength} require that
 * view orientation be provided as a parameter.
 * <p>
 * A glyph has no shape, see {@link Inter} class for glyph interpretation and
 * {@link omr.classifier.Sample} class for training of shape classifiers.
 * <p>
 * Additional features are made available via the {@link Glyphs} class.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicGlyph.Adapter.class)
public interface Glyph
        extends Symbol, NearLine
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Fill the provided table with glyph foreground pixels.
     *
     * @param table       (output) the table to fill
     * @param tableOrigin absolute origin of table
     * @param fat         true to add touching locations
     */
    void fillTable (Table.UnsignedByte table,
                    Point tableOrigin,
                    boolean fat);

    /**
     * Report the glyph ART moments.
     *
     * @return the glyph ART moments
     */
    ARTMoments getARTMoments ();

    /**
     * Report a buffer of the glyph (which can be handed to the OCR)
     *
     * @return a black & white buffer (contour box size )
     */
    ByteProcessor getBuffer ();

    /**
     * Report the glyph geometric moments.
     *
     * @param interline the global sheet interline
     * @return the glyph geometric moments
     */
    GeometricMoments getGeometricMoments (int interline);

    /**
     * Report the containing glyph index
     *
     * @return the containing index
     */
    GlyphIndex getIndex ();

    /**
     * Report the underlying table of runs
     *
     * @return the glyph runTable
     */
    RunTable getRunTable ();

    /**
     * Report a short glyph reference
     *
     * @return glyph reference
     */
    String idString ();

    /**
     * Report whether the glyph has a pixel in common with the provided table.
     *
     * @param table       the table of pixels
     * @param tableOrigin top-left corner of table
     * @return true if connection found
     */
    boolean intersects (Table.UnsignedByte table,
                        Point tableOrigin);

    /**
     * Check whether the glyph intersects the provided AWT shape.
     *
     * @param shape the provided awt shape
     * @return true if intersection is not empty, false otherwise
     */
    boolean intersects (Shape shape);

    /**
     * Report whether this glyph is identical to that glyph
     *
     * @param that the other glyph
     * @return true if their pixels are identical
     */
    boolean isIdentical (Glyph that);

    /**
     * Test whether the glyph is transient (not yet inserted into the index)
     *
     * @return true if transient
     */
    boolean isTransient ();

    /**
     * Report whether this glyph is virtual (rather than real)
     *
     * @return true if virtual
     */
    boolean isVirtual ();

    /**
     * The setter for glyph nest.
     *
     * @param index the containing glyph index
     */
    void setIndex (GlyphIndex index);
}
