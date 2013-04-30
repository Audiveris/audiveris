//----------------------------------------------------------------------------//
//                                                                            //
//                              C o n s t a n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omr.util.DoubleValue;

import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This abstract class handles the mapping between one application
 * variable and a property name and value. 
 * It is meant essentially to handle any kind of symbolic constant, whose value
 * may have to be tuned and saved for future runs of the application.
 *
 * <p>Please refer to {@link ConstantManager} for a detailed explanation on how
 * the current value of any given Constant is determined at run-time.
 *
 * <p> The class {@code Constant} is not meant to be used directly (it is
 * abstract), but rather through any of its subclasses:
 *
 * <ul> <li> {@link Constant.Angle} </li> <li> {@link Constant.Boolean} </li>
 * <li> {@link Constant.Color} </li> <li> {@link Constant.Double} </li> <li>
 * {@link Constant.Integer} </li> <li> {@link Constant.Ratio} </li>
 * <li> {@link Constant.String} </li> 
 * <li>and others...</li></ul> </p>
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public abstract class Constant
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Constant.class);

    //~ Instance fields --------------------------------------------------------
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
    /** Initial Value (used for reset). Assigned once */
    private java.lang.String initialString;

    /** Current data. */
    private AtomicReference<Tuple> tuple = new AtomicReference<>();

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // Constant //
    //----------//
    /**
     * Creates a constant instance, while providing a default value, 
     * in case the external property is not yet defined.
     *
     * @param quantityUnit Unit used as base for measure, if relevant
     * @param sourceString Source value, expressed by a string literal which
     * cannot be null
     * @param description  A quick description of the purpose of this constant
     */
    protected Constant (java.lang.String quantityUnit,
                        java.lang.String sourceString,
                        java.lang.String description)
    {
        if (sourceString == null) {
            logger.warn(
                    "*** Constant with no sourceString. Description: {}",
                    description);
            throw new IllegalArgumentException(
                    "Any constant must have a source-provided String");
        }

        this.quantityUnit = quantityUnit;
        this.sourceString = sourceString;
        this.description = description;

        //        System.out.println(
        //            Thread.currentThread().getName() + ": " + "-- Creating Constant: " +
        //            description);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // setValue //
    //----------//
    /**
     * Modify the current value of the constant.
     * This abstract method is actually defined in each subclass, to enforce 
     * validation of the provided string with respect to the target constant type.
     *
     * @param string the new value, as a string to be checked
     */
    public abstract void setValue (java.lang.String string);

    //------------------//
    // getCurrentString //
    //------------------//
    /**
     * Get the current value, as a String type.
     *
     * @return the String view of the value
     */
    public java.lang.String getCurrentString ()
    {
        return getTuple().currentString;
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

    //------------------//
    // getShortTypeName //
    //------------------//
    /**
     * Report the very last part of the type name of the constant
     *
     * @return the type name (last part)
     */
    public java.lang.String getShortTypeName ()
    {
        final java.lang.String typeName = getClass().getName();
        final int separator = Math.max(
                typeName.lastIndexOf('$'),
                typeName.lastIndexOf('.'));

        if (separator != -1) {
            return typeName.substring(separator + 1);
        } else {
            return typeName;
        }
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

    //------------//
    // isModified //
    //------------//
    /**
     * Checks whether the current value is different from the original one.
     * NOTA_BENE: The test is made on string literal, which may result in false
     * modification signals, simply because the string for example contains an
     * additional space
     *
     * @return The modification status
     */
    public boolean isModified ()
    {
        return !getCurrentString().equals(initialString);
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
        return getCurrentString().equals(sourceString);
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a given constant from memory
     */
    public void remove ()
    {
        ConstantManager.getInstance().removeConstant(this);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Forget any modification made, and reset to the source value.
     */
    public void reset ()
    {
        setTuple(sourceString, decode(sourceString));
    }

    //---------//
    // setUnit //
    //---------//
    /**
     * Allows to record the unit and name of the constant.
     *
     * @param unit the unit (class name) this constant belongs to
     * @param name the constant name
     */
    public void setUnitAndName (java.lang.String unit,
                                java.lang.String name)
    {
        //        System.out.println(
        //            Thread.currentThread().getName() + ": " + "Assigning unit:" + unit +
        //            " name:" + name);
        this.name = name;

        final java.lang.String qName = (unit != null) ? (unit + "." + name) : name;

        // We can now try to register that constant
        try {
            java.lang.String prop = ConstantManager.getInstance().addConstant(
                    qName, this);

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
        sb.append(" (").append(getCurrentString()).append(")");
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
        return (name != null) ? name : "*no name*";
    }

    //--------//
    // decode //
    //--------//
    /**
     * Convert a given string to the proper object value, as 
     * implemented by each subclass.
     *
     * @param str the encoded string
     * @return the decoded object
     */
    protected abstract Object decode (java.lang.String str);

    //----------------//
    // getCachedValue //
    //----------------//
    /**
     * Report the current value of the constant
     *
     * @return the (cached) current value
     */
    protected Object getCachedValue ()
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
                             Object val)
    {
        while (true) {
            Tuple old = tuple.get();
            Tuple temp = new Tuple(str, val);

            if (old == null) {
                if (tuple.compareAndSet(null, temp)) {
                    initialString = str;

                    return;
                }
            } else {
                tuple.set(temp);

                return;
            }
        }
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
        java.lang.String cur = getCurrentString();
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

        // For monitoring/debugging only
        if (i > 1) {
            System.out.println(
                    "*** " + Thread.currentThread().getName()
                    + " checkInitialized loop:" + i);
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
    private Tuple getTuple ()
    {
        checkInitialized();

        return tuple.get();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------//
    // Angle //
    //-------//
    /**
     * A subclass of Double, meant to store an angle (in radians).
     */
    public static class Angle
            extends Constant.Double
    {
        //~ Constructors -------------------------------------------------------

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
            extends Constant
    {
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------
        /**
         * Retrieve the current constant value
         *
         * @return the current (boolean) value
         */
        public boolean getValue ()
        {
            return ((java.lang.Boolean) getCachedValue()).booleanValue();
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

        /**
         * Allows to set a new boolean value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the boolean value as a string
         */
        @Override
        public void setValue (java.lang.String string)
        {
            setValue(java.lang.Boolean.valueOf(string).booleanValue());
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new (boolean) value
         */
        public void setValue (boolean val)
        {
            setTuple(java.lang.Boolean.toString(val), val);
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
     * A subclass of Constant, meant to store a {@link java.awt.Color}
     * value.
     * They have a disk repository which is separate from the other constants.
    */
    public static class Color
            extends Constant
    {
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------
        //-------------//
        // decodeColor //
        //-------------//
        public static java.awt.Color decodeColor (java.lang.String str)
        {
            return java.awt.Color.decode(str);
        }

        //-------------//
        // encodeColor //
        //-------------//
        public static java.lang.String encodeColor (java.awt.Color color)
        {
            return java.lang.String.format(
                    "#%02x%02x%02x",
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue());
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current (Color) value
         */
        public java.awt.Color getValue ()
        {
            return (java.awt.Color) getCachedValue();
        }

        /**
         * Allows to set a new int RGB value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the int value as a string
         */
        @Override
        public void setValue (java.lang.String string)
        {
            setValue(java.awt.Color.decode(string));
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new Color value
         */
        public void setValue (java.awt.Color val)
        {
            setTuple(encodeColor(val), val);
        }

        @Override
        protected java.awt.Color decode (java.lang.String str)
        {
            return decodeColor(str);
        }
    }

    //--------//
    // Double //
    //--------//
    /**
     * A subclass of Constant, meant to store a double value.
     */
    public static class Double
            extends Constant
    {
        //~ Constructors -------------------------------------------------------

        public Double (java.lang.String quantityUnit,
                       double defaultValue,
                       java.lang.String description)
        {
            super(
                    quantityUnit,
                    java.lang.Double.toString(defaultValue),
                    description);
        }

        //~ Methods ------------------------------------------------------------
        public double getValue ()
        {
            return ((DoubleValue) getCachedValue()).doubleValue();
        }

        public DoubleValue getWrappedValue ()
        {
            // Return a copy
            return new DoubleValue(getValue());
        }

        public void setValue (double val)
        {
            setTuple(java.lang.Double.toString(val), new DoubleValue(val));
        }

        public void setValue (DoubleValue val)
        {
            setTuple(val.toString(), val);
        }

        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }

        @Override
        protected DoubleValue decode (java.lang.String str)
        {
            return new DoubleValue(java.lang.Double.valueOf(str));
        }
    }

    //---------//
    // Integer //
    //---------//
    /**
     * A subclass of Constant, meant to store an int value.
     */
    public static class Integer
            extends Constant
    {
        //~ Constructors -------------------------------------------------------

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
            super(
                    quantityUnit,
                    java.lang.Integer.toString(defaultValue),
                    description);
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Retrieve the current constant value
         *
         * @return the current (int) value
         */
        public int getValue ()
        {
            return (java.lang.Integer) getCachedValue();
        }

        /**
         * Allows to set a new int value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the int value as a string
         */
        @Override
        public void setValue (java.lang.String string)
        {
            setValue(java.lang.Integer.valueOf(string).intValue());
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new (int) value
         */
        public void setValue (int val)
        {
            setTuple(java.lang.Integer.toString(val), val);
        }

        @Override
        protected java.lang.Integer decode (java.lang.String str)
        {
            return new java.lang.Integer(str);
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
        //~ Constructors -------------------------------------------------------

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
            extends Constant
    {
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------
        /**
         * Retrieve the current constant value. 
         * Actually this is synonymous with currentString()
         *
         * @return the current (string) value
         */
        public java.lang.String getValue ()
        {
            return (java.lang.String) getCachedValue();
        }

        /**
         * Set a new string value to the constant
         *
         * @param val the new (string) value
         */
        @Override
        public void setValue (java.lang.String val)
        {
            setTuple(val, val);
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
     * Class used to handle the tuple [currentString + currentValue]
     * in an atomic way.
     */
    private static class Tuple
    {
        //~ Instance fields ----------------------------------------------------

        final java.lang.String currentString;

        final Object cachedValue;

        //~ Constructors -------------------------------------------------------
        public Tuple (java.lang.String currentString,
                      Object cachedValue)
        {
            /** Current string Value */
            this.currentString = currentString;

            /** Current cached Value (optimized) */
            this.cachedValue = cachedValue;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public java.lang.String toString ()
        {
            return currentString;
        }
    }
}
