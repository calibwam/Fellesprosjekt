package server;

import java.net.InetAddress;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import client.authentication.Login;
import client.connection.MessageType;
import dataobjects.Appointment;
import dataobjects.ComMessage;
import dataobjects.Meeting;
import dataobjects.Person;

public class MessageReceiver {

	private HashMap<InetAddress, ClientWriter> clients;
	private Database database;

	public MessageReceiver() {
		clients = new HashMap<InetAddress, ClientWriter>();
		database = new Database();
	}

	public synchronized void receiveMessage(InetAddress ip, ComMessage message){
		String messageType = message.getType();
		ClientWriter clientWriter = clients.get(ip);

		if(messageType.equals(MessageType.REQUEST_APPOINTMENTS_AND_MEETINGS)){

			Person p = (Person)message.getData();
			int personid = p.getPersonID();

			try{
				ResultSet appointmentResult = database.executeQuery(Queries.getAppointments(personid)); //Get the meetings where the person is a participants	
				ResultSet meetingResult = database.executeQuery(Queries.getMeetings(personid)); //Get the meetings where the person is a participants	
				
				Collection<Appointment> appointments = resultSetToAppointment(appointmentResult);
				Collection<Meeting> meetings = resultSetToMeeting(meetingResult);
				
				for(Meeting m: meetings){
					ServerConstants.console.writeline(m.getTitle());
					ServerConstants.console.writeline(m.getAppointmentLeader().getFirstname());
					for(Person pp : m.getParticipants().keySet()){
						ServerConstants.console.writeline("\t" + pp.getFirstname());
					}
				}
				
				ComMessage sendapp = new ComMessage(appointments, MessageType.RECEIVE_APPOINTMENTS);
				ComMessage sendmeet = new ComMessage(meetings, MessageType.RECEIVE_MEETINGS);
				
				clientWriter.send(sendapp);
				clientWriter.send(sendmeet);

			}catch(SQLException e){
				e.printStackTrace();
			}
		}
		else if(messageType.equals(MessageType.REQUEST_LOGIN)){
			Person authenticatedPerson = requestLogin(message);
			ComMessage sendLogin = new ComMessage(authenticatedPerson, MessageType.RECEIVE_LOGIN);
			clientWriter.send(sendLogin);
		}
	}
	
	private Person requestLogin(ComMessage message){
		Login login = (Login) message.getData();
		Person person;
		try{
			ResultSet personResult = database.executeQuery(Queries.loginAuthentication(login.getUserName(), login.getPasswordHash()));
			
			return person = resultSetToPerson(personResult).keySet().iterator().next();

			
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
	}

	private ArrayList<Meeting> resultSetToMeeting(ResultSet result){
		ArrayList<Meeting> returnthis = new ArrayList<Meeting>();
		try{
			while(result.next()){
				int id = result.getInt(Database.COL_APPOINTMENTID);
				int leaderid = result.getInt(Database.COL_LEADER);
				String title = result.getString(Database.COL_TITLE);
				String description = result.getString(Database.COL_DESCRIPTION);
				Date start = result.getDate(Database.COL_FROM);
				Date end = result.getDate(Database.COL_TO);

				ResultSet participantRes = database.executeQuery(Queries.getParticipantsForMeeting(id));
				HashMap<Person, Integer> participants = resultSetToPerson(participantRes);
				Person leader = null;
				for(Person p : participants.keySet()){
					if(p.getPersonID() == leaderid){
						leader = p;
					}
				}
				
				returnthis.add(new Meeting(id, leader, title, description, start, end, participants));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnthis;
	}
	
	private ArrayList<Appointment> resultSetToAppointment(ResultSet result){
		ArrayList<Appointment> returnthis = new ArrayList<Appointment>();
		try{
			while(result.next()){
				int id = result.getInt(Database.COL_APPOINTMENTID);
				String title = result.getString(Database.COL_TITLE);
				String description = result.getString(Database.COL_DESCRIPTION);
				Date start = result.getDate(Database.COL_FROM);
				Date end = result.getDate(Database.COL_TO);

				ResultSet participantRes = database.executeQuery(Queries.getParticipantsForMeeting(id));
				Person leader = resultSetToPerson(participantRes).keySet().iterator().next();
				
				returnthis.add(new Appointment(id, leader, title, description, start, end));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnthis;
	}

	private HashMap<Person, Integer> resultSetToPerson(ResultSet rs){
		HashMap<Person, Integer> returnthis = new HashMap<Person, Integer>();
		try{
			while(rs.next()){
				int id = rs.getInt(Database.COL_PERSONID);
				String fornavn = rs.getString(Database.COL_FORNAVN);
				String etternavn = rs.getString(Database.COL_ETTERNAVN);
				String epost = rs.getString(Database.COL_EPOST);
				String brukernavn = rs.getString(Database.COL_BRUKERNAVN);
				String tlf = rs.getString(Database.COL_TLF);
				int svar = rs.getInt(Database.COL_ANSWER);

				returnthis.put(new Person(id, fornavn, etternavn, epost, brukernavn, tlf), svar);
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnthis;
	}

	public synchronized void addClient(ClientWriter clientWriter){
		clients.put(clientWriter.getIP(), clientWriter);
	}

	public synchronized void sendToAll(ComMessage message){
		ServerConstants.console.writeline("send to all");
		for (ClientWriter client : clients.values()) {
			ServerConstants.console.writeline("send to: " + client.getIP());
			client.send(message);
		}
	}
}
