//----------------------------------------------------------------------------//
//                                                                            //
//                       R e c t a n g l e F a c a d e                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RectangleFacade} is a (hopefully temporary) fix to
 * allow Xml binding of standard class Rectangle that we cannot
 * annotate.
 *
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rectangle")
public class RectangleFacade
{
    //~ Instance fields --------------------------------------------------------

    /** The interfaced Rectangle instance */
    private final Rectangle rectangle;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // RectangleFacade //
    //-----------------//
    /**
     * Creates a new instance of RectangleFacade
     */
    public RectangleFacade ()
    {
        rectangle = new Rectangle();
    }

    //-----------------//
    // RectangleFacade //
    //-----------------//
    /**
     * Creates a new RectangleFacade object.
     *
     * @param rectangle the interfaced rectangle
     */
    public RectangleFacade (Rectangle rectangle)
    {
        this.rectangle = rectangle;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getHeight //
    //-----------//
    public int getHeight ()
    {
        return rectangle.height;
    }

    //--------------//
    // getRectangle //
    //--------------//
    /**
     * Report the interfaced rectangle
     *
     * @return the actual rectangle
     */
    public Rectangle getRectangle ()
    {
        return rectangle;
    }

    //----------//
    // getWidth //
    //----------//
    public int getWidth ()
    {
        return rectangle.width;
    }

    //------//
    // getX //
    //------//
    public int getX ()
    {
        return rectangle.x;
    }

    //------//
    // getY //
    //------//
    public int getY ()
    {
        return rectangle.y;
    }

    //-----------//
    // setHeight //
    //-----------//
    @XmlElement
    public void setHeight (int height)
    {
        rectangle.height = height;
    }

    //----------//
    // setWidth //
    //----------//
    @XmlElement
    public void setWidth (int width)
    {
        rectangle.width = width;
    }

    //------//
    // setX //
    //------//
    @XmlElement
    public void setX (int x)
    {
        rectangle.x = x;
    }

    //------//
    // setY //
    //------//
    @XmlElement
    public void setY (int y)
    {
        rectangle.y = y;
    }
}
