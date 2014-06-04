package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.HashMap;

import model.Action;
import model.ObjectBPMN;
import model.Pool;
import model.WorkFlow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GrapheAgent extends Agent {
	WorkFlow wf;

	@Override
	protected void setup() {
		addBehaviour(new GraphBehaviour());

	}

	private class CreationGraph extends OneShotBehaviour {
		@Override
		public void action() {
			wf = new WorkFlow(800, 600);
		}

	}

	private class GraphBehaviour extends CyclicBehaviour {
		@Override
		public void action() {
			Boolean maj = false;
			ACLMessage message = myAgent.receive(MessageTemplate
					.MatchPerformative(ACLMessage.REQUEST));
			if (message != null) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				// r�cup�ration d'une action
				try {
					// act est l'action envoy�es
					Action act = mapper.readValue(message.getContent(),
							Action.class);

					switch (act.getType()) {
					case "create":
						for (int i = 0; i < act.getObjects().size(); i++) {
							wf.addObject(act.getObjects().get(i));
						}
						maj = true;
						break;
					case "remove":
						for (int i = 0; i < act.getId().size(); i++) {
							wf.retirer_objet(Integer.parseInt(act.getId()
									.get(i)));
						}
						maj = true;
						break;
					case "connect":
						wf.linker(Integer.parseInt(act.getId().get(0)),
								Integer.parseInt(act.getId().get(1)));
						maj = true;
						break;
					case "unconnect":
						wf.unlinker(Integer.parseInt(act.getId().get(0)),
								Integer.parseInt(act.getId().get(1)));
						maj = true;
						break;
					case "get_label":
						// si on demande au graph de renvoyer un objet grace �
						// son label
						ObjectBPMN o = wf.get_objet(act.getId().get(0));
						ObjectMapper mapper2 = new ObjectMapper();
						try {
							ACLMessage message_reply = message.createReply();
							message_reply.setPerformative(ACLMessage.REQUEST);

							String s = mapper2.writeValueAsString(o);
							message_reply.setContent(s);
							myAgent.send(message_reply);

						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
						break;
					case "get_pool":
						// m�me chose mais avec une pool
						Pool o2 = wf.get_pool(act.getId().get(0));
						ObjectMapper mapper3 = new ObjectMapper();
						try {
							ACLMessage message_reply = message.createReply();
							message_reply.setPerformative(ACLMessage.REQUEST);

							String s = mapper3.writeValueAsString(o2);
							message_reply.setContent(s);
							myAgent.send(message_reply);

						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
						break;

					}

					// envois d'un message � l'optimisateur pour faire une mise
					// a jour si besoin
					if (maj) {
						WorkFlow wf = mapper.readValue(message.getContent(),
								WorkFlow.class);
						wf.optimise();

						// envois du r�sultat vers un agent d'affichage
						ObjectMapper mapper2 = new ObjectMapper();
						try {
							ACLMessage message_reply = new ACLMessage(
									ACLMessage.REQUEST);

							String s = mapper2.writeValueAsString(wf);
							message_reply.setContent(s);
							message_reply.addReceiver(new AID("Optimisateur",
									AID.ISLOCALNAME));
							myAgent.send(message_reply);

						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	}

	public HashMap<String, String> toMap(String json) {
		HashMap<String, String> map = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper();
		try { // Convert JSON string to Map
			map = mapper.readValue(json, HashMap.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	public String toJSON(Action act) {
		String json = new String();
		ObjectMapper m = new ObjectMapper();
		try {
			json = m.writeValueAsString(act);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return json;
	}
}
