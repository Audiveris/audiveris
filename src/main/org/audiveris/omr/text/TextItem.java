//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t I t e m                                         //
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
package org.audiveris.omr.text;

import org.audiveris.omr.math.GeoUtil;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code TextItem} is an abstract basis for any OCR entity.
 *
 * @author Hervé Bitteur
 */
public abstract class TextItem
{

    /** Lowercase vowel characters. */
    private static final char[] VOWELS = "aeiouy".toCharArray();

    /** Item bounds. */
    protected Rectangle bounds;

    /** Item value. */
    protected String value;

    //----------//
    // TextItem //
    //----------//
    /**
     * Creates a new TextItem object.
     *
     * @param bounds the bounding box of this item wrt the decoded image
     * @param value  the item string value
     */
    public TextItem (Rectangle bounds,
                     String value)
    {
        if (bounds != null) {
            this.bounds = new Rectangle(bounds);
        }

        this.value = value;
    }

    //-----------//
    // hasVowell //
    //-----------//
    /**
     * Report whether the item value contains at least one vowel.
     *
     * @return true if so
     */
    public boolean hasVowell ()
    {
        String lowerCaseValue = getValue().toLowerCase();

        for (char v : VOWELS) {
            if (lowerCaseValue.indexOf(v) != -1) {
                return true;
            }
        }

        return false;
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding box of the item.
     *
     * @return (a copy of) the box
     */
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        } else {
            return null;
        }
    }

    //-----------//
    // setBounds //
    //-----------//
    /**
     * Set a new bounding box of the item.
     *
     * @param bounds the new bounding box
     */
    public void setBounds (Rectangle bounds)
    {
        this.bounds = bounds;
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the bounds center of this item.
     *
     * @return center of item bounds
     */
    public Point2D getCenter2D ()
    {
        if (getBounds() == null) {
            return null;
        }

        return GeoUtil.center2D(bounds);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the string content of this item.
     *
     * @return the value
     */
    public String getValue ()
    {
        return value;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Modify the item value.
     *
     * @param value the new item value
     */
    protected void setValue (String value)
    {
        this.value = value;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        ///sb.append('@').append(Integer.toHexString(hashCode()));
        sb.append(internals());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of this tem
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    public void translate (int dx,
                           int dy)
    {
        if (getBounds() != null) {
            bounds.translate(dx, dy);
        }
    }

    //-----------//
    // internals //
    //-----------//
    /**
     * Report a textual description of object internals
     *
     * @return string of internals
     */
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(" \"").append(getValue()).append("\"");

        if (getBounds() != null) {
            sb.append(
                    String.format(
                            " bounds[%d,%d,%d,%d]",
                            bounds.x,
                            bounds.y,
                            bounds.width,
                            bounds.height));
        }

        return sb.toString();
    }

    //----------//
    // boundsOf //
    //----------//
    /**
     * Compute the bounding box of a collection of TextItem instances.
     *
     * @param items the provided collection of TextItem instances
     * @return the global bounding box
     */
    public static Rectangle boundsOf (Collection<? extends TextItem> items)
    {
        Rectangle bounds = null;

        for (TextItem item : items) {
            if (bounds == null) {
                bounds = item.getBounds();
            } else {
                bounds.add(item.getBounds());
            }
        }

        return bounds;
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Report the string content of a sequence of TextItem.
     *
     * @param items provided sequence
     * @return string value
     */
    public static String valueOf (List<? extends TextItem> items)
    {
        StringBuilder sb = new StringBuilder();

        for (TextItem item : items) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(item.getValue());
        }

        return sb.toString();
    }
}
