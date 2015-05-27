//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t I n t e r V i s i t o r                            //
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
 * Class {@code AbstractInterVisitor} is a void implementation for {@link InterVisitor}
 * meant to ease the coding of concrete sub-classes.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractInterVisitor
        implements InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void visit (AbstractBeamInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (AbstractFlagInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (AbstractHeadInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BarConnectionInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BarlineInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BraceInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BracketConnectionInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BracketInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (ChordInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (ClefInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (EndingInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (Inter inter)
    {
        // Void by default
    }

    @Override
    public void visit (KeyAlterInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (KeyInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (LedgerInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (SentenceInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (SlurInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (StemInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (TimeWholeInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (TimePairInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (WedgeInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (WordInter inter)
    {
        // Void by default
    }
}
