//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C o n s t a n t                                         //
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
package org.audiveris.omr.constant;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This abstract class handles the mapping between one application variable and a
 * property name and value.
 * <p>
 * It is meant essentially to handle any kind of symbolic constant, whose value may have to be tuned
 * and saved for future runs of the application.
 * <p>
 * Please refer to {@link ConstantManager} for a detailed explanation on how the current value of
 * any given Constant is determined at run-time.
 * <p>
 * The class {@code Constant} is not meant to be used directly (it is abstract), but rather through
 * any of its subclasses:
 * <ul>
 * <li>{@link Constant.Angle}</li>
 * <li>{@link Constant.Boolean}</li>
 * <li>{@link Constant.Color}</li>
 * <li>{@link Constant.Double}</li>
 * <li>{@link Constant.Integer}</li>
 * <li>{@link Constant.Ratio}</li>
 * <li>{@link Constant.String}</li>
 * <li>and others...</li>
 * </ul>
 *
 * @author Hervé Bitteur
 * @param <E> specific constant type
 */
@ThreadSafe
public abstract class Constant<E>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Constant.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Data assigned at construction time
    //-----------------------------------
    /** Unit (if relevant) used by the quantity measured. */
    private final java.lang.String quantityUnit;

    /** Source-provided value to be used if needed. */
    private final java.lang.String sourceString;

    /** Semantic. */
    private final java.lang.String description;

    // Data assigned at ConstantSet initMap time
    //------------------------------------------
    /** Name of the Constant. */
    private volatile java.lang.String name;

    /** Fully qualified Constant name. (unit.name) */
    private volatile java.lang.String qualifiedName;

    // Data modified at any time
    //--------------------------
    /** Current data. */
    private AtomicReference<Tuple<E>> tuple = new AtomicReference<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a constant instance, while providing a default value,
     * in case the external property is not yet defined.
     *
     * @param quantityUnit Unit used as base for measure, if relevant
     * @param sourceString Source value, expressed by a string literal which cannot be null
     * @param description  A quick description of the purpose of this constant
     */
    protected Constant (java.lang.String quantityUnit,
                        java.lang.String sourceString,
                        java.lang.String description)
    {
        if (sourceString == null) {
            logger.warn("*** Constant with no sourceString. Description: {}", description);
            throw new IllegalArgumentException("Any constant must have a source-provided String");
        }

        this.quantityUnit = quantityUnit;
        this.sourceString = sourceString;
        this.description = description;
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Get the description sentence recorded with the constant
     *
     * @return the description sentence as a string
     */
    public java.lang.String getDescription ()
    {
        return description;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of the constant
     *
     * @return the constant name
     */
    public java.lang.String getName ()
    {
        return name;
    }

    //------------------//
    // getQualifiedName //
    //------------------//
    /**
     * Report the qualified name of the constant
     *
     * @return the constant qualified name
     */
    public java.lang.String getQualifiedName ()
    {
        return qualifiedName;
    }

    //-----------------//
    // getQuantityUnit //
    //-----------------//
    /**
     * Report the unit, if any, used as base of quantity measure
     *
     * @return the quantity unit, if any
     */
    public java.lang.String getQuantityUnit ()
    {
        return quantityUnit;
    }

    //-----------------//
    // getSourceString //
    //-----------------//
    /**
     * Report the constant source string
     *
     * @return the source string
     */
    public java.lang.String getSourceString ()
    {
        return sourceString;
    }

    //----------------//
    // getSourceValue //
    //----------------//
    /**
     * Report the value corresponding to the source string.
     *
     * @return the source value
     */
    public E getSourceValue ()
    {
        return decode(getSourceString());
    }

    //----------------//
    // getStringValue //
    //----------------//
    /**
     * Get the current value, as a String type.
     *
     * @return the String view of the value
     */
    public java.lang.String getStringValue ()
    {
        return getTuple().currentString;
    }

    //----------------//
    // setStringValue //
    //----------------//
    /**
     * Modify the current value of the constant.
     *
     * @param string the new value, as a string to be checked
     */
    public void setStringValue (java.lang.String string)
    {
        setValue(decode(string));
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the current constant value.
     *
     * @return constant value
     */
    public E getValue ()
    {
        return getCachedValue();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new value to the constant.
     *
     * @param value new value
     */
    public void setValue (E value)
    {
        setTuple(value.toString(), value);
    }

    //---------------//
    // isSourceValue //
    //---------------//
    /**
     * Report whether the current constant value is the source one.
     * (not altered by either properties read from disk, of value changed later)
     *
     * @return true if still the source value, false otherwise
     */
    public boolean isSourceValue ()
    {
        return getStringValue().equals(sourceString);
    }

    //---------------//
    // resetToSource //
    //---------------//
    /**
     * Forget any modification made, and resetToSource to the source value.
     */
    public void resetToSource ()
    {
        setTuple(sourceString, decode(sourceString));
    }

    //------------------//
    // toDetailedString //
    //------------------//
    /**
     * Report detailed data about this constant
     *
     * @return data meant for end user
     */
    public java.lang.String toDetailedString ()
    {
        StringBuilder sb = new StringBuilder(getQualifiedName());
        sb.append(" (").append(getStringValue()).append(")");
        sb.append(" \"").append(getDescription()).append("\"");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Used by UnitTreeTable to display the name of the constant,
     * so only the unqualified name is returned.
     *
     * @return the (unqualified) constant name
     */
    @Override
    public java.lang.String toString ()
    {
        return (name != null) ? name : "NO_NAME";
    }

    //--------//
    // decode //
    //--------//
    /**
     * Convert a given string to proper object value, as implemented by each subclass.
     *
     * @param str the encoded string
     * @return the decoded object
     */
    protected abstract E decode (java.lang.String str);

    //----------------//
    // getCachedValue //
    //----------------//
    /**
     * Report the current value of the constant
     *
     * @return the (cached) current value
     */
    protected E getCachedValue ()
    {
        return getTuple().cachedValue;
    }

    //----------//
    // setTuple //
    //----------//
    /**
     * Modify the current parameter data in an atomic way,
     * and remember the very first value (the initial string).
     *
     * @param str The new value (as a string)
     * @param val The new value (as an object)
     */
    protected void setTuple (java.lang.String str,
                             E val)
    {
        while (true) {
            Tuple<E> old = tuple.get();
            Tuple<E> temp = new Tuple<>(str, val);

            if (old == null) {
                if (tuple.compareAndSet(null, temp)) {
                    return;
                }
            } else {
                tuple.set(temp);

                return;
            }
        }
    }

    //----------------//
    // setUnitAndName //
    //----------------//
    /**
     * Allows to record the unit and name of the constant.
     *
     * @param unit the unit (class name) this constant belongs to
     * @param name the constant name
     */
    protected void setUnitAndName (java.lang.String unit,
                                   java.lang.String name)
    {
        this.name = name;

        final java.lang.String qName = (unit != null) ? (unit + "." + name) : name;

        // We can now try to register that constant
        try {
            java.lang.String prop = ConstantManager.getInstance().addConstant(qName, this);

            // Now we can assign a first current value
            if (prop != null) {
                // Use property value
                setTuple(prop, decode(prop));
            } else {
                // Use source value
                ///logger.info("setUnitAndName. unit:" + unit + " name:" + name);
                setTuple(sourceString, decode(sourceString));
            }

            // Very last thing
            qualifiedName = qName;

            //            System.out.println(
            //                Thread.currentThread().getName() + ": " + "Done unit:" + unit +
            //                " name:" + name);
        } catch (Exception ex) {
            logger.warn("Error registering constant {}", qName);
            ex.printStackTrace();
        }
    }

    //------------------//
    // checkInitialized //
    //------------------//
    /**
     * Check the unit+name have been assigned to this constant object.
     * They are mandatory to link the constant to the persistency mechanism.
     */
    private void checkInitialized ()
    {
        int i = 0;

        // Make sure everything is initialized properly
        while (qualifiedName == null) {
            i++;
            UnitManager.getInstance().checkDirtySets();
        }
    }

    //----------//
    // getTuple //
    //----------//
    /**
     * Report the current tuple data, which may imply to trigger the
     * assignment of qualified name to the constant, in order to get
     * property data
     *
     * @return the current tuple data
     */
    private Tuple<E> getTuple ()
    {
        checkInitialized();

        return tuple.get();
    }

    //----------------//
    // getValueOrigin //
    //----------------//
    /**
     * Convenient method, reporting the origin of the current value for
     * this constant, either SRC or USR.
     *
     * @return a mnemonic for the value origin
     */
    java.lang.String getValueOrigin ()
    {
        ConstantManager mgr = ConstantManager.getInstance();
        java.lang.String cur = getStringValue();
        java.lang.String usr = mgr.getConstantUserValue(qualifiedName);
        java.lang.String src = sourceString;

        if (cur.equals(src)) {
            return "SRC";
        }

        if (cur.equals(usr)) {
            return "USR";
        }

        return "???";
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Angle //
    //-------//
    /**
     * A subclass of Double, meant to store an angle (in radians).
     */
    public static class Angle
            extends Constant.Double
    {

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Angle (double defaultValue,
                      java.lang.String description)
        {
            super("Radians", defaultValue, description);
        }
    }

    //---------//
    // Boolean //
    //---------//
    /**
     * A subclass of Constant, meant to store a boolean value.
     */
    public static class Boolean
            extends Constant<java.lang.Boolean>
    {

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (boolean) default value
         * @param description  the semantic of the constant
         */
        public Boolean (boolean defaultValue,
                        java.lang.String description)
        {
            super(null, java.lang.Boolean.toString(defaultValue), description);
        }

        /**
         * Convenient method to access this boolean value
         *
         * @return true if set, false otherwise
         */
        public boolean isSet ()
        {
            return getValue();
        }

        @Override
        protected java.lang.Boolean decode (java.lang.String str)
        {
            return java.lang.Boolean.valueOf(str);
        }
    }

    //-------//
    // Color //
    //-------//
    /**
     * A subclass of Constant, meant to store a {@link java.awt.Color} value.
     * They have a disk repository which is separate from the other constants.
     */
    public static class Color
            extends Constant<java.awt.Color>
    {

        /**
         * Normal constructor, with a String type for default value
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default (String) RGB value
         * @param description  the semantic of the constant
         */
        public Color (java.lang.String unit,
                      java.lang.String name,
                      java.lang.String defaultValue,
                      java.lang.String description)
        {
            super(null, defaultValue, description);
            setUnitAndName(unit, name);
        }

        @Override
        public void setValue (java.awt.Color val)
        {
            setTuple(encodeColor(val), val);
        }

        @Override
        protected java.awt.Color decode (java.lang.String str)
        {
            return decodeColor(str);
        }

        //-------------//
        // decodeColor //
        //-------------//
        /**
         * Decode a color string.
         *
         * @param str input string
         * @return the color object
         */
        public static java.awt.Color decodeColor (java.lang.String str)
        {
            return java.awt.Color.decode(str);
        }

        //-------------//
        // encodeColor //
        //-------------//
        /**
         * Encode a color as a string
         *
         * @param color Color object to encode
         * @return color string
         */
        public static java.lang.String encodeColor (java.awt.Color color)
        {
            return java.lang.String.format(
                    "#%02x%02x%02x",
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue());
        }
    }

    //------//
    // Date //
    //------//
    public static class Date
            extends Constant<java.util.Date>
    {

        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy",
                                                                           Locale.US);

        public Date (java.util.Date defaultValue,
                     java.lang.String description)
        {
            super(null, encode(defaultValue), description);
        }

        public Date (java.lang.String str,
                     java.lang.String description)
        {
            super(null, str, description);
            setValue(str);
        }

        @Override
        protected java.util.Date decode (java.lang.String str)
        {
            try {
                return DATE_FORMAT.parse(str);
            } catch (ParseException ex) {
                logger.error("Error parsing {}", str, ex);
                return null;
            }
        }

        @Override
        public void setValue (java.util.Date date)
        {
            setTuple(encode(date), date);
        }

        public final void setValue (java.lang.String str)
        {
            setTuple(str, decode(str));
        }

        public static java.lang.String encode (java.util.Date date)
        {
            return DATE_FORMAT.format(date);
        }

        public static java.util.Date decodeDate (java.lang.String str)
        {
            try {
                return DATE_FORMAT.parse(str);
            } catch (ParseException ex) {
                logger.error("Error parsing {}", str, ex);
                return null;
            }
        }
    }

    //--------//
    // Double //
    //--------//
    /**
     * A subclass of Constant, meant to store a double value.
     */
    public static class Double
            extends Constant<java.lang.Double>
    {

        public static final Double ZERO = new Double("none", 0, "Zero");

        public static final Double HALF = new Double("none", 0.5, "Half");

        public static final Double ONE = new Double("none", 1, "One");

        public static final Double TWO = new Double("none", 2, "Two");

        static {
            ZERO.setUnitAndName(Constant.class.getName(), "doubleZero");
            HALF.setUnitAndName(Constant.class.getName(), "doubleHalf");
            ONE.setUnitAndName(Constant.class.getName(), "doubleOne");
            TWO.setUnitAndName(Constant.class.getName(), "doubleTwo");
        }

        /**
         * Create a Double object.
         *
         * @param quantityUnit name of unit used
         * @param defaultValue default value
         * @param description  user description
         */
        public Double (java.lang.String quantityUnit,
                       double defaultValue,
                       java.lang.String description)
        {
            super(quantityUnit, java.lang.Double.toString(defaultValue), description);
        }

        @Override
        protected java.lang.Double decode (java.lang.String str)
        {
            return java.lang.Double.valueOf(str);
        }
    }

    //------//
    // Enum //
    //------//
    /**
     * An enumerated constant.
     *
     * @param <E> underlying enum type
     */
    public static class Enum<E extends java.lang.Enum<E>>
            extends Constant<E>
    {

        private final Class<E> classe;

        /**
         * Create an enum constant.
         *
         * @param classe       enum class
         * @param defaultValue default value
         * @param description  user description
         */
        public Enum (Class<E> classe,
                     E defaultValue,
                     java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
            this.classe = classe;
        }

        @Override
        public E getSourceValue ()
        {
            return decode(getSourceString());
        }

        @Override
        public E getValue ()
        {
            return getCachedValue();
        }

        @Override
        protected E decode (java.lang.String str)
        {
            return java.lang.Enum.valueOf(classe, str);
        }
    }

    //---------//
    // Integer //
    //---------//
    /**
     * A subclass of Constant, meant to store an int value.
     */
    public static class Integer
            extends Constant<java.lang.Integer>
    {

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param quantityUnit unit used by this value
         * @param defaultValue the (int) default value
         * @param description  the semantic of the constant
         */
        public Integer (java.lang.String quantityUnit,
                        int defaultValue,
                        java.lang.String description)
        {
            super(quantityUnit, java.lang.Integer.toString(defaultValue), description);
        }

        @Override
        protected java.lang.Integer decode (java.lang.String str)
        {
            return java.lang.Integer.valueOf(str);
        }
    }

    //-------//
    // Ratio //
    //-------//
    /**
     * A subclass of Double, meant to store a ratio or percentage.
     */
    public static class Ratio
            extends Constant.Double
    {

        public static final Ratio ZERO = new Ratio(0, "zero");

        public static final Ratio ONE = new Ratio(1, "one");

        static {
            ZERO.setUnitAndName(Constant.class.getName(), "ratioZero");
            ONE.setUnitAndName(Constant.class.getName(), "ratioOne");
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Ratio (double defaultValue,
                      java.lang.String description)
        {
            super(null, defaultValue, description);
        }
    }

    //--------//
    // String //
    //--------//
    /**
     * A subclass of Constant, meant to store a string value.
     */
    public static class String
            extends Constant<java.lang.String>
    {

        /**
         * Normal constructor, with a string type for default value
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default (string) value
         * @param description  the semantic of the constant
         */
        public String (java.lang.String unit,
                       java.lang.String name,
                       java.lang.String defaultValue,
                       java.lang.String description)
        {
            this(defaultValue, description);
            setUnitAndName(unit, name);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (string) default value
         * @param description  the semantic of the constant
         */
        public String (java.lang.String defaultValue,
                       java.lang.String description)
        {
            super(null, defaultValue, description);
        }

        @Override
        protected java.lang.String decode (java.lang.String str)
        {
            return str;
        }
    }

    //-------//
    // Tuple //
    //-------//
    /**
     * Class used to handle the tuple [currentString + currentValue] in an atomic way.
     */
    private static class Tuple<E>
    {

        /** Current string Value. */
        final java.lang.String currentString;

        /** Current cached Value (optimized). */
        final E cachedValue;

        Tuple (java.lang.String currentString,
               E cachedValue)
        {
            this.currentString = currentString;
            this.cachedValue = cachedValue;
        }

        @Override
        public java.lang.String toString ()
        {
            return currentString;
        }
    }
}
