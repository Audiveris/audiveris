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

import omr.score.Score;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Barline;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.Measure;
import omr.score.entity.MeasureElement;
import omr.score.entity.MeasureNode;
import omr.score.entity.Note;
import omr.score.entity.Ornament;
import omr.score.entity.PartNode;
import omr.score.entity.Pedal;
import omr.score.entity.ScoreNode;
import omr.score.entity.Segno;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.System;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Wedge;
import omr.score.entity.Words;

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

    boolean visit (Tuplet node);

    boolean visit (Wedge node);

    boolean visit (Words node);
}
