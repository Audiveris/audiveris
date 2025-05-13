//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               W o r d A t t r i b u t e s T a s k                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.inter.WordInter;

/**
 * Class <code>WordAttributesTask</code> handles the font attributes update of a word.
 *
 * @author Hervé Bitteur
 */
public class WordAttributesTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old word font attributes. */
    private final String oldAttrs;

    /** New word font attributes. */
    private final String newAttrs;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>WordAttributesTask</code> object.
     *
     * @param word     the word to modify
     * @param newAttrs new font attributes
     */
    public WordAttributesTask (WordInter word,
                               String newAttrs)
    {
        super(word.getSig(), word, word.getBounds(), null, "wordAttributes");
        this.newAttrs = newAttrs;

        oldAttrs = word.getFontInfo().getAttributesSpec();
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public WordInter getInter ()
    {
        return (WordInter) inter;
    }

    @Override
    public void performDo ()
    {
        getInter().setFontAttributes(newAttrs);
        getInter().freeze();

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public void performUndo ()
    {
        getInter().setFontAttributes(oldAttrs);
        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName);
        sb.append(" ").append(inter);
        sb.append(" from \"").append(oldAttrs).append("\"");
        sb.append(" to \"").append(newAttrs).append("\"");

        return sb.toString();
    }
}
