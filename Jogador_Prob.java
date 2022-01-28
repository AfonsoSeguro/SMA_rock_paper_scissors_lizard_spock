package projeto_final;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Jogador_Prob extends Agent {

    AID mestre;

    List<AID> agents;

    ArrayList<String> armas;
    ArrayList<ArrayList<String>> armas_oponentes;


    Jogada [] jogs;
    Jogada jog;

    ArrayList<String []> dados;

    int index;

    Queue<String> message_queue;


    public Jogador_Prob(){
        super();
        this.index = -1;
        this.mestre = null;
        this.agents = new ArrayList<>();
        this.message_queue = new LinkedList();
        this.dados = new ArrayList();
        this.armas = new ArrayList();
        for (int i = 0; i < 3; i++) {
            this.armas.add("Scissors");
            this.armas.add("Paper");
            this.armas.add("Rock");
            this.armas.add("Lizard");
            this.armas.add("Spock");
        }
        this.armas_oponentes = new ArrayList();
        this.jog = new Jogada("", -60);
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
                    if(result.length != agents.size() + 1) {
                        agents.clear();
                        armas_oponentes.clear();
                        for (int i = 0; i < result.length; i++) {
                            if (!result[i].getName().getName().equals(myAgent.getName())) {
                                agents.add(result[i].getName());
                                ArrayList<String> arm = new ArrayList<>();
                                for (int j = 0; j < 3; j++) {
                                    arm.add("Scissors");
                                    arm.add("Paper");
                                    arm.add("Rock");
                                    arm.add("Lizard");
                                    arm.add("Spock");
                                }
                                armas_oponentes.add(arm);
                            }
                        }
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        });

        pb.addSubBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {
                ACLMessage msg = this.myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
                if(msg != null){
                    message_queue.add(msg.getContent());
                    mestre = msg.getSender();
                }
                else block();
            }
        });


        FSMBehaviour fsm = new FSMBehaviour();
        fsm.registerFirstState(new OneShotBehaviour() {//Informação do agente sobre o seu ambiente, ou seja, que jogadas pode fazer e que jogadas os outros oponentes podem realizar

            int change_state;

            @Override
            public void action() {
                if(message_queue.size() > 0){
                    change_state = 1;
                    String msg = message_queue.remove();
                    if(!msg.contains("play")){
                        String [] jog = msg.split(" ");
                        // O agente fica a saber qual foram as "armas" utilizadas pelos últimos jogadores na última ronda.
                        dados.add(jog);
                        //O agente remove da lista a última jogada dos oponentes e a dele, ficando com as ainda por jogar, tendo noção de quais as possíveis futuras mãos.
                        for (int i = 0; i < armas_oponentes.size(); i++) {
                            ArrayList<String> arm_array = armas_oponentes.get(i);
                            for (int j = 0; j < arm_array.size(); j++) {
                                if(arm_array.get(j).equals(jog[i])){
                                    arm_array.remove(j);
                                    break;
                                }
                            }
                            armas_oponentes.set(i, arm_array);
                        }
                    }
                    else{
                        index = Integer.parseInt(msg.split(" ")[0]);
                    }
                }
                else{
                    change_state = 0;
                }
            }

            @Override
            public int onEnd(){
                return change_state;
            }

        }, "BELIEF");



        fsm.registerState(new OneShotBehaviour(){//Calcula quais as jogadas, ou sequência de jogadas, que pode realizar, ou seja, vários planos

            int change_state;

            @Override
            public void action() {
                jogs = new Jogada[]{new Jogada("Scissors", 0),new Jogada("Paper", 0),new Jogada("Rock", 0),new Jogada("Lizard", 0),new Jogada("Spock", 0)};
                for (int i = 0; i < armas_oponentes.size(); i++) {
                    if(i != index){
                        for (int j = 0; j < armas_oponentes.get(i).size(); j++) {
                            for (int k = 0; k < jogs.length; k++) {
                                if(armas_oponentes.get(i).get(j).equals(jogs[k].jogada)){
                                    jogs[k].valor++;
                                    break;
                                }
                            }
                        }
                    }
                }
                change_state = 1;
            }

            @Override
            public int onEnd(){
                return change_state;
            }

        }, "DESIRE");



        fsm.registerState(new OneShotBehaviour(){//Escolhe dos vários planos apresentados, quais os melhores.
            int change_state;
            @Override
            public void action() {

                Jogada [] ponts = new Jogada[armas.size()];
                for (int i = 0; i < ponts.length; i++) {
                    ponts[i] = new Jogada(armas.get(i), 0);
                }

                for (int i = 0; i < ponts.length; i++) {
                    for (int j = 0; j < jogs.length; j++) {
                        int ganh = ganha(ponts[i].jogada, jogs[j].jogada);
                        if(ganh > 0) ponts[i].valor += jogs[j].valor;
                        else if(ganh < 0) ponts[i].valor -= jogs[j].valor;
                    }
                }
                
                jog = new Jogada("", -60);
                for (int i = 0; i < ponts.length; i++) {
                    if(jog.valor < ponts[i].valor)jog = ponts[i];
                }
                change_state = 1;

            }

            @Override
            public int onEnd(){
                return change_state;
            }
        }, "INTENTION");



        fsm.registerState(new OneShotBehaviour(){//Envia a jogada escolhida ao mestre e retira da lista de mãos disponiveis para jogar

            @Override
            public void action() {
                if(!jog.jogada.equals("")){
                    ACLMessage m = new ACLMessage(ACLMessage.PROPOSE);
                    m.addReceiver(mestre);
                    m.setContent(jog.jogada);
                    send(m);
                    for (int i = 0; i < armas.size(); i++) {
                        if(armas.get(i).equals(jog.jogada)){
                            armas.remove(i);
                            break;
                        }
                    }
                    jog = new Jogada("",-60);
                    if(armas.size() == 0)reini();
                }
            }

            @Override
            public int onEnd(){
                return 1;
            }
        }, "PLAY");

        fsm.registerTransition("BELIEF", "BELIEF", 0);
        fsm.registerTransition("BELIEF", "DESIRE", 1);
        fsm.registerTransition("DESIRE", "INTENTION", 1);
        fsm.registerTransition("INTENTION", "DESIRE", 0);
        fsm.registerTransition("INTENTION", "PLAY", 1);
        fsm.registerTransition("PLAY", "BELIEF", 1);

        pb.addSubBehaviour(fsm);

        addBehaviour(pb);
    }

    class Jogada{
        public String jogada;
        public int valor;

        public Jogada(String jog, int val){
            this.jogada = jog;
            this.valor = val;
        }
    }

    public int ganha(String jogada, String oponente){
        int pontos = 0;
        if(jogada.equals("Scissors")){
            if(oponente.equals("Paper"))pontos++;
            else if(oponente.equals("Rock"))pontos--;
            else if(oponente.equals("Lizard"))pontos++;
            else if(oponente.equals("Spock"))pontos--;
        }
        else if(jogada.equals("Paper")){
            if(oponente.equals("Scissors"))pontos--;
            else if(oponente.equals("Rock"))pontos++;
            else if(oponente.equals("Lizard"))pontos--;
            else if(oponente.equals("Spock"))pontos++;
        }
        else if(jogada.equals("Rock")){
            if(oponente.equals("Scissors"))pontos++;
            else if(oponente.equals("Paper"))pontos--;
            else if(oponente.equals("Lizard"))pontos++;
            else if(oponente.equals("Spock"))pontos--;
        }
        else if(jogada.equals("Lizard")){
            if(oponente.equals("Scissors"))pontos--;
            else if(oponente.equals("Paper"))pontos++;
            else if(oponente.equals("Rock"))pontos--;
            else if(oponente.equals("Spock"))pontos++;
        }
        else if(jogada.equals("Spock")){
            if(oponente.equals("Scissors"))pontos++;
            else if(oponente.equals("Paper"))pontos--;
            else if(oponente.equals("Rock"))pontos++;
            else if(oponente.equals("Lizard"))pontos--;
        }
        return pontos;
    }

    public void reini(){
        agents.clear();
        this.index = -1;
        this.mestre = null;
        this.agents = new ArrayList<>();
        this.message_queue = new LinkedList();
        this.dados = new ArrayList();
        this.armas = new ArrayList();
        for (int i = 0; i < 3; i++) {
            this.armas.add("Scissors");
            this.armas.add("Paper");
            this.armas.add("Rock");
            this.armas.add("Lizard");
            this.armas.add("Spock");
        }
        this.armas_oponentes = new ArrayList();
        this.jog = new Jogada("", -60);
    }

}
