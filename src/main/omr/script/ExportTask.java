//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E x p o r t T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Book;
import omr.sheet.Sheet;

import java.io.File;
import java.nio.file.Path;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code ExportTask} exports score entities to a MusicXML file
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ExportTask
        extends ScriptTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The folder used for exports. */
    @XmlAttribute
    private File folder;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a task to export the related score entities of a sheet
     *
     * @param folder the folder for export files
     */
    public ExportTask (File folder)
    {
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
    @Override
    public void core (Sheet sheet)
    {
        Book book = sheet.getBook();
        Path bookPath = (folder != null) ? new File(folder, book.getRadix()).toPath() : null;
        book.setExportPath(bookPath);
        book.export();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" export");

        if (folder != null) {
            sb.append(" folder=").append(folder);
        }

        return sb.toString();
    }
}
