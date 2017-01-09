//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D a t a H o l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code DataHolder} is a place holder for sheet internal data.
 * <p>
 * It handles: <ul>
 * <li>The data itself, if any.</li>
 * <li>A path to disk where data can be unmarshalled from.</li>
 * </ul>
 *
 * @param <T> specific type for data handled
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "holder")
public class DataHolder<T>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            DataHolder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing sheet. */
    protected Sheet sheet;

    /** Specific class. */
    protected Class<T> classe;

    /** Direct access to data, if any. */
    private T data;

    /** Path to data on disk. */
    @XmlAttribute(name = "path")
    private final String pathString;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DataHolder} object.
     */
    public DataHolder ()
    {
        this.classe = null;
        this.sheet = null;
        this.pathString = null;
    }

    /**
     * Creates a new {@code DataHolder} object.
     *
     * @param sheet      the containing sheet
     * @param classe     specific data class
     * @param pathString dedicated path within sheet internals
     */
    public DataHolder (Sheet sheet,
                       Class<T> classe,
                       String pathString)
    {
        this.sheet = sheet;
        this.classe = classe;
        this.pathString = pathString;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Return the handled data.
     *
     * @return the data, ready to use
     */
    public T getData ()
    {
        if (data == null) {
            final Book book = sheet.getStub().getBook();

            try {
                book.getLock().lock();

                if (data == null) {
                    JAXBContext jaxbContext = JAXBContext.newInstance(classe);
                    Unmarshaller um = jaxbContext.createUnmarshaller();

                    // Open book file system
                    Path dataFile = book.openSheetFolder(sheet.getStub().getNumber())
                            .resolve(pathString);
                    logger.debug("path: {}", dataFile);

                    InputStream is = Files.newInputStream(dataFile, StandardOpenOption.READ);
                    data = (T) um.unmarshal(is);
                    is.close();
                    logger.info("Loaded {}", dataFile);
                    dataFile.getFileSystem().close(); // Close book file system
                }
            } catch (Exception ex) {
                logger.warn("Error unmarshalling from " + pathString, ex);
            } finally {
                book.getLock().unlock();
            }
        }

        return data;
    }

    public boolean hasData ()
    {
        return data != null;
    }

    public void setData (T data)
    {
        this.data = data;
    }
}
