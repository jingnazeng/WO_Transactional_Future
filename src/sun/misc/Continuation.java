/*
 * Copyright 2010 Google, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.misc;

// Note that this is not needed (not even used if not on the bootstrap classpath) for vm2013

/**
 * The JVM Continuation class. The API design is still in progress.
 *
 * @author Hiroshi Yamauchi
 */
public final class Continuation {

    private static native void registerNatives();
    static {
        registerNatives();
    }

    /**
     * The stack frames data
     */
    public volatile Object stack;

    /**
     * The list of compiled code PCs in the stack. Needed to reclaim
     * the compiled code in the code cache.
     */
    protected volatile long[] pcs;

    /**
     * A data field for convenience. This field is set to the second
     * <code>data</code> parameter to {@link #enter} upon a {@link
     * #save} call.
     *
     * <p>For example, this field can be used to pass some data from
     * a scope entry point (a {@link #enter} call site) to the
     * continuation resume point.
     */
    protected volatile Object data1;

    /**
     * A simple data field for convenience. For example, this field
     * can be used to pass some data from the continuation save
     * point to the continuation resume point.
     */
    protected volatile Object data2;   // the user-defined data

    public Object data1() { synchronized(this) { return data1; } }
    public Object data2() { synchronized(this) { return data2; } }
    public void set_data1(Object o) { synchronized(this) { data1 = o; } }
    public void set_data2(Object o) { synchronized(this) { data2 = o; } }
    public boolean isSaved() { return stack != null; }

    /**
     * The continuation may save the compiled stack frames. The
     * reference count of the compiled code (nmethod) is incremented
     * upon a continuation save and decremented by this finalizer.
     */
    @Override
    protected void finalize() throws Throwable {
        if (pcs == null || pcs.length == 0) {
            return;
        }
        for (long pc : pcs) {
            dec_code_cache_ref_count(pc);
        }
    }

    /**
     * Copies the stack frames in the current scope, and stores them
     * in this object.  This method must be called in an enclosing
     * scope. Calling this method causes the stack frames in the
     * scope to suspend (including the current frame) and the enter
     * call at the entry of the current scope to return.
     *
     * @return the parameter passed to the resume call when the saved stack
     *         frames are resumed in the future.
     */
    public Object save() {
        return save_cont(this);
    }

    /**
     * Reactivates the stack frames saved in this object on the
     * current thread.  Overwrites the stack frames in the current
     * scope with the saved stack frames.  This method must be
     * called in an enclosing scope. Calling this method causes the
     * suspended save call to resume from the point where it was
     * suspended.
     *
     * @param rv the value to be returned from the resumed save call site.
     */
    public void resume(Object rv) {
        if (stack == null) {
            throw new IllegalArgumentException(
                "Continuation hasn't been saved or tried to resume for a second time.");
        }
        Object s = stack;
        //stack = null; // resumable only once
        resume_cont(s, rv);
    }

    /**
     * Marks the beginning of a new 'scope' in preparation for stack
     * save/resume.  Executes the given Runnable.
     *
     * @param data any user defined data to be passed from this call
     *             site to the point where {@link #resume} is called
     *             for convenience. The {@link #data1} field will be
     *             set to this object.
     * @return the Continuation object after the scope was saved
     *         into a Continuation object or null if it wasn't and
     *         simply returned
     */
    public static Object enter(Runnable r, Object data) {
        Object rv = enter0(r, data);
        return rv;
    }

    /*
     * This method currently exists just for convenience for the
     * continuation implementation in the JVM. This method along with
     * enter() above will never be jitted. This may go away in the
     * future.
     */
    private static Object enter0(Runnable r, Object data) {
        Object rv = enter1(r, data);
        return rv;
    }

    /*
     * This method currently exists just for convenience for the
     * continuation implementation in the JVM. This may go away in the
     * future.
     */
    private static Object enter1(Runnable r, Object data) {
        r.run();
        return null; // If saved, this will return the CSE.
    }

    private static native Object save_cont(Continuation cont);
    private static native void resume_cont(Object stack, Object rv);
    private static native void dec_code_cache_ref_count(long pc);
}
