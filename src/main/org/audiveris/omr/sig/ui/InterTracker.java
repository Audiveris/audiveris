//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r T r a c k e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ui.SelectionPainter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.Collection;

/**
 * Class {@code InterTracker} paints a moving Inter together with decorations dynamically
 * evaluated, such as its support relations or intermediate ledgers for note heads.
 * <p>
 * It is used in Dnd operation and in inter edition.
 *
 * @author Hervé Bitteur
 */
public class InterTracker
{

    private static final Logger logger = LoggerFactory.getLogger(InterTracker.class);

    /** The Inter instance being tracked. */
    protected final Inter inter;

    /** The containing sheet. */
    protected final Sheet sheet;

    /** The containing system. */
    protected SystemInfo system;

    /**
     * Creates an {@code InterTracker} object.
     *
     * @param inter the inter to follow
     * @param sheet the containing sheet
     */
    public InterTracker (Inter inter,
                         Sheet sheet)
    {
        this.inter = inter;
        this.sheet = sheet;
    }

    public Sheet getSheet ()
    {
        return sheet;
    }

    public Inter getInter ()
    {
        return inter;
    }

    //-----------//
    // setSystem //
    //-----------//
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the inter with its decorations.
     *
     * @param g           graphics context
     * @param renderInter true for rendering inter
     * @return the bounding box of all decorations
     */
    public Rectangle render (Graphics2D g,
                             boolean renderInter)
    {
        final SelectionPainter painter = new SelectionPainter(sheet, g);
        Rectangle box = inter.getBounds();

        if (renderInter) {
            painter.render(inter); // Inter
        }

        // Inter attachments
        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        inter.renderAttachments(g);
        g.setStroke(oldStroke);

        if (system != null) {
            // Inter links
            Collection<Link> links = inter.searchLinks(system);

            for (Link link : links) {
                Line2D line = painter.drawSupport(inter, link.partner, link.relation.getClass());
                box.add(line.getBounds());
            }
        }

        return box;
    }
}
