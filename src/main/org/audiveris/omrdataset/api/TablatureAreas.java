//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T a b l a t u r e A r e a s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omrdataset.api;

import org.audiveris.omr.util.Jaxb.RectangleAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>TablatureAreas</code> describes areas occupied by tablatures in sheet.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "TablatureAreas")
public class TablatureAreas
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TablatureAreas.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //
    @XmlElement(name = "Area")
    @XmlJavaTypeAdapter(RectangleAdapter.class)
    public ArrayList<Rectangle> areas = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>TablatureAreas</code> object.
     *
     * @param areas tablature areas
     */
    public TablatureAreas (List<Rectangle> areas)
    {
        if (areas != null) {
            this.areas.addAll(areas);
        }
    }

    /** No-arg constructor needed by JAXB. */
    private TablatureAreas ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // marshall //
    //----------//
    /**
     * Marshall this instance to the provided XML file.
     *
     * @param path to the XML output file
     * @throws IOException   in case of IO problem
     * @throws JAXBException in case of marshalling problem
     */
    public void marshall (Path path)
            throws IOException,
                   JAXBException
    {
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        try (OutputStream os = new BufferedOutputStream(
                Files.newOutputStream(path, StandardOpenOption.CREATE))) {
            Marshaller m = getJaxbContext().createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(this, os);
            os.flush();
        }
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Load TablatureAreas from the provided XML file.
     *
     * @param path to the XML input file.
     * @return the unmarshalled TablatureAreas object
     * @throws IOException   in case of IO problem
     * @throws JAXBException in case of unmarshalling error
     */
    public static TablatureAreas unmarshal (Path path)
            throws IOException,
                   JAXBException
    {
        try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            return (TablatureAreas) um.unmarshal(is);
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
            jaxbContext = JAXBContext.newInstance(TablatureAreas.class);
        }

        return jaxbContext;
    }
}
