//----------------------------------------------------------------------------//
//                                                                            //
//                      F i l t e r D e s c r i p t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.constant.ConstantSet;

import omr.util.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(FilterDescriptor.class);

    /** Default param. */
    public static final Param<FilterDescriptor> defaultFilter = new Default();

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
                FilterKind.GLOBAL,
                "Default kind of PixelFilter");

    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<FilterDescriptor>
    {

        @Override
        public FilterDescriptor getSpecific ()
        {
            final String method = "getDefaultDescriptor";

            try {
                FilterKind kind = getDefaultKind();

                // Access the underlying class
                Method getDesc = kind.classe.getMethod(method, (Class[]) null);

                if (Modifier.isStatic(getDesc.getModifiers())) {
                    return (FilterDescriptor) getDesc.invoke(null);
                } else {
                    logger.error(method + " must be static");
                }

            } catch (NoSuchMethodException |
                    SecurityException |
                    IllegalAccessException |
                    IllegalArgumentException |
                    InvocationTargetException ex) {
                logger.warn("Could not call " + method, ex);
            }

            return null;
        }

        @Override
        public boolean setSpecific (FilterDescriptor specific)
        {
            if (!getSpecific().equals(specific)) {
                FilterKind kind = specific.getKind();
                FilterDescriptor.setDefaultKind(kind);

                switch (kind) {
                case GLOBAL:
                    GlobalDescriptor gDesc = (GlobalDescriptor) specific;
                    GlobalFilter.setDefaultThreshold(gDesc.threshold);
                    break;
                case ADAPTIVE:
                    AdaptiveDescriptor aDesc = (AdaptiveDescriptor) specific;
                    AdaptiveFilter.setDefaultMeanCoeff(aDesc.meanCoeff);
                    AdaptiveFilter.setDefaultStdDevCoeff(aDesc.stdDevCoeff);
                    break;
                }

                logger.info("Default filter is now ''{}''", specific);

                return true;
            }

            return false;
        }
    }
}
