//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           P u r s e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.basic;

import javax.xml.bind.annotation.*;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Purse
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlElement(name = "tip")
    public double[] tips = new double[]{1.0, 2.345, 4.5};

    //~ Methods ------------------------------------------------------------------------------------
    public Double[] getTips ()
    {
        Double[] dd = null;

        if (tips != null) {
            dd = new Double[tips.length];

            for (int i = 0; i < tips.length; i++) {
                dd[i] = tips[i];
            }
        }

        return dd;
    }

    public void setTips (Double[] tips)
    {
        this.tips = new double[tips.length];

        for (int i = 0; i < tips.length; i++) {
            this.tips[i] = tips[i];
        }
    }
}
