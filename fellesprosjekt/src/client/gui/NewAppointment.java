package client.gui;

import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.toedter.calendar.JDateChooser;

public class NewAppointment extends JPanel{
	
	private JDateChooser datepicker;
	
	private JLabel headlineLabel;
	private JLabel titleLabel;
	private JLabel dateLabel;
	private JLabel startTimeLabel;
	private JLabel endTimeLabel;
	private JLabel descriptionLabel;
	private JLabel placeLabel;
	private JLabel roomInformationLabel;
	
	private JScrollPane scrollPane;
	
	private JTextField nameField; 
	private JTextField dateField; //DATEPICKER
	private JTextField placeField;
	private JTextArea descriptionArea;
	
	private JComboBox startTimeHoursField;
	private JComboBox endTimeHoursField;
	private JComboBox startTimeMinField;
	private JComboBox endTimeMinField;
	
	private JButton saveButton;
	private JButton cancelButton;

	public NewAppointment(){
		String[] min = {"00", "15", "30", "45"};
		String[] hours= {"00","01","02","03","04","05","06","07","08","09","10",
						"11","12","13","14","15","16","17","18","19","20","21","22","23"};
		
		headlineLabel = new JLabel("Ny Avtale");
		titleLabel = new JLabel("Tittel", SwingConstants.RIGHT);
		dateLabel = new JLabel("Dato", SwingConstants.RIGHT);
		startTimeLabel = new JLabel("Fra", SwingConstants.RIGHT);
		endTimeLabel = new JLabel("Til", SwingConstants.RIGHT);
		descriptionLabel = new JLabel("Beskrivelse", SwingConstants.RIGHT);
		placeLabel = new JLabel("Sted", SwingConstants.RIGHT);
		roomInformationLabel =  new JLabel("(min 2 deltakere)", SwingConstants.RIGHT);
				
		
		nameField = new JTextField();
		dateField = new JTextField();
		datepicker = new JDateChooser();
		placeField = new JTextField();
		
		descriptionArea = new JTextArea();
		descriptionArea.setLineWrap(true);
		
		saveButton = new JButton("Lagre");
		cancelButton = new JButton("Avbryt");
		
		startTimeHoursField = new JComboBox(hours);
		endTimeHoursField = new JComboBox(hours);
		startTimeMinField = new JComboBox(min);
		endTimeMinField = new JComboBox(min);
		
		scrollPane = new JScrollPane(descriptionArea);
		
		add(scrollPane);
		add(headlineLabel);
		add(titleLabel);
		add(dateLabel);
		add(startTimeLabel);
		add(endTimeLabel);
		add(descriptionLabel);
		add(placeLabel);
		add(roomInformationLabel);
		add(nameField);
		add(dateField);
		add(placeField);
	//	add(descriptionArea);
		add(saveButton);
		add(cancelButton);
		add(startTimeHoursField);
		add(startTimeMinField);
		add(endTimeHoursField);
		add(endTimeMinField);
		add(datepicker);
		
		setLayout(null);
		resize();
		
	}
	
	public void resize(){
		
		headlineLabel.setBounds(GuiConstants.DISTANCE*27, GuiConstants.GROUP_DISTANCE, GuiConstants.HEADLINE_LENGTH, GuiConstants.HEADLINE_HEIGTH);
		headlineLabel.setFont(new Font(headlineLabel.getFont().getName(), 0, 30));
		
		titleLabel.setBounds(GuiConstants.DISTANCE, headlineLabel.getY() + headlineLabel.getHeight() + GuiConstants.GROUP_DISTANCE, 100, GuiConstants.LABEL_HEIGTH);
		titleLabel.setFont(new Font(headlineLabel.getFont().getName(), 0, 16));
		
		nameField.setBounds(GuiConstants.DISTANCE*2+ titleLabel.getWidth() + titleLabel.getX(), titleLabel.getY(), 190, GuiConstants.TEXTFIELD_HEIGTH);
		
		dateLabel.setBounds(titleLabel.getX(), titleLabel.getY() + titleLabel.getHeight() + GuiConstants.DISTANCE, 100, GuiConstants.LABEL_HEIGTH);
		dateLabel.setFont(new Font(dateLabel.getFont().getName(), 0, 16));
		
		datepicker.setBounds(GuiConstants.DISTANCE*2 + titleLabel.getWidth() + titleLabel.getX(), dateLabel.getY(), 190, GuiConstants.TEXTFIELD_HEIGTH);
		
		startTimeLabel.setBounds(titleLabel.getX(), dateLabel.getY() + dateLabel.getHeight() + GuiConstants.GROUP_DISTANCE, 100, GuiConstants.LABEL_HEIGTH);
		startTimeLabel.setFont(new Font(startTimeLabel.getFont().getName(), 0, 16));
		
		startTimeHoursField.setBounds(GuiConstants.DISTANCE*2+ titleLabel.getWidth() + titleLabel.getX() , startTimeLabel.getY(), 70, GuiConstants.TEXTFIELD_HEIGTH);
		startTimeMinField.setBounds(startTimeHoursField.getX() + 2 + startTimeHoursField.getWidth(), startTimeHoursField.getY(), 70, GuiConstants.TEXTFIELD_HEIGTH);
		
		endTimeLabel.setBounds(startTimeHoursField.getX() + startTimeMinField.getWidth() + GuiConstants.DISTANCE, startTimeHoursField.getY(), 100, GuiConstants.LABEL_HEIGTH);
		endTimeLabel.setFont(new Font(endTimeLabel.getFont().getName(), 0, 16));
		
		endTimeHoursField.setBounds(endTimeLabel.getX() + endTimeLabel.getWidth() + GuiConstants.DISTANCE, endTimeLabel.getY(), 70, GuiConstants.TEXTFIELD_HEIGTH);
		endTimeMinField.setBounds(endTimeHoursField.getX() + 2 + endTimeHoursField.getWidth(), endTimeHoursField.getY(), 70, GuiConstants.TEXTFIELD_HEIGTH);
		
		descriptionLabel.setBounds(titleLabel.getX(), startTimeLabel.getY() + startTimeLabel.getHeight() + GuiConstants.GROUP_DISTANCE, 100, GuiConstants.LABEL_HEIGTH);
		descriptionLabel.setFont(new Font(descriptionLabel.getFont().getName(), 0, 16));
		
		scrollPane.setBounds(GuiConstants.DISTANCE*2+ titleLabel.getWidth() + titleLabel.getX(), descriptionLabel.getY(),  
				(endTimeMinField.getX() + endTimeMinField.getWidth())-startTimeHoursField.getX(), 100);
		
		placeLabel.setBounds(titleLabel.getX(), scrollPane.getY() + scrollPane.getHeight() + GuiConstants.GROUP_DISTANCE, 100, GuiConstants.LABEL_HEIGTH);
		placeLabel.setFont(new Font(placeLabel.getFont().getName(), 0, 16));
		
		placeField.setBounds(scrollPane.getX(), placeLabel.getY(), 190, GuiConstants.TEXTFIELD_HEIGTH);
		
		saveButton.setBounds(placeField.getX(), placeField.getY() + placeField.getHeight() + GuiConstants.GROUP_DISTANCE, 100, GuiConstants.BUTTON_HEIGTH);
		saveButton.setFont(new Font(saveButton.getFont().getName(), 0, 14));
		
		cancelButton.setBounds(saveButton.getX() + saveButton.getWidth() + GuiConstants.DISTANCE, saveButton.getY(), 100, GuiConstants.BUTTON_HEIGTH);
		cancelButton.setFont(new Font(cancelButton.getFont().getName(), 0, 14));
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("NewMeeting");
		NewAppointment appointment = new NewAppointment();
		frame.add(appointment);
		frame.getContentPane();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setSize(700, 700);
		frame.setVisible(true);
	}
	
	
	
}
