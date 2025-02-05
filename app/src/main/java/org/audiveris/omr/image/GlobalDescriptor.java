//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l o b a l D e s c r i p t o r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.image;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import ij.process.ByteProcessor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>GlobalDescriptor</code> describes a {@link GlobalFilter} which applies
 * the same global threshold on all pixels of the input image.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "global-filter")
public class GlobalDescriptor
        extends FilterDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** The maximum value for threshold */
    public static final int MAXTHRESHOLD = 255;

    /** The minimum value for threshold */
    public static final int MINTHRESHOLD = 0;

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The <b>same</b> threshold value to apply on <b>all</b> pixels of the source.
     */
    @XmlAttribute(name = "threshold")
    public final int threshold;

    /** No-argument constructor meant for JAXB. */
    private GlobalDescriptor ()
    {
        threshold = 0;
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new GlobalDescriptor object.
     *
     * @param threshold Global threshold value
     */
    public GlobalDescriptor (int threshold)
    {
        this.threshold = threshold;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if ((obj instanceof GlobalDescriptor) && super.equals(obj)) {
            GlobalDescriptor that = (GlobalDescriptor) obj;

            return this.threshold == that.threshold;
        }

        return false;
    }

    //-----------//
    // getFilter //
    //-----------//
    @Override
    public PixelFilter getFilter (ByteProcessor source)
    {
        return new GlobalFilter(source, threshold);
    }

    //---------//
    // getKind //
    //---------//
    @Override
    public FilterKind getKind ()
    {
        return FilterKind.GLOBAL;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (53 * hash) + this.threshold;

        return hash;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" threshold: ").append(threshold);

        return sb.toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // defaultIsSpecific //
    //-------------------//
    public static boolean defaultIsSpecific ()
    {
        return !constants.defaultThreshold.isSourceValue();
    }

    //------------//
    // getDefault //
    //------------//
    public static GlobalDescriptor getDefault ()
    {
        return new GlobalDescriptor(getDefaultThreshold());
    }

    //---------------------//
    // getDefaultThreshold //
    //---------------------//
    public static int getDefaultThreshold ()
    {
        return constants.defaultThreshold.getValue();
    }

    //----------------//
    // getSourceValue //
    //----------------//
    public static GlobalDescriptor getSourceValue ()
    {
        return new GlobalDescriptor(constants.defaultThreshold.getSourceValue());
    }

    //---------------//
    // resetToSource //
    //---------------//
    public static void resetToSource ()
    {
        constants.defaultThreshold.resetToSource();
    }

    //---------------------//
    // setDefaultThreshold //
    //---------------------//
    public static void setDefaultThreshold (int threshold)
    {
        constants.defaultThreshold.setValue(threshold);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer defaultThreshold = new Constant.Integer(
                "GrayLevel",
                140,
                "Default threshold value (in 0..255)");
    }
}
