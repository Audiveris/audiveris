//----------------------------------------------------------------------------//
//                                                                            //
//                              C o n s t a n t                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.constant;

import omr.util.Logger;

/**
 * This abstract class handles the mapping between one application variable and
 * a property name and value. It is meant essentially to handle any kind of
 * symbolic constant, whose value may have to be tuned and saved for future runs
 * of the application.
 *
 * <p>Please refer to {@link ConstantManager} for a detailed explanation on how
 * the current value of any given Constant is determined at run-time.
 *
 * <p> The class <code>Constant</code> is not meant to be used directly (it is
 * abstract), but rather through any of its subclasses :
 *
 * <ul> <li> {@link Constant.Boolean} </li> <li> {@link Constant.Color}
 * </li> <li> {@link Constant.Double} </li> <li> {@link Constant.Integer}
 * </li> <li> {@link Constant.String} </li> </ul> </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Constant
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Constant.class);

    //~ Instance fields --------------------------------------------------------

    /** Default value to be used if needed */
    private final java.lang.String defaultString;

    /** Semantic */
    private final java.lang.String description;

    /** Initial Value (for reset) */
    private java.lang.String initialString;

    /** Constant name */
    private java.lang.String name;

    /** Fully qualified Constant name */
    private java.lang.String qualifiedName;

    /** Unit (if relevant) of the quantity measured */
    private java.lang.String quantityUnit;

    /** Requiring unit (module) */
    private java.lang.String unit;

    /** Current Value */
    private java.lang.String currentString;

    /** Current Value (optimized) */
    private Object cachedValue;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Constant //
    //----------//
    /**
     * Creates a constant instance, while providing a default value, in case the
     * external property is not yet defined.
     *
     * @param quantityUnit  Unit used as base for measure, if relevant
     * @param defaultString Default value, expressed in a string literal
     * @param description A quick description of the purpose of this constant
     */
    protected Constant (java.lang.String quantityUnit,
                        java.lang.String defaultString,
                        java.lang.String description)
    {
        this.quantityUnit = quantityUnit;
        this.description = description;
        this.defaultString = defaultString;

        // Pure debugging (which cannot use Debug ...)
        //         System.out.println("new Constant" +
        //                            " quantityUnit=" + quantityUnit +
        //                            " defaultString=" + defaultString +
        //                            " description=" + description);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setValue //
    //----------//
    /**
     * Allows to modify the current value of the constant; This abstract method
     * is actually defined in each subclass, to enforce validation of the
     * provided string with respect to the target constant type.
     *
     * @param string the new value, as a string to be checked
     */
    public abstract void setValue (java.lang.String string);

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
     * Report the last part of the type name of the constant
     *
     * @return the type name (last part)
     */
    public java.lang.String getShortTypeName ()
    {
        final java.lang.String typeName = getClass()
                                              .getName();
        final int              separator = Math.max(
            typeName.lastIndexOf('$'),
            typeName.lastIndexOf('.'));

        if (separator != -1) {
            return typeName.substring(separator + 1);
        } else {
            return typeName;
        }
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
        if (currentString == null) {
            return false;
        } else {
            return !currentString.equals(initialString);
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove a given constant from memory
     */
    public void remove ()
    {
        // Remove the underlying property
        ConstantManager.removeProperty(qualifiedName);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Forget any modification eventually made, and reset to the initial value.
     */
    public void reset ()
    {
        setString(initialString);
    }

    //-----------//
    // toBoolean //
    //-----------//
    /**
     * Gets the current value, as a boolean type.
     *
     * @return the boolean view of the value
     */
    public boolean toBoolean ()
    {
        if (cachedValue == null) {
            cachedValue = java.lang.Boolean.valueOf(currentString());
        }

        return ((java.lang.Boolean) cachedValue).booleanValue();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Used by ConstantTreeTable to display the name of the constant, so only
     * the unqualified name is returned
     *
     * @return the (unqualified) constant name
     */
    @Override
    public java.lang.String toString ()
    {
        return name;
    }

    //-----------//
    // setString //
    //-----------//
    /**
     * Modifies the current parameter value, with no validity check of the
     * string
     *
     * @param val The new value (as a string)
     */
    protected void setString (java.lang.String val)
    {
        checkInitialized();

        currentString = val;
        cachedValue = null; // To force update

        // Update the underlying property
        if (qualifiedName != null) {
            ConstantManager.setProperty(qualifiedName, val);
        } else {
            System.out.println(
                "*** Constant " + this + " Cannot setString " + val);
        }
    }

    //--------//
    // toByte //
    //--------//
    /**
     * Gets the current value, as a byte type.
     *
     * @return the byte view of the value
     */
    protected byte toByte ()
    {
        if (cachedValue == null) {
            cachedValue = new Byte(currentString());
        }

        return ((Byte) cachedValue).byteValue();
    }

    //--------//
    // toChar //
    //--------//
    /**
     * Gets the current value, as a character type.
     *
     * @return the character view of the value
     */
    protected char toChar ()
    {
        if (cachedValue == null) {
            cachedValue = new Character(currentString().charAt(0));
        }

        return ((Character) cachedValue).charValue();
    }

    //---------//
    // toColor //
    //---------//
    /**
     * Gets the current value, as a Color type.
     *
     * @return the Color view of the value, which can be null
     */
    protected java.awt.Color toColor ()
    {
        if (cachedValue == null) {
            java.lang.String str = currentString();

            if (str != null) {
                cachedValue = java.awt.Color.decode(str);
            }
        }

        return (java.awt.Color) cachedValue;
    }

    //----------//
    // toDouble //
    //----------//
    /**
     * Gets the current value, as a double type.
     *
     * @return the double view of the value
     */
    protected double toDouble ()
    {
        if (cachedValue == null) {
            cachedValue = new java.lang.Double(currentString());
        }

        return ((java.lang.Double) cachedValue).doubleValue();
    }

    //---------//
    // toFloat //
    //---------//
    /**
     * Gets the current value, as a float type.
     *
     * @return the float view of the value
     */
    protected float toFloat ()
    {
        if (cachedValue == null) {
            cachedValue = new Float(currentString());
        }

        return ((Float) cachedValue).floatValue();
    }

    //-------//
    // toInt //
    //-------//
    /**
     * Gets the current value, as a int type.
     *
     * @return the int view of the value
     */
    protected int toInt ()
    {
        if (cachedValue == null) {
            cachedValue = new java.lang.Integer(currentString());
        }

        return ((java.lang.Integer) cachedValue).intValue();
    }

    //--------//
    // toLong //
    //--------//
    /**
     * Gets the current value, as a long type.
     *
     * @return the long view of the value
     */
    protected long toLong ()
    {
        if (cachedValue == null) {
            cachedValue = new Long(currentString());
        }

        return ((Long) cachedValue).longValue();
    }

    //---------//
    // toShort //
    //---------//
    /**
     * Gets the current value, as a short type.
     *
     * @return the short view of the value
     */
    protected short toShort ()
    {
        if (cachedValue == null) {
            cachedValue = new Short(currentString());
        }

        return ((Short) cachedValue).shortValue();
    }

    //---------------//
    // currentString //
    //---------------//
    /**
     * Gets the current value, as a String type. This is package private.
     *
     * @return the String view of the value
     */
    java.lang.String currentString ()
    {
        if (currentString == null) {
            checkInitialized();
            currentString = ConstantManager.getProperty(qualifiedName);

            if (currentString == null) { // Not defined by property files

                if (defaultString != null) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Property " + qualifiedName + " = " +
                            currentString + " -> " + defaultString);
                    }

                    currentString = defaultString; // Use default
                } else {
                    logger.fine("No value found for Property " + qualifiedName);
                }
            } else if (logger.isFineEnabled()) {
                logger.fine(
                    "Property " + qualifiedName + " = " + currentString);
            }

            // Save this initial value string
            initialString = currentString;
        }

        return currentString;
    }

    //---------//
    // setName //
    //---------//
    /**
     * (package access) Allows to record the name of the constant
     *
     * @param name the constant name
     */
    void setName (java.lang.String name)
    {
        this.name = name;

        if (unit != null) {
            this.qualifiedName = unit + "." + name;
        } else {
            this.qualifiedName = name;
        }
    }

    //---------//
    // setUnit //
    //---------//
    /**
     * (package access) Allows to record the containing unit of the constant
     *
     * @param unit the unit (class name) this constant belongs to
     */
    void setUnit (java.lang.String unit)
    {
        this.unit = unit;

        if (name != null) {
            name = unit + "." + name;
        }
    }

    //------------------//
    // checkInitialized //
    //------------------//
    private final void checkInitialized ()
    {
        // Make sure everything is initialized properly
        if (name == null) {
            UnitManager.getInstance()
                       .checkDirtySets();
        }
    }

    //----------------//
    // getCachedValue //
    //----------------//
    private Object getCachedValue ()
    {
        return cachedValue;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // Angle //
    //-------//
    /**
     * A subclass of Double, meant to store a angle (in radians).
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
        public Angle (double           defaultValue,
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
        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (boolean) default value
         * @param description  the semantic of the constant
         */
        public Boolean (boolean          defaultValue,
                        java.lang.String description)
        {
            super(null, java.lang.Boolean.toString(defaultValue), description);
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current (boolean) value
         */
        public boolean getValue ()
        {
            return toBoolean();
        }

        /**
         * Allows to set a new boolean value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the boolean value as a string
         */
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
            setString(java.lang.Boolean.toString(val));
        }
    }

    //-------//
    // Color //
    //-------//
    /**
     * A subclass of Constant, meant to store a {@link java.awt.Color} value.
     */
    public static class Color
        extends Constant
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
            setUnit(unit);
            setName(name);
        }

        /**
         * Normal constructor, with a String type for default value
         *
         * @param defaultValue the default (String) RGB value
         * @param description  the semantic of the constant
         */
        public Color (java.lang.String defaultValue,
                      java.lang.String description)
        {
            super(null, defaultValue, description);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (int) RGB default value
         * @param description  the semantic of the constant
         */
        public Color (int              defaultValue,
                      java.lang.String description)
        {
            this(java.lang.Integer.toString(defaultValue), description);
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current (Color) value
         */
        public java.awt.Color getValue ()
        {
            return toColor();
        }

        /**
         * Allows to set a new int RGB value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the int value as a string
         */
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
            // TBD : find a more readable format such as #RRGGBB
            //setString(java.lang.Integer.toString(val.getRGB(), 16));
            setString(java.lang.Integer.toString(val.getRGB()));
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
        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Double (java.lang.String quantityUnit,
                       double           defaultValue,
                       java.lang.String description)
        {
            super(
                quantityUnit,
                java.lang.Double.toString(defaultValue),
                description);
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current (double) value
         */
        public double getValue ()
        {
            return toDouble();
        }

        /**
         * Allows to set a new double value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the double value as a string
         */
        public void setValue (java.lang.String string)
        {
            setValue(java.lang.Double.valueOf(string).doubleValue());
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new (double) value
         */
        public void setValue (double val)
        {
            setString(java.lang.Double.toString(val));
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
        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (int) default value
         * @param description  the semantic of the constant
         */
        public Integer (java.lang.String quantityUnit,
                        int              defaultValue,
                        java.lang.String description)
        {
            super(
                quantityUnit,
                java.lang.Integer.toString(defaultValue),
                description);
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current (int) value
         */
        public int getValue ()
        {
            return toInt();
        }

        /**
         * Allows to set a new int value (passed as a string) to this
         * constant. The string validity is actually checked.
         *
         * @param string the int value as a string
         */
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
            setString(java.lang.Integer.toString(val));
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
        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Ratio (double           defaultValue,
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
            setUnit(unit);
            setName(name);
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

        /**
         * Retrieve the current constant value. Actually this is synonymous with
         * currentString()
         *
         * @return the current (string) value
         */
        public java.lang.String getValue ()
        {
            return currentString();
        }

        /**
         * Set a new string value to the constant
         *
         * @param val the new (string) value
         */
        public void setValue (java.lang.String val)
        {
            setString(val);
        }
    }
}
