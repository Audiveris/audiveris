//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            A p p l e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.refs;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Apple}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "apple")
public class Apple
        extends Fruit
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Apple} object.
     */
    public Apple ()
    {
        this.name = null;
    }

    /**
     * Creates a new {@code Apple} object.
     *
     * @param id   DOCUMENT ME!
     * @param name DOCUMENT ME!
     */
    public Apple (String id,
                  String name)
    {
        super(id);
        this.name = name;
    }
}
