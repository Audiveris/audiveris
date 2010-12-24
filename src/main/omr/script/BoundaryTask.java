//----------------------------------------------------------------------------//
//                                                                            //
//                          B o u n d a r y T a s k                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Sheet;
import omr.sheet.SystemBoundary;
import omr.sheet.SystemInfo;

import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BrokenLine;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class {@code BoundaryTask} modifies a system boundary.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class BoundaryTask
    extends SheetTask
{
    //~ Instance fields --------------------------------------------------------

    /** The specific side of the system */
    @XmlAttribute(name = "side")
    private SystemBoundary.Side side;

    /** The containing system id */
    @XmlAttribute(name = "system")
    private int systemId;

    /** The modified line */
    @XmlElement(name = "broken-line")
    private BrokenLine line;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BoundaryTask object.
     *
     * @param system The containing system
     * @param side The north or south side of system boundary
     * @param line The modified broken line
     */
    public BoundaryTask (SystemInfo          system,
                         SystemBoundary.Side side,
                         BrokenLine          line)
    {
        super(system.getSheet());

        systemId = system.getId();
        this.side = side;

        // Make a deep copy of the line points
        List<Point> points = new ArrayList<Point>();

        for (Point p : line.getPoints()) {
            points.add(new Point(p));
        }

        this.line = new BrokenLine(points);
    }

    //--------------//
    // BoundaryTask //
    //--------------//
    /** No-arg constructor needed by JAXB */
    private BoundaryTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        SystemInfo  system = sheet.getSystems()
                                  .get(systemId - 1);
        BrokenLine  brokenLine = system.getBoundary()
                                       .getLimit(side);

        // Modify the points and update listeners
        List<Point> copy = new ArrayList<Point>();

        for (Point p : line.getPoints()) {
            copy.add(new Point(p));
        }

        brokenLine.resetPoints(copy);

        // Update the following steps if any
        sheet.getSystemsBuilder()
             .useBoundaries();
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        Stepping.reprocessSheet(
            Steps.valueOf(Steps.SYMBOLS),
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
        sb.append(" system#")
          .append(systemId);
        sb.append(" ")
          .append(side);
        sb.append(" ")
          .append(line);
        sb.append(" ");

        return sb.toString() + super.internalsString();
    }
}
