package projeto_final;


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;

import java.util.*;

public class Mestre_CN extends Agent {

    List<AID> agents;
    ArrayList<Integer> pontos;
    ArrayList<ArrayList<String>> dados;

    public Mestre_CN() {
        super();
        this.agents = new ArrayList<>();
        this.dados = new ArrayList<>();
    }

    @Override
    public void setup() {
        System.out.println("Agent " + this.getName());
        String service_name = "game";
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service_name);
        sd.setName(this.getName() + "-" + service_name);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        ParallelBehaviour pb = new ParallelBehaviour();

        pb.addSubBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(service_name);
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    agents.clear();
                    for (int i = 0; i < result.length; i++) {
                        if (!result[i].getName().getName().equals(myAgent.getName()))
                            agents.add(result[i].getName());
                            dados.add(new ArrayList());
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        });

        pb.addSubBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage m = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if(m != null)myAgent.addBehaviour(new Play_Game());
                else block();
            }
        });
        addBehaviour(pb);

    }

    class Play_Game extends Behaviour{

        boolean done;
        int i, max;

        public Play_Game(){
            super();
            this.done = true;
            this.i = 0;
            this.max = 15;
        }

        @Override
        public void action() {
            if(done){
                    done = false;

                    ACLMessage message = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agents.size(); i++) message.addReceiver(agents.get(i));
                    message.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    message.setContent("play");


                    myAgent.addBehaviour(new ContractNetInitiator(myAgent, message){
                        protected void handlePropose(ACLMessage propose, Vector v) {
                            //System.out.println("Agent "+propose.getSender().getName()+" proposed "+propose.getContent());
                        }

                        protected void handleRefuse(ACLMessage refuse) {
                            System.out.println("Agent "+refuse.getSender().getName()+" refused");
                        }

                        protected void handleFailure(ACLMessage failure) {
                            if (failure.getSender().equals(myAgent.getAMS())) System.out.println("Responder does not exist");
                            else System.out.println("Agent "+failure.getSender().getName()+" failed");
                        }

                        protected void handleAllResponses(Vector responses, Vector acceptances) {
                            if (responses.size() < agents.size()) System.out.println("Timeout expired: missing "+(agents.size() - responses.size())+" responses");
                            int bestProposal = 11;
                            AID bestProposer = null;
                            ACLMessage accept = null;
                            Enumeration e = responses.elements();
                            int j = 0;
                            System.out.println("Ronda " + i);
                            while (e.hasMoreElements()) {
                                ACLMessage msg = (ACLMessage) e.nextElement();
                                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                                    ACLMessage reply = msg.createReply();
                                    reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    acceptances.addElement(reply);
                                    dados.get(j).add(msg.getContent());
                                    System.out.println(msg.getSender().getName() + " jogou " + msg.getContent());
                                }
                                j++;
                            }
                            i++;
                            done = true;
                            System.out.println("");
                            /*if (accept != null) {
                                System.out.println("Accepting proposal "+bestProposal+" from responder "+bestProposer.getName());
                                accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                accept.setContent("");
                            }*/
                        }

                        protected void handleInform(ACLMessage inform) {
                            System.out.println("Agent "+inform.getSender().getName()+" returned the answer " + inform.getContent());
                        }
                    });
            }
        }

        @Override
        public boolean done() {
            return i >= max - 1;
        }
    }
}