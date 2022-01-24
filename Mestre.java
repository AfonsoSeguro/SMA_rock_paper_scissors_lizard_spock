package projeto_final;


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetInitiator;

import java.util.*;

public class Mestre extends Agent {

    List<AID> agents;
    ArrayList<Integer> pontos;
    ArrayList<ArrayList<String>> dados;
    Queue<ACLMessage> message_queue;


    public Mestre() {
        super();
        this.agents = new ArrayList<>();
        this.pontos = new ArrayList<>();
        this.dados = new ArrayList<>();
        this.message_queue = new LinkedList();
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
                    dados.clear();
                    pontos.clear();
                    for (int i = 0; i < result.length; i++) {
                        if (!result[i].getName().getName().equals(myAgent.getName()))
                            agents.add(result[i].getName());
                            dados.add(new ArrayList());
                            pontos.add(0);
                    }
                    dados.remove(dados.size() - 1);
                    pontos.remove(pontos.size() - 1);
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        });

        pb.addSubBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage m = myAgent.receive(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)));
                if(m != null){
                    if(m.getPerformative() == ACLMessage.REQUEST)pb.addSubBehaviour(new Play_Game());
                    else if(m.getPerformative() == ACLMessage.PROPOSE){
                        message_queue.add(m);
                    }
                }
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
            this.i = 1;
            this.max = 15;
            System.out.println("");
        }

        @Override
        public void action() {
            if(done){
                done = false;
                ACLMessage message = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < agents.size(); i++) message.addReceiver(agents.get(i));
                message.setContent("play");
                send(message);
                System.out.println("Ronda " + i);
            }
            else if(!done && message_queue.size() == agents.size()){
                ArrayList<String> jog_ronda = new ArrayList(agents.size());
                for (int j = 0; j < agents.size(); j++)jog_ronda.add("");
                for (int j = 0; j < agents.size(); j++) {
                    ACLMessage msg = message_queue.remove();
                    int k = 0;
                    for(;k < agents.size(); k++){
                        if(agents.get(k).equals(msg.getSender()))break;
                    }
                    jog_ronda.set(k, msg.getContent());
                    System.out.println(msg.getSender().getName() + " jogou " + msg.getContent());

                }
                dados.add(jog_ronda);
                for (int j = 0; j < pontos.size(); j++) {
                    String jogada = jog_ronda.get(j);
                    for (int k = 0; k < agents.size(); k++) {
                        if(k != j){
                            if(jogada.equals("Scissors")){
                                if(jog_ronda.get(k).equals("Scissors"))pontos.set(j, pontos.get(j));
                                else if(jog_ronda.get(k).equals("Paper"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Rock"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Lizard"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Spock"))pontos.set(j, pontos.get(j) - 1);
                            }
                            else if(jogada.equals("Paper")){
                                if(jog_ronda.get(k).equals("Scissors"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Paper"))pontos.set(j, pontos.get(j));
                                else if(jog_ronda.get(k).equals("Rock"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Lizard"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Spock"))pontos.set(j, pontos.get(j) + 1);
                            }
                            else if(jogada.equals("Rock")){
                                if(jog_ronda.get(k).equals("Scissors"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Paper"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Rock"))pontos.set(j, pontos.get(j));
                                else if(jog_ronda.get(k).equals("Lizard"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Spock"))pontos.set(j, pontos.get(j) - 1);
                            }
                            else if(jogada.equals("Lizard")){
                                if(jog_ronda.get(k).equals("Scissors"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Paper"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Rock"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Lizard"))pontos.set(j, pontos.get(j));
                                else if(jog_ronda.get(k).equals("Spock"))pontos.set(j, pontos.get(j) + 1);
                            }
                            else if(jogada.equals("Spock")){
                                if(jog_ronda.get(k).equals("Scissors"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Paper"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Rock"))pontos.set(j, pontos.get(j) + 1);
                                else if(jog_ronda.get(k).equals("Lizard"))pontos.set(j, pontos.get(j) - 1);
                                else if(jog_ronda.get(k).equals("Spock"))pontos.set(j, pontos.get(j));
                            }
                        }
                    }
                }
                
                done = true;
                i++;
                System.out.println("");
            }
        }

        @Override
        public boolean done() {
            if(i > max){
                int maxi =  -60;
                String winner = "";
                System.out.print("Classificações: ");
                for (int j = 0; j < pontos.size(); j++) {
                    if(pontos.get(j) > maxi){
                        maxi = pontos.get(j);
                        winner = agents.get(j).getLocalName();
                    }
                    System.out.print(pontos.get(j) + " ");
                }
                System.out.println("");
                System.out.println("---------------------------------------------------");
                System.out.println("O Agente " + winner + " ganhou este jogo");
                System.out.println("---------------------------------------------------");
                pontos.clear();
                dados.clear();
                return true;
            }
            return false;
        }
    }
}