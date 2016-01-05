//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D a t a H o l d e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

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
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(classe);
                Unmarshaller um = jaxbContext.createUnmarshaller();

                // Open project file system
                Path dataFile = sheet.getBook().openSheetFolder(sheet.getNumber())
                        .resolve(pathString);
                logger.debug("path: {}", dataFile);

                InputStream is = Files.newInputStream(dataFile, StandardOpenOption.READ);
                data = (T) um.unmarshal(is);
                is.close();

                // Close project file system
                dataFile.getFileSystem().close();

                logger.info("Loaded {}", dataFile);

            } catch (Exception ex) {
                logger.warn("Error unmarshalling from " + pathString, ex);
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
