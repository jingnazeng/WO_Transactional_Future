package jvstm;

import static jvstm.UtilUnsafe.UNSAFE;

public class WriteLockOnVBox {
	
	protected static final WriteLockOnVBox DEFAULT_WRITE_LOCK_ON_VBOX = new WriteLockOnVBox(null,0);
	
    public static class Offsets {

    	public static final long ownerOffset = UtilUnsafe.objectFieldOffset(WriteLockOnVBox.class, "owner");
    	public static final long counterOffset = UtilUnsafe.objectFieldOffset(WriteLockOnVBox.class, "counter");
    }
	
	/**
	 * @return the counter
	 */
	public int getCounter() {
		return counter;
	}
	/**
	 * @param counter the counter to set
	 */
	public void setCounter(int counter) {
		this.counter = counter;
	}
	/**
	 * @return the owner_top_level_transaction_id
	 */
	public ReadWriteTransaction getOwner_top_level_transaction_id() {
		return owner;
	}
	/**
	 * @param owner_top_level_transaction_id the owner_top_level_transaction_id to set
	 */
	public void setOwner_top_level_transaction_id(
			ReadWriteTransaction owner_top_level_transaction_id) {
		this.owner = owner_top_level_transaction_id;
	}
	
	ReadWriteTransaction owner;
	//how many subtransactions(belonging to the same top-level transaction) write to this VBox
	int counter; 	
	
	public WriteLockOnVBox(ReadWriteTransaction owner, int counter){
		this.owner = owner;
		this.counter = counter;
	}
	  
}
