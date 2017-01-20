package org.bildbosnia.miniChat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerChat {
	
	private static final int PORT = 3000; 
	//sva imena klijenata koja su registrovana, registracije nije detaljna, samo pamti ime, onako radi reda odradjeno :)
	static ArrayList<String> clients = new ArrayList<>();
	static String filePath = "clients.txt";
	/*
	 * mapa sa svim ulovovanim ljudima
	 * sluzi da se ne mogu u jednom trenutku ulogovati dva korisnika sa istim nickom
	 * ujedno sadrzi i sve thradove i na osnovu nje saljemo svim korisnicima poruke
	 */
	static Map<String,HandleClient> usersIn = new HashMap<>();
	//brojac anonimusa, da  bi se mogli spremiti u mapu ulogovanih
	static int counter = 0;
	
	public static void main(String[] args) {
		
		ServerSocket server = null;
		try {
			server = new ServerSocket(PORT);
			System.out.println("Chat server pokrenut");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		loadUsers();
		
		while(true) {
			try {
				Socket socket = server.accept();
				new Thread(new HandleClient(socket)).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ucitava sva registrovana imena iz fajla u listu
	 */
	public static void loadUsers() {
		
		if (!Files.exists(Paths.get(filePath), LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.createFile(Paths.get(filePath));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath),StandardCharsets.UTF_8)) {
			String line;
			while ((line = br.readLine()) != null) {
				clients.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * dodaje novo ime u fajl
	 */
	public static void saveUser(String name) {
		
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filePath),StandardCharsets.UTF_8,StandardOpenOption.APPEND)){
			bw.write(name);
			bw.newLine();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class HandleClient implements Runnable {
	
	String name;
	Socket socket;
	DataInputStream fromClient;
	DataOutputStream toClient;
	boolean active = true;
	
	public HandleClient(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {

		try {
			String message = "";
			fromClient = new DataInputStream(socket.getInputStream());
			toClient = new DataOutputStream(socket.getOutputStream());
			
			toClient.writeUTF("Unesite ime(ili GUEST): ");
			message = fromClient.readUTF();
			if (message.equals("GUEST")) {
				name = message+""+(++ServerChat.counter);
			}
			else {
				while (ServerChat.usersIn.containsKey(message)) {
					toClient.writeUTF("Korisnicko ime zauzeto. Izaberite drugo ime.");
					message = fromClient.readUTF();
				}
				name = message;
				if (!ServerChat.clients.contains(name)) {
					ServerChat.clients.add(name);
					ServerChat.saveUser(name);
				}
			}
			ServerChat.usersIn.put(name, this);
			message = name + " dobrodosao/la u chat. Ako zelite da se odlgujete ukucajte LOGOUT";
			toClient.writeUTF(message);
			
			message = "Korisnik "+name + " se ulogovao";

			Set<Map.Entry<String,HandleClient>> entrySet = ServerChat.usersIn.entrySet();
			for (Map.Entry<String,HandleClient> entry: entrySet) {
				if(entry.getValue() != this)
					entry.getValue().toClient.writeUTF(message);
			}
				
			while(active) {
				message = fromClient.readUTF();
				if (message.equals("LOGOUT")) {
					toClient.writeUTF("Dovidjenja");
					message = "Korisnik "+name + " se izlogovao";
					ServerChat.usersIn.remove(name);
					fromClient.close();
					toClient.close();
					socket.close();
					active = false;
				}
				else {
					message = name +": "+ message;
				}
				for (Map.Entry<String,HandleClient> entry: entrySet) {
					if(entry.getValue() != this)
						entry.getValue().toClient.writeUTF(message);
				}				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
}
