package dev.blackilykat.widgets;

import dev.blackilykat.Audio;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SongPathWidget extends Widget {
    public static JTextField inputField = new JTextField("WAV file path", 30);
    public static JButton confirmButton = new JButton("Confirm");

    public SongPathWidget() {
        this.add(inputField);
        this.add(confirmButton);

        layout.putConstraint(SpringLayout.WEST, inputField, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, inputField, 0, SpringLayout.NORTH, this);

        layout.putConstraint(SpringLayout.WEST, confirmButton, 0, SpringLayout.EAST, inputField);
        layout.putConstraint(SpringLayout.NORTH, confirmButton, 0, SpringLayout.NORTH, inputField);

        confirmButton.addActionListener(new ConfirmButtonListener());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(500, 30);
    }

    public static class ConfirmButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String path = inputField.getText();
            System.out.println("Playing song: " + path);
            Audio.INSTANCE.startPlaying(path);
        }
    }
}
