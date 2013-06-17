//----------------------------------------------------------------------------//
//                                                                            //
//                          B o u n d a r y T a s k                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.BrokenLineContext;
import omr.sheet.Sheet;
import omr.sheet.SystemBoundary;
import omr.sheet.SystemInfo;

import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BrokenLine;
import omr.util.VerticalSide;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code BoundaryTask} modifies systems boundaries
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class BoundaryTask
        extends SheetTask
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of line contexts */
    @XmlElement(name = "context")
    private final List<BrokenLineContext> contexts;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // BoundaryTask //
    //--------------//
    /**
     * Creates a new BoundaryTask object.
     *
     * @param sheet    the related sheet
     * @param contexts the collections of lines contexts
     */
    public BoundaryTask (Sheet sheet,
                         List<BrokenLineContext> contexts)
    {
        super(sheet);

        this.contexts = contexts;
    }

    //--------------//
    // BoundaryTask // No-arg constructor for JAXB only
    //--------------//
    private BoundaryTask ()
    {
        contexts = null;
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        for (BrokenLineContext context : contexts) {
            // Use a deep copy of the points sequence
            List<Point> copy = new ArrayList<>();

            for (Point p : context.line.getPoints()) {
                copy.add(new Point(p));
            }

            if (context.systemAbove != 0) {
                SystemInfo system = sheet.getSystems()
                        .get(context.systemAbove - 1);
                SystemBoundary boundary = system.getBoundary();
                BrokenLine brokenLine = boundary.getLimit(VerticalSide.BOTTOM);
                brokenLine.resetPoints(copy);
                system.updateBoundary();
            }

            if (context.systemBelow != 0) {
                SystemInfo system = sheet.getSystems()
                        .get(context.systemBelow - 1);
                SystemBoundary boundary = system.getBoundary();
                BrokenLine brokenLine = boundary.getLimit(VerticalSide.TOP);
                brokenLine.resetPoints(copy);
                system.updateBoundary();
            }
        }
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        // Resplit systems content
        sheet.getSystemsBuilder().splitSystemEntities();

        // Update the following steps if any
        Stepping.reprocessSheet(
                Steps.valueOf(Steps.TEXTS),
                sheet,
                sheet.getSystems(),
                false);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(" boundary");
        sb.append(" [");

        for (BrokenLineContext context : contexts) {
            sb.append(" ")
                    .append(context);
        }

        sb.append("]");

        return sb.toString() + super.internalsString();
    }
}
