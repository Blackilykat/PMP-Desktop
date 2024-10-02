package dev.blackilykat.widgets.filters;

public class LibraryFilterOption implements Comparable<LibraryFilterOption> {
    public final LibraryFilter filter;
    public final String value;
    public LibraryFilterPanel.OptionButton button = null;
    public State state = State.NONE;

    public LibraryFilterOption(LibraryFilter filter, String value) {
        System.out.println("adding option with value " + value + " to filter " + filter.key);
        this.filter = filter;
        this.value = value;
    }

    // so they can be sorted in a stream
    @Override
    public int compareTo(LibraryFilterOption o) {
        return value.compareTo(o.value);
    }

    @Override
    public int hashCode() {
        return filter.hashCode() + value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof LibraryFilterOption option)) return false;
        // yes the == is intentional it must be the same object
        return filter == option.filter && value.equals(option.value);
    }

    public enum State {
        NONE,
        POSITIVE,
        NEGATIVE
    }
}
