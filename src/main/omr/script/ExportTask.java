//----------------------------------------------------------------------------//
//                                                                            //
//                            E x p o r t T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.score.ScoresManager;

import omr.sheet.Sheet;

import java.io.File;

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
    //~ Instance fields --------------------------------------------------------

    /** The file used for export */
    @XmlAttribute
    private String path;

    /** Should we add our signature? */
    @XmlAttribute(name = "inject-signature")
    private Boolean injectSignature;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // ExportTask //
    //------------//
    /**
     * Create a task to export the related score entities of a sheet
     *
     * @param path the full path of the export file
     */
    public ExportTask (String path)
    {
        this.path = path;
    }

    //------------//
    // ExportTask //
    //------------//
    /** No-arg constructor needed by JAXB */
    private ExportTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
    {
        ScoresManager.getInstance()
                .export(
                sheet.getScore(),
                (path != null) ? new File(path) : null,
                injectSignature);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return " export " + path + super.internalsString();
    }
}
