package jvstm;

public class ReadOnlySubTxn extends ReadTransaction {

	public ReadOnlySubTxn(Transaction parent) {
		super(parent);
	}

}
