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

import java.lang.reflect.Array;
import java.util.*;

public class Jogador_MinMax extends Agent {

    List<AID> agents;

    ArrayList<String> armas;
    ArrayList<ArrayList<String>> armas_oponentes;

    ArrayList<String> armas_redux;
    ArrayList<ArrayList<String>> armas_oponentes_redux;

    ArrayList<String []> dados;

    int index;

    Queue<String> message_queue;



    public Jogador_MinMax(){
        super();
        this.index = -1;
        this.agents = new ArrayList<>();
        this.message_queue = new LinkedList();
        this.dados = new ArrayList();
        this.armas = new ArrayList();
        for (int i = 0; i < 3; i++) {
            armas.add("Scissors");
            armas.add("Paper");
            armas.add("Rock");
            armas.add("Lizard");
            armas.add("Spock");
        }
        this.armas_oponentes = new ArrayList();
        this.armas_redux = new ArrayList<>();
        this.armas_redux.add("Scissors");
        this.armas_redux.add("Paper");
        this.armas_redux.add("Rock");
        this.armas_redux.add("Lizard");
        this.armas_redux.add("Spock");
        this.armas_oponentes_redux = new ArrayList<>();
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
                    armas_oponentes.clear();
                    armas_oponentes_redux.clear();
                    for (int i = 0; i < result.length; i++) {
                        if (!result[i].getName().getName().equals(myAgent.getName())){
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
                            ArrayList<String> arm_redux = new ArrayList<>();
                            arm_redux.add("Scissors");
                            arm_redux.add("Paper");
                            arm_redux.add("Rock");
                            arm_redux.add("Lizard");
                            arm_redux.add("Spock");
                            armas_oponentes_redux.add(arm_redux);
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

                        //O agente remove da lista a última jogada dos oponentes, ficando com as ainda por jogar, tendo noção de quais as possíveis futuras mãos.
                        for (int i = 0; i < armas_oponentes.size(); i++) {
                            ArrayList<String> arm_array = armas_oponentes.get(i);
                            for (int j = 0; j < arm_array.size(); j++) {
                                if(arm_array.get(j).equals(jog[i])){
                                    arm_array.remove(j);
                                    break;
                                }
                            }
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

            @Override
            public void action() {
                ArrayList<Jogada> jogs = new ArrayList<>();
                ArrayList<String []> hip = new ArrayList();//pode nao ser necessário
                calc_hip(hip, armas_oponentes, new String[agents.size()], 0);
                for (int i = 0; i < hip.size(); i++) {
                    ArrayList<ArrayList<String>> jog_pos_aux = (ArrayList<ArrayList<String>>)armas_oponentes.clone();
                    for (int j = 0; j < jog_pos_aux.size(); j++) {
                        jog_pos_aux.set(j, (ArrayList<String>) armas_oponentes.get(j).clone());
                        for (int k = 0; k < jog_pos_aux.get(j).size(); k++) {
                            if(jog_pos_aux.get(j).get(k).equals(hip.get(i)[j])){
                                jog_pos_aux.get(j).remove(k);
                                break;
                            }
                        }
                    }
                    jogs.add(new Jogada(hip.get(i)[index], max(jog_pos_aux, hip.get(i),1)));//alterar depth
                }
                System.out.println("");
            }

            @Override
            public int onEnd(){
                return 1;
            }

        }, "DESIRE");



        fsm.registerState(new OneShotBehaviour(){//Escolhe dos vários planos apresentados, quais os melhores.


            @Override
            public void action() {

            }


            @Override
            public int onEnd(){
                return 1;
            }
        }, "INTENTION");

        fsm.registerTransition("BELIEF", "BELIEF", 0);
        fsm.registerTransition("BELIEF", "DESIRE", 1);
        fsm.registerTransition("INTENTION", "INTENTION", 1);
        fsm.registerTransition("DESIRE", "DESIRE", 1);

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

    public int min(ArrayList<ArrayList<String>> jog_pos, String [] jog_ant, int depth){
        if(depth == 0)return calc_pontos(jog_ant, index);
        int pont = +60;
        ArrayList<String []> hip = new ArrayList();//pode nao ser necessário
        calc_hip(hip, armas_oponentes, new String[agents.size()], 0);
        for (int i = 0; i < hip.size(); i++) {
            ArrayList<ArrayList<String>> jog_pos_aux = (ArrayList<ArrayList<String>>)jog_pos.clone();
            for (int j = 0; j < jog_pos_aux.size(); j++) {
                jog_pos_aux.set(j, (ArrayList<String>) jog_pos.get(j).clone());
                for (int k = 0; k < jog_pos_aux.get(j).size(); k++) {
                    if(jog_pos_aux.get(j).get(k).equals(hip.get(i)[j])){
                        jog_pos_aux.get(j).remove(k);
                        break;
                    }
                }
            }
            int val = max(jog_pos_aux, hip.get(i),depth - 1);
            pont = pont < val ? pont : val;
        }
        return pont;
    }

    public int max(ArrayList<ArrayList<String>> jog_pos, String [] jog_ant, int depth){
        if(depth == 0)return calc_pontos(jog_ant, index);
        int pont = -60;
        ArrayList<String []> hip = new ArrayList();//pode nao ser necessário
        calc_hip(hip, armas_oponentes, new String[agents.size()], 0);
        for (int i = 0; i < hip.size(); i++) {
            ArrayList<ArrayList<String>> jog_pos_aux = (ArrayList<ArrayList<String>>)jog_pos.clone();
            for (int j = 0; j < jog_pos_aux.size(); j++) {
                jog_pos_aux.set(j, (ArrayList<String>) jog_pos.get(j).clone());
                for (int k = 0; k < jog_pos_aux.get(j).size(); k++) {
                    if(jog_pos_aux.get(j).get(k).equals(hip.get(i)[j])){
                        jog_pos_aux.get(j).remove(k);
                        break;
                    }
                }
            }
            int val = min(jog_pos_aux, hip.get(i),depth - 1);
            pont = pont > val ? pont : val;
        }
        return pont;
    }

    public void calc_hip(ArrayList<String [] > hip, ArrayList<ArrayList<String>> jog_pos, String [] jog, int nivel){
        if(nivel >= jog.length){
            hip.add(jog);
            return;
        }
        for (int i = 0; i < jog_pos.get(nivel).size(); i++) {
            jog[nivel] = jog_pos.get(nivel).get(i);
            calc_hip(hip, jog_pos, (String [])jog.clone(), nivel + 1);
        }
    }

    public int calc_pontos(String [] jogadas, int index){
        int pontos = 0;
        String jogada = jogadas[index];
        for (int k = 0; k < jogadas.length; k++) {
            if(k != index){
                if(jogada.equals("Scissors")){
                    if(jogadas[k].equals("Paper"))pontos++;
                    else if(jogadas[k].equals("Rock"))pontos--;
                    else if(jogadas[k].equals("Lizard"))pontos++;
                    else if(jogadas[k].equals("Spock"))pontos--;
                }
                else if(jogada.equals("Paper")){
                    if(jogadas[k].equals("Scissors"))pontos--;
                    else if(jogadas[k].equals("Rock"))pontos++;
                    else if(jogadas[k].equals("Lizard"))pontos--;
                    else if(jogadas[k].equals("Spock"))pontos++;
                }
                else if(jogada.equals("Rock")){
                    if(jogadas[k].equals("Scissors"))pontos++;
                    else if(jogadas[k].equals("Paper"))pontos--;
                    else if(jogadas[k].equals("Lizard"))pontos++;
                    else if(jogadas[k].equals("Spock"))pontos--;
                }
                else if(jogada.equals("Lizard")){
                    if(jogadas[k].equals("Scissors"))pontos--;
                    else if(jogadas[k].equals("Paper"))pontos++;
                    else if(jogadas[k].equals("Rock"))pontos--;
                    else if(jogadas[k].equals("Spock"))pontos++;
                }
                else if(jogada.equals("Spock")){
                    if(jogadas[k].equals("Scissors"))pontos++;
                    else if(jogadas[k].equals("Paper"))pontos--;
                    else if(jogadas[k].equals("Rock"))pontos++;
                    else if(jogadas[k].equals("Lizard"))pontos--;
                }
            }
        }
        return pontos;
    }
}
