//----------------------------------------------------------------------------//
//                                                                            //
//                               D e b u g                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.score.ui.ScoreDependent;

/**
 * Convenient class meant to temporarily inject some debugging.
 * To be used in sync with file user-actions.xml in settings folder
 *
 * @author Hervé Bitteur
 */
public class Debug
        extends ScoreDependent
{
    //~ Methods ----------------------------------------------------------------
//    //------------------//
//    // injectChordNames //
//    //------------------//
//    @Action(enabledProperty = SHEET_AVAILABLE)
//    public void injectChordNames (ActionEvent e)
//    {
//        Score score = ScoreController.getCurrentScore();
//
//        if (score == null) {
//            return;
//        }
//
//        ScoreSystem system = score.getFirstPage()
//                                  .getFirstSystem();
//        system.acceptChildren(new ChordInjector());
//    }
    //~ Inner Classes ----------------------------------------------------------
//    //---------------//
//    // ChordInjector //
//    //---------------//
//    private static class ChordInjector
//        extends AbstractScoreVisitor
//    {
//        //~ Static fields/initializers -----------------------------------------
//
//        /** List of symbols to inject. */
//        private static final String[] shelf = new String[] {
//                                                  "BMaj7/D#", "BMaj7", "G#m9",
//                                                  "F#", "C#7sus4", "F#"
//                                              };
//
//        //~ Instance fields ----------------------------------------------------
//
//        /** Current index to symbol to inject. */
//        private int symbolCount = 0;
//
//        //~ Methods ------------------------------------------------------------
//
//        @Override
//        public boolean visit (ChordSymbol symbol)
//        {
//            // Replace chord info by one taken from the shelf
//            if (symbolCount < shelf.length) {
//                symbol.info = ChordInfo.create(shelf[symbolCount++]);
//            }
//
//            return false;
//        }
//    }
}
