//----------------------------------------------------------------------------//
//                                                                            //
//                               A c t i o n s                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.action;

import omr.log.Logger;

import java.io.InputStream;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class <code>Actions</code> handles all actions descriptors.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "actions")
public class Actions
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Actions.class);

    /** Context for JAXB unmarshalling */
    private static volatile JAXBContext jaxbContext;

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


        /** Domain of file actions */
        FILE,
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

        // User-specified ones, except HELP
        for (ActionDescriptor desc : allDescriptors) {
            if (!desc.domain.equalsIgnoreCase(Domain.HELP.toString())) {
                names.add(desc.domain);
            }
        }

        // Add HELP as the very last one
        names.add(Domain.HELP.name());

        return names;
    }

    //-------------//
    // getSections //
    //-------------//
    /**
     * Report the whole collection of sections, the predefined ones and the
     * added ones.
     * @return the collection of sections
     */
    public static SortedSet<Integer> getSections ()
    {
        SortedSet<Integer> sections = new TreeSet<Integer>();

        for (ActionDescriptor desc : allDescriptors) {
            sections.add(desc.section);
        }

        return sections;
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

            if (desc.section == null) {
                logger.warning("No section specified for " + desc);
                it.remove();

                continue;
            }
        }

        allDescriptors.addAll(actions.descriptors);
    }
}
