//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E x p o r t T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Book;
import omr.sheet.ExportPattern;
import omr.sheet.Sheet;

import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ExportTask} exports score entities to a MusicXML file
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "export")
public class ExportTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The full target file used for export. */
    @XmlAttribute
    private Path file;

    /** The target folder used for export. */
    @XmlAttribute
    private Path folder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a task to export the related score entities of a book, providing either
     * the full target file or just the target folder (and using default file name).
     *
     * @param file   the full path of export file, or null
     * @param folder the full path of export folder, or null
     */
    public ExportTask (Path file,
                       Path folder)
    {
        this.file = file;
        this.folder = folder;
    }

    /** No-arg constructor needed by JAXB. */
    private ExportTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    public void core (Book book)
    {
        Path exportPath = (file != null) ? file
                : ((folder != null) ? folder.resolve(book.getRadix()) : null);
        book.setExportPathSansExt(ExportPattern.getPathSansExt(exportPath));
        book.export();
    }

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        core(sheet.getStub().getBook());
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" export");

        if (file != null) {
            sb.append(" file=").append(file);
        }

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
