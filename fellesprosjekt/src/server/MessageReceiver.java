package server;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import server.database.Database;
import server.database.Queries;
import client.authentication.Login;

import common.dataobjects.Appointment;
import common.dataobjects.ComMessage;
import common.dataobjects.Meeting;
import common.dataobjects.Note;
import common.dataobjects.Person;
import common.dataobjects.Room;
import common.sendobjects.AppointmentInvites;
import common.utilities.DateString;
import common.utilities.MessageType;

public class MessageReceiver {

	private HashMap<InetAddress, ClientWriter> ipClients;
	private HashMap<Integer, ClientWriter> idClients;
	private Database database;

	public MessageReceiver() {
		ipClients = new HashMap<InetAddress, ClientWriter>();
		idClients = new HashMap<Integer, ClientWriter>();
		database = new Database();
		database.connect();
	}

	public synchronized void receiveMessage(InetAddress ip, ComMessage message){
		String messageType = message.getType();
		ClientWriter clientWriter = ipClients.get(ip);

		if(messageType.equals(MessageType.REQUEST_APPOINTMENTS_AND_MEETINGS)){

			Person p = (Person)message.getData();
			int personid = p.getId();

			try{
				ResultSet appointmentResult = database.executeQuery(Queries.getAppointments(personid)); //Get the appointments where the person is a participants	
				ResultSet meetingResult = database.executeQuery(Queries.getMeetings(personid)); //Get the meetings where the person is a participants	

				Collection<Appointment> appointments = resultSetToAppointment(appointmentResult, p);
				Collection<Meeting> meetings = resultSetToMeeting(meetingResult, p);

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
			if(authenticatedPerson != null){
				idClients.put(authenticatedPerson.getId(), clientWriter);
				clientWriter.id = authenticatedPerson.getId();
			}
		}
		else if(messageType.equals(MessageType.REQUEST_MEETINGS_AND_APPOINTMENTS_BY_DATE_FILTER)){
			meetingsandappointmentsbydate(message, clientWriter);
		}
		else if(messageType.equals(MessageType.REQUEST_NEW_APPOINTMENT)){
			newAppointment(clientWriter, message);
		}
		else if(messageType.equals(MessageType.REQUEST_NEW_MEETING)){
			newMeeting(message);
		}
		else if(messageType.equals(MessageType.REQUEST_SEARCH_PERSON)){
			ArrayList<Person> filteredPersons = searchForPerson(message);
			ComMessage sendPersons = new ComMessage(filteredPersons, MessageType.RECEIVE_SEARCH_PERSON);
			clientWriter.send(sendPersons);
		}
		else if(messageType.equals(MessageType.REQUEST_ADD_ATTENDANT)){
			addAttendant(message);
		}
		else if(messageType.equals(MessageType.REQUEST_PARTICIPANTS)){
			ArrayList<Person> persons = searchForMeetingParticipants(message);
			ComMessage getParticipants = new ComMessage(persons, MessageType.RECEIVE_PARTICIPANTS);
			clientWriter.send(getParticipants);
		}
		else if(messageType.equals(MessageType.REQUEST_UPDATE_ANSWER)){
			//TODO
			
			
			
			String s = (String)message.getData();
			String[] ss = s.split(":");
			int pid = Integer.parseInt(ss[0]);
			int ans = Integer.parseInt(ss[1]);
			int appid = Integer.parseInt(ss[2]);
			
			System.out.println("server:" + pid + " " + appid + " " + ans);
			
			try{
				database.updateDB(Queries.updateAnswerToInvite(pid, appid, ans));
			}catch(SQLException e){
				e.printStackTrace();
			}
			
			
//			Meeting meettoup = (Meeting) message.getData();
//			
//			
//			System.out.println(meettoup.getId());
//			System.out.println("server: " + meettoup.getAnswers());
//			for(Integer pid : meettoup.getAnswers().keySet()){
//				try{
//					database.updateDB(Queries.updateAnswerToInvite(pid, meettoup.getId(), meettoup.getAnswers().get(pid)));
//				}catch(SQLException e){
//					e.printStackTrace();
//				}
//			}
		}
		else if(messageType.equals(MessageType.REQUEST_UPDATE_APPOINTMENT)){
			Appointment a = (Appointment) message.getData();
			try{
				database.updateDB(Queries.updateAppointment(a.getId(), a.getTitle(), a.getDescription(), a.getStartTime(), a.getEndTime(), a.getPlace()));
			}
			catch(SQLException e){
				e.printStackTrace();
			}

		}
		else if (messageType.equals(MessageType.REQUEST_UPDATE_MEETING)) {
			System.out.println(MessageType.REQUEST_UPDATE_MEETING);
			Meeting um = (Meeting) message.getData();
			try {
				for (Integer pid : um.getAnswers().keySet()) {
					database.updateDB(Queries.updatePersonToAttend(pid, um.getId()));
				}
				database.updateDB(Queries.updateMeeting(um.getId(), um.getTitle(), um.getDescription(), um.getStartTime(), um.getEndTime(), um.getPlace(), um.getRoom() == null ? null :um.getRoom().getRomId()));

				database.updateDB(Queries.updateNote(um.getId()));

				ResultSet noters = database.executeQuery(Queries.getNoteByAppId(um.getId()));
				Note n = resultSetToSingleNote(noters);

				for(Integer pid : um.getAnswers().keySet()){
					database.updateDB(Queries.deleteParticipant(pid, um.getId()));
					database.updateDB(Queries.addPersonToAttend(pid, um.getId()));
					
					if(pid != um.getLeader().getId()){
						database.updateDB(Queries.deleteNoteForPerson(n.getID(), pid));
						database.updateDB(Queries.addNoteToPerson(pid, n.getID()));
					}
					
					ClientWriter cw = idClients.get(pid);
					if(cw != null && pid != um.getLeader().getId()){
						cw.send(new ComMessage(null, MessageType.WARNING));
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if(messageType.equals(MessageType.REQUEST_ROOMS_AVAILABLE)){
			ArrayList<Room> availableRooms = getAvaliableRooms(message);
			ComMessage rooms = new ComMessage(availableRooms, MessageType.RECEIVE_ROOM_AVAILABLE);
			clientWriter.send(rooms);
		}
		else if(messageType.equals(MessageType.REQUEST_BOOK_ROOM)){
			Meeting meeting = (Meeting) message.getData();
			try{
				database.updateDB(Queries.bookRoom(meeting.getId(),meeting.getRoom().getRomId()));
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
		else if (messageType.equals(MessageType.REQUEST_NOTES)) {
			Person p = (Person)message.getData();
			try {
				ResultSet rs = database.executeQuery(Queries.getNotes(p.getId(), message.getProperty("filter")));
				ArrayList<Note> result = resultSetToNotes(rs, p);
	
				clientWriter.send(new ComMessage(result, MessageType.RECEIVE_NOTES));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if (messageType.equals(MessageType.DELETE_NOTE)) {
			@SuppressWarnings("unchecked")
			ArrayList<Note> n = (ArrayList<Note>)message.getData();
			for (Note note : n) {
				try {
					database.updateDB(Queries.deleteNoteForPerson(note.getID(), note.getPersonId()));
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		}
		else if (messageType.equals(MessageType.GET_OLD_NEW_NOTES)) {
			Person p = (Person) message.getData();
			try {
				ResultSet rs = database.executeQuery(Queries.oldNewNotes(p.getId()));
				int i = 0;
				while (rs.next()) {
					i++;
				}
				if (i > 0) {
					clientWriter.send(new ComMessage(null, MessageType.WARNING));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else if (messageType.equals(MessageType.DELETE_APPOINTMENT)) {
			Appointment app = (Appointment) message.getData();
			try {
				database.updateDB(Queries.deleteAppointment(app.getId()));

				database.updateDB(Queries.createNote("Avlyst:" + app.getTitle(), -1));
				ResultSet noters = database.executeQuery(Queries.getLastNote());

				Note n = resultSetToSingleNote(noters);

				if (app instanceof Meeting) {
					Meeting newMeet = (Meeting) app;
					for(Integer pid : newMeet.getAnswers().keySet()){
						if(pid != newMeet.getLeader().getId()){
							database.updateDB(Queries.addNoteToPerson(pid, n.getID()));
							ClientWriter cw = idClients.get(pid);
							if(cw != null){
								cw.send(new ComMessage(null, MessageType.WARNING));
							}
						}
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		else if (messageType.equals(MessageType.UPDATE_NOTE_AS_READ)) {
			Note n = (Note) message.getData();
			try {
				database.updateDB(Queries.upDateNoteToPerson(n.getPersonId(), n.getID()));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		else if (messageType.equals(MessageType.DELETE_PARTICIPANTS)) {
			@SuppressWarnings("unchecked")
			ArrayList<Person> arr =  (ArrayList<Person>) message.getData();
			for (Person p : arr) {
				int personID = p.getId();;
				try {
					database.updateDB(Queries.deleteParticipant(personID, Integer.parseInt(message.getProperty("id"))));
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private ArrayList<Note> resultSetToNotes(ResultSet rs, Person p){
		
		ArrayList<Note> notes = new ArrayList<Note>(); 
		try {
			int numnotes = 0;
			while (rs.next()) {
				
				String title = rs.getString(Database.COL_TITLE);
				int varselID = rs.getInt(Database.COL_VARSELID);
				Timestamp timesend = rs.getTimestamp(Database.COL_TIMESEND);
				int appointmentID = rs.getInt(Database.COL_APPOINTMENTID);
				boolean hasRead = rs.getBoolean(Database.COL_HASREAD);

				if(appointmentID == -1){
					Note n = new Note(varselID, title, new DateString(timesend), null, hasRead);
					notes.add(n);
				}else{
					ArrayList<Meeting> appointment = resultSetToMeeting(database.executeQuery(Queries.getAppointmentById(appointmentID)), p);
					if(appointment != null && appointment.size() > 0){
						Note n = new Note(varselID, title, new DateString(timesend), appointment.get(0), hasRead);
						notes.add(n);
					}else{
						Note n = new Note(varselID, title, new DateString(timesend), null, hasRead);
						notes.add(n);
					}
				}
				numnotes++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return notes;
	}

	private Note resultSetToSingleNote(ResultSet rs){
		Note notes = null;
		try {
			while (rs.next()) {
				String title = rs.getString(Database.COL_TITLE);
				int varselID = rs.getInt(Database.COL_VARSELID);
				Timestamp timesend = rs.getTimestamp(Database.COL_TIMESEND);

				notes = new Note(varselID, title, new DateString(timesend), null, false);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return notes;
	}

	private void meetingsandappointmentsbydate(ComMessage message, ClientWriter cw) {
		Person p = (Person)message.getData();

		DateString startd = new DateString(message.getProperty("dstart"));
		DateString endd = new DateString(message.getProperty("dend"));

		int personid = p.getId();

		try{
			ResultSet appointmentResult = database.executeQuery(Queries.getAppointmentsByDate(personid, startd, endd)); //Get the appointments where the person is a participants	
			ResultSet meetingResult = database.executeQuery(Queries.getMeetingsByDate(personid, startd, endd)); //Get the meetings where the person is a participants	

			ArrayList<Appointment> appointments = resultSetToAppointment(appointmentResult, p);
			ArrayList<Meeting> meetings = resultSetToMeeting(meetingResult, p);

			ComMessage sendapp = new ComMessage(appointments, MessageType.RECEIVE_APPOINTMENTS_BY_DATE_FILTER);
			ComMessage sendmeet = new ComMessage(meetings, MessageType.RECEIVE_MEETINGS_BY_DATE_FILTER);

			cw.send(sendapp);
			cw.send(sendmeet);

		}catch(SQLException e){
			e.printStackTrace();
		}

	}


	private ArrayList<Person> searchForMeetingParticipants(ComMessage message){
		Appointment query = (Appointment) message.getData();
		try{
			ResultSet rs = database.executeQuery(Queries.getParticipantsForMeeting(query.getId()));
			ArrayList<Person> persons = resutlSetToPerson(rs);
			return persons;
		}
		catch(SQLException e){
			e.printStackTrace();
		}
		return null;
	}

	private ArrayList<Room> getAvaliableRooms(ComMessage message){
		Meeting meeting = (Meeting) message.getData();
		try{
			ResultSet rs = database.executeQuery(Queries.getRoomsForTimeSlot(meeting.getStartTime(), meeting.getEndTime(), meeting.getNumberOfParticipants()));
			return resultSetToRooms(rs);
		}catch(SQLException e){
			e.printStackTrace();
		}
		return null;
	}

	private Person requestLogin(ComMessage message){
		Login login = (Login) message.getData();
		try{
			ResultSet personResult = database.executeQuery(Queries.loginAuthentication(login.getUserName(), login.getPasswordHash()));

			return resultSetToLoginPerson(personResult);

		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
	}

	private void newAppointment(ClientWriter from, ComMessage message){
		Appointment newApp = (Appointment) message.getData();
		try{
			database.updateDB(Queries.createNewAppointment(newApp.getTitle(), newApp.getDescription(), newApp.getStartTime(), newApp.getEndTime(),newApp.getPlace(), newApp.getLeader().getId()));
			ResultSet rs = database.executeQuery(Queries.getLastAppointment());

			Appointment appo = resultSetToAppointment(rs, newApp.getLeader()).get(0);
			ComMessage comMesNewApp = new ComMessage(appo, MessageType.RECEIVE_NEW_APPOINTMENT);
			database.updateDB(Queries.addPersonToAttend(appo.getLeader().getId(), appo.getId()));
			from.send(comMesNewApp);
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	private void newMeeting(ComMessage message){
		Meeting newMeet = (Meeting) message.getData();
		try{
			database.updateDB(Queries.createNewMeeting(newMeet.getTitle(), newMeet.getDescription(), newMeet.getStartTime(), newMeet.getEndTime(),newMeet.getPlace(), newMeet.getRoom() == null ? null : newMeet.getRoom().getRomId(), newMeet.getLeader().getId()));
			ResultSet rs = database.executeQuery(Queries.getLastMeeting());

			Meeting meeti = resultSetToMeeting(rs, newMeet.getLeader()).get(0);

			database.updateDB(Queries.createNote("Invitasjon:" + meeti.getTitle(), meeti.getId()));
			ResultSet noters = database.executeQuery(Queries.getLastNote());

			Note n = resultSetToSingleNote(noters);

			for(Integer pid : newMeet.getAnswers().keySet()){
				database.updateDB(Queries.addNoteToPerson(pid, n.getID()));
				database.updateDB(Queries.addPersonToAttend(pid, meeti.getId()));
				ClientWriter cw = idClients.get(pid);
				if(cw != null && pid != newMeet.getLeader().getId()){
					cw.send(new ComMessage(null, MessageType.WARNING));
				}
			}
			database.updateDB(Queries.addPersonToAttend(meeti.getLeader().getId(), meeti.getId()));			
			if(meeti.getRoom() != null){
				database.updateDB(Queries.bookRoom(meeti.getId(), meeti.getRoom().getRomId()));
			}

		}catch(SQLException e){
			e.printStackTrace();
		}
	}

	private ArrayList<Person> searchForPerson(ComMessage message){
		String query = (String) message.getData();
		try{
			return resutlSetToPerson(database.executeQuery(Queries.getPersonsByFilter(query)));
		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
	}

//	private ArrayList<Note> searchForNotes(ComMessage message){
//		String query = (String) message.getData();
//		int pid = Integer.parseInt(message.getProperty("person"));
//		try {
//			return resultSetToNotes(database.executeQuery(Queries.getNotes(pid, query)), null);
//		} catch (SQLException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	private void addAttendant(ComMessage message){
		AppointmentInvites received = (AppointmentInvites) message.getData();
		ArrayList<Person> invited = received.getInvited();
		Appointment appointment = received.getAppointment();
		for(Person p : invited){
			try{
				database.updateDB(Queries.addPersonToAttend(p.getId(), appointment.getId()));
			}
			catch(SQLException e){
				e.printStackTrace();
			}
		}
	}

	private ArrayList<Meeting> resultSetToMeeting(ResultSet result, Person p){
		ArrayList<Meeting> returnthis = new ArrayList<Meeting>();
		try{
			while(result.next()){
				int id = result.getInt(Database.COL_APPOINTMENTID);
				String title = result.getString(Database.COL_TITLE);
				String description = result.getString(Database.COL_DESCRIPTION);
				String place = result.getString(Database.COL_PLACE);
				String room = result.getString(Database.COL_ROOM);
				DateString start = new DateString(result.getTimestamp(Database.COL_FROM));
				DateString end = new DateString(result.getTimestamp(Database.COL_TO));
				int external = result.getInt(Database.COL_EXTERNAL);

				ResultSet participantRes = database.executeQuery(Queries.getAnsFromParticipants(id));
				HashMap<Person, Integer> participants = resultSetToPersonWithAnswer(participantRes);

				Person leader = resutlSetToPerson(database.executeQuery(Queries.getLeaderForMeeting(id))).get(0);

				ArrayList<Room> rom = resultSetToRooms(database.executeQuery(Queries.getRoom(room)));
				Room orom = rom.size() > 0 ? rom.get(0) : null;

				returnthis.add(new Meeting(id, leader, p, title, description, place, orom, start, end, participants, external));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnthis;
	}

	private ArrayList<Room> resultSetToRooms(ResultSet rs){
		ArrayList<Room> returnThis = new ArrayList<Room>();
		try{
			while(rs.next()){
				String room = rs.getString(Database.COL_ROMID);
				int cap = rs.getInt(Database.COL_ROMKAPASITET);
				returnThis.add(new Room(room, cap));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnThis;
	}

	private ArrayList<Appointment> resultSetToAppointment(ResultSet result, Person p){
		ArrayList<Appointment> returnthis = new ArrayList<Appointment>();
		try{
			while(result.next()){
				int id = result.getInt(Database.COL_APPOINTMENTID);
				String title = result.getString(Database.COL_TITLE);
				String description = result.getString(Database.COL_DESCRIPTION);
				String place = result.getString(Database.COL_PLACE);
				DateString start = new DateString(result.getTimestamp(Database.COL_FROM));
				DateString end = new DateString(result.getTimestamp(Database.COL_TO));

				Person leader = resutlSetToPerson(database.executeQuery(Queries.getLeaderForMeeting(id))).get(0);

				returnthis.add(new Appointment(id, leader, p, title, description, place, start, end));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnthis;
	}

	private HashMap<Person, Integer> resultSetToPersonWithAnswer(ResultSet rs){
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

	private ArrayList<Person> resutlSetToPerson(ResultSet rs){
		ArrayList<Person> returnSet = new ArrayList<Person>();
		try{
			while(rs.next()){
				int id = rs.getInt(Database.COL_PERSONID);
				String fornavn = rs.getString(Database.COL_FORNAVN);
				String etternavn = rs.getString(Database.COL_ETTERNAVN);
				String epost = rs.getString(Database.COL_EPOST);
				String brukernavn = rs.getString(Database.COL_BRUKERNAVN);
				String tlf = rs.getString(Database.COL_TLF);

				returnSet.add(new Person(id, fornavn, etternavn, epost, brukernavn, tlf));
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return returnSet;
	}

	private Person resultSetToLoginPerson(ResultSet rs){
		try{
			while(rs.next()){
				int id = rs.getInt(Database.COL_PERSONID);
				String fornavn = rs.getString(Database.COL_FORNAVN);
				String etternavn = rs.getString(Database.COL_ETTERNAVN);
				String epost = rs.getString(Database.COL_EPOST);
				String brukernavn = rs.getString(Database.COL_BRUKERNAVN);
				String tlf = rs.getString(Database.COL_TLF);
				return new Person(id, fornavn, etternavn, epost, brukernavn, tlf);
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return null;
	}

	public synchronized void addClient(ClientWriter clientWriter){
		ipClients.put(clientWriter.getIP(), clientWriter);
	}

	public synchronized void removeClient(InetAddress inetAddress){
		idClients.remove(ipClients.get(inetAddress).id);
		ipClients.remove(inetAddress);
	}

}
