//----------------------------------------------------------------------------//
//                                                                            //
//                  S c o r e P h y s i c a l P a i n t e r                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Chord;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sheet.Ending;
import omr.sheet.Ledger;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import java.awt.*;

/**
 * Class <code>ScorePhysicalPainter</code> paints the recognized score
 * entities at the location of their image counterpart, so that discrepancies
 * between them can be easily seen.
 *
 * <p>TODO:
 * - Paint breath marks
 *
 * @author Hervé Bitteur
 */
public class ScorePhysicalPainter
    extends ScorePainter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ScorePhysicalPainter.class);

    //~ Constructors -----------------------------------------------------------

    //----------------------//
    // ScorePhysicalPainter //
    //----------------------//
    /**
     * Creates a new ScorePhysicalPainter object.
     *
     * @param graphics Graphic context
     * @param color the color to be used
     */
    public ScorePhysicalPainter (Graphics graphics,
                                 Color    color)
    {
        super(graphics);

        // Use a specific color for all score entities
        g.setColor(color);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        //barline.render(g);
        Stroke oldStroke = g.getStroke();

        for (Glyph glyph : barline.getGlyphs()) {
            Shape shape = glyph.getShape();

            if (glyph.isBar()) {
                Stick stick = (Stick) glyph;
                float thickness = (float) stick.getWeight() / stick.getLength();
                g.setStroke(new BasicStroke(thickness));
                stick.renderLine(g);
            } else if ((shape == REPEAT_DOTS) || (shape == DOT)) {
                paint(layout(DOT), location(glyph.getAreaCenter()));
            } else {
                // ???
            }
        }

        g.setStroke(oldStroke);

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        // Super: check, flags
        if (!super.visit(chord)) {
            return false;
        }

        // Stem (physical)
        if (chord.getStem() != null) {
            Stroke oldStroke = g.getStroke();
            g.setStroke(stemStroke);

            Stick stick = (Stick) chord.getStem();
            stick.renderLine(g);
            g.setStroke(oldStroke);
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        // Paint note head and accidentals
        super.visit(note);

        // Augmentation dots ?
        if (note.getFirstDot() != null) {
            paint(layout(DOT), location(note.getFirstDot().getAreaCenter()));
        }

        if (note.getSecondDot() != null) {
            paint(layout(DOT), location(note.getSecondDot().getAreaCenter()));
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        this.score = score;

        final Sheet sheet = score.getSheet();
        scale = sheet.getScale();

        if (scale == null) {
            return false;
        }

        // Set all painting parameters
        initParameters();

        if (!score.getSystems()
                  .isEmpty()) {
            // Normal (full) rendering of the score
            score.acceptChildren(this);
        } else {
            // Render only what we have got so far...
            g.setStroke(lineStroke);

            ///g.setColor(lineColor);

            // Staff lines
            if (sheet.getStaves() != null) {
                for (StaffInfo staff : sheet.getStaves()) {
                    staff.render(g);
                }
            }

            if (sheet.getHorizontals() != null) {
                // Horizontals
                g.setStroke(lineStroke);

                // Ledgers
                for (Ledger ledger : sheet.getHorizontals()
                                          .getLedgers()) {
                    ledger.render(g);
                }

                // Endings
                for (Ending ending : sheet.getHorizontals()
                                          .getEndings()) {
                    ending.render(g);
                }
            }
        }

        return false;
    }

    //-------------------//
    // visit ScoreSystem //
    //-------------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        this.system = system;
        this.systemInfo = system.getInfo();

        if (!visit(systemInfo)) {
            return false;
        }

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    /**
     * This specific version paints the staff lines as closely as possible to
     * the physical sheet lines
     * @param staff the staff to handle
     * @return true if actually painted
     */
    @Override
    public boolean visit (Staff staff)
    {
        staff.getInfo()
             .render(g);

        return true;
    }

    //------------------//
    // visit SystemInfo //
    //------------------//
    public boolean visit (SystemInfo systemInfo)
    {
        // Check that this system is visible
        PixelRectangle bounds = systemInfo.getBounds();

        if ((bounds == null) || !bounds.intersects(g.getClipBounds())) {
            return false;
        }

        // Determine proper font size for the system, based on staff height
        StaffInfo firstStaff = systemInfo.getStaves()
                                         .get(0);
        int       staffHeight = firstStaff.getHeight();
        musicFont = MusicFont.getFont(staffHeight + scale.mainFore());

        // Compute delta y for FLAG_2 & FLAG_2_UP
        FLAG_2_DY = (int) Math.rint(
            layout(COMBINING_FLAG_2).getBounds().getHeight() * 0.5);

        g.setStroke(lineStroke);

        ///g.setColor(lineColor);

        // Ledgers
        for (Ledger ledger : systemInfo.getLedgers()) {
            ledger.render(g);
        }

        // Endings
        for (Ending ending : systemInfo.getEndings()) {
            ending.render(g);
        }

        return true;
    }

    //--------------------//
    // accidentalLocation //
    //--------------------//
    @Override
    protected PixelPoint accidentalLocation (Note  note,
                                             Glyph accidental)
    {
        return new PixelPoint(accidental.getAreaCenter().x, note.getCenter().y);
    }

    //----------//
    // braceBox //
    //----------//
    @Override
    protected PixelRectangle braceBox (SystemPart part)
    {
        return part.getBrace()
                   .getContourBox();
    }

    //--------------//
    // noteLocation //
    //--------------//
    protected PixelPoint noteLocation (Note note)
    {
        final PixelPoint center = note.getCenter();
        final Chord      chord = note.getChord();
        final Glyph      stem = chord.getStem();

        if (stem != null) {
            return location(center, chord);
        } else {
            return location(center);
        }
    }
}
