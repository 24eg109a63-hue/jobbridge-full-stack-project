package com.jobbridge.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity @Table(name="portal_users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class PortalUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank @Size(max=80) private String fullName;
 @Email @NotBlank @Size(max=160) private String email;
 @NotBlank @Size(min=8,max=72) private String password;
 private String role="USER";
 public Long getId(){return id;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getPassword(){return password;} public void setPassword(String v){password=v;}
 public String getRole(){return role;} public void setRole(String v){role=v;}
}
