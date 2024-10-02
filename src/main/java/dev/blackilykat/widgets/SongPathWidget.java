/*
 * PMP-Desktop - A desktop client for Personal Music Platform, a
 * self-hosted platform to play music and make sure everything is
 * always synced across devices.
 * Copyright (C) 2024 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, inputField, (int) -confirmButton.getPreferredSize().getWidth()/2, SpringLayout.HORIZONTAL_CENTER, this);
        layout.putConstraint(SpringLayout.NORTH, inputField, 0, SpringLayout.NORTH, this);

        layout.putConstraint(SpringLayout.WEST, confirmButton, 0, SpringLayout.EAST, inputField);
        layout.putConstraint(SpringLayout.NORTH, confirmButton, 0, SpringLayout.NORTH, inputField);

        confirmButton.addActionListener(new ConfirmButtonListener());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.getParent().getWidth(), 30);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
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
