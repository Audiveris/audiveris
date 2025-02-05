//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          V e r s i o n                                         //
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
package org.audiveris.omr.util;

import org.audiveris.omr.sheet.SheetStub;

import java.util.Objects;

/**
 * Class <code>Version</code> handles the different components of a version string.
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
    //~ Instance fields ----------------------------------------------------------------------------

    public final String value;

    public final int major;

    public final int minor;

    public final int patch;

    public final String label;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>Version</code> object and parse its components.
     * <p>
     * Accepted tag formats to infer version number:
     * <ul>
     * <li>5.2.1-alpha
     * <li>5.2.1-beta
     * <li>5.2.1
     * <li>5.3-alpha
     * <li>5.3
     * <li>6.0
     * </ul>
     * NOTA: For convenience, we also accept "v5.2.1" as synonym for "5.2.1"
     *
     * @param value tag name string
     */
    public Version (String value)
    {
        this.value = value;

        // Parse value
        final int len = value.length();
        final int dot1 = value.indexOf('.', 0);

        if (dot1 == -1) {
            throw new IllegalArgumentException("Illegal version structure: " + value);
        }

        // Protection against "v123" or "V123" to just get 123 integer string
        final String majorString = value.substring(0, dot1).replaceAll("[vV]", "");
        major = Integer.decode(majorString);

        final int dot2 = value.indexOf('.', dot1 + 1);

        if (dot2 == -1) {
            // Perhaps 5.3
            // Perhaps 5.3-alpha
            patch = 0;
            final int dash = value.indexOf('-', dot1 + 1);

            if (dash == -1) {
                minor = Integer.decode(value.substring(dot1 + 1, len));
                label = "";
            } else {
                minor = Integer.decode(value.substring(dot1 + 1, dash));
                label = value.substring(dash + 1, len);
            }
        } else {
            // Perhaps 5.2.1-beta
            // Perhaps 5.2.1
            minor = Integer.decode(value.substring(dot1 + 1, dot2));

            final int dash = value.indexOf('-', dot2 + 1);

            if (dash == -1) {
                patch = Integer.decode(value.substring(dot2 + 1, len));
                label = "";
            } else {
                patch = Integer.decode(value.substring(dot2 + 1, dash));
                label = value.substring(dash + 1, len);
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

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
     * @return <code>true</code> if this object is the same as the obj argument;
     *         <code>false</code> otherwise.
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
        hash = (83 * hash) + major;
        hash = (83 * hash) + minor;
        hash = (83 * hash) + patch;

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

    //~ Static Methods -----------------------------------------------------------------------------

    //-----//
    // min //
    //-----//
    public static Version min (Version v1,
                               Version v2)
    {
        Objects.requireNonNull(v1, "Version v1 is null");
        Objects.requireNonNull(v2, "Version v2 is null");

        return v1.compareTo(v2) <= 0 ? v1 : v2;
    }

    //--------------//
    // minWithLabel //
    //--------------//
    public static Version minWithLabel (Version v1,
                                        Version v2)
    {
        Objects.requireNonNull(v1, "Version v1 is null");
        Objects.requireNonNull(v2, "Version v2 is null");

        return v1.compareWithLabelTo(v2) <= 0 ? v1 : v2;
    }

    //~ Inner classes ------------------------------------------------------------------------------

    //----------------//
    // UpgradeVersion //
    //----------------//
    /**
     * A version with specific rules for detecting needed upgrade.
     */
    public static class UpgradeVersion
            extends Version
    {
        public UpgradeVersion (String value)
        {
            super(value);
        }

        // Override if needed
        public boolean upgradeNeeded (SheetStub stub)
        {
            // Test based purely on version value
            return stub.getVersion().compareTo(this) < 0;
        }
    }
}
