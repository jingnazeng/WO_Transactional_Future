/*
 * JVSTM: a Java library for Software Transactional Memory
 * Copyright (C) 2005 INESC-ID Software Engineering Group
 * http://www.esw.inesc-id.pt
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Author's contact:
 * INESC-ID Software Engineering Group
 * Rua Alves Redol 9
 * 1000 - 029 Lisboa
 * Portugal
 */
package jvstm;

import jvstm.Transaction;

import java.util.concurrent.Callable;


public class ParallelTask<T> extends TransactionalTask<T> {

	public ParallelTask(){}

	public ParallelTask(Callable<T> c) {
		super();
		asyncMethod = c;
	}

	@Override
	public T execute() throws Throwable { T n = null; return n; }

	@Override
	public T call() throws Exception {
		T result =null;
		try {
			// parent may be null if this is a "top-level" task
			if (super.parent != null) {
				super.parent.start();
			}
			while(true) {
				Transaction tx;
				if (super.parent != null) {
					// we force the creation of a parallel nested, otherwise it would be a linear nested
					tx = Transaction.beginParallelNested(isReadOnly(), seqNumber);
					//added
					tx.setAsyncMethod(asyncMethod);
				} else {
					tx = Transaction.begin(isReadOnly());
				}
				try {
					//long startInFuture = System.currentTimeMillis();
					result = asyncMethod.call();
					//System.out.println("******************************************************");
					//System.out.println("duration for one future" + (System.currentTimeMillis()-startInFuture));
					//System.out.println("******************************************************");
					tx.commitTx(true); //should not call commit otherwise it will commit the parent
					parent.seqClock = this.seqNumber;
					tx = null;
					break;
				}
				catch(ExecuteParallelNestedTxSequentiallyException e){
					//"inter-tx W-W conflict"
					tx.abort();
					parent.seqClock = this.seqNumber;
					tx = null;
					return null;//this is pretty dirty....
				}
				catch(EarlyAbortException eae) {
					//versao mais recente, committed por uma top-level tx
					tx.abort();
					parent.seqClock = this.seqNumber;
					tx = null;
					if (super.parent != null) {
						throw eae;
					}
				} 
				catch (CommitException ce) {
					//conflito sequencial
					tx.abort();
					tx = null;
				}
			}
		}
		catch(EarlyAbortException eae) {
			throw eae;
		}
		catch (Throwable t) {
			t.printStackTrace();

			if (t instanceof Exception) {
				throw (Exception) t;
			} else {
				throw new RuntimeException(t);
			}
		}
		return result;
	}

}
