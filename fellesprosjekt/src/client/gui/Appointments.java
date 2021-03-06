package client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.Client;
import client.connection.MessageListener;
import client.connection.ServerData;

import com.toedter.calendar.JDateChooser;
import common.dataobjects.Appointment;
import common.dataobjects.ComMessage;
import common.dataobjects.Meeting;
import common.utilities.DateString;
import common.utilities.MessageType;

@SuppressWarnings("serial")
public class Appointments extends JPanel implements MessageListener{

	private JDateChooser datepickerFromDate;
	private JDateChooser datepickerToDate;

	private JLabel headlineLabel;
	private JLabel dateLabel;
	private JLabel startDateLabel;
	private JLabel endDateLabel;

	private JCheckBox appointmentCheckBox;
	private JCheckBox meetingCheckBox;

	private JTextField startDateField;
	private JTextField endDateField;

	private JList list;
	private DefaultListModel listModel;

	private JButton toCalendarButton;

	private JScrollPane listScrollPane;

	private CalendarPanel calendarpanel;

	private LinkedList<Appointment> appointments;
	private LinkedList<Meeting> meetings;
	private LinkedList<Appointment> nyliste = new LinkedList<Appointment>();

	public Appointments(CalendarPanel calendarPanel) {
		appointments = new LinkedList<Appointment>();
		meetings = new LinkedList<Meeting>();
		nyliste = new LinkedList<Appointment>();

		calendarpanel = calendarPanel;

		datepickerFromDate = new JDateChooser();
		datepickerFromDate.getJCalendar().setDate(new Date(System.currentTimeMillis()));
		datepickerFromDate.repaint();
		datepickerToDate = new JDateChooser();
		datepickerToDate.getJCalendar().setDate(new Date(System.currentTimeMillis() + 1000*60*60*24*7));
		datepickerToDate.repaint();


		datepickerFromDate.getJCalendar().addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent arg0) {
				changeDateFilter();
			}
		});
		datepickerToDate.getJCalendar().addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {

				String dateStart = datepickerFromDate.getJCalendar().getCalendar().get(Calendar.YEAR) + 
						"-" + (datepickerFromDate.getJCalendar().getCalendar().get(Calendar.MONTH) + 1) + 
						"-" + datepickerFromDate.getJCalendar().getCalendar().get(Calendar.DAY_OF_MONTH) + " 00:00:00";
				String dateEnd = datepickerToDate.getJCalendar().getCalendar().get(Calendar.YEAR) + 
						"-" + (datepickerToDate.getJCalendar().getCalendar().get(Calendar.MONTH) + 1) + 
						"-" + datepickerToDate.getJCalendar().getCalendar().get(Calendar.DAY_OF_MONTH) + " 23:59:59";

				DateString ds = new DateString(dateStart);
				DateString de = new DateString(dateEnd);
				
//				if(ds.after(de)){
//					datepickerToDate.setDate(datepickerFromDate.getDate());
//					return;
//				}
				
				changeDateFilter();
			}
		});

		headlineLabel = new JLabel("Mine Avtaler");
		dateLabel = new JLabel("Dato:");
		startDateLabel = new JLabel("Fra"); 	
		endDateLabel = new JLabel("Til");		

		appointmentCheckBox = new JCheckBox("Personlige Avtaler");
		meetingCheckBox = new JCheckBox("Møter");
		appointmentCheckBox.setSelected(true);
		meetingCheckBox.setSelected(true);		

		appointmentCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				displaydata();
			}
		});
		meetingCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displaydata();
			}
		});

		startDateField = new JTextField();
		endDateField = new JTextField();

		list = new JList(new DefaultListModel());
		listModel = (DefaultListModel)list.getModel();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				int i = list.getSelectedIndex();
				if(i >= 0 ){
					if(list.getSelectedValue() instanceof Meeting)
						calendarpanel.goToAppointmentView((Meeting)listModel.getElementAt(i));
					else{
						calendarpanel.goToAppointmentView((Appointment)listModel.getElementAt(i));
					}
				}
			}
		});

		ServerData.addMessageListener(this);

		listScrollPane = new JScrollPane(list);

		toCalendarButton = new JButton("Til Kalender");
		toCalendarButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				calendarpanel.goToCalender();
			}
		});

		displaydata();
		add(headlineLabel);
		add(dateLabel);
		add(startDateLabel);
		add(endDateLabel);
		add(appointmentCheckBox);
		add(meetingCheckBox);
		add(startDateField);
		add(endDateField);
		add(toCalendarButton);
		add(listScrollPane);
		add(datepickerFromDate);
		add(datepickerToDate);

		setLayout(null);
		resize();
		Client.getFrame().resize(GuiConstants.FRAME_WIDTH+1, GuiConstants.FRAME_HEIGTH+1);
		Client.getFrame().resize(GuiConstants.FRAME_WIDTH, GuiConstants.FRAME_HEIGTH);
		
		changeDateFilter();
	}

	public void resize(){
		headlineLabel.setBounds(GuiConstants.HEADLINE_X, GuiConstants.HEADLINE_Y, GuiConstants.HEADLINE_WIDTH, GuiConstants.HEADLINE_HEIGTH);
		headlineLabel.setFont(GuiConstants.FONT_30);

		startDateLabel.setBounds(headlineLabel.getX() - GuiConstants.LABEL_WIDTH - GuiConstants.DISTANCE, headlineLabel.getY() + headlineLabel.getHeight() + GuiConstants.GROUP_DISTANCE , GuiConstants.LABEL_WIDTH, GuiConstants.LABEL_HEIGTH);
		startDateLabel.setFont(GuiConstants.FONT_16);
		startDateLabel.setHorizontalAlignment(JLabel.RIGHT);

		datepickerFromDate.setBounds(startDateLabel.getX() + startDateLabel.getWidth() + GuiConstants.DISTANCE, startDateLabel.getY(), GuiConstants.TEXTFIELD_WIDTH, GuiConstants.TEXTFIELD_HEIGTH);

		endDateLabel.setBounds(datepickerFromDate.getX() + datepickerFromDate.getWidth() + GuiConstants.DISTANCE, datepickerFromDate.getY(), 20, GuiConstants.TEXTFIELD_HEIGTH);
		endDateLabel.setFont(GuiConstants.FONT_16);
		startDateLabel.setHorizontalAlignment(JLabel.RIGHT);

		datepickerToDate.setBounds(endDateLabel.getX() + endDateLabel.getWidth() + GuiConstants.DISTANCE, endDateLabel.getY(), GuiConstants.TEXTFIELD_WIDTH, GuiConstants.TEXTFIELD_HEIGTH);

		appointmentCheckBox.setBounds(datepickerFromDate.getX(), startDateLabel.getY() + startDateLabel.getHeight() + GuiConstants.GROUP_DISTANCE, GuiConstants.TEXTFIELD_WIDTH, GuiConstants.TEXTFIELD_HEIGTH);
		appointmentCheckBox.setFont(GuiConstants.FONT_14);

		meetingCheckBox.setBounds(datepickerFromDate.getX(), appointmentCheckBox.getY() + appointmentCheckBox.getHeight() + GuiConstants.DISTANCE, GuiConstants.TEXTFIELD_WIDTH, GuiConstants.TEXTFIELD_HEIGTH);
		meetingCheckBox.setFont(GuiConstants.FONT_14);

		listScrollPane.setBounds(meetingCheckBox.getX(), meetingCheckBox.getY() + meetingCheckBox.getHeight() + GuiConstants.DISTANCE, 400 , 250);

		list.setBounds(listScrollPane.getX(), listScrollPane.getY(), listScrollPane.getWidth() - 5 , listScrollPane.getHeight());

		toCalendarButton.setBounds(listScrollPane.getX(), listScrollPane.getY() + listScrollPane.getHeight() + GuiConstants.DISTANCE , GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGTH);
	}

	@Override
	public void receiveMessage(ComMessage m) {
		if(m.getType().equals(MessageType.RECEIVE_APPOINTMENTS_BY_DATE_FILTER )){
			appointments.clear();
			for (Appointment a : (ArrayList<Appointment>)m.getData()) {
				appointments.add(a);
			}
		}
		if(m.getType().equals(MessageType.RECEIVE_MEETINGS_BY_DATE_FILTER)){
			meetings.clear();
			for (Meeting me : (ArrayList<Meeting>)m.getData()) {
				meetings.add(me);
			}
		}
		displaydata();
	}
	private void displaydata(){
		listModel.clear();
		nyliste.clear();
		if(meetingCheckBox.isSelected()){
			nyliste.addAll(meetings);
		}
		if(appointmentCheckBox.isSelected()){
			nyliste.addAll(appointments);
		}
		Collections.sort(nyliste);
		for (Appointment appointment : nyliste) {		
			listModel.addElement(appointment);
		}
		Client.getFrame().resize(GuiConstants.FRAME_WIDTH+1, GuiConstants.FRAME_HEIGTH+1);
		Client.getFrame().resize(GuiConstants.FRAME_WIDTH, GuiConstants.FRAME_HEIGTH);
	}

	private void changeDateFilter(){
		String dateStart = datepickerFromDate.getJCalendar().getCalendar().get(Calendar.YEAR) + 
				"-" + (datepickerFromDate.getJCalendar().getCalendar().get(Calendar.MONTH) + 1) + 
				"-" + datepickerFromDate.getJCalendar().getCalendar().get(Calendar.DAY_OF_MONTH) + " 00:00:00";
		String dateEnd = datepickerToDate.getJCalendar().getCalendar().get(Calendar.YEAR) + 
				"-" + (datepickerToDate.getJCalendar().getCalendar().get(Calendar.MONTH) + 1) + 
				"-" + datepickerToDate.getJCalendar().getCalendar().get(Calendar.DAY_OF_MONTH) + " 23:59:59";

		ServerData.requestAppointmensAndMeetingByDateFilter(Client.getUser(), new DateString(dateStart), new DateString(dateEnd));
	}



}
