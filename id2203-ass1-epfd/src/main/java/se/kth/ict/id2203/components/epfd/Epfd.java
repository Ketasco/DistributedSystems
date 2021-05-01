package se.kth.ict.id2203.components.epfd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.epfd.EventuallyPerfectFailureDetector;
import se.kth.ict.id2203.ports.epfd.Restore;
import se.kth.ict.id2203.ports.epfd.Suspect;
import se.kth.ict.id2203.ports.pp2p.PerfectPointToPointLink;
import se.kth.ict.id2203.ports.pp2p.Pp2pSend;
import se.sics.kompics.*;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

import java.util.HashSet;
import java.util.Set;

public class Epfd extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(Epfd.class);

	private final long delta;
	private long delay;
	private Integer sequenceNumber;
	private final Set<Address> alive;
	private final Set<Address> suspected;
	private final Address self;
	private final Set<Address> nodes;

	private Positive<PerfectPointToPointLink> pp2p = requires(PerfectPointToPointLink.class);
	private final Positive<Timer> timer = requires(Timer.class);
	private Negative<EventuallyPerfectFailureDetector> epfd = provides(EventuallyPerfectFailureDetector.class);

	public Epfd(EpfdInit init) {
		delta = init.getDeltaDelay();
		delay = init.getInitialDelay();
		sequenceNumber = 0;
		alive = new HashSet<>(init.getAllAddresses());
		suspected = new HashSet<>();
		self = init.getSelfAddress();
		nodes = init.getAllAddresses();

		Handler<Start> handleStart = new Handler<Start>() {

			@Override
			public void handle(Start event) {
				logger.info("Epfd created");
				setTimer(delay);
			}
		};
		subscribe(handleStart, control);

		Handler<CheckTimeout> handleCheckTimeout = new Handler<CheckTimeout>() {

			@Override
			public void handle(CheckTimeout event) {
				for (Address node : alive) {
					if (suspected.contains(node)) {
						delay += delta;
						break;
					}
				}
				sequenceNumber++;

				for (Address node : nodes) {
					if ((!alive.contains(node)) && (!suspected.contains(node))) {
						suspected.add(node);
						Suspect suspectEvent = new Suspect(node);
						trigger(suspectEvent, epfd);
					} else if (alive.contains(node) && suspected.contains(node)) {
						suspected.remove(node);
						Restore restoreEvent = new Restore(node);
						trigger(restoreEvent, epfd);
					}

					HeartbeatRequestMessage hbRequestMsg = new HeartbeatRequestMessage(self, sequenceNumber);
					trigger(new Pp2pSend(node, hbRequestMsg), pp2p);
				}

				alive.clear();

				setTimer(delay);
			}
		};
		subscribe(handleCheckTimeout, timer);
		Handler<HeartbeatRequestMessage> handleHbRequests = new Handler<HeartbeatRequestMessage>() {

			@Override
			public void handle(HeartbeatRequestMessage event) {
				HeartbeatReplyMessage hbReplyMsg = new HeartbeatReplyMessage(
						self, event.getSequenceNumber());
				trigger(new Pp2pSend(event.getSource(), hbReplyMsg), pp2p);
			}
		};
		subscribe(handleHbRequests, pp2p);
		Handler<HeartbeatReplyMessage> handleHbReplies = new Handler<HeartbeatReplyMessage>() {

			@Override
			public void handle(HeartbeatReplyMessage event) {
				if (event.getSequenceNumber().equals(sequenceNumber)
						|| suspected.contains(event.getSource())) {
					alive.add(event.getSource());
				}
			}
		};
		subscribe(handleHbReplies, pp2p);
	}

	public Negative<EventuallyPerfectFailureDetector> getEpfd() {
		return epfd;
	}

	public void setEpfd(Negative<EventuallyPerfectFailureDetector> epfd) {
		this.epfd = epfd;
	}

	public void setTimer(long delay) {
		ScheduleTimeout timeout = new ScheduleTimeout(delay);
		timeout.setTimeoutEvent(new CheckTimeout(timeout));
		trigger(timeout, timer);
	}

	public Positive<PerfectPointToPointLink> getPp2p() {
		return pp2p;
	}

	public void setPp2p(Positive<PerfectPointToPointLink> pp2p) {
		this.pp2p = pp2p;
	}
}