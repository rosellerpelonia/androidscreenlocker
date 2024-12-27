class ChildUser {
    public String name;
    public String email;
    public String role;
    public boolean lock;

    public ChildUser() {
        // Default constructor required for calls to DataSnapshot.getValue(ChildUser.class)
    }

    public ChildUser(String name, String email, String role, boolean lock) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.lock = lock;
    }

    // Getter and Setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }
}
