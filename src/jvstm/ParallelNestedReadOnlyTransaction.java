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

import contlib.Continuation;
import jvstm.util.Cons;

/**
 * Parallel Nested Transaction that may only abort when reading a VBoxBody. This
 * means that if it only reads RAWs, then it necessarily commits.
 *
 * It is not as efficient as a Top Level RO Tx because it still has to maintain
 * the read-set of globally consolidated entries that were read, for the nested
 * RW ancestor to validate on its commit.
 *
 * @author nmld
 *
 */

public class ParallelNestedReadOnlyTransaction extends ParallelNestedTransaction {

    public ParallelNestedReadOnlyTransaction(ReadWriteTransaction parent, int sequentialVersion, Continuation c) {
        super(parent, sequentialVersion, c);
    }

    @Override
    public Transaction makeParallelNestedTransaction(boolean readOnly) {
        if (!readOnly) {
            throw new WriteOnReadException();
        }
        return new ParallelNestedReadOnlyTransaction(this,this.sequentialVersion+2, nextSpeculationCheckpoint);
    }
    
    //added
    @Override
    public Transaction makeParallelNestedTransaction(boolean readOnly,int seqNumber) {
        if (!readOnly) {
            throw new WriteOnReadException();
        } else {
            return new ParallelNestedReadOnlyTransaction(this, seqNumber,nextSpeculationCheckpoint);
        }
    }

//    @Override
//    protected void tryCommit() {
//        ReadWriteTransaction parent = getRWParent();
//        Cons<ParallelNestedTransaction> currentOrecs;
//        Cons<ParallelNestedTransaction> modifiedOrecs;
//
//        do {
//            try{
//            	snapshotValidation(-1);
//
//            }catch(CommitException e){
//            	manualAbort();
//            	getTopLevelParent().incFuturesAborts();
//            	if(speculationCheckpoint != null){
////            		incAbortedContinuations();
//                	this.abort();
//            		Continuation.resume(speculationCheckpoint);
//            	}
////            	incAbortedFutures();
//            	throw e;
//            } 
//        	
//            currentOrecs = parent.mergedTxs;
//            modifiedOrecs = currentOrecs.cons(this);
//            for (ParallelNestedTransaction child : mergedTxs) {
//                modifiedOrecs = modifiedOrecs.cons(child);
//            }
//        } while (!parent.CASmergedTxs(currentOrecs, modifiedOrecs));
//
//        if (!this.arraysRead.isEmpty()) {
//            synchronized (parent) {
//                parent.arraysRead = this.arraysRead.reverseInto(parent.arraysRead);
//            }
//        }
//    }
    
    @Override
    protected void tryCommit() {
        ReadWriteTransaction parent = getRWParent();
        Cons<ParallelNestedTransaction> currentOrecs;
        Cons<ParallelNestedTransaction> modifiedOrecs;

        do {
            currentOrecs = parent.mergedTxs;
            modifiedOrecs = currentOrecs.cons(this);
            for (ParallelNestedTransaction child : mergedTxs) {
                modifiedOrecs = modifiedOrecs.cons(child);
            }
        } while (!parent.CASmergedTxs(currentOrecs, modifiedOrecs));

        if (!this.arraysRead.isEmpty()) {
            synchronized (parent) {
                parent.arraysRead = this.arraysRead.reverseInto(parent.arraysRead);
            }
        }
    }

    @Override
    public <T> T getBoxValue(VBox<T> vbox) {
        InplaceWrite<T> inplaceWrite = vbox.inplace;
        T value = inplaceWrite.tempValue;
        OwnershipRecord inplaceOrec = inplaceWrite.orec;

        if (inplaceOrec.version > 0 && inplaceOrec.version <= number) {
            value = readGlobal(vbox);
            return value;
        }

        do {
            int entryNestedVersion = inplaceOrec.txTreeVersion;
            int versionOnAnc = retrieveAncestorVersion(inplaceOrec.owner);
            if (versionOnAnc >= 0 && entryNestedVersion <= versionOnAnc) {
                return (value == NULL_VALUE) ? null : value;
            }
            inplaceWrite = inplaceWrite.next;
            if (inplaceWrite == null) {
                break;
            }
            value = inplaceWrite.tempValue;
            inplaceOrec = inplaceWrite.orec;
        } while (true);

        if (boxesWritten != EMPTY_MAP) {
            value = (T) boxesWritten.get(vbox);
            if (value != null) {
                return (value == NULL_VALUE) ? null : value;
            }
        }

        value = readGlobal(vbox);
        return value;

    }

    @Override
    protected <T> T getLocalArrayValue(VArrayEntry<T> entry) {
        ReadWriteTransaction iter = getRWParent();
        while (iter != null) {
            if (iter.arrayWrites != EMPTY_MAP) {
                VArrayEntry<T> wsEntry = (VArrayEntry<T>) iter.arrayWrites.get(entry);
                if (wsEntry == null) {
                    iter = iter.getRWParent();
                    continue;
                }

                if (wsEntry.nestedVersion <= retrieveAncestorVersion(iter)) {
                    return (wsEntry.getWriteValue() == null ? (T) NULL_VALUE : wsEntry.getWriteValue());
                }
            }
            iter = iter.getRWParent();
        }

        return null;
    }

    @Override
    public <T> void setArrayValue(jvstm.VArrayEntry<T> entry, T value) {
        throw new WriteOnReadException();
    }

    @Override
    public <T> void setBoxValue(jvstm.VBox<T> vbox, T value) {
        throw new WriteOnReadException();
    }

    @Override
    protected <T> T getPerTxValue(PerTxBox<T> box) {
        throw new RuntimeException("Not implemented for NestedReadOnlyTransactions");
    }

    @Override
    public <T> void setPerTxValue(jvstm.PerTxBox<T> box, T value) {
        throw new WriteOnReadException();
    }

    @Override
    public boolean isWriteTransaction() {
        return false;
    }

}
