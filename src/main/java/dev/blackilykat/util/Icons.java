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

package dev.blackilykat.util;

import au.id.mcc.adapted.swing.SVGIcon;
import dev.blackilykat.Main;
import org.apache.batik.transcoder.TranscoderException;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.net.URISyntaxException;

public class Icons {
    public static final String PLAY = "play.svg";
    public static final String PAUSE = "pause.svg";
    public static final String FORWARD = "forward.svg";
    public static final String BACKWARD = "backward.svg";
    public static final String REPEAT_ALL = "repeat.svg";
    public static final String REPEAT_ONE = "repeat-once.svg";
    public static final String REPEAT_OFF = "repeat-off.svg";
    public static final String SHUFFLE_ON = "shuffle.svg";
    public static final String SHUFFLE_OFF = "shuffle-disabled.svg";
    public static final String MENU = "menu.svg";

    public static Icon svgIcon(String name, int width, int height) {
        try {
            // cannot use URI.getPath() because for unexplainable reasons (probably explainable) it just returns null
            // when ran not in ide
            return new SVGIcon(Main.class.getResource("/" + name).toURI().toString(), width, height);
        } catch (TranscoderException | URISyntaxException e) {
            return UIManager.getIcon("OptionPane.errorIcon");
        }
    }
}
