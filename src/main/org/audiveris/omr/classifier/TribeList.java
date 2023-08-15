//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T r i b e L i s t                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.classifier;

import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;

/**
 * Value class meant for JAXB.
 */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "tribes")
class TribeList
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TribeList.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------
    /** Used only to include sheet-name within the written file. */
    @XmlAttribute(name = "sheet-name")
    private final String name;

    /** The collection of tribes in sample sheet. */
    @XmlElement(name = "tribe")
    private final ArrayList<Tribe> tribes = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------

    // Meant for JAXB
    private TribeList ()
    {
        name = null;
    }

    /**
     * Creates a new <code>TribeList</code> object.
     *
     * @param sampleSheet the containing sample sheet
     */
    TribeList (SampleSheet sampleSheet)
    {
        name = sampleSheet.getDescriptor().getName();

        for (Tribe tribe : sampleSheet.getTribes()) {
            tribes.add(tribe);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getTribes //
    //-----------//
    /**
     * @return the tribes
     */
    public ArrayList<Tribe> getTribes ()
    {
        return tribes;
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal this instance to disk.
     *
     * @param tribesPath path to tribes file
     */
    public void marshal (Path tribesPath)
    {
        try {
            logger.debug("Marshalling {}", this);
            Jaxb.marshal(this, tribesPath, getJaxbContext());
            logger.info("Stored {}", tribesPath);
        } catch (IOException | JAXBException | XMLStreamException ex) {
            logger.error("Error marshalling " + this + " " + ex, ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(TribeList.class);
        }

        return jaxbContext;
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Load a TribeList from the provided path.
     *
     * @param path the source path
     * @return the unmarshalled instance
     * @throws IOException
     */
    static TribeList unmarshal (Path path)
        throws IOException
    {
        logger.debug("TribeList unmarshalling {}", path);

        try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            TribeList tribeList = (TribeList) um.unmarshal(is);
            return tribeList;
        } catch (JAXBException ex) {
            logger.warn("Error unmarshalling " + path + " " + ex, ex);
            return null;
        }
    }
}
