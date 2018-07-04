//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    W o r d V a l u e T a s k                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
 * Class {@code WordValueTask} handle the text value update of a word.
 *
 * @author Hervé Bitteur
 */
public class WordValueTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old word content. */
    private final String oldValue;

    /** New word content. */
    private final String newValue;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code WordTask} object.
     *
     * @param word     the word to modify
     * @param newValue new word value
     */
    public WordValueTask (WordInter word,
                          String newValue)
    {
        super(word.getSig(), word, word.getBounds(), null);
        this.newValue = newValue;

        oldValue = word.getValue();
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
        getInter().setValue(newValue);

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public void performUndo ()
    {
        getInter().setValue(oldValue);

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName());
        sb.append(" ").append(inter);
        sb.append(" from \"").append(oldValue).append("\"");
        sb.append(" to \"").append(newValue).append("\"");

        return sb.toString();
    }

    @Override
    protected String actionName ()
    {
        return "word";
    }
}
