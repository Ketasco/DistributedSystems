package se.kth.ict.id2203.components.crb;

import se.kth.ict.id2203.ports.crb.CrbDeliver;
import se.kth.ict.id2203.ports.rb.RbDeliver;
import se.sics.kompics.address.Address;

public class CrbDataMessage extends RbDeliver {
    private static final long serialVersionUID = 5972998019486521099L;
    private final int[] vector;
    private final CrbDeliver data;

    public CrbDataMessage(Address source, CrbDeliver data, int[] vector) {
        super(source);
        this.vector = vector;
        this.data = data;
    }

    public int[] getVector() {
        return vector;
    }

    public CrbDeliver getData() {
        return data;
    }
}
