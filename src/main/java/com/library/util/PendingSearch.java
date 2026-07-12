package com.library.util;

/**
 * Tiny handoff for the top bar's global search box.
 *
 * Each screen in this app is its own top-level FXML, and Navigator.goTo()
 * loads a brand-new controller instance every time you navigate — so there's
 * no shared in-memory state between "the top bar you typed a search into"
 * and "the Books/Members page that opens next". This class is the bridge:
 * TopBarController stores the query and which page it's meant for right
 * before navigating, and that page's controller reads (and clears) it once,
 * during its own initialize().
 */
public final class PendingSearch {

    public enum Target { BOOKS, MEMBERS }

    private static String query;
    private static Target target;

    private PendingSearch() {
    }

    public static void set(Target target, String query) {
        PendingSearch.target = target;
        PendingSearch.query = query;
    }

    /**
     * If a pending search exists for {@code forTarget}, returns it and clears
     * it (so navigating back to the same page later doesn't re-apply a stale
     * query). Returns null if there's nothing pending for this target.
     */
    public static String consumeIfFor(Target forTarget) {
        if (target == forTarget && query != null) {
            String result = query;
            query = null;
            target = null;
            return result;
        }
        return null;
    }
}
