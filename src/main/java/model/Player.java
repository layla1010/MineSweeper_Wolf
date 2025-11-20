package model;

import java.util.Objects;

public class Player {
	
    private String officialName;
    private String password;
    
    public Player(String officialName, String password) {
    	setOfficialName(officialName);
    	setPassword(password);
    }
    
    public String getOfficialName() {
		return officialName;
	}
    
    public void setOfficialName (String officialName) {
    	if(officialName == null || officialName.trim().isEmpty()) {
    		throw new IllegalArgumentException("Official Name cannot be empty");
    	}
    	this.officialName = officialName.trim();
    }
    
    public void setPassword (String password) {
    	if(password == null || password.trim().isEmpty()) {
    		throw new IllegalArgumentException("password cannot be empty");
    	}
    	this.password = password.trim();
    }

	
    
	
}
