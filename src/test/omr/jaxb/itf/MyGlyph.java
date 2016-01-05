//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M y G l y p h                                         //
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
 * Class {@code MyGlyph}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MyGlyph
        extends MyAbstractEntity
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    private final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MyGlyph} object.
     */
    public MyGlyph ()
    {
        this.name = null;
    }

    /**
     * Creates a new {@code MyGlyph} object.
     *
     * @param name DOCUMENT ME!
     */
    public MyGlyph (String name)
    {
        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        sb.append(name);
        sb.append(" id:").append(getId());
        sb.append("}");

        return sb.toString();
    }
}
