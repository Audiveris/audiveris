//----------------------------------------------------------------------------//
//                                                                            //
//                      P l u g i n P a r a m e t e r s                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact dev@audiveris.dev.java.net to report bugs & suggestions.   //
//----------------------------------------------------------------------------//
//
package omr.plugin;

import omr.util.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class <code>PluginParameters</code> gathers the parameters related to an
 * action considered as a plugin.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PluginParameters
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        PluginParameters.class);

    /** Un/marshalling context for use with JAXB */
    private static JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------

    /** Class name */
    @XmlAttribute(name = "class")
    String className;

    /** Name of method within class */
    @XmlAttribute(name = "method")
    String methodName;

    /** Which UI section should host this action */
    @XmlElement(name = "type")
    PluginType type;

    /** Which precise kind of menu item should be generated for this action */
    @XmlElement(name = "item")
    String itemClassName;

    /** Should a toolbar button be generated for this action */
    @XmlElement(name = "toolbar")
    Boolean onToolbar;

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{plugin");
        sb.append(" class:")
          .append(className);
        sb.append(" method:")
          .append(methodName);

        if (type != null) {
            sb.append(" type:")
              .append(type);
        }

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

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding
     * PluginParameters
     *
     * @param in the input stream that contains the plugin definition in XML
     * format. The stream is not closed by this method
     *
     * @return the allocated plugin parameters.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static PluginCollection unmarshal (InputStream in)
        throws JAXBException
    {
        Unmarshaller     um = getJaxbContext()
                                  .createUnmarshaller();
        PluginCollection pc = (PluginCollection) um.unmarshal(in);

        for (PluginParameters pp : pc.parameters) {
            logger.info("PluginParameters unmarshalled " + pp);
        }

        return pc;
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(PluginCollection.class);
        }

        return jaxbContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    @XmlRootElement(name = "plugins")
    public static class PluginCollection
    {
        //~ Instance fields ----------------------------------------------------

        @XmlElement(name = "plugin")
        public List<PluginParameters> parameters = new ArrayList<PluginParameters>();
    }
}
