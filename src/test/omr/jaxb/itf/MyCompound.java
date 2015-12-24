//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       M y C o m p o u n d                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.itf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code MyCompound}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MyCompound
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlIDREF
    MyGlyph topGlyph;

    @XmlIDREF
    MyGlyph bottomGlyph;

    @XmlIDREF
    MySymbol leftSymbol;

    @XmlIDREF
    MySymbol rightSymbol;

    MyBasicIndex<MyEntity> index;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MyCompound} object.
     */
    public MyCompound ()
    {
    }
}
