package model;

/**
 * Represents a system user (Admin, Manager, Accountant, or Merchant login).
 */
public class User {

    public enum Role { ADMIN, MANAGER, ACCOUNTANT, MERCHANT }

    private int     userId;
    private String  username;
    private String  passwordHash;
    private Role    role;
    private boolean isActive;

    // ── Constructors ──────────────────────────────────────────────────────

    public User() {}

    public User(int userId, String username, String passwordHash,
                Role role, boolean isActive) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.isActive     = isActive;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int     getUserId()       { return userId; }
    public String  getUsername()     { return username; }
    public String  getPasswordHash() { return passwordHash; }
    public Role    getRole()         { return role; }
    public boolean isActive()        { return isActive; }

    public void setUserId(int userId)             { this.userId = userId; }
    public void setUsername(String username)       { this.username = username; }
    public void setPasswordHash(String hash)       { this.passwordHash = hash; }
    public void setRole(Role role)                 { this.role = role; }
    public void setActive(boolean active)          { this.isActive = active; }

    @Override
    public String toString() {
        return "User{id=" + userId + ", username='" + username
             + "', role=" + role + ", active=" + isActive + "}";
    }
}
