package util;

import model.Player;

public class SessionManager {

    private static Player loggedInUser;
    private static Player loggedInPlayer1;
    private static Player loggedInPlayer2;
    private static String LoggedInPlayer1Name;
    private static String LoggedInPlayer2Name;
    private static boolean guestMode = false;

    public static void setGuestMode(boolean guest) {
        guestMode = guest;
    }

    public static boolean isGuestMode() {
        return guestMode;
    }


    public static void setLoggedInUser(Player p) {
        loggedInUser = p;
        guestMode = false;
    }
    
    public static boolean isAdminMode() {
        Player u = getLoggedInUser(); 
        return u != null && u.isAdmin();
    }

    public static String getRegisteredUserKey() {
        Player u = getLoggedInUser();
        if (u == null) return null;
        if (isGuestMode()) return null;
        if (u.isAdmin()) return null;
        return u.getEmail();
    }

    public static Player getLoggedInUser() {
        return loggedInUser;
    }

    public static boolean isAdminLoggedIn() {
        return loggedInUser != null && loggedInUser.isAdmin();
    }

    public static void logout() {
        loggedInUser = null;
    }

    public static void setPlayers(Player p1, String p1Name, Player p2, String p2Name) {
        loggedInPlayer1 = p1;
        LoggedInPlayer1Name = p1Name;
        loggedInPlayer2 = p2;
        LoggedInPlayer2Name = p2Name;
        guestMode = false;
    }
    
    public static String getOnboardingUserKey() {
        if (isAdminMode() || isGuestMode()) return null;

        Player p1 = getPlayer1();   // or however you store them
        Player p2 = getPlayer2();

        if (p1 == null || p2 == null) return null;

        String e1 = p1.getEmail();
        String e2 = p2.getEmail();
        if (e1 == null || e2 == null) return null;

        // make it order-independent
        return (e1.compareToIgnoreCase(e2) <= 0) ? (e1 + "|" + e2) : (e2 + "|" + e1);
    }


	public static Player getPlayer1()         { return loggedInPlayer1; }
    public static Player getPlayer2()         { return loggedInPlayer2; }
    public static String getPlayer1Nickname() { return LoggedInPlayer1Name; }
    public static String getPlayer2Nickname() { return LoggedInPlayer2Name; }

    
    public static void clear() {
        loggedInUser = null;
        loggedInPlayer1 = null;
        loggedInPlayer2 = null;
        LoggedInPlayer1Name = null;
        LoggedInPlayer2Name = null;
        guestMode = false;  
    }

}
