package se.kth.ict.id2203.components.rb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.beb.BebBroadcast;
import se.kth.ict.id2203.ports.beb.BestEffortBroadcast;
import se.kth.ict.id2203.ports.rb.RbBroadcast;
import se.kth.ict.id2203.ports.rb.ReliableBroadcast;
import se.sics.kompics.*;
import se.sics.kompics.address.Address;

import java.util.HashSet;
import java.util.Set;

public class EagerRb extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(EagerRb.class);

	private final Positive<BestEffortBroadcast> beb = requires(BestEffortBroadcast.class);
	private final Negative<ReliableBroadcast> rb = provides(ReliableBroadcast.class);

	private final Address self;
	private Integer sequenceNumber;
	private final Set<RbDataMessage> delivered;

	public EagerRb(EagerRbInit init) {
		this.self = init.getSelfAddress();
		new HashSet<>(init.getAllAddresses());
		this.sequenceNumber = 0;
		this.delivered = new HashSet<>();

		Handler<Start> startHandler = new Handler<Start>() {

			@Override
			public void handle(Start event) {
				logger.info("EagerRB created");
			}
		};
		subscribe(startHandler, control);
		Handler<RbBroadcast> rbcastHandler = new Handler<RbBroadcast>() {

			@Override
			public void handle(RbBroadcast event) {
				sequenceNumber++;
				RbDataMessage msg = new RbDataMessage(self, event.getDeliverEvent(), sequenceNumber);
				trigger(new BebBroadcast(msg), beb);
			}

		};
		subscribe(rbcastHandler, rb);
		Handler<RbDataMessage> rbDelivery = new Handler<RbDataMessage>() {

			@Override
			public void handle(RbDataMessage event) {
				if (!delivered.contains(event)) {
					delivered.add(event);
					trigger(event.getData(), rb);
					trigger(new BebBroadcast(event), beb);
				}
			}
		};
		subscribe(rbDelivery, beb);
	}

}
