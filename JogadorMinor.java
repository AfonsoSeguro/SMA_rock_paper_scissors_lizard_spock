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
import jade.proto.ContractNetResponder;

import java.util.*;

public class JogadorMinor extends Agent {
    AID mestre;
    List<AID> agents;
    ArrayList<String> armas;
    Random rand;
    String myname;

    ArrayList<ArrayList<String>> armas_oponentes;
    ArrayList<String []> dados;
    int index;
    int AcardsScissors;
    int AcardsRock;
    int AcardsPaper;
    int AcardsLizard;
    int AcardsSpock;
    // ArrayList<Jogada> jogs;
    // Jogada jog;
    Queue<String> message_queue;

    public JogadorMinor(){
        super();
        this.index = -1;
        this.mestre = null;
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
        this.rand = new Random();

        this.armas_oponentes = new ArrayList();

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
                            index=agents.size();
                            AcardsScissors = index*3;
                            AcardsRock=AcardsScissors;
                            AcardsPaper=AcardsScissors;
                            AcardsLizard=AcardsScissors;
                            AcardsSpock=AcardsScissors;



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
                if (msg != null) {
                    message_queue.add(msg.getContent());
                    mestre = msg.getSender();
                } else block();
                myname=myAgent.getName();
                calcartas();

            }
        });



        addBehaviour(pb);
    }

    public void calcartas() {
        String content ="";
        if(message_queue.size() > 0) {
            String msg = message_queue.remove();

            if (!msg.contains("play")) {
                String[] jog = msg.split(" ");
                dados.add(jog);

                for (int i = 0; i < dados.size(); i++) {

                    for (int j = 0; j < dados.get(i).length; j++) {

                        String dado;
                        dado=dados.get(i)[j];
                        if(dado.contains("Scissors")){
                            for(int k = 0; k<dado.length();k++){
                                if(dado.indent(k).equals("Scissors")){
                                    AcardsScissors--;
                                }
                            }if(AcardsScissors==0)AcardsScissors=index*3;
                        }
                        if(dado.contains("Paper")){
                            for(int k= 0; k<dado.length();k++){
                                if(dado.indent(k).equals("Paper")){
                                    AcardsPaper--;
                                }
                            }if(AcardsPaper==0)AcardsPaper=index*3;
                        }
                        if(dado.contains("Rock")){
                            for(int k = 0; k<dado.length();k++){
                                if(dado.indent(k).equals("Rock")){
                                    AcardsRock--;
                                }
                            }if(AcardsRock==0)AcardsRock=index*3;
                        }
                        if(dado.contains("Lizard")){
                            for(int k= 0; k<dado.length();k++){
                                if(dado.indent(k).equals("Lizard")){
                                    AcardsLizard--;
                                }
                            }
                            if(AcardsLizard==0)AcardsLizard=index*3;
                        }
                        if(dado.contains("Spock")){
                            for(int k= 0; k<dado.length();k++){
                                if(dado.indent(k).equals("Spock")){
                                    AcardsSpock--;
                                }
                            }
                            if(AcardsSpock==0)AcardsSpock=index*3;
                        }





                    }

                }

                int minor = Math.min(AcardsSpock,Math.min(AcardsLizard,Math.min(AcardsPaper,Math.min(AcardsRock,AcardsScissors))));
                if(minor==AcardsPaper) {
                    if (armas.contains("Scissors") || armas.contains("Lizard")) {
                        int scissors=0;
                        int lizard=0;
                        for(int i = 0; i< armas.size();i++ ){
                            if(armas.get(i).equals("Scissors"))scissors++;
                            if(armas.get(i).equals("Lizard"))lizard++;
                        }
                        if (scissors>lizard){
                            armas.remove("Scissors");
                            content = "Scissors";
                        }else {
                            content = "Lizard";
                            armas.remove("Lizard");
                        }

                    }else if(armas.contains("Paper")) {
                        content = "Paper";
                        armas.remove("Paper");
                    }
                }else if(minor==AcardsLizard){
                    if (armas.contains("Scissors") || armas.contains("Rock")) {
                        int scissors=0;
                        int rock=0;
                        for(int i = 0; i< armas.size();i++ ){
                            if(armas.get(i).equals("Scissors"))scissors++;
                            if(armas.get(i).equals("Rock"))rock++;
                        }
                        if (scissors>rock){
                            content = "Scissors";
                            armas.remove("Scissors");


                        }else {
                            content = "Rock";
                            armas.remove("Rock");
                        }

                    }else if(armas.contains("Lizard")){
                        content = "Lizard";
                        armas.remove("Lizard") ;
                    }
                }else if(minor==AcardsRock){
                    if (armas.contains("Paper") || armas.contains("Spock")) {
                        int paper=0;
                        int spock=0;
                        for(int i = 0; i< armas.size();i++ ){
                            if(armas.get(i).equals("Paper"))paper++;
                            if(armas.get(i).equals("Spock"))spock++;
                        }
                        if (paper>spock){
                            content = "Paper";
                            armas.remove("Paper");
                        }else{
                            content = "Spock";
                            armas.remove("Spock");
                        }

                    }else if(armas.contains("Rock")){
                        content = "Rock";
                        armas.remove("Rock") ;
                    }
                }else if(minor==AcardsScissors){
                    if (armas.contains("Rock") || armas.contains("Spock")) {
                        int rock=0;
                        int spock=0;
                        for(int i = 0; i< armas.size();i++ ){
                            if(armas.get(i).equals("Rock"))rock++;
                            if(armas.get(i).equals("Spock"))spock++;
                        }
                        if (rock>spock){
                            content = "Rock";
                            armas.remove("Rock");
                        }else {
                            content = "Spock";
                            armas.remove("Spock");
                        }

                    }else if(armas.contains("Scissors")){
                        content = "Scissors";
                        armas.remove("Scissors");
                    }
                }else if(minor==AcardsSpock){
                    if (armas.contains("Paper") || armas.contains("Scissors")) {
                        int paper=0;
                        int scissors=0;
                        for(int i = 0; i< armas.size();i++ ){
                            if(armas.get(i).equals("Paper"))paper++;
                            if(armas.get(i).equals("Scissors"))scissors++;
                        }
                        if (paper>scissors){
                            content = "Paper";
                            armas.remove("Paper");
                        }else{
                            content = "Scissors";
                            armas.remove("Scissors");
                        }

                    }else if(armas.contains("Spock")){
                        content = "Spock";
                        armas.remove("Spock");
                    }
                }


            } send(content);
        }





    }
    public void send(String content){
        ACLMessage m = new ACLMessage(ACLMessage.PROPOSE);
        m.addReceiver(mestre);
        if(content.equals(""))content=armas.remove(rand.nextInt(armas.size())).toString();
        m.setContent(content);
        send(m);
    }


}
