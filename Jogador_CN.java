package projeto_final;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Jogador_CN extends Agent {

    List<AID> agents;
    ArrayList<String> armas;
    Random rand;

    public Jogador_CN(){
        super();
        this.agents = new ArrayList<>();
        this.armas = new ArrayList();
        for (int i = 0; i < 3; i++) {
            armas.add("Scissors");
            armas.add("Paper");
            armas.add("Rock");
            armas.add("Lizard");
            armas.add("Spock");
        }
        this.rand = new Random();
    }


    @Override
    public void setup(){
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
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        });
        MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET), MessageTemplate.MatchPerformative(ACLMessage.CFP) );
        pb.addSubBehaviour(new ContractNetResponder(this, template) {
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
                //System.out.println("Agent "+getLocalName()+": CFP received from "+cfp.getSender().getName()+". Action is "+cfp.getContent());
                int index = rand.nextInt(armas.size());
                String r = armas.get(index);
                armas.remove(index);
                //System.out.println("Agent "+getLocalName()+": Proposing "+ r);
                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(r + "");
                return propose;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                /*System.out.println("Agent "+getLocalName()+": Proposal accepted");
                ACLMessage inform = accept.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setContent(Math.random() + "");
                return inform;*/
                return null;
            }

            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                //System.out.println("Agent "+getLocalName()+": Proposal rejected");
            }
        });

        addBehaviour(pb);
    }
}
