//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e V i s i t o r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.visitor;

import omr.score.Score;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
import omr.score.entity.Barline;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.ChordSymbol;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.DirectionStatement;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.Measure;
import omr.score.entity.MeasureElement;
import omr.score.entity.MeasureNode;
import omr.score.entity.Note;
import omr.score.entity.Ornament;
import omr.score.entity.Page;
import omr.score.entity.PartNode;
import omr.score.entity.Pedal;
import omr.score.entity.ScoreNode;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.TimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.VisitableNode;
import omr.score.entity.Wedge;

/**
 * Interface {@code ScoreVisitor} is meant to visit any node of the
 * Score hierarchy.
 *
 * <p>The hierarchy is meant to be browsed "depth-first".</p>
 * <p>
 * All the polymorphic visit(node) methods return a boolean which
 * tells whether the visit shall continue to the children of this class.
 * <ul>
 * <li>It is true by default (the whole visitable hierarchy is meant to be
 * visited).</li>
 * <li>Returning false avoids the automatic visit of the children of the class
 * for the specific visitor, it is then up to the caller to potentially handle
 * the children by another way.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public interface ScoreVisitor
{
    //~ Methods ----------------------------------------------------------------

    boolean visit (Articulation node);

    boolean visit (Arpeggiate node);

    boolean visit (Barline node);

    boolean visit (Beam node);

    boolean visit (Chord node);

    boolean visit (ChordSymbol node);

    boolean visit (Clef node);

    boolean visit (Coda node);

    boolean visit (DirectionStatement node);

    boolean visit (Dynamics node);

    boolean visit (Fermata node);

    boolean visit (KeySignature node);

    boolean visit (Measure node);

    boolean visit (MeasureElement node);

    boolean visit (MeasureNode node);

    boolean visit (Note node);

    boolean visit (Ornament node);

    boolean visit (Page node);

    boolean visit (PartNode node);

    boolean visit (Pedal node);

    boolean visit (Score node);

    boolean visit (ScoreNode node);

    boolean visit (Segno node);

    boolean visit (Slur node);

    boolean visit (Staff node);

    boolean visit (ScoreSystem node);

    boolean visit (SystemPart node);

    boolean visit (Text node);

    boolean visit (TimeSignature node);

    boolean visit (Tuplet node);

    boolean visit (VisitableNode node);

    boolean visit (Wedge node);
}
