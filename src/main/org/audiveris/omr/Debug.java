//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         D e b u g                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr;

import org.audiveris.omr.image.TemplateFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubDependent;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.ui.symbol.MusicFamily;

import org.jdesktop.application.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Convenient class meant to temporarily inject some debugging.
 * To be used in sync with file user-actions.xml in config folder
 *
 * @author Hervé Bitteur
 */
public class Debug
        extends StubDependent
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Debug.class);

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // checkSources //
    //--------------//
    /**
     * Check which sources are still in Picture cache.
     *
     * @param e unused
     */
    @Action
    public void checkSources (ActionEvent e)
    {
        SheetStub stub = StubsController.getCurrentStub();

        if ((stub != null) && stub.hasSheet()) {
            Picture picture = stub.getSheet().getPicture();

            if (picture != null) {
                picture.checkSources();
            }
        }
    }

    //----------------//
    // checkTemplates //
    //----------------//
    /**
     * Generate the templates for all relevant shapes for a range of interline values.
     *
     * @param e unused
     */
    @Action
    public void checkTemplates (ActionEvent e)
    {
        TemplateFactory factory = TemplateFactory.getInstance();

        for (MusicFamily family : MusicFamily.values()) {
            for (int i = 40; i < 160; i++) {
                logger.info("Catalog for point size {}", i);
                factory.getCatalog(family, i);
            }
        }

        logger.info("Done.");
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (OutputStream os)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(os, WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (UnsupportedEncodingException ex) {
            logger.warn("Error creating PrintWriter " + ex, ex);

            return null;
        }
    }
}
