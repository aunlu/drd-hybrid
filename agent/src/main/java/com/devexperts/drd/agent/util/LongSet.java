/*
 * QDS - Quick Data Signalling Library
 * Copyright (C) 2002-2013 Devexperts LLC
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package com.devexperts.drd.agent.util;

import java.util.Set;

/**
 * This class extends {@link Set} with methods that are specific
 * for <code>long</code> values.
 */
public interface LongSet extends LongCollection, Set<Long> {}
