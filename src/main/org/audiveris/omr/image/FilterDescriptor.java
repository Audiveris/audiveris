//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                F i l t e r D e s c r i p t o r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Management data meant to describe an implementation instance of a PixelFilter.
 * (kind of filter + related parameters)
 *
 * @author Hervé Bitteur
 */
public abstract class FilterDescriptor
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FilterDescriptor.class);

    /** Default param. */
    public static final Param<FilterDescriptor> defaultFilter = new Default();

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        return (obj instanceof FilterDescriptor);
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
    public abstract PixelFilter getFilter (ByteProcessor source);

    //---------//
    // getKind //
    //---------//
    /**
     * Report the kind of filter used.
     *
     * @return the filter kind
     */
    public abstract FilterKind getKind ();

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
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
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
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Enum<FilterKind> defaultKind = new Constant.Enum<>(
                FilterKind.class,
                FilterKind.ADAPTIVE,
                "Default kind of PixelFilter (GLOBAL or ADAPTIVE)");
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<FilterDescriptor>
    {

        @Override
        public FilterDescriptor getSourceValue ()
        {
            final FilterKind sourceKind = constants.defaultKind.getSourceValue();

            switch (sourceKind) {
            case GLOBAL:
                return GlobalDescriptor.getSourceValue();

            default:
            case ADAPTIVE:
                return AdaptiveDescriptor.getSourceValue();
            }
        }

        @Override
        public FilterDescriptor getSpecific ()
        {
            if (isSpecific()) {
                return getValue();
            } else {
                return null;
            }
        }

        @Override
        public FilterDescriptor getValue ()
        {
            switch (getDefaultKind()) {
            case GLOBAL:
                return GlobalDescriptor.getDefault();

            default:
            case ADAPTIVE:
                return AdaptiveDescriptor.getDefault();
            }
        }

        @Override
        public boolean isSpecific ()
        {
            if (!constants.defaultKind.isSourceValue()) {
                return true;
            }

            switch (getDefaultKind()) {
            case GLOBAL:
                return GlobalDescriptor.defaultIsSpecific();

            default:
            case ADAPTIVE:
                return AdaptiveDescriptor.defaultIsSpecific();
            }
        }

        @Override
        public boolean setSpecific (FilterDescriptor specific)
        {
            if (!getValue().equals(specific)) {
                FilterDescriptor desc;

                if (specific == null) {
                    if (!getValue().equals(getSourceValue())) {
                        // Reset to source
                        constants.defaultKind.resetToSource();
                        GlobalDescriptor.resetToSource();
                        AdaptiveDescriptor.resetToSource();

                        desc = getValue();
                        logger.info("Default binarization filter reset to {}", desc);
                    } else {
                        return false;
                    }
                } else {
                    desc = specific;
                    logger.info("Default binarization filter set to {}", desc);

                    FilterKind kind = specific.getKind();
                    FilterDescriptor.setDefaultKind(kind);

                    switch (kind) {
                    case GLOBAL:

                        GlobalDescriptor gDesc = (GlobalDescriptor) specific;
                        GlobalDescriptor.setDefaultThreshold(gDesc.threshold);
                        AdaptiveDescriptor.resetToSource();

                        break;

                    case ADAPTIVE:

                        AdaptiveDescriptor aDesc = (AdaptiveDescriptor) specific;
                        AdaptiveDescriptor.setDefaultMeanCoeff(aDesc.meanCoeff);
                        AdaptiveDescriptor.setDefaultStdDevCoeff(aDesc.stdDevCoeff);
                        GlobalDescriptor.resetToSource();

                        break;
                    }
                }

                return true;
            }

            return false;
        }
    }
}
