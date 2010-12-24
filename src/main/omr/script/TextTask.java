//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t T a s k                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Evaluation;
import omr.glyph.facets.Glyph;
import omr.glyph.text.TextRole;

import omr.score.entity.Text.CreatorText.CreatorType;

import omr.sheet.Sheet;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code TextTask} records the assignment of textual characteristics to a
 * collection of glyphs
 *
 * @author Hervé Bitteur
 */
public class TextTask
    extends GlyphUpdateTask
{
    //~ Instance fields --------------------------------------------------------

    /** Type of the textual glyph */
    @XmlAttribute
    private final CreatorType type;

    /** Role of the textual glyph */
    @XmlAttribute
    private final TextRole role;

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
     * @param sheet the sheet impacted
     * @param type the type of creator (if relevant, otherwise null)
     * @param role the role of this text item
     * @param content The content as a string
     * @param glyphs the impacted glyph(s)
     */
    public TextTask (Sheet             sheet,
                     CreatorType       type,
                     TextRole          role,
                     String            content,
                     Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
        this.type = type;
        this.role = role;
        this.content = content;
    }

    //----------//
    // TextTask //
    //----------//
    /** No-arg constructor needed by JAXB */
    private TextTask ()
    {
        type = null;
        role = null;
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
            type,
            role,
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
          .append(role);

        if (type != null) {
            sb.append(" ")
              .append(type);
        }

        sb.append(" \"")
          .append(content)
          .append("\"");

        return sb.toString();
    }
}
