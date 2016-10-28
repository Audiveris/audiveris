//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             Z i p                                              //
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
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Class {@code Zip} is a convenient utility that provides both writing and reading
 * from a file which is transparently compressed in Zip.
 *
 * @author Hervé Bitteur
 */
public abstract class Zip
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Zip.class);

    //~ Constructors -------------------------------------------------------------------------------
    private Zip ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // createFileSystem //
    //------------------//
    /**
     * Create a new zip file system at the location provided by '{@code path}' parameter.
     * If such file already exists, it is deleted beforehand.
     * <p>
     * When IO operations are finished, the file system must be closed via {@link FileSystem#close}
     *
     * @param path path to zip file system
     * @return the root path of the (zipped) file system
     * @throws java.io.IOException
     */
    public static Path createFileSystem (Path path)
            throws IOException
    {
        Objects.requireNonNull(path, "Zip.createFileSystem: path is null");

        Files.deleteIfExists(path);

        // Make sure the containing folder exists
        Files.createDirectories(path.getParent());

        // Make it a zip file
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path.toFile()));
        zos.close();

        // Finally open the file system just created
        return openFileSystem(path);
    }

    //--------------------//
    // createInputStream //
    //-------------------//
    /**
     * Create a InputStream on a given file, by looking for a zip
     * archive whose path is the file path with ".zip" appended,
     * and by reading the first entry in this zip file.
     *
     * @param file the file (with no zip extension)
     *
     * @return a InputStream on the zip entry
     */
    public static InputStream createInputStream (File file)
    {
        try {
            String path = file.getCanonicalPath();

            //ZipFile zf = new ZipFile(path + ".zip");
            ZipFile zf = new ZipFile(path);

            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                return zf.getInputStream(entry);
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.toString());
            System.err.println(file + " not found");
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }

        return null;
    }

    //--------------------//
    // createOutputStream //
    //-------------------//
    /**
     * Create a OutputStream on a given file, transparently compressing the data
     * to a Zip file whose name is the provided file path, with a ".zip"
     * extension added.
     *
     * @param file the file (with no zip extension)
     *
     * @return a OutputStream on the zip entry
     */
    public static OutputStream createOutputStream (File file)
    {
        try {
            String path = file.getCanonicalPath();
            FileOutputStream fos = new FileOutputStream(path + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);

            return zos;
        } catch (FileNotFoundException ex) {
            System.err.println(ex.toString());
            System.err.println(file + " not found");
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }

        return null;
    }

    //--------------//
    // createReader //
    //--------------//
    /**
     * Create a Reader on a given file, by looking for a zip archive whose path
     * is the file path with ".zip" appended, and by reading the first entry in
     * this zip file.
     *
     * @param file the file (with no zip extension)
     *
     * @return a reader on the zip entry
     */
    public static Reader createReader (File file)
    {
        try {
            String path = file.getCanonicalPath();

            ZipFile zf = new ZipFile(path + ".zip");

            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                InputStream is = zf.getInputStream(entry);

                return new InputStreamReader(is);
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex.toString());
            System.err.println(file + " not found");
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }

        return null;
    }

    //--------------//
    // createWriter //
    //--------------//
    /**
     * Create a Writer on a given file, transparently compressing the data to a
     * Zip file whose name is the provided file path, with a ".zip" extension
     * added.
     *
     * @param file the file (with no zip extension)
     *
     * @return a writer on the zip entry
     */
    public static Writer createWriter (File file)
    {
        try {
            String path = file.getCanonicalPath();
            FileOutputStream fos = new FileOutputStream(path + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);

            return new OutputStreamWriter(zos);
        } catch (FileNotFoundException ex) {
            System.err.println(ex.toString());
            System.err.println(file + " not found");
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }

        return null;
    }

    //----------------//
    // openFileSystem //
    //----------------//
    /**
     * Open a zip file system (supposed to already exist at location provided by
     * '{@code path}' parameter) for reading or writing.
     * <p>
     * When IO operations are finished, the file system must be closed via {@link FileSystem#close}
     *
     * @param path (zip) file path
     * @return the root path of the (zipped) file system
     * @throws java.io.IOException
     */
    public static Path openFileSystem (Path path)
            throws IOException
    {
        Objects.requireNonNull(path, "Zip.openFileSystem: path is null");

        FileSystem fileSystem = FileSystems.newFileSystem(path, null);

        return fileSystem.getPath(fileSystem.getSeparator());
    }
}
