//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S e n t e n c e R o l e T a s k                                //
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

import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.text.TextRole;

/**
 * Class {@code SentenceRoleTask} change the role of a sentence.
 *
 * @author Hervé Bitteur
 */
public class SentenceRoleTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Old sentence role. */
    final TextRole oldRole;

    /** New sentence role. */
    final TextRole newRole;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SentenceTask} object.
     *
     * @param sentence the sentence to modify
     * @param newRole  the new role for the sentence
     */
    public SentenceRoleTask (SentenceInter sentence,
                             TextRole newRole)
    {
        super(sentence.getSig(), sentence, sentence.getBounds(), null);
        this.newRole = newRole;

        oldRole = sentence.getRole();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public SentenceInter getInter ()
    {
        return (SentenceInter) inter;
    }

    /**
     * @return the newRole
     */
    public TextRole getNewRole ()
    {
        return newRole;
    }

    /**
     * @return the oldRole
     */
    public TextRole getOldRole ()
    {
        return oldRole;
    }

    @Override
    public void performDo ()
    {
        getInter().setRole(newRole);

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public void performUndo ()
    {
        getInter().setRole(oldRole);

        sheet.getInterIndex().publish(getInter());
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName());
        sb.append(" ").append(inter);
        sb.append(" from ").append(oldRole);
        sb.append(" to ").append(newRole);

        return sb.toString();
    }

    @Override
    protected String actionName ()
    {
        return "role";
    }
}
