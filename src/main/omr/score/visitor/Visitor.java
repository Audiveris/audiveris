//----------------------------------------------------------------------------//
//                                                                            //
//                               V i s i t o r                                //
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

/**
 * Interface <code>Visitor</code> is meant to visit any node of the Score
 * hierarchy
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface Visitor
{
    //~ Methods ----------------------------------------------------------------

    boolean visit (Barline node);

    boolean visit (Clef node);

    boolean visit (KeySignature node);

    boolean visit (Measure node);

    boolean visit (MusicNode node);

    boolean visit (Score node);

    boolean visit (Slur node);

    boolean visit (Staff node);

    boolean visit (StaffNode node);

    boolean visit (System node);

    boolean visit (TimeSignature node);
}
