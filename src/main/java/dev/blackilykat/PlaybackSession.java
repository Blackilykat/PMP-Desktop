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

package dev.blackilykat;

import dev.blackilykat.messages.PlaybackSessionUpdateMessage;
import dev.blackilykat.util.Icons;
import dev.blackilykat.widgets.playbar.PlayBarWidget;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class PlaybackSession {
    private static final List<PlaybackSession> availableSessions = new ArrayList<>();
    private static final List<SessionListener> registerListeners = new ArrayList<>();
    private final List<SessionListener> unregisterListeners = new ArrayList<>();

    private final Library library;
    private Random random = new Random();

    private Stack<Track> previousTracks = new Stack<>();
    private Track currentTrack = null;

    //TODO forced queue where user can add a track to a queue and it will take priority over the normal next tracks

    // this is a stack because if the user goes to a previous track they should be able to get back. normally it would
    // only have 1 element
    private Stack<Track> nextTracks = new Stack<>();

    private ShuffleOption shuffle = ShuffleOption.OFF;
    private RepeatOption repeat = RepeatOption.OFF;
    private final List<SessionListener> updateListeners = new ArrayList<>();

    private boolean playing;
    private int position;
    public int lastSharedPosition = 0;
    public Instant lastSharedPositionTime = null;

    // not final cause it'll default to 0 when offline but let the server decide when connected
    public int id;

    private int ownerId = -1;

    public boolean acknowledgedByServer = false;

    public PlaybackSession(Library library, int id) {
        this.library = library;
        this.id = id;
        playing = false;
        position = 0;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position, boolean isJump) {
        this.position = position;
        if(isJump) {
            PlaybackSessionUpdateMessage.doUpdate(id, null, null, null, null, position, null);
            callUpdateListeners();
        }
    }

    public void recalculatePosition(Instant atTime) {
        int offset = 0;
        if(playing && lastSharedPositionTime != null) {
            offset = (int) (((atTime.toEpochMilli() - lastSharedPositionTime.toEpochMilli()) * 44100 * 4) / 1000);
        }
        offset -= offset % 4;
        setPosition(lastSharedPosition + offset, true);
    }

    public boolean getPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        PlaybackSessionUpdateMessage.doUpdate(id, null, null, null, playing, null, null);
        if(Audio.INSTANCE.currentSession == this) {
            PlayBarWidget.setPlaying(playing);
        }
        callUpdateListeners();
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
        if(acknowledgedByServer) {
            PlaybackSessionUpdateMessage.doUpdate(id, null, null, null, null, null, ownerId);
        }
        callUpdateListeners();
    }

    public void register() {
        availableSessions.add(this);
        registerListeners.forEach(l -> l.run(this));
    }

    public void unregister() {
        availableSessions.remove(this);
        unregisterListeners.forEach(l -> l.run(this));
    }

    public void registerUpdateListener(SessionListener listener) {
        updateListeners.add(listener);
    }

    public void callUpdateListeners() {
        updateListeners.forEach(l -> l.run(this));
    }

    public static PlaybackSession[] getAvailableSessions() {
        return availableSessions.toArray(new PlaybackSession[0]);
    }

    public static void registerRegisterListener(SessionListener listener) {
        registerListeners.add(listener);
    }
    public void registerUnregisterListener(SessionListener listener) {
        unregisterListeners.add(listener);
    }

    public interface SessionListener {
        void run(PlaybackSession session);
    }

    public void nextTrack() {
        if(library.filteredTracks.isEmpty()) return;
        if(currentTrack != null) {
            previousTracks.push(currentTrack);
        }
        currentTrack = nextTracks.isEmpty() ? pickNext() : nextTracks.pop();
        Track next = pickNext();
        if(nextTracks.isEmpty() && next != null) {
            nextTracks.push(next);
        }
        this.callUpdateListeners();
    }

    public void previousTrack() {
        if(previousTracks.isEmpty()) return;
        if(currentTrack != null) {
            nextTracks.push(currentTrack);
        }
        currentTrack = previousTracks.pop();
        this.callUpdateListeners();
    }

    public void setCurrentTrack(Track track) {
        if(currentTrack != null) {
            previousTracks.push(currentTrack);
        }
        currentTrack = track;
        reloadNext();
        this.callUpdateListeners();
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public void reloadNext() {
        nextTracks.clear();
    }

    public void clearPrevious() {
        previousTracks.clear();
    }

    public void setShuffle(ShuffleOption option) {
        shuffle = option;
        PlayBarWidget.shuffleButton.setIcon(switch(shuffle) {
            case ON -> Icons.svgIcon(Icons.SHUFFLE_ON, 16, 16);
            case OFF -> Icons.svgIcon(Icons.SHUFFLE_OFF, 16, 16);
        });
        reloadNext();
        PlaybackSessionUpdateMessage.doUpdate(id, null, option, null, null, null, null);
        callUpdateListeners();
    }

    public ShuffleOption getShuffle() {
        return shuffle;
    }

    public void setRepeat(RepeatOption option) {
        repeat = option;
        PlayBarWidget.repeatButton.setIcon(switch(repeat) {
            case ALL -> Icons.svgIcon(Icons.REPEAT_ALL, 16, 16);
            case TRACK -> Icons.svgIcon(Icons.REPEAT_ONE, 16, 16);
            case OFF -> Icons.svgIcon(Icons.REPEAT_OFF, 16, 16);
        });
        reloadNext();
        PlaybackSessionUpdateMessage.doUpdate(id, null, null, option, null, null, null);
        callUpdateListeners();
    }

    public RepeatOption getRepeat() {
        return repeat;
    }

    private Track pickNext() {
        System.out.println("Filtered tracks size: " + library.filteredTracks.size());
        System.out.println("First filtered track: " + library.filteredTracks.getFirst());
        System.out.println("Last filtered track: " + library.filteredTracks.getLast());
        System.out.println("Current track: " + currentTrack);
        System.out.println("Shuffle: " + shuffle);
        System.out.println("Repeat: " + repeat);
        if(library.filteredTracks.isEmpty()) return null;
        if(currentTrack == null) {
            if(shuffle == ShuffleOption.ON) {
                return library.filteredTracks.get(random.nextInt(library.filteredTracks.size()));
            } else {
                return library.filteredTracks.getFirst();
            }
        } else {
            if(repeat == RepeatOption.TRACK) {
                return currentTrack;
            } else if(shuffle == ShuffleOption.ON) {
                return library.filteredTracks.get(random.nextInt(library.filteredTracks.size()));
            } else if(library.filteredTracks.getLast() != currentTrack) {
                return library.filteredTracks.get(library.filteredTracks.indexOf(currentTrack) + 1);
                //  == is intentional
            } else if(library.filteredTracks.getLast() == currentTrack && repeat == RepeatOption.ALL) {
                return library.filteredTracks.getFirst();
            }
        }
        return null;
    }




    public enum ShuffleOption {
        ON,
        OFF
    }

    public enum RepeatOption {
        /**
         * Repeat this track. Takes priority over shuffle.
         */
        TRACK,
        /**
         * When at the end of the track list, get back to the start. Does nothing if shuffle is on.
         */
        ALL,
        /**
         * When at the end of the track list, stop playing.
         */
        OFF
    }

    // temporary
    @Override
    public String toString() {

        return "Session{\n" +
                "currentTrack: " + currentTrack + "\n" +
                "shuffle: " + shuffle + "\n" +
                "repeat: " + repeat + "\n" +
                "playing: " + playing + "\n" +
                "position: " + position + "\n" +
                "lastSharedPosition: " + lastSharedPosition + "\n" +
                "lastSharedPositionTime: " + lastSharedPositionTime + "\n" +
                "id: " + id + "\n" +
                "ownerId: " + ownerId + "\n" +
                "acknowledgedByServer: " + acknowledgedByServer + "\n" +
                "}";
    }
}
