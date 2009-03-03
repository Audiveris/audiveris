//----------------------------------------------------------------------------//
//                                                                            //
//                              P a r t N o d e                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;

import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>PartNode</code> is an abstract class that is subclassed for any
 * ScoreNode that is contained in a system part. So this class encapsulates a
 * direct link to the enclosing part.
 *
 * <p>A link to a related staff is provided as a potential tag only, since all
 * PartNode instances (Slur for example) are not related to a specific staff,
 * whereas a Wedge is.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class PartNode
    extends SystemNode
{
    //~ Instance fields --------------------------------------------------------

    /** Containing part */
    private SystemPart part;

    /** Related staff, if relevant */
    private Staff staff;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // PartNode //
    //----------//
    /**
     * Create a PartNode
     *
     * @param container the (direct) container of the node
     */
    public PartNode (SystemNode container)
    {
        super(container);

        // Set the part link
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof SystemPart) {
                part = (SystemPart) c;

                break;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getContextString //
    //------------------//
    @Override
    public String getContextString ()
    {
        StringBuilder sb = new StringBuilder(super.getContextString());
        sb.append("P")
          .append(part.getId());

        return sb.toString();
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the containing part
     *
     * @return the containing part entity
     */
    public SystemPart getPart ()
    {
        return part;
    }

    //----------//
    // getStaff //
    //----------//
    /**
     * Report the related staff if any
     *
     * @return the related staff, or null
     */
    public Staff getStaff ()
    {
        return staff;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //------------------//
    // retrieveSequence //
    //------------------//
    /**
     * Starting from a glyph, build a sequence of glyphs Ok with a provided
     * criteria, up to a sequence final criteria or when it's over.
     *
     * @param seed the first glyph of the sequence
     * @return the sequence of glyphs captured
     */
    public static List<Glyph> retrieveSequence (Glyph             seed,
                                                Collection<Glyph> glyphs,
                                                SequenceAdapter   adapter)
    {
        List<Glyph> sequence = new ArrayList<Glyph>();
        boolean     started = false;

        // Browse the provided glyphs, & start from the seed
        for (Glyph glyph : glyphs) {
            if (glyph == seed) {
                started = true;
                sequence.add(glyph);
            } else if (started) {
                if (adapter.isFinal(glyph)) {
                    sequence.add(glyph);

                    break;
                } else if (adapter.isOver(glyph)) {
                    break;
                } else if (adapter.isOk(glyph)) {
                    sequence.add(glyph);
                }
            }
        }

        return sequence;
    }

    //----------//
    // setStaff //
    //----------//
    /**
     * Assign the related staff
     *
     * @param staff the related staff
     */
    public void setStaff (Staff staff)
    {
        this.staff = staff;
    }

    //~ Inner Interfaces -------------------------------------------------------

    //-----------------//
    // SequenceAdapter //
    //-----------------//
    /**
     * Interface <code>SequenceAdapter</code> defines the checks that drive
     * the retrieval of a glyph sequence.
     */
    public static interface SequenceAdapter
    {
        //~ Methods ------------------------------------------------------------

        /** Check whether this glyph completes the sequence */
        boolean isFinal (Glyph glyph);

        /** Check whether this glyph should be part of the sequence */
        boolean isOk (Glyph glyph);

        /** Check whether this glyph leads to give up the sequence */
        boolean isOver (Glyph glyph);
    }
}
