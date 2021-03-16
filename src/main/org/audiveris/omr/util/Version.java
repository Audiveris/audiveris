//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          V e r s i o n                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

/**
 * Class {@code Version} handles the different components of a version string.
 * <p>
 * (Quoting semantic versioning)
 * <br>
 * Given a version number MAJOR.MINOR.PATCH, increment the:
 * <ol>
 * <li>MAJOR version when you make incompatible API changes,
 * <li>MINOR version when you add functionality in a backwards-compatible manner, and
 * <li>PATCH version when you make backwards-compatible bug fixes.
 * </ol>
 * Additional labels for pre-release and build metadata are available as extensions to the
 * MAJOR.MINOR.PATCH format.
 * <p>
 * Example: "5.1"
 * <p>
 * Example: "5.2.0-alpha"
 *
 * @author Hervé Bitteur
 */
public class Version
        implements Comparable<Version>
{

    public final String value;

    public final int major;

    public final int minor;

    public final int patch;

    public final String label;

    /**
     * Create a {@code Version} object and parse its components.
     *
     * @param value version string
     */
    public Version (String value)
    {
        this.value = value.trim();

        // Parse value
        final int len = this.value.length();
        final int dot1 = this.value.indexOf('.', 0);

        if (dot1 == -1) {
            throw new IllegalArgumentException("Illegal version structure: " + value);
        }

        major = Integer.decode(this.value.substring(0, dot1));

        final int dot2 = this.value.indexOf('.', dot1 + 1);

        if (dot2 == -1) {
            minor = Integer.decode(this.value.substring(dot1 + 1, len));
            patch = 0;
            label = null;
        } else {
            minor = Integer.decode(this.value.substring(dot1 + 1, dot2));

            final int dash = this.value.indexOf('-', dot2 + 1);

            if (dash == -1) {
                patch = Integer.decode(this.value.substring(dot2 + 1, len));
                label = null;
            } else {
                patch = Integer.decode(this.value.substring(dot2 + 1, dash));
                label = this.value.substring(dash + 1, len);
            }
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (Version that)
    {
        if (major != that.major) {
            return Integer.signum(major - that.major);
        }

        if (minor != that.minor) {
            return Integer.signum(minor - that.minor);
        }

        if (patch != that.patch) {
            return Integer.signum(patch - that.patch);
        }

        return 0;
    }

    //--------------------//
    // compareWithLabelTo //
    //--------------------//
    /**
     * More strict comparison, where label is involved.
     *
     * @param that the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareWithLabelTo (Version that)
    {
        final int comp = compareTo(that);

        if (comp != 0) {
            return comp;
        }

        if (label.equals(that.label)) {
            return 0;
        }

        if (label.isEmpty()) {
            return 1;
        }

        if (that.label.isEmpty()) {
            return -1;
        }

        return label.compareToIgnoreCase(that.label);
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof Version)) {
            return false;
        }

        Version that = (Version) obj;

        return (major == that.major) && (minor == that.minor) && (patch == that.patch);
    }

    //-----------------//
    // equalsWithLabel //
    //-----------------//
    /**
     * More strict equality check, where label is involved.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument;
     *         {@code false} otherwise.
     */
    public boolean equalsWithLabel (Object obj)
    {
        final boolean eq = equals(obj);

        if (!eq) {
            return false;
        }

        Version that = (Version) obj;

        return label.equalsIgnoreCase(that.label);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = 83 * hash + major;
        hash = 83 * hash + minor;
        hash = 83 * hash + patch;
        return hash;
    }

    //--------------//
    // toLongString //
    //--------------//
    /**
     * Report a detailed string representation of the object.
     *
     * @return detailed representation
     */
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder("Version{");
        sb.append("major:").append(major);
        sb.append(" minor:").append(minor);
        sb.append(" patch:").append(patch);
        sb.append(" label:").append(label);
        sb.append('}');

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return value;
    }
}
