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
    public static final String REPEAT = "repeat.svg";
    public static final String SHUFFLE = "shuffle.svg";
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
