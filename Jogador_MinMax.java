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

    AID mestre;

    List<AID> agents;

    ArrayList<String> armas;
    ArrayList<ArrayList<String>> armas_oponentes;

    ArrayList<String> armas_redux;
    ArrayList<ArrayList<String>> armas_oponentes_redux;

    ArrayList<Jogada> jogs;
    Jogada jog;

    ArrayList<String []> dados;

    int index;

    Queue<String> message_queue;

    public Jogador_MinMax(){
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
        this.jogs = new ArrayList<>();
        this.jog = new Jogada("", -60);
        this.armas_redux = convert_redux(armas);
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
                    if(result.length != agents.size() + 1) {
                        agents.clear();
                        armas_oponentes.clear();
                        armas_oponentes_redux.clear();
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
                                armas_oponentes_redux = convert_redux_array(armas_oponentes);
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
        fsm.registerFirstState(new OneShotBehaviour() {//Informa????o do agente sobre o seu ambiente, ou seja, que jogadas pode fazer e que jogadas os outros oponentes podem realizar

            int change_state;

            @Override
            public void action() {
                if(message_queue.size() > 0){
                    change_state = 1;
                    String msg = message_queue.remove();
                    if(!msg.contains("play")){
                        String [] jog = msg.split(" ");
                        // O agente fica a saber qual foram as "armas" utilizadas pelos ??ltimos jogadores na ??ltima ronda.
                        dados.add(jog);
                        //O agente remove da lista a ??ltima jogada dos oponentes e a dele, ficando com as ainda por jogar, tendo no????o de quais as poss??veis futuras m??os.
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
                        armas_oponentes_redux = convert_redux_array(armas_oponentes);
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



        fsm.registerState(new OneShotBehaviour(){//Calcula quais as jogadas, ou sequ??ncia de jogadas, que pode realizar, ou seja, v??rios planos

            int change_state;

            @Override
            public void action() {
                jogs.clear();
                ArrayList<String []> hip = new ArrayList();//pode nao ser necess??rio
                calc_hip(hip, armas_oponentes_redux, new String[agents.size()], 0);
                for (int i = 0; i < hip.size(); i++) {
                    ArrayList<ArrayList<String>> jog_pos_aux = (ArrayList<ArrayList<String>>)armas_oponentes_redux.clone();
                    for (int j = 0; j < jog_pos_aux.size(); j++) {
                        jog_pos_aux.set(j, (ArrayList<String>) armas_oponentes_redux.get(j).clone());
                        for (int k = 0; k < jog_pos_aux.get(j).size(); k++) {
                            if(jog_pos_aux.get(j).get(k).equals(hip.get(i)[j])){
                                jog_pos_aux.get(j).remove(k);
                                break;
                            }
                        }
                    }
                    jogs.add(new Jogada(hip.get(i)[index], min(jog_pos_aux, hip.get(i), -60, 60,1)));//alterar depth
                }
                change_state = 1;
            }

            @Override
            public int onEnd(){
                return change_state;
            }

        }, "DESIRE");



        fsm.registerState(new OneShotBehaviour(){//Escolhe dos v??rios planos apresentados, quais os melhores.
            int change_state;
            @Override
            public void action() {
                if (jogs.size() > 0) {
                    jog = new Jogada("", -60);
                    int val = -60;
                    for (int i = 0; i < jogs.size(); i++) {
                        if(jogs.get(i).valor > jog.valor || (jogs.get(i).valor == jog.valor && Math.random() > 0.9))jog = jogs.get(i);
                    }
                    jogs.clear();
                    change_state = 1;
                }
                else {
                    change_state = 0;
                }
            }

            @Override
            public int onEnd(){
                return change_state;
            }
        }, "INTENTION");



        fsm.registerState(new OneShotBehaviour(){//Envia a jogada escolhida ao mestre e retira da lista de m??os disponiveis para jogar

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
                    armas_redux = convert_redux(armas);
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

    public ArrayList<String> convert_redux(ArrayList<String> array){
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            boolean contains = false;
            for (int j = 0; j < result.size(); j++) {
                if(result.get(j).equals(array.get(i))){
                    contains = true;
                    break;
                }
            }
            if(!contains)result.add(array.get(i));
        }
        return result;
    }

    public ArrayList<ArrayList<String>> convert_redux_array(ArrayList<ArrayList<String>> array){
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(convert_redux(array.get(i)));
        }
        return result;
    }

    public int min(ArrayList<ArrayList<String>> jog_pos, String [] jog_ant, int alpha, int beta, int depth){
        if(depth == 0 || jog_pos.size() <= 2)return calc_pontos(jog_ant, index);
        int pont = +60;
        ArrayList<String []> hip = new ArrayList();//pode nao ser necess??rio
        calc_hip(hip, jog_pos, new String[agents.size()], 0);
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
            int val = max(jog_pos_aux, hip.get(i), alpha, beta,depth - 1);
            pont = (pont < val || (pont == val && Math.random() > 0.9)) ? pont : val;
            alpha = alpha > val ? alpha : val;
            if(beta <= alpha)break;
        }
        return pont;
    }

    public int max(ArrayList<ArrayList<String>> jog_pos, String [] jog_ant, int alpha, int beta, int depth){
        if(depth == 0 || jog_pos.size() <= 2)return calc_pontos(jog_ant, index);
        int pont = -60;
        ArrayList<String []> hip = new ArrayList();//pode nao ser necess??rio
        calc_hip(hip, jog_pos, new String[agents.size()], 0);
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
            int val = min(jog_pos_aux, hip.get(i), alpha, beta,depth - 1);
            pont = (pont > val  || (pont == val && Math.random() > 0.9))? pont : val;
            beta = beta < val ? beta : val;
            if(beta <= alpha)break;
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
        this.jogs = new ArrayList<>();
        this.jog = new Jogada("", -60);
        this.armas_redux = convert_redux(armas);
        this.armas_oponentes_redux = new ArrayList<>();
    }
}
