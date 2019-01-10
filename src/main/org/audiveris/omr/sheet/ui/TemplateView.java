//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e m p l a t e V i e w                                    //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.Anchored;
import org.audiveris.omr.image.AnchoredTemplate;
import org.audiveris.omr.image.ChamferDistance;
import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.image.PixelDistance;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.AnchoredTemplateEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import static org.audiveris.omr.ui.selection.SelectionHint.CONTEXT_INIT;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

/**
 * Class {@code TemplateView} handles a view of Template on top of the image of a
 * distance table.
 * <p>
 * This view is not meant for the end user but rather for the developer to precisely study how
 * template matching works.
 * <p>
 * Right mouse allows to position a template at a given location.
 * Left mouse allows to read distance value and template value at a given location (without moving
 * the template).
 *
 * @author Hervé Bitteur
 */
public class TemplateView
        extends ImageView
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TemplateView.class);

    private static final AlphaComposite templateComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.25f);

    private final Sheet sheet;

    private final DistanceTable table;

    /** Service where templates can be read. */
    private final SelectionService templateService;

    /** Template reference point. */
    private Point refPoint;

    /**
     * Creates a new {@code TemplateView} object.
     *
     * @param sheet           related sheet
     * @param image           image of distance table
     * @param table           distance table
     * @param templateService source of templates
     */
    public TemplateView (Sheet sheet,
                         BufferedImage image,
                         DistanceTable table,
                         SelectionService templateService)
    {
        super(image);
        this.sheet = sheet;
        this.table = table;
        this.templateService = templateService;

        templateService.subscribeStrongly(AnchoredTemplateEvent.class, this);
    }

    //-----------------//
    // contextSelected //
    //-----------------//
    @Override
    public void contextSelected (Point pt,
                                 MouseMovement movement)
    {
        // Remember the template reference point
        refPoint = new Point(pt);
        setFocusLocation(new Rectangle(pt), movement, CONTEXT_INIT);
        repaint();
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        // So that rubber get displayed at current location
        super.onEvent(event);

        // Take new template definition into account
        repaint();
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        AnchoredTemplate anchoredTemplate = (AnchoredTemplate) templateService.getSelection(
                AnchoredTemplateEvent.class);

        if (anchoredTemplate != null) {
            if (refPoint != null) {
                // Render the template at specified location
                Template template = anchoredTemplate.template;
                Anchored.Anchor anchor = anchoredTemplate.anchor;
                Rectangle tplRect = template.getBoundsAt(refPoint.x, refPoint.y, anchor);

                // Draw reference point
                g.setColor(Color.BLACK);
                g.fillOval(refPoint.x, refPoint.y, 1, 1);

                // Draw template box
                Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
                g.draw(tplRect);

                // Paint shape symbol
                Composite oldComposite = g.getComposite();
                g.setComposite(templateComposite);

                ShapeSymbol symbol = Symbols.getSymbol(template.getShape());
                Rectangle symRect = template.getSymbolBoundsAt(refPoint.x, refPoint.y, anchor);
                Scale scale = sheet.getScale();
                MusicFont musicFont = MusicFont.getHeadFont(scale, scale.getInterline());
                symbol.paintSymbol(g, musicFont, symRect.getLocation(), Alignment.TOP_LEFT);
                g.setComposite(oldComposite);

                // Draw template key points (only for high zoom values)
                if (g.getTransform().getScaleX() >= constants.minZoomRatio.getValue()) {
                    // A 1x1 square for each key point
                    g.setColor(Color.BLACK);

                    for (PixelDistance pix : template.getKeyPoints()) {
                        int x = tplRect.x + pix.x;
                        int y = tplRect.y + pix.y;
                        g.drawRect(x, y, 1, 1);
                    }

                    // Expected foreground (O for success, X for failure)
                    UIUtil.setAbsoluteStroke(g, 2f);
                    g.setColor(Color.ORANGE);

                    for (PixelDistance pix : template.getKeyPoints()) {
                        if (pix.d != 0) {
                            continue;
                        }

                        int x = tplRect.x + pix.x;
                        int y = tplRect.y + pix.y;
                        int val = table.getValue(x, y);

                        if (val != ChamferDistance.VALUE_UNKNOWN) {
                            if (val == 0) {
                                g.drawOval(x, y, 1, 1);
                            } else {
                                g.drawLine(x, y, x + 1, y + 1);
                                g.drawLine(x, y + 1, x + 1, y);
                            }
                        }
                    }

                    // Expected background (O for success, X for failure)
                    g.setColor(Color.BLUE);

                    for (PixelDistance pix : template.getKeyPoints()) {
                        if (pix.d == 0) {
                            continue;
                        }

                        int x = tplRect.x + pix.x;
                        int y = tplRect.y + pix.y;
                        int val = table.getValue(x, y);

                        if (val != ChamferDistance.VALUE_UNKNOWN) {
                            if (val != 0) {
                                g.drawOval(x, y, 1, 1);
                            } else {
                                g.drawLine(x, y, x + 1, y + 1);
                                g.drawLine(x, y + 1, x + 1, y);
                            }
                        }
                    }
                }

                g.setStroke(oldStroke);
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio minZoomRatio = new Constant.Ratio(
                4.0,
                "Minimum zoom ratio to show template key points");
    }
}
