package com.example.unimarket.data.model;

public class User {
    private String id;
    private String email;
    private String password_hash;
    private String full_name;
    private String phone;
    private String university;
    private String avatar_url;
    private boolean is_verified;
    private String created_at;
    private String updated_at;

    public User() {
    }

    public User(String id, String email, String password_hash, String full_name, String phone,
                String university, String avatar_url, boolean is_verified, String created_at, String updated_at) {
        this.id = id;
        this.email = email;
        this.password_hash = password_hash;
        this.full_name = full_name;
        this.phone = phone;
        this.university = university;
        this.avatar_url = avatar_url;
        this.is_verified = is_verified;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword_hash() { return password_hash; }
    public void setPassword_hash(String password_hash) { this.password_hash = password_hash; }
    public String getFull_name() { return full_name; }
    public void setFull_name(String full_name) { this.full_name = full_name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }
    public String getAvatar_url() { return avatar_url; }
    public void setAvatar_url(String avatar_url) { this.avatar_url = avatar_url; }
    public boolean isVerified() { return is_verified; }
    public void setIs_verified(boolean is_verified) { this.is_verified = is_verified; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }
}
