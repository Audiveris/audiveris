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

import omr.sheet.BookManager;
import omr.sheet.Sheet;

import java.nio.file.Paths;

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

    /** The file used for export. */
    @XmlAttribute
    private String path;

    /** Should we add our signature?. */
    @XmlAttribute(name = "signed")
    private Boolean signed;

    /** Should we compress the output?. */
    @XmlAttribute
    private boolean compressed;

    //~ Constructors -------------------------------------------------------------------------------
    //------------//
    // ExportTask //
    //------------//
    /**
     * Create a task to export the related score entities of a sheet
     *
     * @param path       the path sans extension to the export file
     * @param compressed true for a compressed output (mxl) rather than uncompressed (xml)
     */
    public ExportTask (String path,
                       boolean compressed)
    {
        this.path = path;
        this.compressed = compressed;
    }

    //------------//
    // ExportTask //
    //------------//
    /** No-arg constructor needed by JAXB */
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
        BookManager.getInstance().export(
                sheet.getBook(),
                (path != null) ? Paths.get(path) : null,
                signed,
                compressed);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" export");

        if (compressed) {
            sb.append(" compressed");
        }

        sb.append(" ").append(path);

        return sb.toString();
    }
}
