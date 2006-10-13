//----------------------------------------------------------------------------//
//                                                                            //
//                      R e n d e r i n g V i s i t o r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Barline;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Score;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.System;
import omr.score.TimeSignature;

import omr.sheet.StaffInfo;

import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.Graphics;

/**
 * Class <code>RenderingVisitor</code> defines for every node in Score hierarchy
 * the rendering of related sections (with preset colors) in the dedicated
 * <b>Sheet</b> display.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class RenderingVisitor
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(
        RenderingVisitor.class);

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics g;

    /** Display zoom */
    private final Zoom z;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // RenderingVisitor //
    //------------------//
    /**
     * Creates a new RenderingVisitor object.
     *
     * @param g Graphic context
     * @param z zoom factor
     */
    public RenderingVisitor (Graphics g,
                             Zoom     z)
    {
        this.g = g;
        this.z = z;
    }

    //~ Methods ----------------------------------------------------------------

    public boolean visit (Barline barline)
    {
        return true;
    }

    public boolean visit (Clef clef)
    {
        return true;
    }

    public boolean visit (KeySignature keySignature)
    {
        return true;
    }

    public boolean visit (Measure measure)
    {
        // Render the measure ending barline, if within the clipping area
        measure.getBarline()
               .render(g, z);

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
        return true;
    }

    public boolean visit (Staff staff)
    {
        StaffInfo info = staff.getInfo();

        // Render the staff lines, if within the clipping area
        if ((info != null) && info.render(g, z)) {
            // Render the staff starting barline, if any
            if (staff.getStartingBarline() != null) {
                staff.getStartingBarline()
                     .render(g, z);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    public boolean visit (System system)
    {
        return true;
    }

    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }
}
