//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P l u g i n S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.step;

import org.audiveris.omr.OMR;
import org.audiveris.omr.plugin.Plugin;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code PluginStep} launches the default plugin.
 * TODO: This should not be a step!
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class PluginStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PluginStep.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The current default plugin. */
    private Plugin plugin;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PluginStep object.
     */
    public PluginStep (Plugin plugin)
    {
        ///super(Steps.PLUGIN, DATA_TAB, "Launch the default plugin");
        this.plugin = plugin;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        Book book = sheet.getStub().getBook();

        // Interactive or Batch?
        if (OMR.gui != null) {
            plugin.getTask(book).execute();
        } else {
            plugin.runPlugin(book);
        }
    }

    //
    //    //----------------//
    //    // getDescription //
    //    //----------------//
    //    /**
    //     * Augment the description with the plugin title
    //     *
    //     * @return a named description
    //     */
    //    @Override
    //    public String getDescription ()
    //    {
    //        return super.getDescription() + " (" + plugin.getTitle() + ")";
    //    }
    //
    //-----------//
    // setPlugin //
    //-----------//
    /**
     * Set the default plugin.
     *
     * @param plugin the (new) default plugin
     */
    public void setPlugin (Plugin plugin)
    {
        Plugin oldPlugin = this.plugin;
        this.plugin = plugin;

        //        if (oldPlugin != null) {
        //            // Update step tooltip with this new plugin
        //            MainGui gui = (MainGui) OMR.gui;
        //            gui.getStepMenu().updateMenu();
        //        }
    }
}
