//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e m p l a t e V i e w                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.Anchored;
import omr.image.AnchoredTemplate;
import omr.image.ChamferDistance;
import omr.image.DistanceTable;
import omr.image.PixelDistance;
import omr.image.Template;

import omr.selection.AnchoredTemplateEvent;
import omr.selection.MouseMovement;
import static omr.selection.SelectionHint.CONTEXT_INIT;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;
import omr.ui.util.UIUtil;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final AlphaComposite templateComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.25f);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Sheet sheet;

    private final DistanceTable table;

    /** Service where templates can be read. */
    private final SelectionService templateService;

    /** Template reference point. */
    private Point refPoint;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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

                MusicFont musicFont = MusicFont.getFont(sheet.getScale().getInterline());
                ShapeSymbol symbol = Symbols.getSymbol(template.getShape());
                Rectangle symRect = template.getSymbolBoundsAt(refPoint.x, refPoint.y, anchor);
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio minZoomRatio = new Constant.Ratio(
                4.0,
                "Minimum zoom ratio to show template key points");
    }
}
