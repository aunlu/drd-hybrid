/**
 * Copy of Cliff Click's high-scale-lib v.1.1.2.
 *
 * Written by Cliff Click and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 *
 * Used in DRD internals instead of corresponding JDK classes
 * Copied directly into DRD because agent works in same class loader as target app, therefore we have to instrument all
 * code, starting not from com.devexperts.drd
 *
 * TODO: find maven plugin that is able to make this copy on-the-fly from maven dependency on high-scale-lib
 *
 */
package com.devexperts.drd.agent.high_scale_lib;