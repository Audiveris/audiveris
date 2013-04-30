//----------------------------------------------------------------------------//
//                                                                            //
//                      A c t i o n D e s c r i p t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ActionDescriptor} gathers parameters related to an action
 * from the User Interface point of view
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "action")
public class ActionDescriptor
{
    //~ Instance fields --------------------------------------------------------

    /** Class name */
    @XmlAttribute(name = "class")
    public String className;

    /** Name of method within class */
    @XmlAttribute(name = "method")
    public String methodName;

    /** Which UI domain (menu) should host this action */
    @XmlAttribute(name = "domain")
    public String domain;

    /**
     * Which UI section should host this action.
     * Any value is OK, but items with the same section value will be gathered
     * together in the menu, while different sections will be separated by a
     * menu separator
     */
    @XmlAttribute(name = "section")
    public Integer section;

    /**
     * Which kind of menu item should be generated for this action,
     * default is JMenuItem
     */
    @XmlAttribute(name = "item")
    public String itemClassName;

    /**
     * Which kind of (toolbar) button should be generated for this action,
     * default is null
     */
    @XmlAttribute(name = "button")
    public String buttonClassName;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // ActionDescriptor //
    //------------------//
    /**
     * To force instantiation through JAXB unmarshalling only.
     */
    private ActionDescriptor ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    /**
     * Report a one-line information on this descriptor
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{action");
        sb.append(" class:").
                append(className).
                append(" method:").
                append(methodName);

        sb.append(" domain:").
                append(domain);
        sb.append(" section:").
                append(section);

        if (itemClassName != null) {
            sb.append(" item:").
                    append(itemClassName);
        }

        if (buttonClassName != null) {
            sb.append(" button:").
                    append(buttonClassName);
        }

        sb.append("}");

        return sb.toString();
    }
}
