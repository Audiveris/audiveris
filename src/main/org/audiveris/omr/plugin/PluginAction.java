//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P l u g i n A c t i o n                                     //
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
package org.audiveris.omr.plugin;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.ui.StubsController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;

/**
 * Class {@code PluginAction} implements the concrete user action related to a
 * registered plugin.
 *
 * @author Hervé Bitteur
 */
class PluginAction
        extends AbstractAction
{

    private static final Logger logger = LoggerFactory.getLogger(PluginAction.class);

    /** The related plugin. */
    private final Plugin plugin;

    /**
     * Creates a new PluginAction object.
     *
     * @param plugin the underlying scripting plugin
     */
    PluginAction (Plugin plugin)
    {
        super(plugin.getId());
        this.plugin = plugin;
        putValue(SHORT_DESCRIPTION, plugin.getDescription());
    }

    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        final Book book = StubsController.getCurrentBook();

        if (book != null) {
            plugin.getTask(book).execute();
        }
    }

    //-----------//
    // getPlugin //
    //-----------//
    public Plugin getPlugin ()
    {
        return plugin;
    }
}
