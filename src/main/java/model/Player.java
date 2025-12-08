package model;


public class Player {
	
    private String officialName;
    private final String email;
    private String password;
    private Role role;
    private String avatarId;
    
    
    public Player(String officialName, String email, String password, Role role, String avatarId) {
        if (officialName == null || officialName.trim().isEmpty()) {
            throw new IllegalArgumentException("Official Name cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        this.officialName = officialName.trim();
        this.email = email.trim();
        this.password = password.trim();
        this.role = role;
        this.avatarId = avatarId;

    }
    
    
	public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
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
    
    public String getPassword() {
        return password;
    }


    
    public void setPassword (String password) {
    	if(password == null || password.trim().isEmpty()) {
    		throw new IllegalArgumentException("password cannot be empty");
    	}
    	this.password = password.trim();
    }

    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
	
    
	
}
