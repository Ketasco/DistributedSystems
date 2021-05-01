package se.kth.ict.id2203.components.riwcm;

import se.kth.ict.id2203.ports.beb.BebDeliver;
import se.sics.kompics.address.Address;

public class ReadBebDataMessage extends BebDeliver {

	private static final long serialVersionUID = -8839794388294522572L;
	private final Integer rid;
	
	public ReadBebDataMessage(Address source, Integer rid) {
		super(source);
		this.rid = rid;
	}

	public Integer getRid() {
		return rid;
	}

}
