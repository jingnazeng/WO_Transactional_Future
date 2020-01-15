/*
 * Copyright 2012-2013 Gephi Consortium 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy of 
 * the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License. 
 */

package jvstm.graph;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;


public class GraphLock {
	/**
	 * @return the associateDAG
	 */
	public DAG getAssociateDAG() {
		return associateDAG;
	}

	/**
	 * @param associateDAG the associateDAG to set
	 */
	public void setAssociateDAG(DAG associateDAG) {
		this.associateDAG = associateDAG;
	}

	/**
	 * @return the rwLock
	 */
	public ReentrantReadWriteLock getRwLock() {
		return rwLock;
	}

	/**
	 * @param rwLock the rwLock to set
	 */
	public void setRwLock(ReentrantReadWriteLock rwLock) {
		this.rwLock = rwLock;
	}

	ReentrantReadWriteLock rwLock;
	ReadLock readLock;
	WriteLock writeLock;
	
	DAG associateDAG;
	
	public GraphLock(){
		rwLock = new ReentrantReadWriteLock();
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();		
	}
	
	public GraphLock(DAG dag){
		rwLock = new ReentrantReadWriteLock();
		readLock = rwLock.readLock();
		writeLock = rwLock.writeLock();		
		associateDAG = dag;
	}
	
	public void readAcquireLock(){
	
		readLock.lock();
	}
	
	public void readReleaselock(){
		readLock.unlock();
	}
	
	public void readUnlockAll() { 
        final int nReadLocks = rwLock.getReadHoldCount(); 
        for (int n = 0; n < nReadLocks; n++) { 
            readLock.unlock(); 
        } 
    } 
 
    public void writeLock() { 
        while (rwLock.getReadHoldCount() > 0 && !rwLock.isWriteLockedByCurrentThread()) { 
           // throw new IllegalMonitorStateException("Impossible to acquire a write lock when a read lock is being held"); 
        	Thread.yield();
        } 
        writeLock.lock(); 
    } 
 
    public void writeUnlock() { 
    	if(writeLock.isHeldByCurrentThread()){
    		writeLock.unlock(); 
    	}
//    	System.out.println("write unlock on DAG: "+ this + "by transaction: "+ Transaction.current().getTopLevelTrasanction());
    } 
 
    public void checkHoldWriteLock() { 
        if (!rwLock.isWriteLockedByCurrentThread()) { 
            throw new IllegalMonitorStateException("Impossible to perform a write operation without lock. Wrap your code with a write lock to solve this."); 
        } 
    } 
}
