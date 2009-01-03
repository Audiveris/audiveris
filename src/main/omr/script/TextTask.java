//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t T a s k                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.script;

import omr.glyph.Glyph;
import omr.glyph.text.TextType;

import omr.sheet.Sheet;

import omr.step.StepException;
import static omr.util.Synchronicity.*;

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
    private final TextType type;

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
    public TextTask (TextType          type,
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

    //-----//
    // run //
    //-----//
    @Override
    public void run (Sheet sheet)
        throws StepException
    {
        super.run(sheet);

        sheet.getSymbolsModel()
             .assignText(SYNC, glyphs, type, content, true);
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

        return sb.toString() + super.internalsString();
    }
}
