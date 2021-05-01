package se.kth.ict.id2203.components.epfd;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class HeartbeatRequestMessage extends Pp2pDeliver {
    private static final long serialVersionUID = 8532717952493185604L;
    private final Integer sequenceNumber;

    public HeartbeatRequestMessage(Address source, Integer sequenceNumber) {
        super(source);
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
}
