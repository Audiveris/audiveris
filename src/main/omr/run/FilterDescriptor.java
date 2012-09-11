//----------------------------------------------------------------------------//
//                                                                            //
//                      F i l t e r D e s c r i p t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.constant.ConstantSet;

import omr.log.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Management data meant to describe an implementation instance of
 * a PixelFilter.
 * (kind of filter + related parameters)
 */
public abstract class FilterDescriptor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(FilterDescriptor.class);

    //~ Methods ----------------------------------------------------------------
    //
    //---------//
    // getKind //
    //---------//
    /**
     * Report the kind of filter used.
     *
     * @return the filter kind
     */
    public abstract FilterKind getKind ();

    //----------------//
    // getDefaultKind //
    //----------------//
    public static FilterKind getDefaultKind ()
    {
        return constants.defaultKind.getValue();
    }

    //----------------//
    // setDefaultKind //
    //----------------//
    public static void setDefaultKind (FilterKind kind)
    {
        constants.defaultKind.setValue(kind);
    }

    //------------//
    // getDefault //
    //------------//
    /**
     * Report the default descriptor.
     *
     * @return the default descriptor
     */
    public static FilterDescriptor getDefault ()
    {
        final String method = "getDefaultDescriptor";

        try {
            FilterKind kind = getDefaultKind();

            // Access the underlying class
            Method getDesc = kind.classe.getMethod(method, (Class[]) null);

            if (Modifier.isStatic(getDesc.getModifiers())) {
                return (FilterDescriptor) getDesc.invoke(null);
            } else {
                logger.severe(method + " must be static");
            }

        } catch (NoSuchMethodException |
                SecurityException |
                IllegalAccessException |
                IllegalArgumentException |
                InvocationTargetException ex) {
            logger.warning("Could not call " + method, ex);
        }

        return null;
    }

    //------------//
    // setDefault //
    //------------//
    /**
     * Record the default descriptor from now on.
     *
     * @param desc the default descriptor
     */
    public static void setDefault (FilterDescriptor desc)
    {
        if (desc != null) {
            FilterKind kind = desc.getKind();
            FilterDescriptor.setDefaultKind(kind);
            switch (kind) {
            case GLOBAL:
                GlobalDescriptor gDesc = (GlobalDescriptor) desc;
                GlobalFilter.setDefaultThreshold(gDesc.threshold);
                break;
            case ADAPTIVE:
                AdaptiveDescriptor aDesc = (AdaptiveDescriptor) desc;
                AdaptiveFilter.setDefaultMeanCoeff(aDesc.meanCoeff);
                AdaptiveFilter.setDefaultStdDevCoeff(aDesc.stdDevCoeff);
                break;
            }
        }

    }

    //-----------//
    // getFilter //
    //-----------//
    /**
     * Create a filter instance compatible with the descriptor and
     * the underlying pixel source.
     *
     * @param source the underlying pixel source
     * @return the filter instance, ready to use
     */
    public abstract PixelFilter getFilter (PixelSource source);

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        return (obj instanceof FilterDescriptor);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        ///sb.append(getClass().getSimpleName());
        sb.append(internalsString());
        sb.append('}');

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getKind());

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        FilterKind.Constant defaultKind = new FilterKind.Constant(
                FilterKind.ADAPTIVE,
                "Default kind of PixelFilter");

    }
}
