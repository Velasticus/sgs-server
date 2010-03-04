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

package com.sun.sgs.tutorial.server.lesson5;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;

/**
 * Simple example of listening for user {@linkplain AppListener#loggedIn login}
 * in the Project Darkstar Server.
 * <p>
 * Logs each time a user logs in, then kicks them off immediately.
 */
public class HelloUser
    implements AppListener, // to get called during startup and login.
               Serializable // since all AppListeners are ManagedObjects.
{
    /** The version of the serialized form of this class. */
    private static final long serialVersionUID = 1L;

    /** The {@link Logger} for this class. */
    private static final Logger logger =
        Logger.getLogger(HelloUser.class.getName());

    //  implement AppListener

    /** {@inheritDoc} */
    public void initialize(Properties props) {
        // empty
    }

    /**
     * {@inheritDoc}
     * <p>
     * Logs a message each time a new session tries to login, then
     * kicks them out by returning {@code null}.
     */
    public ClientSessionListener loggedIn(ClientSession session) {
        // User has logged in
        logger.log(Level.INFO, "User {0} almost logged in", session.getName());

        // Kick the user out immediately by returning a null listener
        return null;
    }
}
