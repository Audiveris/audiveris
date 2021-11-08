//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r T r a c k e r                                    //
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
import java.util.Collection;

/**
 * Class <code>InterTracker</code> paints a moving Inter together with attachments and
 * decorations dynamically evaluated (support relations, intermediate ledgers, etc).
 * <p>
 * It is used by {@link InterDnd} and by {@link InterEditor}.
 *
 * @author Hervé Bitteur
 */
public class InterTracker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterTracker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The Inter instance being tracked. */
    protected final Inter inter;

    /** The containing sheet. */
    protected final Sheet sheet;

    /** The containing system, if any. */
    protected SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an <code>InterTracker</code> object.
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

    //~ Methods ------------------------------------------------------------------------------------
    public Inter getInter ()
    {
        return inter;
    }

    public Sheet getSheet ()
    {
        return sheet;
    }

    //-----------//
    // setSystem //
    //-----------//
    /**
     * Assign the current containing system.
     * A non-null system allows to search for supporting links, etc.
     *
     * @param system the new containing system
     */
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the inter with its attachments and decorations.
     *
     * @param g graphics context
     */
    public void render (Graphics2D g)
    {
        final SelectionPainter painter = new SelectionPainter(sheet, g);

        // Inter itself
        painter.render(inter);

        // Inter attachments
        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        inter.renderAttachments(g);
        g.setStroke(oldStroke);

        if (system != null) {
            // Inter links
            Collection<Link> links = inter.searchLinks(system);

            for (Link link : links) {
                painter.drawLink(inter, link.partner, link.relation.getClass());
            }
        }
    }

    //----------------//
    // getSceneBounds //
    //----------------//
    /**
     * Report the bounding box of the whole scene to be drawn.
     *
     * @return the scene bounds
     */
    public Rectangle getSceneBounds ()
    {
        Rectangle box = inter.getBounds();

        if (system != null) {
            // Inter links
            for (Link link : inter.searchLinks(system)) {
                box.add(link.partner.getRelationCenter());
            }
        }

        return box;
    }
}
