package model;


public class Player {
	
    private String officialName;
    private final String email;
    private String password;
    
    public Player(String officialName, String email, String password) {
        if (officialName == null || officialName.trim().isEmpty()) {
            throw new IllegalArgumentException("Official Name cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        this.officialName = officialName.trim();
        this.email = email.trim();
        this.password = password.trim();
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
    
    public String getEmail() {
        return email;
    }
    
    public boolean checkPassword(String attemptedPassword) {
        return this.password.equals(attemptedPassword);
    }

    
    public void setPassword (String password) {
    	if(password == null || password.trim().isEmpty()) {
    		throw new IllegalArgumentException("password cannot be empty");
    	}
    	this.password = password.trim();
    }

	
    
	
}
