package org.bildbosnia.miniChat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientChat {
	static Socket socket = null;
	static boolean active = true;
	DataOutputStream toServer;
	DataInputStream fromServer;
	

	public static void main(String[] args) throws Exception {
		
		int port = 3000;
	
		Socket socket = new Socket("localhost", port);
		DataOutputStream toServer = new DataOutputStream(socket.getOutputStream());
		
		Scanner input = new Scanner(System.in);
		String message;
		
		new Thread(new InputListener(socket)).start();
		while(active) {
			message = input.nextLine();
			if (message.equals("LOGOUT")) {
				toServer.writeUTF(message);
				active=false;
			}
			toServer.writeUTF(message);			
		}
		input.close();
	}
	
}
class InputListener implements Runnable{
	 
	Socket socket;
	DataInputStream fromServer;
	boolean active = true;
	
	InputListener(Socket socket) {
		this.socket = socket;
	}
		
	@Override
	public void run() {
		try {
			fromServer = new DataInputStream(socket.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
		while(active) {
			try {
				String message = fromServer.readUTF();
				System.out.println(message);
				if (message.equals("Dovidjenja")) {
					active=false;
					fromServer.close();
					socket.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


}
