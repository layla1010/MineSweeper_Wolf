package control;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import util.ValidationUtil;
//This test class implements a j unit test on the method allDistinctIgnoreCase in AddQuestionController
//The method checks whether all provided strings (which are the answers options) are unique, ignoring letter case.
//The method returns true if all strings are different regardless of letter casing, otherwise returns false.
class AllDistinctIgnoreCaseTest {
	//TestID: JU-ADI-1
	@Test
    void allDistinctIgnoreCase_allDifferent_returnsTrue() {
       boolean result = ValidationUtil.allDistinctIgnoreCase(
                "Software", "Hardware", "Maintainance", "Development"
        );

        assertTrue(result);
    }
	//TestID: JU-ADI-2
    @Test
    void allDistinctIgnoreCase_caseInsensitiveDuplicate_returnsFalse() {
        boolean result = ValidationUtil.allDistinctIgnoreCase(
        		"Software", "SOFTWARE", "Maintainance", "Development"
        );

        assertFalse(result);
    }
  //TestID: JU-ADI-3
    @Test
    void allDistinctIgnoreCase_exactDuplicate_returnsFalse() {
        boolean result = ValidationUtil.allDistinctIgnoreCase(
        		"Software", "Software", "Maintainance", "Development"
        );

        assertFalse(result);
    }

}
