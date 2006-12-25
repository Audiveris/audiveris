//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e V i s i t o r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MeasureNode;
import omr.score.Note;
import omr.score.PartNode;
import omr.score.Score;
import omr.score.ScoreNode;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.TimeSignature;

/**
 * Interface <code>ScoreVisitor</code> is meant to visit any node of the Score
 * hierarchy
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface ScoreVisitor
{
    //~ Methods ----------------------------------------------------------------

    boolean visit (Barline node);

    boolean visit (Beam node);

    boolean visit (Chord node);

    boolean visit (Clef node);

    boolean visit (KeySignature node);

    boolean visit (Measure node);

    boolean visit (MeasureNode node);

    boolean visit (Note node);

    boolean visit (PartNode node);

    boolean visit (Score node);

    boolean visit (ScoreNode node);

    boolean visit (Slur node);

    boolean visit (Staff node);

    boolean visit (System node);

    boolean visit (SystemPart node);

    boolean visit (TimeSignature node);
}
