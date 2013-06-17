//----------------------------------------------------------------------------//
//                                                                            //
//                       D i m e n s i o n F a c a d e                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Dimension;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code DimensionFacade} is a (hopefully temporary) fix
 * to allow Xml binding of standard class Dimension that we cannot
 * annotate.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "dimension")
public class DimensionFacade
{
    //~ Instance fields --------------------------------------------------------

    /** The interfaced Dimension instance */
    private Dimension dimension;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // DimensionFacade //
    //-----------------//
    /**
     * Creates a new instance of DimensionFacade
     */
    public DimensionFacade ()
    {
    }

    //-----------------//
    // DimensionFacade //
    //-----------------//
    /**
     * Creates a new DimensionFacade object.
     *
     * @param dimension the interfaced dimension
     */
    public DimensionFacade (Dimension dimension)
    {
        this.dimension = dimension;
    }

    //~ Methods ----------------------------------------------------------------
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
