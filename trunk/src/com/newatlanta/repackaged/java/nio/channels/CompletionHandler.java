/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.newatlanta.repackaged.java.nio.channels;

/**
 * A handler for consuming the result of an asynchronous I/O operation.
 *
 * <p> The asynchronous channels defined in this package allow a completion
 * handler to be specified to consume the result of an asynchronous operation.
 * The {@link #completed completed} method is invoked when the I/O operation
 * completes successfully. The {@link #failed failed} method is invoked if the
 * I/O operations fails. The {@link #cancelled cancelled} method is invoked when
 * the I/O operation is cancelled by invoking the {@link
 * java.util.concurrent.Future#cancel cancel} method. The implementations of
 * these methods should complete in a timely manner so as to avoid keeping the
 * invoking thread from dispatching to other completion handlers.
 *
 * @param   <V>     The result type of the I/O operation
 * @param   <A>     The type of the object attached to the I/O operation
 *
 * @since 1.7
 */

public interface CompletionHandler<V,A> {

    /**
     * Invoked when an operation has completed.
     *
     * @param   result
     *          The result of the I/O operation.
     * @param   attachment
     *          The object attached to the I/O operation when it was initiated.
     */
    void completed(V result, A attachment);

    /**
     * Invoked when an operation fails.
     *
     * @param   exc
     *          The exception to indicate why the I/O operation failed
     * @param   attachment
     *          The object attached to the I/O operation when it was initiated.
     */
    void failed(Throwable exc, A attachment);

    /**
     * Invoked when an operation is cancelled by invoking the {@link
     * java.util.concurrent.Future#cancel cancel} method.
     *
     * @param   attachment
     *          The object attached to the I/O operation when it was initiated.
     */
    void cancelled(A attachment);
}