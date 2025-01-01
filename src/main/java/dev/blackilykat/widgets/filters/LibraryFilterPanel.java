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

package dev.blackilykat.widgets.filters;

import dev.blackilykat.Main;
import dev.blackilykat.widgets.ScrollablePanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryFilterPanel extends JPanel {
    public LibraryFilter filter;
    private LibraryFiltersWidget widget = null;
    SpringLayout layout = new SpringLayout();
    JLabel header = new JLabel();
    JPanel optionsContainer = new ScrollablePanel();
    JScrollPane optionsScrollContainer = new JScrollPane(optionsContainer);
    JPopupMenu popup;

    public LibraryFilterPanel(LibraryFilter filter, LibraryFiltersWidget widget) {
        this.filter = filter;
        this.widget = widget;
        if(filter.panel == null) {
            filter.panel = this;
        }

        optionsScrollContainer.getVerticalScrollBar().setUnitIncrement(16);

        this.setLayout(layout);

        this.add(header);
        header.setText(filter.key.substring(0, 1).toUpperCase() + filter.key.substring(1));
        layout.putConstraint(SpringLayout.NORTH, header, 0, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.WEST, header, 0, SpringLayout.WEST, this);

        this.add(optionsScrollContainer);
        layout.putConstraint(SpringLayout.NORTH, optionsScrollContainer, 0, SpringLayout.SOUTH, header);
        layout.putConstraint(SpringLayout.WEST, optionsScrollContainer, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, optionsScrollContainer, 0, SpringLayout.EAST, this);
        layout.putConstraint(SpringLayout.SOUTH, optionsScrollContainer, 0, SpringLayout.SOUTH, this);

        filter.reloadMatching();

        popup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if(widget != null) {
                    widget.panels.remove(LibraryFilterPanel.this);
                    filter.library.filters.remove(filter);
                    widget.reloadElements();
                    filter.library.reloadFilters();
                } else {
                    System.err.println("WARNING: Attempted to delete widgetless filter! Ignoring");
                }
            }
        });
        popup.add(deleteItem);
        popup.add(widget.getAddFilterPopupItem());

        MouseListener popupListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if(e.isPopupTrigger()) {
                    popup.show(LibraryFilterPanel.this, e.getX(), e.getY());
                }
            }
        };
        this.addMouseListener(popupListener);
        optionsContainer.addMouseListener(popupListener);
    }

    public void reloadOptions() {
        optionsContainer.removeAll();

        List<LibraryFilterOption> options = new ArrayList<>(Arrays.stream(filter.getOptions()).sorted().toList());

        LibraryFilterOption everything = new LibraryFilterOption(filter, LibraryFilter.OPTION_EVERYTHING);
        if(options.contains(everything)) {
            everything = filter.getOption(LibraryFilter.OPTION_EVERYTHING);
            options.remove(everything);
            options.addFirst(everything);
        }

        LibraryFilterOption unknown = new LibraryFilterOption(filter, LibraryFilter.OPTION_UNKNOWN);
        if(options.contains(unknown)) {
            unknown = filter.getOption(LibraryFilter.OPTION_UNKNOWN);
            options.remove(unknown);
            options.addLast(unknown);
        }

        for(LibraryFilterOption option : options) {
            optionsContainer.add(new OptionButton(option));
        }
    }

    public static class OptionButton extends JButton {
        public static final Color POSITIVE_BACKGROUND_COLOR = new Color(0, 255, 0, 100);
        public static final Color NEGATIVE_BACKGROUND_COLOR = new Color(255, 0, 0, 100);
        public static final Color NEUTRAL_BACKGROUND_COLOR = new Color(0, 0, 0, 10);
        public static final Color MOUSE_DOWN_BACKGROUND_COLOR = new Color(0, 0, 127, 70);
        public final LibraryFilterOption option;
        private boolean mouseDown = false;

        public OptionButton(LibraryFilterOption option) {
            this.option = option;
            if(option.button == null) {
                option.button = this;
            }
            String buttonText = switch(this.option.value) {
                case LibraryFilter.OPTION_EVERYTHING -> "All";
                case LibraryFilter.OPTION_UNKNOWN -> "Unknown";
                default -> this.option.value;
            };
            this.setText(buttonText);
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    mouseDown = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    mouseDown = false;
                    repaint();

                    if(e.getButton() == MouseEvent.BUTTON1) {
                        if(option.state == LibraryFilterOption.State.POSITIVE) {
                            option.state = LibraryFilterOption.State.NONE;
                        } else {
                            option.state = LibraryFilterOption.State.POSITIVE;
                        }
                    } else if(e.getButton() == MouseEvent.BUTTON3) {
                        // negative everything doesn't really make sense
                        if(option.value.equals(LibraryFilter.OPTION_EVERYTHING)) {
                            return;
                        }
                        if(option.state == LibraryFilterOption.State.NEGATIVE) {
                            option.state = LibraryFilterOption.State.NONE;
                        } else {
                            option.state = LibraryFilterOption.State.NEGATIVE;
                        }
                    }

                    // no reason to have no filters enabled, intent of disabling is probably to enable all
                    // for the same logic, if you have all enabled and enable something else you probably intend to
                    // only select the thing you're clicking on
                    if(!option.value.equals(LibraryFilter.OPTION_EVERYTHING)) {
                        boolean hasPositive = false;
                        LibraryFilterOption everything = option.filter.getOption(LibraryFilter.OPTION_EVERYTHING);
                        for(LibraryFilterOption filterOption : option.filter.getOptions()) {
                            if(filterOption == everything) continue;
                            if(filterOption.state == LibraryFilterOption.State.POSITIVE) {
                                hasPositive = true;
                                break;
                            }
                        }
                        if(!hasPositive) {
                            everything.state = LibraryFilterOption.State.POSITIVE;
                        } else {
                            everything.state = LibraryFilterOption.State.NONE;
                        }
                        if(everything.button != null) {
                            everything.button.repaint();
                        }
                    } else {
                        if(option.state == LibraryFilterOption.State.NONE) {
                            boolean hasPositive = false;
                            for(LibraryFilterOption filterOption : option.filter.getOptions()) {
                                if(filterOption.state == LibraryFilterOption.State.POSITIVE) {
                                    hasPositive = true;
                                    break;
                                }
                            }
                            if(!hasPositive) {
                                option.state = LibraryFilterOption.State.POSITIVE;
                            }
                        } else {
                            for(LibraryFilterOption filterOption : option.filter.getOptions()) {
                                // don't make it undo what it just did
                                if(filterOption == option) continue;
                                if(filterOption.state == option.state) {
                                    filterOption.state = LibraryFilterOption.State.NONE;
                                    if(filterOption.button != null) {
                                        filterOption.button.repaint();
                                    }
                                }
                            }
                        }
                    }

                    repaint();

                    option.filter.library.reloadFilters();
                    option.filter.library.reloadSorting();
                    Main.songListWidget.refreshTracks();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(this.getParent().getParent().getWidth(), (int) super.getMinimumSize().getHeight());
        }

        @Override
        public Dimension getMinimumSize() {
            return this.getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return this.getPreferredSize();
        }

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(this.getParent().getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            if(mouseDown) {
                g.setColor(MOUSE_DOWN_BACKGROUND_COLOR);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            ui.paint(g, this);
        }

        @Override
        public void paintBorder(Graphics g) {
        }

        @Override
        public Color getBackground() {
            if(option == null) return NEUTRAL_BACKGROUND_COLOR;
            return switch(option.state) {
                case POSITIVE -> POSITIVE_BACKGROUND_COLOR;
                case NEGATIVE -> NEGATIVE_BACKGROUND_COLOR;
                default -> NEUTRAL_BACKGROUND_COLOR;
            };
        }


    }

}
