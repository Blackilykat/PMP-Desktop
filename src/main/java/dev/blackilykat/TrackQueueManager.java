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

import dev.blackilykat.util.Icons;
import dev.blackilykat.widgets.playbar.PlayBarWidget;

import java.util.Random;
import java.util.Stack;

/**
 * Picks which tracks to play in which order, and whether to stop after playing one.
 */
public class TrackQueueManager {
    private final Library library;
    private Random random = new Random();

    private Stack<Library.Track> previousTracks = new Stack<>();
    public Library.Track currentTrack = null;

    //TODO forced queue where user can add a track to a queue and it will take priority over the normal next tracks

    // this is a stack because if the user goes to a previous track they should be able to get back. normally it would
    // only have 1 element
    private Stack<Library.Track> nextTracks = new Stack<>();

    private ShuffleOption shuffle = ShuffleOption.OFF;
    private RepeatOption repeat = RepeatOption.OFF;


    public TrackQueueManager(Library library) {
        this.library = library;
    }

    public void nextTrack() {
        if(library.filteredTracks.isEmpty()) return;
        if(currentTrack != null) {
            previousTracks.push(currentTrack);
        }
        currentTrack = nextTracks.isEmpty() ? pickNext() : nextTracks.pop();
        Library.Track next = pickNext();
        if(nextTracks.isEmpty() && next != null) {
            nextTracks.push(next);
        }
    }

    public void previousTrack() {
        if(previousTracks.isEmpty()) return;
        if(currentTrack != null) {
            nextTracks.push(currentTrack);
        }
        currentTrack = previousTracks.pop();
    }

    public void setCurrentTrack(Library.Track track) {
        if(currentTrack != null) {
            previousTracks.push(currentTrack);
        }
        currentTrack = track;
        reloadNext();
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
    }

    public RepeatOption getRepeat() {
        return repeat;
    }

    private Library.Track pickNext() {
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
}
