/**
 * This file is part of the ID2203 course assignments kit.
 * 
 * Copyright (C) 2009-2013 KTH Royal Institute of Technology
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.ict.id2203.components.crb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.crb.CausalOrderReliableBroadcast;
import se.kth.ict.id2203.ports.crb.CrbBroadcast;
import se.kth.ict.id2203.ports.rb.RbBroadcast;
import se.kth.ict.id2203.ports.rb.ReliableBroadcast;
import se.sics.kompics.*;
import se.sics.kompics.address.Address;

import java.util.*;

public class WaitingCrb extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(WaitingCrb.class);

	private final Positive<ReliableBroadcast> rb = requires(ReliableBroadcast.class);
	private final Negative<CausalOrderReliableBroadcast> corb = provides(CausalOrderReliableBroadcast.class);

	private final Address self;
	private Integer sequenceNumber;
	private final int[] vector;
	private final List<CrbDataMessage> pending;

	public WaitingCrb(WaitingCrbInit init) {
		this.self = init.getSelfAddress();
		Set<Address> nodes = new HashSet<>(init.getAllAddresses());
		this.sequenceNumber = 0;
		this.vector = new int[nodes.size()];
		pending = new LinkedList<>();

		Arrays.fill(vector, 0);

		Handler<Start> startHandler = new Handler<Start>() {
			@Override
			public void handle(Start event) {
				logger.info("WaitingCrb created");
			}
		};
		subscribe(startHandler, control);
		Handler<CrbBroadcast> bcastHandler = new Handler<CrbBroadcast>() {
			@Override
			public void handle(CrbBroadcast event) {
				int[] newVector = vector.clone();
				newVector[self.getId() - 1] = sequenceNumber;
				sequenceNumber++;
				CrbDataMessage msg = new CrbDataMessage(self, event.getDeliverEvent(), newVector);
				trigger(new RbBroadcast(msg), rb);
			}
		};
		subscribe(bcastHandler, corb);
		Handler<CrbDataMessage> crbDeliver = new Handler<CrbDataMessage>() {
			@Override
			public void handle(CrbDataMessage event) {
				pending.add(event);

				pending.sort((obj0, obj1) -> {
					if (Arrays.equals(obj0.getVector(), obj1.getVector())) {
						return obj0.getSource().getId() < obj1.getSource().getId() ? -1 : 1;
					} else if (lessEqual(obj0.getVector(), obj1.getVector())) {
						return -1;
					} else {
						return 1;
					}
				});

				Iterator<CrbDataMessage> pendingIter = pending.iterator();
				CrbDataMessage tmpMsg;

				while (pendingIter.hasNext()) {
					tmpMsg = pendingIter.next();

					if (lessEqual(tmpMsg.getVector(), vector)) {
						pendingIter.remove();
						vector[tmpMsg.getSource().getId() - 1]++;
						trigger(tmpMsg.getData(), corb);
					} else {
						break;
					}
				}
			}
		};
		subscribe(crbDeliver, rb);
	}

	private boolean lessEqual(int[] vector0, int[] vector1) {
		boolean result = false;

		if (vector0.length == vector1.length) {
			for (int i = 0; i < vector0.length; i++) {
				if (vector0[i] <= vector1[i])
					result = true;
				else {
					result = false;
					break;
				}
			}
		}

		return result;
	}
}
