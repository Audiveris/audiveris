//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            F r u i t                                           //
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
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Fruit}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fruit")
public abstract class Fruit
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlID
    @XmlAttribute
    final String id;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Fruit} object.
     */
    public Fruit ()
    {
        this.id = null;
    }

    /**
     * Creates a new {@code Fruit} object.
     *
     * @param id DOCUMENT ME!
     */
    public Fruit (String id)
    {
        this.id = id;
    }
}
