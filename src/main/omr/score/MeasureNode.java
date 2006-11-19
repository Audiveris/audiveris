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

import omr.glyph.Glyph;
import static omr.score.ScoreConstants.*;
import omr.score.visitor.Visitor;

import omr.sheet.PixelPoint;
import omr.sheet.Scale;

import omr.util.TreeNode;

import java.awt.*;
import java.util.Collection;

/**
 * Class <code>MeasureNode</code> is an abstract class that is subclassed for
 * any ScoreNode with a containing measure. So this class encapsulates a direct
 * link to the enclosing measure.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class MeasureNode
    extends PartNode
{
    //~ Instance fields --------------------------------------------------------

    /** Containing measure */
    protected Measure measure;

    /** Related staff, if relevant */
    private Staff staff;

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
        for (TreeNode c = this; c != null; c = c.getContainer()) {
            if (c instanceof Measure) {
                measure = (Measure) c;

                break;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // getContainmentString //
    //----------------------//
    /**
     * Report a string that describes the containment (measure, part, staff) of
     * this entity
     *
     * @return the properly filled containment string
     */
    public String getContainmentString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Measure ")
          .append(getMeasure().getId());

        sb.append(" Part ")
          .append(getPart().getId());

        if (getStaff() != null) {
            sb.append(" Staff ")
              .append(getStaff().getStaffIndex() + 1);
        }

        sb.append(" : ");

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
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of this entity, wrt the measure top-left corner.
     * Unless overridden, this method raises an exception.
     */
    protected void computeCenter ()
    {
        throw new RuntimeException(
            "computeCenter() not implemented in " + getClass().getName());
    }
}
