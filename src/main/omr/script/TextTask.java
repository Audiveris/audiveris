//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t T a s k                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Evaluation;
import omr.glyph.facets.Glyph;

import omr.sheet.Sheet;

import omr.text.TextRoleInfo;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code TextTask} records the assignment of textual
 * characteristics to a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class TextTask
        extends GlyphUpdateTask
{
    //~ Instance fields --------------------------------------------------------

    /** Role of the textual glyph */
    // TODO: Define proper mapping for RoleInfo class
    @XmlElement(name = "role-info")
    private final TextRoleInfo roleInfo;

    /** String content of the textual glyph */
    @XmlAttribute
    private final String content;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // TextTask //
    //----------//
    /**
     * Creates a new TextTask object.
     *
     * @param sheet    the sheet impacted
     * @param roleInfo the role of this text item
     * @param content  The content as a string
     * @param glyphs   the impacted glyph(s)
     */
    public TextTask (Sheet sheet,
                     TextRoleInfo roleInfo,
                     String content,
                     Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
        this.roleInfo = roleInfo;
        this.content = content;
    }

    //----------//
    // TextTask //
    //----------//
    /** No-arg constructor for JAXB only */
    private TextTask ()
    {
        roleInfo = null;
        content = null;
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController()
                .getModel()
                .assignText(
                getInitialGlyphs(),
                roleInfo,
                content,
                Evaluation.MANUAL);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" text");

        sb.append(" ")
                .append(roleInfo.role);

        if (roleInfo.creatorType != null) {
            sb.append(" ")
                    .append(roleInfo.creatorType);
        }

        sb.append(" \"")
                .append(content)
                .append("\"");

        return sb.toString();
    }
}
