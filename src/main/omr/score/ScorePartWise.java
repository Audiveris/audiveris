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

import omr.score.visitor.ScoreExporter;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class <code>ScorePartWise</code> is the root object used for storing and
 * loading using MusicXML format in a score-partwise structure.
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
    private List<Measure> measures;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScorePartWise object.
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
        logger.info("getMeasures");

        if (measures == null) {
            // Prepare the measure list
            measures = new ArrayList<Measure>();
            score.accept(new ScoreExporter(measures));
            logger.info("measures built nb=" + measures.size());
        }

        return measures;
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
