//----------------------------------------------------------------------------//
//                                                                            //
//                               A c t i o n s                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.action;

import static omr.action.ActionDescriptor.*;

import omr.log.Logger;

import java.io.InputStream;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class <code>Actions</code> handles all actions descriptors.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "actions")
public class Actions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Actions.class);

    /** Context for JAXB unmarshalling */
    private static JAXBContext jaxbContext;

    /** The collection of all actions loaded so far */
    private static final Set<ActionDescriptor> allDescriptors = new LinkedHashSet<ActionDescriptor>();

    //~ Enumerations -----------------------------------------------------------

    /**
     * Predefined list of domain names. Through the action list files, the user
     * will be able to add new domain names.
     * This classification is mainly used to define the related pull-down menus.
     */
    public static enum Domain {
        //~ Enumeration constant initializers ----------------------------------


        /** Domain of sheet actions */
        SHEET,
        /** Domain of individual steps */
        STEP, 
        /** Domain of score actions */
        SCORE, 
        /** Domain of MIDI features */
        MIDI, 
        /** Domain of various view features */
        VIEW, 
        /** Domain of utilities */
        TOOL, 
        /** Domain of help information */
        HELP;
    }

    /**
     * Predefined list of section names within a domain. The user can add new
     * section names, they will be added to the end of the predefined list.
     */
    public static enum Section {
        //~ Enumeration constant initializers ----------------------------------


        /** Section #1 */
        IMPORT,
        /** Section #2 */
        EDIT, 
        /** Section #3 */
        EXPORT, 
        /** Section #4 */
        END;
    }

    //~ Instance fields --------------------------------------------------------

    /** Collection of descriptors loaded by unmarshalling one file */
    @XmlElement(name = "action")
    private List<ActionDescriptor> descriptors = new ArrayList<ActionDescriptor>();

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Actions //
    //---------//
    /** Not meant to be instantiated */
    private Actions ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getAllDescriptors //
    //-------------------//
    /**
     * Report the collection of descriptors loaded so far
     * @return all the loaded action descriptors
     */
    public static Set<ActionDescriptor> getAllDescriptors ()
    {
        return allDescriptors;
    }

    //----------------//
    // getDomainNames //
    //----------------//
    /**
     * Report the whole collection of domain names, starting with the predefined
     * ones
     * @return the collection of domain names
     */
    public static Set<String> getDomainNames ()
    {
        Set<String> names = new LinkedHashSet<String>();

        // Predefined ones, except HELP
        for (Domain domain : Domain.values()) {
            if (domain != Domain.HELP) {
                names.add(domain.name());
            }
        }

        // User-specified ones
        for (ActionDescriptor desc : allDescriptors) {
            names.add(desc.domain);
        }

        // Add HELP as the last one
        names.add(Domain.HELP.name());

        return names;
    }

    //-----------------//
    // getSectionNames //
    //-----------------//
    /**
     * Report the whole collection of section names, starting with the
     * predefined ones
     * @return the collection of section names
     */
    public static Set<String> getSectionNames ()
    {
        Set<String> names = new LinkedHashSet<String>();

        // Predefined ones
        for (Section section : Section.values()) {
            names.add(section.name());
        }

        // User-specified ones
        for (ActionDescriptor desc : allDescriptors) {
            names.add(desc.section);
        }

        return names;
    }

    //-----------------//
    // loadActionsFrom //
    //-----------------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding
     * collection of action descriptors
     *
     * @param in the input stream that contains the collection of action
     * descriptors in XML format. The stream is not closed by this method
     * @throws javax.xml.bind.JAXBException if unmarshalling fails
     */
    public static void loadActionsFrom (InputStream in)
        throws JAXBException
    {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Actions.class);
        }

        Unmarshaller um = jaxbContext.createUnmarshaller();
        Actions      actions = (Actions) um.unmarshal(in);

        if (logger.isFineEnabled()) {
            for (ActionDescriptor desc : actions.descriptors) {
                logger.fine("Descriptor unmarshalled " + desc);
            }
        }

        // Verify that all actions have a domain and a section assigned
        for (Iterator<ActionDescriptor> it = actions.descriptors.iterator();
             it.hasNext();) {
            ActionDescriptor desc = it.next();

            if (desc.domain == null) {
                logger.warning("No domain specified for " + desc);
                it.remove();

                continue;
            }

            if (desc.section != null) {
                desc.section = desc.section.toUpperCase();
            } else {
                logger.warning("No section specified for " + desc);
                it.remove();

                continue;
            }
        }

        allDescriptors.addAll(actions.descriptors);
    }
}
