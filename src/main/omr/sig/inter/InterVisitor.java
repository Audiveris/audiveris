//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r V i s i t o r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

/**
 * Interface {@code InterVisitor} is used to visit any instance of shape interpretation.
 *
 * @author Hervé Bitteur
 */
public interface InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    void visit (AbstractBeamInter inter);

    void visit (AbstractHeadInter inter);

    void visit (BarConnectionInter inter);

    void visit (BarlineInter inter);

    void visit (BraceInter inter);

    void visit (BracketConnectionInter inter);

    void visit (BracketInter inter);

    void visit (ChordInter inter);

    void visit (ClefInter inter);

    void visit (EndingInter inter);

    void visit (FlagInter inter);

    void visit (Inter inter);

    void visit (KeyAlterInter inter);

    void visit (KeyInter inter);

    void visit (LedgerInter inter);

    void visit (SentenceInter inter);

    void visit (SlurInter inter);

    void visit (StemInter inter);

    void visit (TimeWholeInter inter);

    void visit (TimePairInter inter);

    void visit (WedgeInter inter);

    void visit (WordInter inter);
}
