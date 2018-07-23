/*
 * QDS - Quick Data Signalling Library
 * Copyright (C) 2002-2013 Devexperts LLC
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package com.devexperts.drd.agent.util;

public class QuickSort {
    private static void sort(long[] a, int l, int r) {
        // Quick sort large blocks.
        while (l + 5 < r) {
            int ll = l;
            int rr = r;
            long m = a[(ll + rr) >>> 1];
            do {
                while (ll <= rr && a[ll] < m)
                    ll++;
                while (ll <= rr && a[rr] > m)
                    rr--;
                if (ll > rr)
                    break;
                long tmp = a[ll];
                a[ll] = a[rr];
                a[rr] = tmp;
                ll++;
                rr--;
            } while (ll <= rr);
            // Do recursion into narrower diapason, then do wider diapason ourselves.
            if (rr - l < r - ll) {
                sort(a, l, rr);
                l = ll;
            } else {
                sort(a, ll, r);
                r = rr;
            }
        }
        // Bubble sort the remainder.
        for (int i = l; ++i <= r; ) {
            long tmp = a[i];
            for (int j = i; --j >= l && a[j] > tmp; ) {
                a[j + 1] = a[j];
                a[j] = tmp;
            }
        }
    }

    public static void sort(long[] a) {
        sort(a, 0, a.length - 1);
    }
}
