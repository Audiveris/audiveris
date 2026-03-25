//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           S e n t e n c e A t t r i b u t e s T a s k                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.sig.inter.SentenceInter;

/**
 * Class <code>SentenceAttributesTask</code> handles the font attributes update
 * of a whole sentence.
 *
 * @author Hervé Bitteur
 */
public class SentenceAttributesTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old font attributes. */
    private final String oldAttrs;

    /** New font attributes. */
    private final String newAttrs;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SentenceAttributesTask</code> object.
     *
     * @param sentence the sentence to modify
     * @param newAttrs new font attributes
     */
    public SentenceAttributesTask (SentenceInter sentence,
                                   String newAttrs)
    {
        super(sentence.getSig(), sentence, sentence.getBounds(), null, "sentenceAttributes");
        this.newAttrs = newAttrs;

        oldAttrs = sentence.getMeanFont().getAttributesSpec();
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public SentenceInter getInter ()
    {
        return (SentenceInter) inter;
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
