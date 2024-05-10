/**
 * Package for handling all application logical constants in a common way.
 * <p>
 * Handling concerns their definition in hosting classes, their potential on-line modification, and
 * their persistency on disk.
 * <p>
 * <img src="doc-files/Constant.png" alt="Persistency of constants">
 * <p>
 * <a href="Constant.html">Constant</a> instances represent a logical application constant,
 * whose persistency is to be managed from one application run to the other.
 * <p>
 * <b>Definition</b> of a constant (and its subclasses) can be:
 * <ul>
 * <li>A Constant declaration enclosed in a <a href="ConstantSet.html">ConstantSet</a> instance.
 * This is by far the easiest way to define constants related to a specific application class.</li>
 * <li>A standalone Constant defined outside the scope of any ConstantSet.
 * This case is used when the name of the constant must be forged programmatically.</li>
 * </ul>
 * <p>
 * <b>Modification</b> of a Constant value is preferably performed through dedicated interfaces.
 * You can also directly edit the property files, but you do so at your own risks.
 * <br>
 * The <a href="UnitTreeTable.html">UnitTreeTable</a> is the GUI related to the
 * <a href="UnitManager.html">UnitManager</a> singleton which handles all the units (classes) in
 * which a ConstantSet is defined.
 * This GUI is launched from Tools | Options menu and handles the tree of these units.
 * <p>
 * <b>Persistency</b> of each constant is handled by the
 * <a href= "ConstantManager.html">ConstantManager</a> singleton, which uses the qualified name
 * of the constant as the key to retrieve a related property (if any) specified in either the
 * DEFAULT and/or the USER property files.
 * Please refer to the <a href= "ConstantManager.html">ConstantManager</a> documentation about
 * the details on how a constant value is determined.
 */
package org.audiveris.omr.constant;
