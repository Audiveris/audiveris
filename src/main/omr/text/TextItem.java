//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t I t e m                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import omr.score.common.PixelRectangle;

/**
 * Class {@code TextItem} is an abstract basis for any Ocr entity.
 *
 * @author Hervé Bitteur
 */
public abstract class TextItem
{
    //~ Instance fields --------------------------------------------------------

    /** Item bounds. */
    private PixelRectangle bounds;

    /** Item value. */
    private final String value;

    //~ Constructors -----------------------------------------------------------
    //
    //---------//
    // TextItem //
    //---------//
    /**
     * Creates a new TextItem object.
     *
     * @param bounds the bounding box of this item wrt the decoded image
     * @param value  the item string value
     */
    public TextItem (Rectangle bounds,
                     String value)
    {
        this.bounds = new PixelRectangle(bounds);
        this.value = value;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //
    //-----------//
    // setBounds //
    //-----------//
    /**
     * Set a new bounding box of the item..
     *
     * @param bounds the new bounding box
     */
    public void setBounds (PixelRectangle bounds)
    {
        this.bounds = bounds;
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding box of the item..
     *
     * @return (a copy of) the box
     */
    public PixelRectangle getBounds ()
    {
        if (bounds != null) {
            return new PixelRectangle(bounds);
        } else {
            return null;
        }
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
    public static PixelRectangle boundsOf (Collection<? extends TextItem> items)
    {
        PixelRectangle bounds = null;

        for (TextItem item : items) {
            if (bounds == null) {
                bounds = item.getBounds();
            } else {
                bounds.add(item.getBounds());
            }
        }

        return bounds;
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

    //---------//
    // valueOf //
    //---------//
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        ///sb.append('@').append(Integer.toHexString(hashCode()));

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(" \"").append(getValue()).append("\"");

        if (getBounds() != null) {
            sb.append(String.format(" bounds[%d,%d,%d,%d]",
                                    bounds.x, bounds.y,
                                    bounds.width, bounds.height));
        }

        return sb.toString();
    }
}
