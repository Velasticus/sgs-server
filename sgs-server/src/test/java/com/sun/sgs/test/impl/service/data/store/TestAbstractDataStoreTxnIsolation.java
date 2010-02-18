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

package com.sun.sgs.test.impl.service.data.store;

import com.sun.sgs.service.store.DataStore;
import com.sun.sgs.test.util.InMemoryDataStore;
import com.sun.sgs.tools.test.FilteredNameRunner;
import org.junit.runner.RunWith;

/**
 * Tests the isolation that {@link AbstractDataStore} enforces between
 * transactions.
 */
@RunWith(FilteredNameRunner.class)
public class TestAbstractDataStoreTxnIsolation extends BasicTxnIsolationTest {

    /** Creates an {@link InMemoryDataStore}. */
    protected DataStore createDataStore() {
	return new InMemoryDataStore(props, env.systemRegistry, txnProxy);
    }
}
