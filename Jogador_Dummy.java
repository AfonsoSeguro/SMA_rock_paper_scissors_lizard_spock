package projeto_final;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Jogador_Dummy extends Agent {

    List<AID> agents;
    ArrayList<String> armas;
    Random rand;

    public Jogador_Dummy(){
        super();
        this.agents = new ArrayList<>();
        this.armas = new ArrayList();
        for (int i = 0; i < 3; i++) {
            this.armas.add("Scissors");
            this.armas.add("Paper");
            this.armas.add("Rock");
            this.armas.add("Lizard");
            this.armas.add("Spock");
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
                        if (!result[i].getName().getName().equals(myAgent.getName()))agents.add(result[i].getName());
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        });
        pb.addSubBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
                ACLMessage msg = this.myAgent.receive();
                if(msg != null){
                    ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
                    message.setContent(armas.remove(rand.nextInt(armas.size())).toString());
                    message.addReceiver(msg.getSender());
                    send(message);
                    if(armas.size() == 0)reini();
                }
                else block();
            }
        });

        addBehaviour(pb);
    }

    public void reini(){
        agents.clear();
        this.agents = new ArrayList<>();
        this.armas = new ArrayList();
        for (int i = 0; i < 3; i++) {
            this.armas.add("Scissors");
            this.armas.add("Paper");
            this.armas.add("Rock");
            this.armas.add("Lizard");
            this.armas.add("Spock");
        }
        this.rand = new Random();
    }
}
