//----------------------------------------------------------------------------//
//                                                                            //
//                       D i m e n s i o n F a c a d e                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.awt.Dimension;

import javax.xml.bind.annotation.*;

/**
 * Class <code>DimensionFacade</code> is a (hopefully temporary) fix to allow Xml
 * binding of standard class Dimension that we cannot annotate
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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

    //----------//
    // setWidth //
    //----------//
    @XmlElement
    public void setWidth (int width)
    {
        dimension.width = width;
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

    //-----------//
    // getHeight //
    //-----------//
    public int getHeight ()
    {
        return dimension.height;
    }
}
