//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A c t i o n D e s c r i p t o r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.action;

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
    //~ Instance fields ----------------------------------------------------------------------------

    /** Which UI domain (menu) should host this action. */
    @XmlAttribute(name = "domain")
    public String domain;

    /**
     * Which UI section should host this action.
     * Any value is OK, but items with the same section value will be gathered together in the menu,
     * while different sections will be separated by a menu separator
     */
    @XmlAttribute(name = "section")
    public Integer section;

    /** Sub-menu name. (exclusive of class+method) */
    @XmlAttribute(name = "menu")
    public String menuName;

    /** Class name. (exclusive of menu) */
    @XmlAttribute(name = "class")
    public String className;

    /** Name of method within class. (exclusive of menu) */
    @XmlAttribute(name = "method")
    public String methodName;

    /**
     * Which kind of menu item should be generated for this action, default is JMenuItem.
     */
    @XmlAttribute(name = "item")
    public String itemClassName;

    /**
     * Which kind of (toolbar) button should be generated for this action, default is null.
     */
    @XmlAttribute(name = "button")
    public String buttonClassName;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * To force instantiation through JAXB unmarshalling only.
     */
    private ActionDescriptor ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{action");

        sb.append(" domain:").append(domain);
        sb.append(" section:").append(section);

        if (className != null) {
            sb.append(" class:").append(className);
        }

        if (methodName != null) {
            sb.append(" method:").append(methodName);
        }

        if (menuName != null) {
            sb.append(" menu:").append(menuName);
        }

        if (itemClassName != null) {
            sb.append(" item:").append(itemClassName);
        }

        if (buttonClassName != null) {
            sb.append(" button:").append(buttonClassName);
        }

        sb.append("}");

        return sb.toString();
    }
}
