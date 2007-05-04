//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e V i s i t o r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Arpeggiate;
import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.Coda;
import omr.score.Dynamics;
import omr.score.Fermata;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MeasureElement;
import omr.score.MeasureNode;
import omr.score.Note;
import omr.score.Ornament;
import omr.score.PartNode;
import omr.score.Pedal;
import omr.score.Score;
import omr.score.ScoreNode;
import omr.score.Segno;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.TimeSignature;
import omr.score.Wedge;

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

    boolean visit (Arpeggiate node);

    boolean visit (Barline node);

    boolean visit (Beam node);

    boolean visit (Chord node);

    boolean visit (Clef node);

    boolean visit (Coda node);

    boolean visit (Dynamics node);

    boolean visit (Fermata node);

    boolean visit (KeySignature node);

    boolean visit (Measure node);

    boolean visit (MeasureElement node);

    boolean visit (MeasureNode node);

    boolean visit (Note node);

    boolean visit (Ornament node);

    boolean visit (PartNode node);

    boolean visit (Pedal node);

    boolean visit (Score node);

    boolean visit (ScoreNode node);

    boolean visit (Segno node);

    boolean visit (Slur node);

    boolean visit (Staff node);

    boolean visit (System node);

    boolean visit (SystemPart node);

    boolean visit (TimeSignature node);

    boolean visit (Wedge node);
}
