package testclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class Main {

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException{
		Socket socket = new Socket("localhost", 4536);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		
		writer.write("Mari");
		writer.newLine();
		writer.flush();
		String s = reader.readLine();
		
		
		while (true) {
			System.out.println(s);
			if (s == null) {
				break;
			}
			s = reader.readLine();
			
		}
		
		socket.close();
	}	

}//END
