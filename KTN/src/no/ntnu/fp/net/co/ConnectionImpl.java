/*
 * Created on Oct 27, 2004
 */
package no.ntnu.fp.net.co;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import no.ntnu.fp.net.cl.ClException;
import no.ntnu.fp.net.cl.ClSocket;
import no.ntnu.fp.net.cl.KtnDatagram;
import no.ntnu.fp.net.cl.KtnDatagram.Flag;

/**
 * Implementation of the Connection-interface. <br>
 * <br>
 * This class implements the behaviour in the methods specified in the interface
 * {@link Connection} over the unreliable, connectionless network realised in
 * {@link ClSocket}. The base class, {@link AbstractConnection} implements some
 * of the functionality, leaving message passing and error handling to this
 * implementation.
 * 
 * @author Sebj�rn Birkeland and Stein Jakob Nordb�
 * @see no.ntnu.fp.net.co.Connection
 * @see no.ntnu.fp.net.cl.ClSocket
 */
public class ConnectionImpl extends AbstractConnection {

	/** Keeps track of the used ports for each server port. */
	private static Map<Integer, Boolean> usedPorts = Collections.synchronizedMap(new HashMap<Integer, Boolean>());

	private int randomPort(){
		int nextport;
		do{
			nextport = 1050 + (int)(Math.random()*63050);
		}while(usedPorts.containsKey(nextport));
		return nextport;
	}


	/**
	 * Initialise initial sequence number and setup state machine.
	 * 
	 * @param myPort
	 *            - the local port to associate with this connection
	 */
	public ConnectionImpl(int myPort) {
		usedPorts.put(myPort, true);
		this.myPort = myPort;
		this.myAddress = getIPv4Address();
	}

	private String getIPv4Address() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		}catch (UnknownHostException e) {
			return "127.0.0.1";
		}
	}

	/**
	 * Establish a connection to a remote location.
	 * 
	 * @param remoteAddress
	 *            - the remote IP-address to connect to
	 * @param remotePort
	 *            - the remote portnumber to connect to
	 * @throws IOException
	 *             If there's an I/O error.
	 * @throws java.net.SocketTimeoutException
	 *             If timeout expires before connection is completed.
	 * @see Connection#connect(InetAddress, int)
	 */
	public void connect(InetAddress remoteAddress, int remotePort) throws IOException, SocketTimeoutException {
		if(state != State.CLOSED){
			throw new IOException("Attempted connect on connected socket");
		}
		this.remoteAddress = remoteAddress.getHostAddress();
		this.remotePort = remotePort;

		try{
			KtnDatagram syn = constructInternalPacket(Flag.SYN);
			state = State.SYN_SENT;
			KtnDatagram synack = sendPacketWithTimeout(syn);
			this.remotePort = synack.getSrc_port();

			sendAck(synack, false);
			state = State.ESTABLISHED;

		}catch(IOException e){
			state = State.CLOSED;
			throw new IOException("Could not connect!");
		}
	}

	/**
	 * Listen for, and accept, incoming connections.
	 * 
	 * @return A new ConnectionImpl-object representing the new connection.
	 * @see Connection#accept()
	 */
	public Connection accept() throws IOException, SocketTimeoutException {
		if(state != State.CLOSED){
			throw new IOException("Attempted accept on connected socket");
		}
		
		while(true){
			state = State.LISTEN;

			KtnDatagram syn = null;

			syn = receivePacket(true);
			while(!isValid(syn)){
				syn = receivePacket(true);
			}

			ConnectionImpl c = new ConnectionImpl(randomPort());
			c.state = State.SYN_RCVD;
			c.remotePort = syn.getSrc_port();
			c.remoteAddress = syn.getSrc_addr();
			c.sendAck(syn, true);
		
			//TODO hvis vi ikke får cack er den andre enden connaca mens denne ikke er det, vet ikke hvordan dette skal fikses
			//derfor er det unødvendig med whileløkka den men på en annen side burde den ikke returne med mindre den faktisk har noe
			KtnDatagram cack = c.receiveAck();
			if(cack != null){
				c.state = State.ESTABLISHED;
				state = State.CLOSED;
				return c;
			}
		}
	}

	/**
	 * Send a message from the application.
	 * 
	 * @param msg
	 *            - the String to be sent.
	 * @throws ConnectException
	 *             If no connection exists.
	 * @throws IOException
	 *             If no ACK was received.
	 * @see AbstractConnection#sendDataPacketWithRetransmit(KtnDatagram)
	 * @see no.ntnu.fp.net.co.Connection#send(String)
	 */
	public void send(String msg) throws ConnectException, IOException {
		if(state != State.ESTABLISHED){
			throw new IOException("Attempted send on disconnected socket");
		}
		KtnDatagram tosend = constructDataPacket(msg);
		lastDataPacketSent = tosend;
		KtnDatagram ack = sendDataPacketWithRetransmit(tosend);
		int again = 5;
		while(!isValid(ack)){
			if(ack == null){
				if(again > 0){
					ack = sendDataPacketWithRetransmit(tosend);
					again--;
				}else{
					throw new IOException("Connection lost");
				}
				//TODO disconnect?? eller kanskje ikke, connection er jo lost, kanskje prøve uansett?
			}else{
				ack = sendDataPacketWithRetransmit(tosend);
			}
		}
	}

	/**
	 * Wait for incoming data.
	 * 
	 * @return The received data's payload as a String.
	 * @see Connection#receive()
	 * @see AbstractConnection#receivePacket(boolean)
	 * @see AbstractConnection#sendAck(KtnDatagram, boolean)
	 */
	public String receive() throws ConnectException, IOException {
		if(state != State.ESTABLISHED){
			throw new IOException("Attempted receive on disconnected socket");
		}
		
		KtnDatagram packet = null;
		try{
			do{
				packet = receivePacket(false);
			}while(!isValid(packet));
		}catch(EOFException e){
			//TODO GOT FIN, disconnect
		}

		sendAck(packet, false);
		lastValidPacketReceived = packet;
		return packet.getPayload().toString();
	}


	private KtnDatagram sendPacketWithTimeout(KtnDatagram packet) throws IOException {
		int maxAttempts = 5;
		int attempts = 0;
		while(attempts < maxAttempts){
			try{
				simplySendPacket(packet);
			}catch(ClException e){
				//prøver igjen
			}
			KtnDatagram ack = null;
			try{
				ack = receiveAck();
			}catch(EOFException e){
				//TODO got FIN, disconnect
			}
			
			if(ack == null || !isValid(ack)){
				attempts++;
			}else{
				return ack;
			}
		}
		throw new IOException("Failed to send packet, connection timed out");
	}

	/**
	 * Close the connection.
	 * 
	 * @see Connection#close()
	 */
	public void close() throws IOException {
		state = State.CLOSED;
	}

	/**
	 * Test a packet for transmission errors. This function should only called
	 * with data or ACK packets in the ESTABLISHED state.
	 * 
	 * @param packet
	 *            Packet to test.
	 * @return true if packet is free of errors, false otherwise.
	 */
	protected boolean isValid(KtnDatagram packet) {
		if(packet == null){
			System.err.println("*******************************************NULLPAKKE");
			return false;
		}

		if(packet.calculateChecksum() != packet.getChecksum()){
			System.err.println("*******************************************INVALID CHECKSUM");
			return false;
		}

		if(packet.getFlag() == Flag.NONE && packet.getPayload() == null){
			System.err.println("*******************************************NO DATA IN DATA PACKET");
			return false;
		}

		if(state == State.LISTEN && packet.getFlag() != Flag.SYN){
			return false;
		}
		
		//TODO sjekke at pakker kommer fra remoteaddr og port???

		if(lastDataPacketSent != null && packet.getFlag() == Flag.ACK && packet.getAck() != lastDataPacketSent.getSeq_nr()){
			System.err.println("*******************************************WRONG ACKSEQ");
			return false;
		}
		
		if(lastValidPacketReceived != null && packet.getFlag() == Flag.NONE && packet.getSeq_nr() < lastValidPacketReceived.getSeq_nr()){
			System.err.println("*******************************************OUT OF ORDER");
			return false;
		}
			
		return true;
	}
}
