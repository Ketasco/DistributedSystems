package se.kth.ict.id2203.components.riwcm;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class ArAckMessage extends Pp2pDeliver {

	private static final long serialVersionUID = 3179581774819528826L;
	private final Integer rid;
	
	protected ArAckMessage(Address source, Integer rid) {
		super(source);
		this.rid = rid;
	}

	public Integer getRid() {
		return rid;
	}
}
