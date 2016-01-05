//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l o b a l D e s c r i p t o r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import ij.process.ByteProcessor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code GlobalDescriptor} describes a {@link GlobalFilter}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "global-filter")
public class GlobalDescriptor
        extends FilterDescriptor
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The threshold value for the whole pixel source. */
    @XmlAttribute(name = "threshold")
    public final int threshold;

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

    /** No-arg constructor meant for JAXB. */
    private GlobalDescriptor ()
    {
        threshold = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // getDefault //
    //------------//
    public static GlobalDescriptor getDefault ()
    {
        return new GlobalDescriptor(GlobalFilter.getDefaultThreshold());
    }

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

    //
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
}
