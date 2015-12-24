//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S a v e T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.OMR;

import omr.sheet.Book;
import omr.sheet.Sheet;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code SaveTask} saves project on disk.
 *
 * @author Hervé Bitteur
 */
public class SaveTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The full target file. */
    @XmlAttribute(name = "file")
    private File file;

    /** Just the target folder. */
    @XmlAttribute(name = "folder")
    private File folder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to save a project, providing either the full target
     * file or just the target folder (and using default file name).
     *
     * @param file   the full target file, or null
     * @param folder just the target folder, or null
     */
    public SaveTask (File file,
                     File folder)
    {
        this.file = file;
        this.folder = folder;
    }

    /** No-arg constructor needed by JAXB. */
    private SaveTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        Book book = sheet.getBook();
        Path projectPath = (file != null) ? file.toPath()
                : ((folder != null)
                        ? Paths.get(
                                folder.toString(),
                                book.getRadix() + OMR.PROJECT_EXTENSION) : null);
        book.store(projectPath);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" save");

        if (file != null) {
            sb.append(" file=").append(file);
        }

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
