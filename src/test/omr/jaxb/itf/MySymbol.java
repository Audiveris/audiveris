//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M y S y m b o l                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.itf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code MySymbol}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MySymbol
        extends MyAbstractEntity
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    private final int weight;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MySymbol} object.
     */
    public MySymbol ()
    {
        this.weight = 0;
    }

    /**
     * Creates a new {@code MySymbol} object.
     *
     * @param weight DOCUMENT ME!
     */
    public MySymbol (int weight)
    {
        this.weight = weight;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        sb.append("w:").append(weight);
        sb.append(" id:").append(getId());
        sb.append("}");

        return sb.toString();
    }
}
