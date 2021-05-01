package se.kth.ict.id2203.components.riwcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.ar.*;
import se.kth.ict.id2203.ports.beb.BebBroadcast;
import se.kth.ict.id2203.ports.beb.BestEffortBroadcast;
import se.kth.ict.id2203.ports.pp2p.PerfectPointToPointLink;
import se.kth.ict.id2203.ports.pp2p.Pp2pSend;
import se.sics.kompics.*;
import se.sics.kompics.address.Address;

import java.util.*;

public class ReadImposeWriteConsultMajority extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(ReadImposeWriteConsultMajority.class);

	private final Positive<BestEffortBroadcast> beb = requires(BestEffortBroadcast.class);
	private final Positive<PerfectPointToPointLink> pp2p = requires(PerfectPointToPointLink.class);
	private final Negative<AtomicRegister> ar = provides(AtomicRegister.class);

	private final Address self;
	private final Integer numberOfNodes;
	private final List<ReadObject> readList;
	private Integer rid;
	private Integer requests;
	private Integer wr, val, writeVal, readVal;
	private Integer ts;
	private Boolean reading;
	private Integer rr;
	private Integer maxTs;

	public ReadImposeWriteConsultMajority(ReadImposeWriteConsultMajorityInit event) {
		this.self = event.getSelfAddress();
		Set<Address> nodes = new HashSet<>(event.getAllAddresses());
		this.readList = new LinkedList<>();
		this.numberOfNodes = nodes.size();
		this.rid = 0;
		this.requests = 0;
		this.reading = false;
		this.ts = 0;
		this.wr = 0;
		this.val = 0;
		this.writeVal = null;
		this.readVal = null;
		this.rr = 0;
		this.maxTs = -1;

		Handler<Start> startHandler = new Handler<Start>() {
			@Override
			public void handle(Start event) {
				logger.info("Atomic Register created");
			}
		};
		subscribe(startHandler, control);

		Handler<ArReadRequest> readRequest = new Handler<ArReadRequest>() {
			@Override
			public void handle(ArReadRequest event) {
				rid++;
				requests = 0;
				readList.clear();
				reading = true;
				ReadBebDataMessage bebMessage = new ReadBebDataMessage(self, rid);
				trigger(new BebBroadcast(bebMessage), beb);
			}
		};
		subscribe(readRequest, ar);

		Handler<ArWriteRequest> writeRequest = new Handler<ArWriteRequest>() {
			@Override
			public void handle(ArWriteRequest event) {
				rid++;
				writeVal = event.getValue();
				requests = 0;
				readList.clear();
				reading = false;
				ReadBebDataMessage bebMessage = new ReadBebDataMessage(self, rid);
				trigger(new BebBroadcast(bebMessage), beb);
			}
		};
		subscribe(writeRequest, ar);

		Handler<ReadBebDataMessage> bebDeliver = new Handler<ReadBebDataMessage>() {
			@Override
			public void handle(ReadBebDataMessage event) {
				ArDataMessage arMessage = new ArDataMessage(self, event.getRid(), ts, wr, val);
				trigger(new Pp2pSend(event.getSource(), arMessage), pp2p);
			}
		};
		subscribe(bebDeliver, beb);

		Handler<WriteBebDataMessage> bebWriteValue = new Handler<WriteBebDataMessage>() {
			@Override
			public void handle(WriteBebDataMessage event) {
				if (event.getTs().equals(ts)) {
					if (event.getWr() > wr) {
						ts = event.getTs();
						wr = event.getWr();
						val = event.getVal();
					}
				} else if (event.getTs() > ts) {
					ts = event.getTs();
					wr = event.getWr();
					val = event.getVal();
				}
				trigger(new Pp2pSend(event.getSource(), new ArAckMessage(self, event.getRid())), pp2p);
			}
		};
		subscribe(bebWriteValue, beb);

		Handler<ArDataMessage> arDeliver = new Handler<ArDataMessage>() {
			@Override
			public void handle(ArDataMessage event) {
				if (event.getRid().equals(rid)) {
					readList.add(new ReadObject(event.getTs(), event.getWr(), event.getVal(), event.getSource().getId()));

					if (readList.size() > (numberOfNodes / 2)) {
						readList.sort((obj0, obj1) -> {
							if (obj0.equals(obj1)) { return 0;}
							if (obj0.getTs().equals(obj1.getTs())) {
								int res = -1;
								if (obj0.getNodeId() >= obj1.getNodeId()) {
									res = 1;
								}
								return res;
							}
							if (obj0.getTs() < obj1.getTs()) { return -1;}
							else { return 1; }
						});

						ReadObject highest = readList.get(readList.size() - 1);

						rr = highest.getWr();
						readVal = highest.getVal();
						maxTs = highest.getTs();

						readList.clear();

						WriteBebDataMessage wBebMsg;
						if (reading) {
							wBebMsg = new WriteBebDataMessage(self, rid, maxTs, rr, readVal);
						} else {
							wBebMsg = new WriteBebDataMessage(self, rid, maxTs + 1, self.getId(), writeVal);
						}
						trigger(new BebBroadcast(wBebMsg), beb);
					}
				}
			}
		};
		subscribe(arDeliver, pp2p);

		Handler<ArAckMessage> ArAckHandler = new Handler<ArAckMessage>() {
			@Override
			public void handle(ArAckMessage event) {
				if (event.getRid().equals(rid)) {
					requests++;
					if (requests > (numberOfNodes / 2)) {
						requests = 0;

						if (reading) {
							reading = false;
							ArReadResponse resp = new ArReadResponse(readVal);
							trigger(resp, ar);
						} else {
							trigger(new ArWriteResponse(), ar);
						}
					}
				}
			}
		};
		subscribe(ArAckHandler, pp2p);
	}
}
