//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s e C o l o r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.icon;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 *
 * @author hb115668
 */
@XmlRootElement(name = "base-color")
public class BaseColor
{
    //~ Instance fields --------------------------------------------------------

    @XmlAttribute(name = "R")
    public int                          R;
    @XmlAttribute(name = "G")
    public int                          G;
    @XmlAttribute(name = "B")
    public int                          B;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of BaseColor */
    public BaseColor ()
    {
    }
}
