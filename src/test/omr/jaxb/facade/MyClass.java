//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M y C l a s s                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.facade;

import omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code MyClass}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class MyClass
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public final String name;

    @XmlElement
    public final Point origin;

    @XmlElement
    public final Rectangle box;

    /** Global id to uniquely identify an entity instance. */
    @XmlAttribute(name = "last-id")
    @XmlJavaTypeAdapter(Jaxb.AtomicIntegerAdapter.class)
    public final AtomicInteger lastId = new AtomicInteger(0);

    /** Collection of all entities registered. */
    @XmlElementWrapper(name = "entities")
    public final Map<Integer, String> allEntities = new ConcurrentHashMap<Integer, String>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MyClass} object.
     */
    public MyClass ()
    {
        this.name = null;
        this.origin = null;
        this.box = null;
    }

    /**
     * Creates a new {@code MyClass} object.
     *
     * @param name   DOCUMENT ME!
     * @param origin DOCUMENT ME!
     * @param box    DOCUMENT ME!
     */
    public MyClass (String name,
                    Point origin,
                    Rectangle box)
    {
        this.name = name;
        this.origin = origin;
        this.box = box;
    }
}
