package hospital;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Medico {
    private static final int PORT = 4446;
    private static ArrayList<String> topicosDisponiveis = new ArrayList<>();
    private MulticastSocket socket;
    private List<String> gruposSelecionados = new ArrayList<>();
    private InetAddress ia;
    private boolean running = true;
    private int escolha;
    private String nomeRemetente;
    private Boolean loop = true;

    Scanner scanner = new Scanner(System.in);

    public Medico() throws IOException {
        this.socket = new MulticastSocket(PORT);
    }
    
    public void entrada() throws IOException{
    	addNome();
    	escolherTopico(); 
    }

	public void escolherTopico() throws IOException {
		topicosDisponiveis.add("230.0.0.1");
		topicosDisponiveis.add("230.0.0.2");
		topicosDisponiveis.add("230.0.0.3");
        
		while(loop) {
	        System.out.println("Escolha um tópico (ou digite 0 para sair):");
	        for (int i = 0; i < topicosDisponiveis.size(); i++) {
	            System.out.println((i + 1) + ". " + traduzir(topicosDisponiveis.get(i)));
	        }
	        
	        escolha = scanner.nextInt();
	        scanner.nextLine();
	        
	        if (escolha == 0) {
	            System.out.println("Encerrando aplicação.");
	            this.running = false;
	            return;
	        }
	        
	        if (escolha > 0 && escolha <= topicosDisponiveis.size()) {
	            String enderecoTopico = topicosDisponiveis.get(escolha - 1);
	            gruposSelecionados.add(enderecoTopico);
	        } else {
	            System.out.println("Escolha inválida. Por favor, escolha um número de tópico válido.");
	        }
	        
	        System.out.println("Deseja se juntar a outro grupo?\r\n"
	        		+ "1- Sim\r\n"
	        		+ "2- Não");
	        String continuar = scanner.nextLine();
	        
	        topicosDisponiveis.remove(topicosDisponiveis.get(escolha - 1));
	        
	        if(continuar.equals("2")){
	        	loop = false; 
	        }
		}
        for(String grupo : gruposSelecionados) {
            this.ia = InetAddress.getByName(grupo);
            InetSocketAddress isa = new InetSocketAddress(ia, PORT);
            NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
            
            this.socket.joinGroup(isa, ni);            

        	String header = "entrada " + grupo + " " + nomeRemetente; 
        	byte[] buffer = header.getBytes();
        	DatagramPacket messageOut = new DatagramPacket(buffer, buffer.length, ia, PORT);
            socket.send(messageOut);      
            
            Thread thread = new Thread(new ReceberMensagens());
            thread.start(); 
            
        }        

        if(gruposSelecionados.contains("230.0.0.3")) {
        	System.out.println("Você pode enviar mensagens para o chat agora (digite '/sair' para sair do tópico");
        }else {       	            	
        	System.out.println("Você se juntou a grupos apenas de aviso (digite '/sair' para sair do tópico");
        }
        
        Boolean flag = true;                
        while(flag) {                	
            String lido = scanner.nextLine();
            if(lido.equals("/saida")) {
            	System.out.println("Escolha um dos tópicos para sair:");
    	        for (int i = 0; i < gruposSelecionados.size(); i++) {
    	            System.out.println((i + 1) + ". " + traduzir(gruposSelecionados.get(i)));
    	        }
    	        int saida = scanner.nextInt();
    	        
    	        this.ia = InetAddress.getByName(gruposSelecionados.get(saida-1));
                InetSocketAddress isa = new InetSocketAddress(ia, PORT);
                NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
                
                sairTopico(isa, ni);
                gruposSelecionados.remove(gruposSelecionados.get(saida-1));
                
                if(gruposSelecionados.isEmpty()) {
                	flag = false; 
                	System.out.println("Você não está mais em num grupo. Programa será encerrado.");
                }
            }else {
            	if(gruposSelecionados.contains("230.0.0.3")) {
            		SimpleDateFormat dateFormat = new SimpleDateFormat("[dd/MM/yyyy - HH:mm]");
            		String mensagemFormatada ="Chat" + dateFormat.format(new Date()) + " " + nomeRemetente + " : " + lido;
                    byte[] buffer = mensagemFormatada.getBytes();
                    DatagramPacket messageOut = new DatagramPacket(buffer, buffer.length, ia, PORT);
                    socket.send(messageOut);
            	}else {
            		continue;
            	} 
            }
        }
        
    }
    
    private void addNome () {
        System.out.println("Informe seu nome (ou identificador):");
        nomeRemetente = scanner.nextLine();
    }
    
    public class ReceberMensagens implements Runnable{
		@Override
		public void run() {
			while (running) {
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(messageIn);
                    String received = new String(messageIn.getData(), 0, messageIn.getLength());
                    String[] palavras = received.split("\\s+");
                    if(palavras[0].equals("entrada")) {
                    	continue; 
                    }else {
                        System.out.println(received);
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao receber mensagem: " + e.getMessage());
                }			
			}
		}
    }

   private void sairTopico(InetSocketAddress isa, NetworkInterface ni) throws IOException {
        socket.leaveGroup(isa, ni);;
        System.out.println("Você saiu do tópico.");
    }
   
   private static String traduzir(String endereco) {
	   String nomeGrupo = null; 
	   if(endereco.equals("230.0.0.1")) {
		   nomeGrupo = "Avisos Gerais";
	   }else if(endereco.equals("230.0.0.2")){
		   nomeGrupo = "Avisos de Emergência";
	   }else if(endereco.equals("230.0.0.3")) {
		   nomeGrupo = "Chat";		   
	   }
	   return nomeGrupo; 
   }

    public static void main(String[] args) throws IOException {
        new Medico().entrada();
    }
}
