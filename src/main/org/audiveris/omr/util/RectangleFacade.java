//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 R e c t a n g l e F a c a d e                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.util;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RectangleFacade} is a (hopefully temporary) fix to allow Xml binding of
 * standard class Rectangle that we cannot annotate.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rectangle")
public class RectangleFacade
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The interfaced Rectangle instance */
    private final Rectangle rectangle;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of RectangleFacade.
     */
    public RectangleFacade ()
    {
        rectangle = new Rectangle();
    }

    /**
     * Creates a new RectangleFacade object.
     *
     * @param rectangle the interfaced rectangle
     */
    public RectangleFacade (Rectangle rectangle)
    {
        this.rectangle = rectangle;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
