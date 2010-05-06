/*
 * Copyright 2007-2010 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * --
 */

package com.sun.sgs.impl.service.data;

import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.TransactionNotActiveException;
import com.sun.sgs.impl.util.WeakIdentityMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Stores information about managed references within a particular transaction.
 * This class is logically part of the ManagedReferenceImpl class.
 */
final class ReferenceTable {

    /**
     * A map whose keys are stale managed objects, if tracking stale objects.
     */
    private static final WeakIdentityMap<Object, Boolean>
	staleObjects = new WeakIdentityMap<Object, Boolean>();

    /** Maps object IDs to managed references. */
    private final SortedMap<Long, ManagedReferenceImpl<?>> oids =
	new TreeMap<Long, ManagedReferenceImpl<?>>();

    /**
     * Maps managed objects to managed references.  The objects are compared by
     * identity, not the equals method.
     */
    private final Map<ManagedObject, ManagedReferenceImpl<?>> objects =
	new IdentityHashMap<ManagedObject, ManagedReferenceImpl<?>>();

    /** Whether to track stale objects. */
    private final boolean trackStaleObjects;

    /**
     * Creates an instance of this class.
     *
     * @param	trackStaleObjects whether to track stale objects
     */
    ReferenceTable(boolean trackStaleObjects) {
	this.trackStaleObjects = trackStaleObjects;
    }

    /**
     * Finds the managed reference associated with a managed object, returning
     * null if no reference is found.
     */
    ManagedReferenceImpl<?> find(ManagedObject object) {
	assert object != null : "Object is null";
        ManagedReferenceImpl<?> result = objects.get(object);
	if (result == null &&
	    trackStaleObjects &&
	    staleObjects.containsKey(object))
	{
	    throw new TransactionNotActiveException(
		"Attempt to access an object of type " +
		DataServiceImpl.typeName(object) +
		" whose transaction is no longer active");
	}
	return result;
    }

    /**
     * Finds the managed reference associated with an object ID, returning null
     * if no reference is found.
     */
    ManagedReferenceImpl<?> find(long oid) {
	assert oid >= 0 : "Object ID is negative";
	return oids.get(oid);
    }

    /** Adds a new managed reference to this table. */
    void add(ManagedReferenceImpl<?> ref) {
	assert !oids.containsKey(ref.oid)
	    : "Found existing reference for oid:" + ref.oid;
	oids.put(ref.oid, ref);
	ManagedObject object = ref.getObject();
	if (object != null) {
	    assert !objects.containsKey(object)
		: "Found existing reference for object with oid:" + ref.oid;
	    objects.put(object, ref);
	}
    }

    /**
     * Updates this table for a reference that has been newly associated with
     * an object.
     */
    void registerObject(ManagedReferenceImpl<?> ref) {
	assert oids.get(ref.oid) == ref
	    : "Found duplicate references for oid: " + ref.oid;
	assert ref.getObject() != null : "Object is null for oid:" + ref.oid;
	assert !objects.containsKey(ref.getObject())
	    : "Found existing reference for object with oid: " + ref.oid;
	objects.put(ref.getObject(), ref);
    }

    /**
     * Updates this table for a reference that is no longer associated with an
     * object.
     */
    void unregisterObject(ManagedObject object) {
	assert objects.containsKey(object) : "Object was not found";
	objects.remove(object);
	if (trackStaleObjects) {
	    staleObjects.put(object, Boolean.TRUE);
	}
    }

    /** Removes a managed reference from this table. */
    void remove(ManagedReferenceImpl<?> ref) {
	Object existing = oids.remove(ref.oid);
	assert existing == ref
	    : "Found duplicate reference for oid:" + ref.oid;
	ManagedObject object = ref.getObject();
	if (object != null) {
	    existing = objects.remove(object);
	    assert existing == ref
		: "Found duplicate reference for oid:" + ref.oid;
	}
    }

    /**
     * Returns the next object ID in the reference table of a newly created
     * object, or -1 if none is found.  Does not return IDs for removed
     * objects.  Specifying -1 requests the first ID.
     */
    long nextNewObjectId(long oid) {
	for (Entry<Long, ManagedReferenceImpl<?>> entry :
		 oids.tailMap(oid).entrySet())
	{
	    long key = entry.getKey();
	    if (key > oid && entry.getValue().isNew()) {
		return key;
	    }
	}
	return -1;
    }

    /**
     * Flushes all references.  Returns information about any objects found to
     * be modified, or null if none were modified.
     */
    FlushInfo flushModifiedObjects() {
	FlushInfo flushInfo = null;
	for (ManagedReferenceImpl<?> ref : oids.values()) {
	    byte[] data = ref.flush();
	    if (data != null) {
		if (flushInfo == null) {
		    flushInfo = new FlushInfo();
		}
		flushInfo.add(ref.oid, data);
	    }
	}
	return flushInfo;
    }

    /**
     * Checks the consistency of this table, throwing an assertion error if a
     * problem is found.
     */
    void checkAllState() {
	int objectCount = 0;
	for (Entry<Long, ManagedReferenceImpl<?>> entry : oids.entrySet()) {
	    long oid = entry.getKey();
	    ManagedReferenceImpl<?> ref = entry.getValue();
	    ref.checkState();
	    if (oid != ref.oid) {
		throw new AssertionError(
		    "Wrong oids entry: oid = " + oid + ", ref.oid = " +
		    ref.oid);
	    }
	    Object object = ref.getObject();
	    if (object != null) {
		ManagedReferenceImpl<?> objectsRef = objects.get(object);
		if (objectsRef == null) {
		    throw new AssertionError(
			"Missing objects entry for oid = " + ref.oid);
		} else if (!ref.equals(objectsRef)) {
		    throw new AssertionError(
			"Wrong objects entry for oid = " + ref.oid);
		}
		objectCount++;
	    }
	}
	if (objectCount != objects.size()) {
	    throw new AssertionError(
		"Objects table has wrong size: was " + objects.size() +
		", expected " + objectCount);
	}
    }
}
