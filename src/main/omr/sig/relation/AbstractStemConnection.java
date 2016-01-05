//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           A b s t r a c t S t e m C o n n e c t i o n                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.sheet.Scale;

import omr.sig.inter.Inter;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code AbstractStemConnection} is the basis for connections to a stem.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStemConnection
        extends AbstractConnection
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Logical extension point. */
    @XmlElement(name = "extension-point")
    protected Point2D extensionPoint;

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getStemPortion //
    //----------------//
    /**
     * Report the portion of the stem the provided source is connected to
     *
     * @param source   the item connected to the stem (head, beam, flag)
     * @param stemLine logical range of the stem
     * @param scale    global scale
     * @return the stem Portion
     */
    public abstract StemPortion getStemPortion (Inter source,
                                                Line2D stemLine,
                                                Scale scale);

    //-------------------//
    // getExtensionPoint //
    //-------------------//
    /**
     * Report the logical connection point, which is defined as the point with maximum
     * extension along the logical stem.
     * This definition allows to use the extension ordinate to determine the precise stem portion of
     * the connection.
     *
     * @return the extension point
     */
    public Point2D getExtensionPoint ()
    {
        return extensionPoint;
    }

    //-------------------//
    // setExtensionPoint //
    //-------------------//
    /**
     * Set the logical extension point.
     *
     * @param extensionPoint the extension point to set
     */
    public void setExtensionPoint (Point2D extensionPoint)
    {
        this.extensionPoint = extensionPoint;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (extensionPoint != null) {
            sb.append(
                    String.format(" [x:%.0f,y:%.0f]", extensionPoint.getX(), extensionPoint.getY()));
        }

        return sb.toString();
    }
}
