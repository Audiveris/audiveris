//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E d i t i o n T a s k                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.sig.relation.Link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Class <code>EditionTask</code> modifies the location and/or geometry of an inter
 *
 * @author Hervé Bitteur
 */
public class EditionTask
        extends InterTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The editor used on inter. */
    private final InterEditor editor;

    /** The relations to remove. */
    private final Collection<Link> unlinks;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>EditionTask</code> object.
     *
     * @param editor  DOCUMENT ME!
     * @param links   DOCUMENT ME!
     * @param unlinks DOCUMENT ME!
     */
    public EditionTask (InterEditor editor,
                        Collection<Link> links,
                        Collection<Link> unlinks)
    {
        super(editor.getInter().getSig(), editor.getInter(), null, links, "edit");
        this.editor = editor;

        if (unlinks != null) {
            this.unlinks = new ArrayList<>(unlinks);
        } else {
            this.unlinks = Collections.emptySet();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void performDo ()
    {
        editor.doit();

        for (Iterator<Link> it = links.iterator(); it.hasNext();) {
            Link link = it.next();

            if (!link.applyTo(inter)) {
                it.remove();
            }
        }

        for (Link unlink : unlinks) {
            unlink.removeFrom(inter);
        }

        sheet.getInterIndex().publish(inter);
    }

    @Override
    public void performUndo ()
    {
        editor.undo();

        for (Link unlink : unlinks) {
            unlink.applyTo(inter);
        }

        for (Link link : links) {
            link.removeFrom(inter);
        }

        sheet.getInterIndex().publish(inter);
    }

    @Override
    public String toString ()
    {
        return new StringBuilder("EditionTask{").append(editor).append('}').toString();
    }
}
