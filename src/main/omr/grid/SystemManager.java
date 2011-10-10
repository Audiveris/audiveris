//----------------------------------------------------------------------------//
//                                                                            //
//                         S y s t e m M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Navigable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code SystemManager} handles physical information about all the
 * systems of a given sheet.
 *
 * @author Hervé Bitteur
 */
public class SystemManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemManager.class);

    //~ Instance fields --------------------------------------------------------

    /** The related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** The sequence of systems, from top to bottom */
    private final List<SystemInfo> systems = new ArrayList<SystemInfo>();

    /** The systems tops per staff */
    private Integer[] systemTops;

    /** The parts tops per staff */
    private Integer[] partTops;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SystemManager //
    //---------------//
    /**
     * Creates a new SystemManager object.
     * @param sheet the related sheet
     */
    public SystemManager (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getFirstSystem //
    //----------------//
    public SystemInfo getFirstSystem ()
    {
        if (systems.isEmpty()) {
            return null;
        } else {
            return systems.get(0);
        }
    }

    //------------//
    // getIndexOf //
    //------------//
    ///TODO @Deprecated
    public int getIndexOf (SystemInfo system)
    {
        return systems.indexOf(system);
    }

    //-------------//
    // setPartTops //
    //-------------//
    /**
     * @param partTops the partTops to set
     */
    public void setPartTops (Integer[] partTops)
    {
        this.partTops = partTops;
    }

    //-------------//
    // getPartTops //
    //-------------//
    /**
     * @return the partTops
     */
    public Integer[] getPartTops ()
    {
        return partTops;
    }

    //----------//
    // getRange //
    //----------//
    /**
     * Report a view on the range of systems from first to last (both inclusive)
     * @param first the first system of the range
     * @param last the last system of the range
     * @return a view on this range
     */
    public List<SystemInfo> getRange (SystemInfo first,
                                      SystemInfo last)
    {
        return systems.subList(getIndexOf(first), getIndexOf(last) + 1);
    }

    //-----------//
    // getSystem //
    //-----------//
    ///TODO @Deprecated
    public SystemInfo getSystem (int index)
    {
        return systems.get(index);
    }

    //    //-------------//
    //    // getSystemAt //
    //    //-------------//
    //    /**
    //     * Report the system whose area contains the provided point
    //     * @param point the provided point
    //     * @return the nearest system, or null if none found
    //     */
    //    public SystemInfo getSystemAt (Point2D point)
    //    {
    //        for (SystemInfo system : systems) {
    //            Rectangle2D box = system.getAreaBounds();
    //
    //            if (point.getY() > box.getMaxY()) {
    //                continue;
    //            }
    //
    //            if (point.getY() < box.getMinY()) {
    //                break;
    //            }
    //
    //            if (system.getArea()
    //                     .contains(point)) {
    //                return system;
    //            }
    //        }
    //
    //        return null;
    //    }

    //----------------//
    // getSystemCount //
    //----------------//
    /**
     * Report the total number of systems
     * @return the count of systems
     */
    public int getSystemCount ()
    {
        return systems.size();
    }

    //---------------//
    // setSystemTops //
    //---------------//
    /**
     * @param systemTops the systemTops to set
     */
    public void setSystemTops (Integer[] systemTops)
    {
        this.systemTops = systemTops;
    }

    //---------------//
    // getSystemTops //
    //---------------//
    /**
     * @return the systemTops
     */
    public Integer[] getSystemTops ()
    {
        return systemTops;
    }

    //------------//
    // setSystems //
    //------------//
    /**
     * Assign the whole sequence of systems
     * @param systems  the (new) systems
     */
    public void setSystems (Collection<SystemInfo> systems)
    {
        reset();
        this.systems.addAll(systems);
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report an unmodifiable view (perhaps empty) of list of current systems
     * @return a view on systems
     */
    public List<SystemInfo> getSystems ()
    {
        return Collections.unmodifiableList(systems);
    }

    //-----------//
    // addSystem //
    //-----------//
    /**
     * Append one system to the current collection
     * @param system the system to add
     */
    public void addSystem (SystemInfo system)
    {
        systems.add(system);
    }

    //    //--------------------//
    //    // computeSystemLimits //
    //    //--------------------//
    //    public void computeSystemLimits ()
    //    {
    //        final int width = sheet.getWidth();
    //        final int height = sheet.getHeight();
    //        SystemInfo prevSystem = null;
    //        double    samplingDx = sheet.getScale()
    //                                    .toPixelsDouble(constants.samplingDx);
    //        final int sampleCount = (int) Math.rint(width / samplingDx);
    //        samplingDx = width / sampleCount;
    //
    //        for (SystemInfo system : systems) {
    //            if (prevSystem == null) {
    //                // Very first system
    //                system.setLimit(
    //                    TOP,
    //                    new GeoPath(new Line2D.Double(0, 0, width, 0)));
    //            } else {
    //                // Define a middle line between last line of previous system 
    //                // and first line of current system
    //                LineInfo prevLine = prevSystem.getLastLine();
    //                LineInfo nextLine = system.getFirstLine();
    //                GeoPath  middle = new GeoPath();
    //
    //                for (int i = 0; i <= sampleCount; i++) {
    //                    int    x = (int) Math.rint(i * samplingDx);
    //                    double y = (prevLine.yAt(x) + nextLine.yAt(x)) / 2;
    //
    //                    if (i == 0) {
    //                        middle.moveTo(x, y);
    //                    } else {
    //                        middle.lineTo(x, y);
    //                    }
    //                }
    //
    //                prevSystem.setLimit(BOTTOM, middle);
    //                system.setLimit(TOP, middle);
    //            }
    //
    //            // Remember this system for next one
    //            prevSystem = system;
    //        }
    //
    //        // Bottom of last system
    //        prevSystem.setLimit(
    //            BOTTOM,
    //            new GeoPath(new Line2D.Double(0, height, width, height)));
    //    }

    //    //--------//
    //    // render //
    //    //--------//
    //    /**
    //     * Paint all the system lines
    //     * @param g the graphics context (including current color and stroke)
    //     */
    //    public void render (Graphics2D g)
    //    {
    //        for (SystemInfo system : systems) {
    //            system.render(g);
    //        }
    //    }

    //-------//
    // reset //
    //-------//
    /**
     * Empty the whole collection of systems
     */
    public void reset ()
    {
        systems.clear();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction samplingDx = new Scale.Fraction(
            4d,
            "Abscissa sampling to determine vertical limits of system areas");
    }
}
