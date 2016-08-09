//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t T a s k                                         //
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
package omr.script;

import omr.classifier.Evaluation;
import omr.glyph.Glyph;

import omr.sheet.Sheet;

import omr.text.TextRole;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code TextTask} records the assignment of textual characteristics to a
 * collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class TextTask
        extends GlyphUpdateTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Role of the textual glyph */
    // TODO: Define proper mapping for RoleInfo class
    @XmlElement(name = "role")
    private final TextRole role;

    /** String content of the textual glyph */
    @XmlAttribute
    private final String content;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TextTask object.
     *
     * @param sheet   the sheet impacted
     * @param role    the role of this text item
     * @param content The content as a string
     * @param glyphs  the impacted glyph(s)
     */
    public TextTask (Sheet sheet,
                     TextRole role,
                     String content,
                     Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
        this.role = role;
        this.content = content;
    }

    /** No-arg constructor for JAXB only. */
    private TextTask ()
    {
        role = null;
        content = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController().getModel().assignText(
                getInitialGlyphs(),
                role,
                content,
                Evaluation.MANUAL);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" text");

        sb.append(" ").append(role);

        sb.append(" \"").append(content).append("\"");

        return sb.toString();
    }
}
