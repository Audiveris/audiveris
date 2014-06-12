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
package omr.sig;

/**
 * Interface {@code InterVisitor} is meant to visit any instance of
 * shape interpretation.
 *
 * @author Hervé Bitteur
 */
public interface InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    void visit (Inter inter);

    void visit (AbstractBeamInter inter);

    void visit (AbstractNoteInter inter);

    void visit (KeyAlterInter inter);

    void visit (BraceInter inter);

    void visit (StemInter inter);

    void visit (LedgerInter inter);

    void visit (SlurInter inter);

    void visit (WedgeInter inter);

    void visit (EndingInter inter);

    void visit (BarlineInter inter);

    void visit (BarConnectionInter inter);
}
