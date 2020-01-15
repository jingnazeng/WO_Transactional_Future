package benchmark.vacation;

import jvstm.CommitException;
import jvstm.TransactionSignaller;
import jvstm.VBoxInt;

/* =============================================================================
 *
 * reservation.c
 * -- Representation of car, flight, and hotel relations
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 *
 * =============================================================================
 *
 * For the license of bayes/sort.h and bayes/sort.c, please see the header
 * of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of kmeans, please see kmeans/LICENSE.kmeans
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of ssca2, please see ssca2/COPYRIGHT
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of lib/mt19937ar.c and lib/mt19937ar.h, please see the
 * header of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of lib/rbtree.h and lib/rbtree.c, please see
 * lib/LEGALNOTICE.rbtree and lib/LICENSE.rbtree
 * 
 * ------------------------------------------------------------------------
 * 
 * Unless otherwise noted, the following license applies to STAMP files:
 * 
 * Copyright (c) 2007, Stanford University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 * 
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 * 
 *     * Neither the name of Stanford University nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY STANFORD UNIVERSITY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL STANFORD UNIVERSITY BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * =============================================================================
 */

public class Reservation implements Comparable<Reservation> {
    // VBoxes on these
    final VBoxInt id;
    final VBoxInt numUsed;
    final VBoxInt numFree;
    final VBoxInt numTotal;
    final VBoxInt price;

    public Reservation(int id, int numTotal, int price) {
	this.id = new VBoxInt(id);
	this.numUsed = new VBoxInt(0);
	this.numFree = new VBoxInt(numTotal);
	this.numTotal = new VBoxInt(numTotal);
	this.price = new VBoxInt(price);
	checkReservation();
    }

    public void checkReservation() {
	int numUsed = this.numUsed.get();
	if (numUsed < 0) {
//	    jvstm.util.Debug.print("COMMIT EXCEPTION - checkReservation numUsed < 0" + Thread.currentThread().getId());
//	    throw new CommitException();
	    TransactionSignaller.SIGNALLER.signalCommitFail();
	}

	int numFree = this.numFree.get();
	if (numFree < 0) {
//	    jvstm.util.Debug.print("COMMIT EXCEPTION - checkReservation numFree < 0" + Thread.currentThread().getId());
//	    throw new CommitException();
	    TransactionSignaller.SIGNALLER.signalCommitFail();
	}

	int numTotal = this.numTotal.get();
	if (numTotal < 0) {
//	    jvstm.util.Debug.print("COMMIT EXCEPTION - checkReservation numTotal < 0" + Thread.currentThread().getId());
//	    throw new CommitException();
	    TransactionSignaller.SIGNALLER.signalCommitFail();
	}

	if ((numUsed + numFree) != numTotal) {
//	    jvstm.util.Debug.print("COMMIT EXCEPTION - checkReservation does not match" + Thread.currentThread().getId());
//	    throw new CommitException();
	    TransactionSignaller.SIGNALLER.signalCommitFail();
	}

	int price = this.price.get();
	if (price < 0) {
//	    jvstm.util.Debug.print("COMMIT EXCEPTION - checkReservation price < 0 " + Thread.currentThread().getId());
//	    throw new CommitException();
	    TransactionSignaller.SIGNALLER.signalCommitFail();
	}
    }

    boolean reservation_addToTotal(int num) {
	if (numFree.get() + num < 0) {
	    return false;
	}

	numFree.put(numFree.get() + num);
	numTotal.put(numTotal.get() + num);
	checkReservation();
	return true;
    }

    /*
     * ==========================================================================
     * === reservation_make -- Returns TRUE on success, else FALSE
     * ==============
     * ===============================================================
     */
    public boolean reservation_make() {
	if (numFree.get() < 1) {
	    return false;
	}
	numUsed.put(numUsed.get() + 1);
	numFree.put(numFree.get() - 1);
	checkReservation();
	return true;
    }

    /*
     * ==========================================================================
     * === reservation_cancel -- Returns TRUE on success, else FALSE
     * ============
     * =================================================================
     */
    boolean reservation_cancel() {
	if (numUsed.get() < 1) {
	    return false;
	}
	numUsed.put(numUsed.get() - 1);
	numFree.put(numFree.get() + 1);
	checkReservation();
	return true;
    }

    /*
     * ==========================================================================
     * === reservation_updatePrice -- Failure if 'price' < 0 -- Returns TRUE on
     * success, else FALSE
     * ======================================================
     * =======================
     */
    boolean reservation_updatePrice(int newPrice) {
	if (newPrice < 0) {
	    return false;
	}

	this.price.put(newPrice);
	checkReservation();
	return true;
    }

    /*
     * ==========================================================================
     * === reservation_compare -- Returns -1 if A < B, 0 if A = B, 1 if A > B
     * ====
     * =========================================================================
     */
    int reservation_compare(Reservation aPtr, Reservation bPtr) {
	return aPtr.id.get() - bPtr.id.get();
    }

    /*
     * ==========================================================================
     * === reservation_hash
     * ======================================================
     * =======================
     */
    int reservation_hash() {
	return id.get();
    }

    @Override
    public int compareTo(Reservation arg0) {
	int myId = this.id.get();
	int hisId = arg0.id.get();
	if (myId < hisId) {
	    return -1;
	} else if (myId == hisId) {
	    return 0;
	} else {
	    return 1;
	}
    }

}
