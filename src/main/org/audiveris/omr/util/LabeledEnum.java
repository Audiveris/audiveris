//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L a b e l e d E n u m                                     //
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
package org.audiveris.omr.util;

import org.jdesktop.application.ResourceMap;

import java.util.Objects;

/**
 * Class {@code LabeledEnum} is an entry composed of an enum value and a string value.
 * <p>
 * This can be used to mimic documented (or localized) Enum type.
 *
 * @author Hervé Bitteur
 * @param <E> enum type
 */
public class LabeledEnum<E extends Enum<E>>
{

    /** The enum value. */
    public final E value;

    /** The corresponding label. */
    public final String label;

    /**
     * Create a LabeledEnum entry.
     *
     * @param value enum value
     * @param label corresponding label
     */
    public LabeledEnum (E value,
                        String label)
    {
        this.value = value;
        this.label = label;
    }

    @Override
    public String toString ()
    {
        return label;
    }

    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof LabeledEnum)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final LabeledEnum<E> that = (LabeledEnum<E>) obj;
        return (value == that.value) && label.equals(that.label);
    }

    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.value);
        hash = 17 * hash + Objects.hashCode(this.label);
        return hash;
    }

    //--------//
    // values //
    //--------//
    /**
     * Report the LabeledEnum entries that handle the localized version of 'E' enum.
     *
     * @param <E>       the enum type to localize
     * @param values    enum.values()
     * @param resources the bundle resources
     * @param classe    E class used to forge key prefix in resources
     * @return the LabeledEnum entries
     */
    public static <E extends Enum<E>> LabeledEnum<E>[] values (E[] values,
                                                               ResourceMap resources,
                                                               Class<? extends E> classe)
    {
        final String prefix = classe.getSimpleName() + ".";

        return values(values, resources, prefix);
    }

    /**
     * Report the array of LabeledEnum values that represents the localized version
     * of 'E' enum type.
     *
     * @param <E>       the enum type to localize
     * @param values    enum.values()
     * @param resources the bundle resources
     * @param prefix    the prefix used to forge key prefix in resources
     * @return the LabeledEnum entries
     */
    public static <E extends Enum<E>> LabeledEnum<E>[] values (E[] values,
                                                               ResourceMap resources,
                                                               String prefix)
    {
        @SuppressWarnings("unchecked")
        final LabeledEnum<E>[] labeled = new LabeledEnum[values.length];

        for (int i = 0; i < values.length; i++) {
            final E value = values[i];
            final String key = prefix + value.name();
            final String label = resources.getString(key);
            labeled[i] = new LabeledEnum<E>(value, (label != null) ? label : value.name());
        }

        return labeled;
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Report the LabeledEnum for provided enum value in LabeledEnum entries.
     *
     * @param <E>       the underlying enum type
     * @param enumValue the E value
     * @param entries   the labeled entries
     * @return the LabeledEnum instance or null if not found
     */
    public static <E extends Enum<E>> LabeledEnum<E> valueOf (E enumValue,
                                                              LabeledEnum<E>[] entries)
    {
        for (LabeledEnum<E> le : entries) {
            if (le.value == enumValue) {
                return le;
            }
        }

        return null;
    }
}
