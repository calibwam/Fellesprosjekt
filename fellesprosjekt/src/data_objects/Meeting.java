package data_objects;

import java.sql.Date;
import java.util.HashMap;

public class Meeting extends Appointment{
	
	
	private Room room;
	//Integer er svaret fra person
	private HashMap<Person, Integer> participants;
	private int externalParticipants;
	
	public Meeting(String title, Date start, Date end, HashMap<Person, Integer> participants){
		super(title, start, end);
		this.participants = participants;
	}

	public Room getRoom() {
		return room;
	}
	
	public void setRoom(Room room) {
		this.room = room;
	}
	
	public HashMap<Person, Integer> getParticipants() {
		return participants;
	}
	
	//Sjekke om deltakeren er med fra f�r?
	public void addParticipant(Person person, int answear) {
		participants.put(person, answear);
	}
	
	public void removeParticipants(Person person){
		participants.remove(person);
	}
	
	public int getParticipantsCount(){
		return participants.size() + externalParticipants;
	}
	
	public void changeCountExternalParticipants(int num){
		this.externalParticipants = num;
	}
	
	
}