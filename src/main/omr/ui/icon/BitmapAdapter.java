//----------------------------------------------------------------------------//
//                                                                            //
//                         B i t m a p A d a p t e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.icon;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/*
 *
 *  PurchaseList - ValueType
 *  HashMap - BoundType
 */
public class BitmapAdapter
    extends XmlAdapter<String[], BufferedImage>
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BitmapAdapter object.
     */
    public BitmapAdapter ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // marshal //
    //---------//
    /**
     * Convert a bound type to a value type.
     * write Java content into class that generates desired XML
     */
    public String[] marshal (BufferedImage image)
    {
        System.out.println("BA marshal");

        return IconManager.getInstance()
                          .encodeImage(image);
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Convert a value type to a bound type.
     * read xml content and put into Java class.
     */
    public BufferedImage unmarshal (String[] rows)
    {
        System.out.println("BA unmarshal rows.length=" + rows.length);

        return IconManager.getInstance()
                          .decodeImage(rows, Color.BLACK);
    }
}
