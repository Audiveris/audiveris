//----------------------------------------------------------------------------//
//                                                                            //
//                      A c t i o n D e s c r i p t o r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.action;

import javax.xml.bind.annotation.*;

/**
 * Class <code>ActionDescriptor</code> gathers parameters related to an action
 * from the User Interface point of view
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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

    /** Which UI section should host this action */
    @XmlAttribute(name = "section")
    public String section;

    /**
     * Which kind of menu item should be generated for this action,
     * default is JMenuItem
     */
    @XmlAttribute(name = "item")
    public String itemClassName;

    /**
     * Should a toolbar button be generated for this action,
     * default is false
     */
    @XmlAttribute(name = "toolbar")
    public Boolean onToolbar;

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
        sb.append(" class:")
          .append(className)
          .append(" method:")
          .append(methodName);

        sb.append(" domain:")
          .append(domain);
        sb.append(" section:")
          .append(section);

        if (itemClassName != null) {
            sb.append(" item:")
              .append(itemClassName);
        }

        if (onToolbar != null) {
            sb.append(" toolbar:")
              .append(onToolbar);
        }

        sb.append("}");

        return sb.toString();
    }
}
