package client.gui.calendar;

import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import common.dataobjects.ComMessage;

import client.connection.*;
import client.gui.CalendarPanel;
import client.gui.GuiConstants;
import common.utilities.MessageType;

@SuppressWarnings("serial")
public class Calendar extends JPanel implements MessageListener {

	private JTable table;
	private JScrollPane scrollPane;
	private JLabel weekLabel;
	private JButton lastWeek;
	private JButton nextWeek;
	private JTextField weekNumberField;
	private JTextField yearField;
	private CalModel calModel;

	private String[] dayName = {"", "Mandag", "Tirsdag", "Onsag", "Torsdag", "Fredag", "Lørdag", "Søndag"};

	public Calendar(CalendarPanel panel){
		setLayout(null);

		ServerData.getCalendar().getCalendar().setTimeInMillis(System.currentTimeMillis());

		calModel = new CalModel();
		weekLabel = new JLabel("Uke");
		lastWeek = new JButton("<");
		nextWeek = new JButton(">");
		weekNumberField = new JTextField(""+calModel.getWeek());
		yearField = new JTextField(""+calModel.getYear());

		table = new JTable();
		//table.addMouseListener(new JTableButtonMouseListener(table));

		CalRenderer renderer = new CalRenderer(panel);
		table.setModel(calModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setRowHeight(60);
		table.setDefaultRenderer(Object.class, renderer);
		table.getColumnModel().getColumn(0).setMaxWidth(45);
		table.getColumnModel().getColumn(0).setHeaderValue(dayName[0]);
		for(int i = 1; i < table.getModel().getColumnCount(); i++){
			TableColumn col = table.getColumnModel().getColumn(i);
			col.setMinWidth(134);
			col.setHeaderValue(dayName[i]);
		}

		lastWeek.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				calModel.lastWeek();
				weekNumberField.setText(""+calModel.getWeek());
				yearField.setText("" + calModel.getYear());
				repaint();

			}
		});

		nextWeek.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				calModel.nextWeek();
				weekNumberField.setText("" + calModel.getWeek());
				yearField.setText("" + calModel.getYear());
				repaint();
			}
		});

		weekNumberField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if(!weekNumberField.getText().equals((""+calModel.getWeek()))){
					calModel.setWeek(Integer.parseInt(weekNumberField.getText()));
					repaint();
				}
			}
		});

		yearField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (!yearField.getText().equals(("" + calModel.getYear()))) {
					calModel.setYear(Integer.parseInt(yearField.getText()));
					repaint();
				}
			}
		});

		scrollPane = new JScrollPane(table);

		resize();

		add(lastWeek);
		add(scrollPane);
		add(weekLabel);
		add(weekNumberField);
		add(yearField);
		add(nextWeek);
	}

	private void resize(){
		lastWeek.setBounds(GuiConstants.DISTANCE*30, GuiConstants.DISTANCE, 50, GuiConstants.BUTTON_HEIGTH);
		lastWeek.setFont(GuiConstants.BUTTON_FONT);
		weekLabel.setBounds(lastWeek.getX() + lastWeek.getWidth() + GuiConstants.DISTANCE, lastWeek.getY() + 5, 40, GuiConstants.LABEL_HEIGTH);
		weekLabel.setFont(GuiConstants.BUTTON_FONT);
		weekNumberField.setBounds(weekLabel.getX() + weekLabel.getWidth() + GuiConstants.DISTANCE, weekLabel.getY(), 50, GuiConstants.TEXTFIELD_HEIGTH);
		weekNumberField.setFont(GuiConstants.BUTTON_FONT);
		yearField.setBounds(weekNumberField.getX() + weekNumberField.getWidth() + GuiConstants.DISTANCE, weekNumberField.getY(), 70, GuiConstants.TEXTFIELD_HEIGTH);
		yearField.setFont(GuiConstants.BUTTON_FONT);
		nextWeek.setBounds(yearField.getX() + yearField.getWidth() + GuiConstants.DISTANCE, lastWeek.getY(), 50, GuiConstants.BUTTON_HEIGTH);
		nextWeek.setFont(GuiConstants.BUTTON_FONT);
		scrollPane.setBounds(0, lastWeek.getHeight() + lastWeek.getY() + GuiConstants.GROUP_DISTANCE + 5, 1002, 620);
	}

	@Override
	public void receiveMessage(ComMessage m){
		if(m.getType().equals(MessageType.RECEIVE_APPOINTMENTS) || m.getType().equals(MessageType.RECEIVE_MEETINGS)){
			table.repaint();
		}
	}
	class JTableButtonMouseListener implements MouseListener {

		JTable table;
		public JTableButtonMouseListener(JTable table){
			this.table = table;
		}
		@Override
		public void mouseClicked(MouseEvent e){
			CalRenderer renderer = (CalRenderer)table.getDefaultRenderer(Object.class);
			
			
			ArrayList<JButton> buttons = renderer.getButtons;
			//System.out.print("Clicked: " + e.getLocationOnScreen());
			//System.out.println("Buttons: " + renderer.getButtons.size());
			for(JButton b: buttons){
				Point p = b.getLocation();
				Point ep = e.getPoint();
				
				SwingUtilities.convertPoint(table, p, b);
				
				try{
				}catch (IllegalComponentStateException  is){
				}
			}	
		}
		public void mouseEntered(MouseEvent arg0){
		}
		public void mouseExited(MouseEvent arg0){
		}
		public void mousePressed(MouseEvent arg0){
		}
		public void mouseReleased(MouseEvent arg0){
		}
	}
}