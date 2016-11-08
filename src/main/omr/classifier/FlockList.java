//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F l o c k L i s t                                       //
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
package omr.classifier;

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

/**
 * Value class meant for JAXB.
 */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "flocks")
class FlockList
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            FlockList.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlAttribute(name = "id")
    private final int id;

    @XmlElement(name = "flock")
    private final ArrayList<Flock> flocks = new ArrayList<Flock>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FlockList} object.
     *
     * @param id          DOCUMENT ME!
     * @param sampleSheet DOCUMENT ME!
     */
    public FlockList (int id,
                      SampleSheet sampleSheet)
    {
        this.id = id;

        for (Flock flock : sampleSheet.getFlocks()) {
            flocks.add(flock);
        }
    }

    /**
     * Creates a new {@code FlockList} object.
     *
     * @param sampleSheet DOCUMENT ME!
     */
    public FlockList (SampleSheet sampleSheet)
    {
        this.id = sampleSheet.getId();

        for (Flock flock : sampleSheet.getFlocks()) {
            flocks.add(flock);
        }
    }

    // Meant for JAXB
    private FlockList ()
    {
        id = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the flocks
     */
    public ArrayList<Flock> getFlocks ()
    {
        return flocks;
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Load a FlockList from the provided path.
     *
     * @param path the source path
     * @return the unmarshalled instance
     * @throws IOException
     */
    static FlockList unmarshal (Path path)
            throws IOException
    {
        logger.debug("FlockList unmarshalling {}", path);

        try {
            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            FlockList flockList = (FlockList) um.unmarshal(is);
            is.close();

            return flockList;
        } catch (JAXBException ex) {
            logger.warn("Error unmarshalling " + path + " " + ex, ex);

            return null;
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(FlockList.class);
        }

        return jaxbContext;
    }
}
