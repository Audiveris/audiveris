//----------------------------------------------------------------------------//
//                                                                            //
//                       P a i n t i n g V i s i t o r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.Barline;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Score;
import static omr.score.ScoreConstants.*;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.StaffPoint;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.TimeSignature;
import omr.score.UnitDimension;

import omr.ui.icon.IconManager;
import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Class <code>ScorePainter</code> defines for every node in Score hierarchy
 * the painting of node in the <b>Score</b> display.
 * 
 * 
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePainter
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger     logger = Logger.getLogger(
        ScorePainter.class);

    /** Brace icon */
    private static final SymbolIcon icon = IconManager.getInstance()
                                                      .loadSymbolIcon("BRACE");

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics g;

    /** Display zoom */
    private final Zoom zoom;

    /** Used for icon image transformation */
    private final AffineTransform transform = new AffineTransform();

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScorePainter //
    //--------------//
    /**
     * Creates a new ScorePainter object.
     * 
     * 
     * @param g Graphic context
     * @param z zoom factor
     */
    public ScorePainter (Graphics g,
                            Zoom     z)
    {
        this.g = g;
        this.zoom = z;
    }

    //~ Methods ----------------------------------------------------------------

    public boolean visit (Barline barline)
    {
        Shape shape = barline.getShape();

        if (shape != null) {
            // Draw the barline symbol
            barline.getStaff()
                   .paintSymbol(
                g,
                zoom,
                (SymbolIcon) shape.getIcon(),
                barline.getCenter(),
                0);
        } else {
            logger.warning("No shape for barline " + this);
        }

        return true;
    }

    public boolean visit (Clef clef)
    {
        // Draw the clef symbol
        clef.getStaff()
            .paintSymbol(
            g,
            zoom,
            (SymbolIcon) clef.getShape().getIcon(),
            clef.getCenter(),
            clef.getPitchPosition());

        return true;
    }

    public boolean visit (KeySignature keySignature)
    {
        logger.warning(
            "KeySignature to be implemented in " + getClass().getName());

        return true;
    }

    public boolean visit (Measure measure)
    {
        // Draw the measure id, if on the first staff only
        if (measure.getStaff()
                   .getStafflink() == 0) {
            Point origin = measure.getOrigin();
            g.setColor(Color.lightGray);
            g.drawString(
                Integer.toString(measure.getId()),
                zoom.scaled(origin.x + measure.getLeftX()) - 5,
                zoom.scaled(origin.y) - 15);
        }

        return true;
    }

    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }

    public boolean visit (Slur slur)
    {
        slur.getArc()
            .draw(g, slur.getOrigin(), zoom);

        return true;
    }

    public boolean visit (Staff staff)
    {
        Point origin = staff.getOrigin();
        g.setColor(Color.black);

        // Draw the staff lines
        for (int i = 0; i < LINE_NB; i++) {
            // Y of this staff line
            int y = zoom.scaled(origin.y + (i * INTER_LINE));
            g.drawLine(
                zoom.scaled(origin.x),
                y,
                zoom.scaled(origin.x + staff.getWidth()),
                y);
        }

        // Draw the starting barline, if any
        if (staff.getStartingBarline() != null) {
            staff.getStartingBarline()
                 .accept(this);
        }

        return true;
    }

    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    public boolean visit (System system)
    {
        // Check whether our system is impacted)
        Rectangle clip = g.getClipBounds();
        int       xMargin = INTER_SYSTEM;
        int       systemLeft = system.getRightPosition() + xMargin;
        int       systemRight = system.getOrigin().x - xMargin;

        if ((zoom.unscaled(clip.x) > systemLeft) ||
            (zoom.unscaled(clip.x + clip.width) < systemRight)) {
            return false;
        } else {
            UnitDimension dimension = system.getDimension();
            Point         origin = system.getOrigin();
            g.setColor(Color.lightGray);

            // Draw the system left edge
            g.drawLine(
                zoom.scaled(origin.x),
                zoom.scaled(origin.y),
                zoom.scaled(origin.x),
                zoom.scaled(origin.y + dimension.height + STAFF_HEIGHT));

            // Draw the system right edge
            g.drawLine(
                zoom.scaled(origin.x + dimension.width),
                zoom.scaled(origin.y),
                zoom.scaled(origin.x + dimension.width),
                zoom.scaled(origin.y + dimension.height + STAFF_HEIGHT));

            // Draw the braces if any
            for (SystemPart part : system.getParts()) {
                List<Staff> staves = part.getStaves();

                if (staves.size() > 1) {
                    // Top & bottom of brace to draw
                    int        top = staves.get(0)
                                           .getOrigin().y;
                    int        bot = staves.get(staves.size() - 1)
                                           .getOrigin().y + STAFF_HEIGHT;
                    double     height = zoom.scaled(bot - top + 1);

                    // Vertical ratio to extend the icon */
                    double     ratio = height / icon.getIconHeight();

                    // Offset on left of system
                    int        dx = 10;

                    Graphics2D g2 = (Graphics2D) g;
                    g.setColor(Color.black);
                    transform.setTransform(
                        1,
                        0,
                        0,
                        ratio,
                        zoom.scaled(origin.x) - dx,
                        zoom.scaled(top));
                    g2.drawRenderedImage(icon.getImage(), transform);
                }
            }

            return true;
        }
    }

    public boolean visit (TimeSignature timeSignature)
    {
        Shape shape = timeSignature.getShape();
        Staff staff = timeSignature.getStaff();

        if (shape != null) {
            switch (shape) {
            // If this is an illegal shape, do not draw anything.
            // TBD: we could draw a special sign for this
            case NO_LEGAL_SHAPE :
                break;

            // Is it a complete (one-symbol) time signature ?
            case TIME_FOUR_FOUR :
            case TIME_TWO_TWO :
            case TIME_TWO_FOUR :
            case TIME_THREE_FOUR :
            case TIME_SIX_EIGHT :
            case COMMON_TIME :
            case CUT_TIME :
                staff.paintSymbol(
                    g,
                    zoom,
                    (SymbolIcon) shape.getIcon(),
                    timeSignature.getCenter());

                break;
            }
        } else {
            // Assume a (legal) multi-symbol signature
            for (Glyph glyph : timeSignature.getGlyphs()) {
                Shape s = glyph.getShape();

                if (s != null) {
                    StaffPoint center = timeSignature.computeGlyphCenter(glyph);
                    int        pitch = staff.unitToPitch(center.y);
                    staff.paintSymbol(
                        g,
                        zoom,
                        (SymbolIcon) s.getIcon(),
                        center,
                        pitch);
                }
            }
        }

        return true;
    }
}
