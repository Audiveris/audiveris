//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e P a r t W i s e                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.Main;

import omr.score.visitor.Visitor;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class <code>ScorePartWise</code> is the root object used for storing and
 * loading using MusicXML format in a score-partwise structure. It is a kind of
 * facade of Score, meant to be marshalled by JiBX.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePartWise
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(ScorePartWise.class);

    //~ Instance fields --------------------------------------------------------

    /** Related score */
    private final Score score;

    /** List of measures elaborated on the fly */
    private List<Measure> measures = new ArrayList<Measure>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Create a new ScorePartWise object.
     *
     * @param score the score to export
     */
    public ScorePartWise (Score score)
    {
        this.score = score;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getDate //
    //---------//
    public String getDate ()
    {
        return String.format("%tF", new Date());
    }

    //-------------//
    // getMeasures //
    //-------------//
    public List<Measure> getMeasures ()
    {
        return measures;
    }

    //-------------//
    // getPartList //
    //-------------//
    public List<ScorePart> getPartList ()
    {
        return score.getPartList();
    }

    //-------------//
    // getSoftware //
    //-------------//
    public String getSoftware ()
    {
        return Main.getToolName() + " " + Main.getToolVersion();
    }

    //-----------//
    // getSource //
    //-----------//
    public String getSource ()
    {
        return score.getSheet()
                    .getPath();
    }
}
