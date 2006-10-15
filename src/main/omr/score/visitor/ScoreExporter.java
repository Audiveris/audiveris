//----------------------------------------------------------------------------//
//                                                                            //
//                      E x p o r t i n g V i s i t o r                       //
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

import omr.util.Logger;

import java.util.List;

/**
 * Class <code>ScoreExporter</code> can visit the score hierarchy to export
 * the score to a MusicXML file
 * 
 * 
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreExporter
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(
        ScoreExporter.class);

    //~ Instance fields --------------------------------------------------------

    private final List<Measure> measures;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreExporter object.
     */
    public ScoreExporter (List<Measure> measures)
    {
        this.measures = measures;
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
        if (measure.getLeftX() != null) {
            logger.info("Adding " + measure);
            measures.add(measure);
        } else {
            logger.info("Skipping " + measure);
        }

        return true;
    }

    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Exporting score ...");
        }

        score.acceptChildren(this);

        return false;
    }

    public boolean visit (Slur slur)
    {
        return true;
    }

    public boolean visit (Staff staff)
    {
        return true;
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
