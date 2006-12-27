//----------------------------------------------------------------------------//
//                                                                            //
//                           M e a s u r e N o d e                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import static omr.score.ScoreConstants.*;
import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import java.util.Comparator;

/**
 * Class <code>MeasureNode</code> is an abstract class that is subclassed for
 * any PartNode with a containing measure. So this class encapsulates a direct
 * link to the enclosing measure. 
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class MeasureNode
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * Specific comparator to sort collections of MeasureNode instances,
     * according first to staff index, then to abscissa.
     */
    public static final Comparator<TreeNode> staffComparator = new Comparator<TreeNode>() {
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            MeasureNode mn1 = (MeasureNode) tn1;
            MeasureNode mn2 = (MeasureNode) tn2;
            int         deltaStaff = mn1.getStaff()
                                        .getId() - mn2.getStaff()
                                                      .getId();

            if (deltaStaff != 0) {
                // Staves are different
                return deltaStaff;
            } else {
                // Staves are the same, use abscissae to differentiate
                return mn1.getCenter().x - mn2.getCenter().x;
            }
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Containing measure */
    private Measure measure;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // MeasureNode //
    //-------------//
    /**
     * Create a MeasureNode
     *
     * @param container the (direct) container of the node
     */
    public MeasureNode (ScoreNode container)
    {
        super(container);

        // Set the measure link
        for (TreeNode c = this; c != null; c = c.getParent()) {
            if (c instanceof Measure) {
                measure = (Measure) c;

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
        StringBuilder sb = new StringBuilder();

        sb.append(getPart().getContextString());

        sb.append("M")
          .append(getMeasure().getId());

        if (getStaff() != null) {
            sb.append("T")
              .append(getStaff().getId());
        }

        return sb.toString();
    }

    //------------//
    // getMeasure //
    //------------//
    /**
     * Report the containing measure
     *
     * @return the containing measure entity
     */
    public Measure getMeasure ()
    {
        return measure;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }
}
