//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R u n T a b l e H o l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.run.RunTable;

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

/**
 * Class {@code RunTableHolder} holds the reference to a run table, at least the path
 * to its marshalled data on disk, and on demand the unmarshalled run table itself.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
public class RunTableHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            RunTableHolder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Direct access to data, if any. */
    private RunTable data;

    /** Path to data on disk. */
    @XmlAttribute(name = "path")
    private final String pathString;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RunTableHolder} object.
     *
     * @param pathString path to file on disk
     */
    public RunTableHolder (String pathString)
    {
        this.pathString = pathString;
    }

    private RunTableHolder ()
    {
        this.pathString = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    /**
     * Return the handled data.
     *
     * @param sheet the containing sheet instance
     * @return the data, ready to use
     */
    public RunTable getData (Sheet sheet)
    {
        if (data == null) {
            synchronized (this) {
                // Double-check is needed!
                if (data == null) {
                    try {
                        JAXBContext jaxbContext = JAXBContext.newInstance(RunTable.class);
                        Unmarshaller um = jaxbContext.createUnmarshaller();

                        // Open book file system
                        Path dataFile = sheet.getBook().openSheetFolder(sheet.getNumber()).resolve(
                                pathString);
                        logger.debug("path: {}", dataFile);

                        InputStream is = Files.newInputStream(dataFile, StandardOpenOption.READ);
                        data = (RunTable) um.unmarshal(is);
                        is.close();
                        // Close book file system
                        dataFile.getFileSystem().close();
                        logger.info("Loaded {}", dataFile);
                    } catch (Exception ex) {
                        logger.warn("Error unmarshalling from " + pathString, ex);
                    }
                }
            }
        }

        return data;
    }

    //---------//
    // hasData //
    //---------//
    public boolean hasData ()
    {
        return data != null;
    }

    //---------//
    // setData //
    //---------//
    public void setData (RunTable data)
    {
        this.data = data;
    }
}
