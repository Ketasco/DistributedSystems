package se.kth.ict.id2203.components.riwcm;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class ArDataMessage extends Pp2pDeliver {

	private static final long serialVersionUID = 7421676511929598584L;
	private final Integer ts, rid, wr, val;

	protected ArDataMessage(Address source, Integer rid, Integer ts, Integer wr, Integer val) {
		super(source);
		this.rid = rid;
		this.ts = ts;
		this.wr = wr;
		this.val = val;
	}

	public Integer getRid() {
		return rid;
	}
	public Integer getTs() {
		return ts;
	}
	public Integer getWr() {
		return wr;
	}
	public Integer getVal() {
		return val;
	}
}
