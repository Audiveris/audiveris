//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t T a s k                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Evaluation;
import omr.glyph.Glyph;
import omr.glyph.text.TextRole;

import omr.sheet.Sheet;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>TextTask</code> records the assignment of textual characteristics
 * to a collection of glyphs
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TextTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Type of the textual glyph */
    @XmlAttribute
    private final TextRole type;

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
     * @param type the type of this text item
     * @param content The content as a string
     * @param glyphs the impacted glyph(s)
     */
    public TextTask (TextRole          type,
                     String            content,
                     Collection<Glyph> glyphs)
    {
        super(glyphs);
        this.type = type;
        this.content = content;
    }

    //----------//
    // TextTask //
    //----------//
    /** No-arg constructor needed by JAXB */
    private TextTask ()
    {
        type = null;
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
             .assignText(getInitialGlyphs(), type, content, Evaluation.MANUAL);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" text");

        sb.append(" ")
          .append(type);

        sb.append(" \"")
          .append(content)
          .append("\"");

        return sb + super.internalsString();
    }
}
