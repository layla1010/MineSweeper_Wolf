package control;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import model.Player;
import model.Role;

public class AdminLoginEvaluationTest {

	//TestID: JU-ALA-1
    @Test
    void evaluateAdminLogin_adminNull_returnsInvalidCredentials() {
        var result = PlayLoginController.evaluateAdminLogin(null, "anything");
        assertEquals(PlayLoginController.AdminLoginResult.INVALID_CREDENTIALS, result);
    }
    
    
	//TestID: JU-ALA-2
    @Test
    void evaluateAdminLogin_wrongPassword_returnsInvalidCredentials() {
        Player admin1 = new Player("Admin1", "a@a.com", "CORRECTPASS", Role.ADMIN, "S1.png");
        var result = PlayLoginController.evaluateAdminLogin(admin1, "WRONGPASS");
        assertEquals(PlayLoginController.AdminLoginResult.INVALID_CREDENTIALS, result);
    }
    
    
	//TestID: JU-ALA-3
    @Test
    void evaluateAdminLogin_correctPasswordButNotAdmin_returnsNotAdmin() {
        Player normalUser = new Player("User1", "u@u.com", "PASS", Role.PLAYER, "S1.png");
        var result = PlayLoginController.evaluateAdminLogin(normalUser, "PASS");
        assertEquals(PlayLoginController.AdminLoginResult.NOT_ADMIN, result);
    }
    
    
	//TestID: JU-ALA-4
    @Test
    void evaluateAdminLogin_correctPasswordAndAdmin_returnsSuccess() {
        Player admin = new Player("Admin2", "a2@a.com", "PASS", Role.ADMIN, "S1.png");
        var result = PlayLoginController.evaluateAdminLogin(admin, "PASS");
        assertEquals(PlayLoginController.AdminLoginResult.SUCCESS, result);
    }
}
