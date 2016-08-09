//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 D i m e n s i o n F a c a d e                                  //
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
package omr.util;

import java.awt.Dimension;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code DimensionFacade} is a (hopefully temporary) fix to allow Xml binding of
 * standard class Dimension that we cannot annotate.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "dimension")
public class DimensionFacade
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The interfaced Dimension instance */
    private Dimension dimension;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of DimensionFacade.
     */
    public DimensionFacade ()
    {
    }

    /**
     * Creates a new DimensionFacade object.
     *
     * @param dimension the interfaced dimension
     */
    public DimensionFacade (Dimension dimension)
    {
        this.dimension = dimension;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the interfaced dimension
     *
     * @return the actual dimension
     */
    public Dimension getDimension ()
    {
        return dimension;
    }

    //-----------//
    // getHeight //
    //-----------//
    public int getHeight ()
    {
        return dimension.height;
    }

    //----------//
    // getWidth //
    //----------//
    public int getWidth ()
    {
        return dimension.width;
    }

    //-----------//
    // setHeight //
    //-----------//
    @XmlElement
    public void setHeight (int height)
    {
        dimension.height = height;
    }

    //----------//
    // setWidth //
    //----------//
    @XmlElement
    public void setWidth (int width)
    {
        dimension.width = width;
    }
}
