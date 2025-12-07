package util;

import model.Player;

public class SessionManager {

    private static Player loggedInUser;
    private static Player loggedInPlayer1;
    private static Player loggedInPlayer2;
    private static String LoggedInPlayer1Name;
    private static String LoggedInPlayer2Name;
    

    public static void setLoggedInUser(Player p) {
        loggedInUser = p;
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
        }
}
